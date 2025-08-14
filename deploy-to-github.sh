#!/bin/bash
# Usage: ./deploy-to-github.sh "TrackerMobileBackend" "Node.js + SQLite backend for TrackerMobile"

REPO_NAME="${1:-TrackerMobileBackend}"
DESCRIPTION="${2:-Node.js + SQLite backend for TrackerMobile}"


# Set git user config
git config --global user.email "djal3221@gmail.com"
git config --global user.name "ArjantjuhDev"

# Check for gh
if ! command -v gh &> /dev/null; then
  echo "GitHub CLI (gh) is not installed. Install it with: sudo apt install gh"
  exit 1
fi

# Check for git
if ! command -v git &> /dev/null; then
  echo "Git is not installed. Install it with: sudo apt install git"
  exit 1
fi

# Add .env to .gitignore
if [ ! -f .gitignore ]; then touch .gitignore; fi
if ! grep -q "^.env$" .gitignore; then echo ".env" >> .gitignore; fi

# Ensure .env.example exists
if [ ! -f .env.example ]; then echo "API_KEY=y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" > .env.example; fi

# Initialize git repo
if [ ! -d .git ]; then git init; fi
git add .
git commit -m "Initial commit"

# Create GitHub repo and push
gh repo create "$REPO_NAME" --description "$DESCRIPTION" --public --source . --remote origin --push

echo "Repository '$REPO_NAME' created and code pushed to GitHub!"
