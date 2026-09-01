#!/usr/bin/env bash
# release-prepare.sh — Prepares the Maven project for an artifact release.
#
# Usage:
#   ./release-prepare.sh             # Commit + tag release version locally, then revert pom changes.
#                                    # Tag points to the release commit. No push performed.
#   ./release-prepare.sh --dry-run   # Validates the full workflow then reverts everything.
#
# Steps performed:
#   1. Ensure working tree is clean (excluding this script)
#   2. Remove -SNAPSHOT:         mvn versions:set -DremoveSnapshot
#   3. Verify no -SNAPSHOT remains in any pom.xml
#   4. Clean up backup files:    mvn versions:commit
#   5. Extract release version
#   6. Verify build: mvn clean verify
#   7. Git commit the release pom changes
#   8. Git tag that commit with the release version
#   9. Revert the local commit (git reset HEAD~1 + git checkout -- .)
#      so working tree is back to SNAPSHOT — or print manual instructions if not possible
#  10. (--dry-run only) Also delete the tag — full revert
#  11. (default only) Print instruction to push only the tag

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
warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
error()   { echo -e "${RED}[ERROR]${RESET} $*" >&2; }
step()    { echo -e "\n${BOLD}▶ $*${RESET}"; }

# ─── Parse arguments ──────────────────────────────────────────────────────────
DRY_RUN=false
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=true ;;
    *) error "Unknown argument: $arg"; echo "Usage: $0 [--dry-run]"; exit 1 ;;
  esac
done

if $DRY_RUN; then
  warn "DRY-RUN mode — all changes will be reverted at the end."
fi

# ─── Step 1: Ensure clean working tree (excluding release-prepare.sh) ─────────
step "Step 1: Checking working tree is clean"
DIRTY_FILES=$(git status --porcelain | grep -v '^ \?M\? release-prepare.sh$' | grep -v '^?? release-prepare.sh$' || true)
if [ -n "$DIRTY_FILES" ]; then
  error "Working tree is not clean. Please commit or stash all changes before running this script."
  echo "$DIRTY_FILES"
  exit 1
fi
success "Working tree is clean (release-prepare.sh excluded from check)."

# ─── Step 2: Remove -SNAPSHOT ─────────────────────────────────────────────────
step "Step 2: Removing -SNAPSHOT suffix (mvn versions:set -DremoveSnapshot)"
mvn --no-transfer-progress versions:set -DremoveSnapshot
success "mvn versions:set completed."

# ─── Step 3: Verify no -SNAPSHOT remains ──────────────────────────────────────
step "Step 3: Verifying no -SNAPSHOT remains in pom files"
SNAPSHOT_COUNT=$(grep -r "\-SNAPSHOT" --include="pom.xml" . | grep -v "target/" | wc -l | tr -d ' ' || true)
if [ "$SNAPSHOT_COUNT" -gt 0 ]; then
  error "Found ${SNAPSHOT_COUNT} remaining -SNAPSHOT reference(s) in pom.xml files:"
  grep -r "\-SNAPSHOT" --include="pom.xml" . | grep -v "target/"
  find . -name "pom.xml.versionsBackup" -not -path "*/target/*" -delete
  git checkout -- .
  exit 1
fi
success "No -SNAPSHOT references found."

# ─── Step 4: Remove backup files ──────────────────────────────────────────────
step "Step 4: Removing versions backup files (mvn versions:commit)"
mvn --no-transfer-progress versions:commit
success "Backup files removed."

# ─── Step 5: Extract release version ──────────────────────────────────────────
info "Extracting project version..."
PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout --no-transfer-progress)
if [ -z "$PROJECT_VERSION" ]; then
  error "Could not determine project version. Aborting."
  git checkout -- .
  exit 1
fi
info "Project version: ${BOLD}${PROJECT_VERSION}${RESET}"

# ─── Step 6: Verify build ─────────────────────────────────────────────────────
step "Step 6: Verifying build (mvn clean verify)"
if ! mvn --no-transfer-progress clean verify; then
  error "Build failed. Aborting release — fixing build errors before tagging."
  find . -name "pom.xml.versionsBackup" -not -path "*/target/*" -delete
  git checkout -- .
  exit 1
fi
success "Build passed."

# ─── Step 7: Git commit the release pom changes ───────────────────────────────
step "Step 7: Creating local release commit"
POM_CHANGED=false
if ! git diff --quiet; then
  git add $(git diff --name-only | grep "pom.xml")
  git commit -m "release: prepare version ${PROJECT_VERSION}"
  success "Release commit created."
  POM_CHANGED=true
else
  warn "No pom.xml changes detected (already a release version?). Proceeding with tag on current HEAD."
fi

# ─── Step 8: Git tag the release commit ───────────────────────────────────────
step "Step 8: Tagging release commit as '${PROJECT_VERSION}'"
if git tag -l "${PROJECT_VERSION}" | grep -q "${PROJECT_VERSION}"; then
  error "Tag '${PROJECT_VERSION}' already exists. Delete it first: git tag -d ${PROJECT_VERSION}"
  if $POM_CHANGED; then git reset HEAD~1; fi
  git checkout -- .
  exit 1
fi
git tag "${PROJECT_VERSION}"
success "Tag '${PROJECT_VERSION}' created → points to release commit."

# ─── Step 9: Revert local commit so working tree returns to SNAPSHOT ──────────
step "Step 9: Reverting local release commit (keeping tag)"
if $POM_CHANGED; then
  if git reset HEAD~1 2>/dev/null; then
    git checkout -- .
    success "Local commit reverted — working tree restored to SNAPSHOT state."
    REVERTED=true
  else
    warn "Could not automatically revert the release commit."
    REVERTED=false
  fi
else
  REVERTED=true  # nothing to revert
fi

# ─── Step 10 (dry-run): Delete tag too ───────────────────────────────────────
if $DRY_RUN; then
  step "Step 10 (dry-run): Deleting tag '${PROJECT_VERSION}'"
  git tag -d "${PROJECT_VERSION}"
  echo ""
  success "Dry-run complete. Repository is fully back to its original state."
  echo -e "  ${GREEN}✔${RESET} All release steps would succeed — the release is ready."
  echo -e "  Run ${BOLD}./release-prepare.sh${RESET} (without --dry-run) to perform the actual release."
  exit 0
fi

# ─── Step 11: Print push instructions ───────────────────────────────────────
echo ""
echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${GREEN}${BOLD}  Release tag '${PROJECT_VERSION}' is ready${RESET}"
echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""

if $REVERTED; then
  echo -e "  ${GREEN}✔${RESET} pom files are restored to SNAPSHOT — developers can continue working normally."
else
  echo -e "  ${YELLOW}⚠${RESET}  Could not auto-revert the release commit. To restore SNAPSHOT manually:"
  echo ""
  echo -e "    ${BOLD}git reset HEAD~1${RESET}    # unstage the release commit"
  echo -e "    ${BOLD}git checkout -- .${RESET}   # discard pom changes"
  echo ""
fi

echo -e "  Push only the tag when ready to publish:"
echo ""
echo -e "    ${BOLD}git push origin ${PROJECT_VERSION}${RESET}"
echo ""
echo -e "  Do ${RED}NOT${RESET} push the release commit — developers continue on SNAPSHOT."
echo ""
