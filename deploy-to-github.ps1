# PowerShell script to create a new GitHub repo and push all files
# Usage: .\deploy-to-github.ps1 -RepoName "TrackerMobileBackend" -Description "Node.js + SQLite backend for TrackerMobile"

param(
    [string]$RepoName = "TrackerMobileBackend",
    [string]$Description = "Node.js + SQLite backend for TrackerMobile"
)

# Check for GitHub CLI
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Host "GitHub CLI (gh) is not installed. Install it from https://cli.github.com/ and login with 'gh auth login'." -ForegroundColor Red
    exit 1
}

# Check for git
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "Git is not installed. Install it from https://git-scm.com/." -ForegroundColor Red
    exit 1
}

# Add .env to .gitignore
if (!(Test-Path .gitignore)) { New-Item .gitignore -ItemType File }
if (-not (Select-String -Path .gitignore -Pattern "^\.env$")) {
    Add-Content .gitignore ".env"
}

# Ensure .env.example exists
if (!(Test-Path .env.example)) {
    Set-Content .env.example "API_KEY=your_api_key_here"
}

# Initialize git repo
if (!(Test-Path .git)) {
    git init
}
git add .
git commit -m "Initial commit"

# Create GitHub repo
$repoCreate = gh repo create $RepoName --description "$Description" --public --source . --remote origin --push
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to create/push repo. Check your GitHub authentication and permissions." -ForegroundColor Red
    exit 1
}

Write-Host "Repository '$RepoName' created and code pushed to GitHub!" -ForegroundColor Green
