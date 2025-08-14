#!/bin/bash
# Usage: ./deploy-to-render.sh <repo-name> <repo-description> <render-service-name>
# Example: ./deploy-to-render.sh TrackerMobileBackend "Node.js + SQLite backend for TrackerMobile" TrackerMobileBackend

set -e

REPO_NAME="$1"
REPO_DESC="$2"
RENDER_SERVICE_NAME="$3"
GITHUB_USER=$(git config user.name)
RENDER_API_KEY="861396c20f3ffc10ae7af8def0783aeb" # <-- Replace with your Render API key
RENDER_ROOT="https://api.render.com/v1/services"

if [ -z "$REPO_NAME" ] || [ -z "$REPO_DESC" ] || [ -z "$RENDER_SERVICE_NAME" ]; then
  echo "Usage: $0 <repo-name> <repo-description> <render-service-name>"
  exit 1
fi

# Check for required tools
type git >/dev/null 2>&1 || { echo "git is not installed"; exit 1; }
type gh >/dev/null 2>&1 || { echo "GitHub CLI (gh) is not installed"; exit 1; }
type curl >/dev/null 2>&1 || { echo "curl is not installed"; exit 1; }

# Set git config if not set
git config user.name "$GITHUB_USER"
git config user.email "your@email.com"

# Add .env to .gitignore if not present
grep -q '^.env$' .gitignore || echo ".env" >> .gitignore

# Ensure .env.example exists
if [ ! -f .env.example ]; then
  echo "API_KEY=your_api_key_here" > .env.example
fi

# Initialize git and commit
if [ ! -d .git ]; then
  git init
fi
git add .
git commit -m "Initial commit for Render deployment"

# Create GitHub repo and push
gh repo create "$GITHUB_USER/$REPO_NAME" --public --description "$REPO_DESC" --source=. --remote=origin --push

echo "GitHub repo created and code pushed."

echo "Creating Render service..."

# Create Render service (replace RENDER_API_KEY with your actual key)
REPO_URL="https://github.com/$GITHUB_USER/$REPO_NAME"
RENDER_PAYLOAD="{\"name\": \"$RENDER_SERVICE_NAME\", \"repoUrl\": \"$REPO_URL\", \"branch\": \"main\", \"env\": \"node\", \"region\": \"oregon\"}"
curl -X POST "$RENDER_ROOT" \
  -H "Authorization: Bearer $RENDER_API_KEY" \
  -H "Content-Type: application/json" \
  -d "$RENDER_PAYLOAD"

echo "Render service created."
echo "Remember to set your API_KEY environment variable in the Render dashboard!"
