# List available recipes
default:
    @just --list


# Clean build artifacts and tooling state that commonly cause issues
clean_tooling:
    # Clean BSP state
    fd -H -t d "^\.bsp$" -x echo "Cleaning BSP state: {}" \; -x rm -rf {} \;
    # Clean Metals LSP state
    fd -H -t d "^\.metals$" -x echo "Cleaning Metals state: {}" \; -x rm -rf {} \;
    fd -H -t f "^metals\.sbt$" -x echo "Cleaning Metals sbt file: {}" \; -x rm -f {} \;
    # Clean Bloop compilation server files
    fd -H -t d "^\.bloop$" -x echo "Cleaning Bloop state: {}" \; -x rm -rf {} \;
    # Clean lock files that might cause SBT to hang
    fd -H -t f ".*\.lock$" -x echo "Cleaning lock files: {}" \; -x rm -f {} \;
    @echo "Tooling state cleaned. Restart your IDE/editor for changes to take effect."
# Clean compilation artifacts
clean_artifacts:
    # Clean target directories (compiled code)
    fd -H -t d "^target$" -x echo "Cleaning compilation artifacts: {}" \; -x rm -rf {} \;
    # Clean nested project artifacts (but preserve project/plugins.sbt!)
    fd -H -t d "^project$" -d 1 --base-directory ./project/project -x echo "Cleaning nested project artifacts: {}" \; -x rm -rf {} \;
    # Clean class files
    fd -H -t f ".*\.class$" -x echo "Cleaning class files: {}" \; -x rm -f {} \;
    @echo "Build artifacts cleaned. Run 'sbt compile' to rebuild."
# Clean IDE-specific files
clean_ide:
    # Remove IntelliJ IDEA files
    fd -H -t d "^\.idea$" -x echo "Cleaning IDEA files: {}" \; -x rm -rf {} \;
    fd -H -t f ".*\.iml$" -x echo "Cleaning IDEA module files: {}" \; -x rm -f {} \;
    # Clean VS Code logs (preserve settings)
    fd -H -t d "^\.vscode$" -x bash -c 'echo "Cleaning VS Code logs: {}" && rm -f {}/*.log {}/*.bak' \;
    @echo "IDE files cleaned."
# Reset project state (safe version)
clean_everything:
    @echo "Resetting project state..."
    just clean_tooling
    just clean_artifacts
    just clean_ide
    @echo "Project reset complete. You may need to:"
    @echo "1. Restart your IDE/editor"
    @echo "2. Run 'sbt bloopInstall' to regenerate build files"


# Start backend server with Application class
backend-serve:
    sbt "project server; runMain com.rockthejvm.reviewboard.Application"

backend-compile:
    sbt "project server; ~compile"

db-serve:
    #!/bin/bash
    set -euo pipefail

    # Cleanup function
    cleanup() {
        echo "Shutting down database..."
        docker-compose down
        # any other cleanup
    }

    # Register cleanup on various exit signals
    trap cleanup EXIT SIGTERM SIGINT

    echo "Starting database..."
    docker-compose up

db-up:
    docker-compose up

db-down:
    docker-compose down

# Access PostgreSQL database for exploration
db-access:
    docker-compose exec db psql -U docker -d reviewboard

# Alternative: explore database with psql (same as db-access)
db-explore: db-access

# Continuously compile frontend
frontend-compile:
    sbt "project app; ~fastLinkJS"

# Check if npm deps are installed and start frontend dev server
frontend-serve:
    #!/usr/bin/env bash
    cd modules/app

    # Check if node_modules exists
    if [ ! -d "node_modules" ]; then
        echo "Installing npm dependencies (first time setup)..."
        npm install
    fi

    # Try to start the server
    if npm start; then
        echo "Server started successfully!"
    else
        # If it fails, we'll try fixing common issues
        echo "Error detected. Trying to fix dependencies..."

        # Install base-x explicitly (common issue with Parcel)
        echo "Installing base-x dependency..."
        npm install --save-dev base-x@latest

        # If that doesn't work, try a clean install as last resort
        if ! npm start; then
            echo "Still having issues. Performing clean reinstall..."
            rm -rf node_modules package-lock.json
            npm cache clean --force
            npm install

            # Final attempt
            if ! npm start; then
                echo "Failed to start server after multiple attempts."
                exit 1
            fi
        fi
    fi

# Install frontend dependencies explicitly
frontend-install:
    cd modules/app && npm install

# One-command setup for development (opens tmux with all needed panes)
dev:
    #!/usr/bin/env bash
    SESSION="zio-dev"

    # Create session if it doesn't exist
    tmux has-session -t $SESSION 2>/dev/null || tmux new-session -d -s $SESSION

    # Configure windows and panes
    tmux rename-window -t $SESSION:0 'backend'
    tmux send-keys -t $SESSION:0 'just backend' C-m

    tmux new-window -t $SESSION:1 -n 'frontend-compile'
    tmux send-keys -t $SESSION:1 'just frontend-compile' C-m

    tmux new-window -t $SESSION:2 -n 'frontend-serve'
    tmux send-keys -t $SESSION:2 'just frontend-serve' C-m

    tmux new-window -t $SESSION:3 -n 'shell'

    # Attach to session
    tmux select-window -t $SESSION:0
    tmux attach-session -t $SESSION
