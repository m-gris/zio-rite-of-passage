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
