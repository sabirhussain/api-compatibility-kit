#!/usr/bin/env bash
# version-bump.sh — Increments the Maven project version (major, minor, or patch).
#
# Usage:
#   ./version-bump.sh major   # e.g. 1.0.0-SNAPSHOT → 2.0.0-SNAPSHOT
#   ./version-bump.sh minor   # e.g. 1.0.0-SNAPSHOT → 1.1.0-SNAPSHOT
#   ./version-bump.sh patch   # e.g. 1.0.0-SNAPSHOT → 1.0.1-SNAPSHOT
#
# Steps performed:
#   1. Validate argument (major | minor | patch)
#   2. Increment version: mvn versions:set -DnextSnapshot=true -DnextSnapshotIndexToIncrement=<index>
#   3. Verify build:      mvn clean verify
#   4. Clean up backups:  mvn versions:commit

set -euo pipefail

# ─── Colours ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

info()    { echo -e "${CYAN}[INFO]${RESET}  $*"; }
success() { echo -e "${GREEN}[OK]${RESET}    $*"; }
error()   { echo -e "${RED}[ERROR]${RESET} $*" >&2; }
step()    { echo -e "\n${BOLD}▶ $*${RESET}"; }

# ─── Step 1: Validate argument ────────────────────────────────────────────────
if [ $# -ne 1 ]; then
  error "Missing argument."
  echo -e "Usage: $0 ${BOLD}major${RESET} | ${BOLD}minor${RESET} | ${BOLD}patch${RESET}"
  exit 1
fi

case "$1" in
  major) INDEX=1 ;;
  minor) INDEX=2 ;;
  patch) INDEX=3 ;;
  *)
    error "Unknown argument: '$1'"
    echo -e "Usage: $0 ${BOLD}major${RESET} | ${BOLD}minor${RESET} | ${BOLD}patch${RESET}"
    exit 1
    ;;
esac

BUMP_TYPE="$1"

# ─── Show current version ─────────────────────────────────────────────────────
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout --no-transfer-progress)
info "Current version : ${BOLD}${CURRENT_VERSION}${RESET}"
info "Bumping         : ${BOLD}${BUMP_TYPE}${RESET} (index ${INDEX})"

# ─── Step 2: Increment version ────────────────────────────────────────────────
step "Step 1: Incrementing ${BUMP_TYPE} version"
mvn --no-transfer-progress versions:set \
  -DnextSnapshot=true \
  -DnextSnapshotIndexToIncrement=${INDEX}
success "Version incremented."

# ─── Step 3: Verify build ─────────────────────────────────────────────────────
step "Step 2: Verifying build (mvn clean verify)"
if ! mvn --no-transfer-progress clean verify; then
  error "Build failed. Reverting pom changes."
  find . -name "pom.xml.versionsBackup" -not -path "*/target/*" -delete
  git checkout -- .
  exit 1
fi
success "Build passed."

# ─── Step 4: Clean up backup files ────────────────────────────────────────────
step "Step 3: Removing versions backup files (mvn versions:commit)"
mvn --no-transfer-progress versions:commit
success "Backup files removed."

# ─── Done ─────────────────────────────────────────────────────────────────────
NEW_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout --no-transfer-progress)
echo ""
echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${GREEN}${BOLD}  Version bumped: ${CURRENT_VERSION} → ${NEW_VERSION}${RESET}"
echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
echo -e "  pom files are updated locally. Commit when ready:"
echo -e "    ${BOLD}git add -A && git commit -m \"chore: bump version to ${NEW_VERSION}\"${RESET}"
echo ""
