# Clean build artifacts and tooling state that commonly cause issues
clean_tooling:
    # Clean BSP state
    find . -name ".bsp" -type d -exec echo "Cleaning BSP state: {}" \; -exec rm -rf {} +
    # Clean Metals LSP state
    find . -name ".metals" -type d -exec echo "Cleaning Metals state: {}" \; -exec rm -rf {} +
    find . -name "metals.sbt" -type f -exec echo "Cleaning Metals sbt file: {}" \; -exec rm -f {} +
    # Clean Bloop compilation server files
    find . -name ".bloop" -type d -exec echo "Cleaning Bloop state: {}" \; -exec rm -rf {} +
    # Clean lock files that might cause SBT to hang
    find . -name "*.lock" -type f -exec echo "Cleaning lock files: {}" \; -exec rm -f {} +
    @echo "Tooling state cleaned. Restart your IDE/editor for changes to take effect."

# Clean compilation artifacts
clean_artifacts:
    # Clean target directories (compiled code)
    find . -name "target" -type d -exec echo "Cleaning compilation artifacts: {}" \; -exec rm -rf {} +
    # Clean nested project artifacts (but preserve project/plugins.sbt!)
    find ./project/project -maxdepth 1 -type d -exec echo "Cleaning nested project artifacts: {}" \; -exec rm -rf {} +
    # Clean class files
    find . -name "*.class" -type f -exec echo "Cleaning class files: {}" \; -exec rm -f {} +
    @echo "Build artifacts cleaned. Run 'sbt compile' to rebuild."

# Clean IDE-specific files
clean_ide:
    # Remove IntelliJ IDEA files
    find . -name ".idea" -type d -exec echo "Cleaning IDEA files: {}" \; -exec rm -rf {} +
    find . -name "*.iml" -type f -exec echo "Cleaning IDEA module files: {}" \; -exec rm -f {} +
    # Clean VS Code logs (preserve settings)
    find . -name ".vscode" -type d -exec echo "Cleaning VS Code logs: {}" \; -exec rm -f {}/*.log {}/*.bak \;
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
