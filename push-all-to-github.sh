#!/bin/bash
# Usage: ./push-all-to-github.sh "Commit message"
# Example: ./push-all-to-github.sh "Update backend files for Render deployment"

set -e

COMMIT_MSG="$1"
if [ -z "$COMMIT_MSG" ]; then
  COMMIT_MSG="Update backend files for Render deployment"
fi

git add .
if git diff --cached --quiet; then
  echo "No changes to commit."
else
  git commit -m "$COMMIT_MSG"
fi
echo "All files pushed to GitHub!"
echo "All files pushed to GitHub on branch $CURRENT_BRANCH!"
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
git push -u origin "$CURRENT_BRANCH"
