# Nuke project-local artifacts
nuke_artifacts:
    find . -name "target" -type d -exec echo "Deleting {}" \; -exec rm -rf {} +
    find . -name ".bloop" -type d -exec echo "Deleting {}" \; -exec rm -rf {} +
    find . -name ".metals" -type d -exec echo "Deleting {}" \; -exec rm -rf {} +
    find . -name "out" -type d -exec echo "Deleting {}" \; -exec rm -rf {} +
    find . -name "*.class" -type f -exec echo "Deleting {}" \; -exec rm -f {} +
    @echo "Artifacts nuked. Run 'sbt bloopInstall' to regenerate build files."

# Nuke caches (dependencies, etc.)
nuke_caches:
    rm -rf ~/.sbt/ ~/.ivy2/ ~/.cache/bloop/ ~/.cache/metals/
    @echo "Global caches nuked. Dependencies will re-download on next build."

nuke_everything:
    just nuke_caches
    just nuke_artifacts
