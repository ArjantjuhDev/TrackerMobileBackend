# PowerShell script to download and place dependencies for com.android.application plugin version 8.9.1

# Define the base URL for Google's Maven repository
$baseUrl = "https://dl.google.com/dl/android/maven2"

# Define the dependencies to download
$dependencies = @(
    "com/android/tools/build/gradle/8.9.1/gradle-8.9.1.pom",
    "com/android/tools/build/gradle/8.9.1/gradle-8.9.1.jar"
)

# Define the local Maven repository path
$localRepo = "local-maven-repo"

# Function to download a file and place it in the correct directory
function Download-Dependency {
    param (
        [string]$relativePath
    )

    $url = "$baseUrl/$relativePath"
    $localPath = Join-Path -Path $localRepo -ChildPath $relativePath

    # Create the directory if it doesn't exist
    $directory = Split-Path -Path $localPath -Parent
    if (-not (Test-Path -Path $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }

    # Download the file
    Invoke-WebRequest -Uri $url -OutFile $localPath -UseBasicParsing
    Write-Host "Downloaded: $url to $localPath"
}

# Download each dependency
foreach ($dependency in $dependencies) {
    Download-Dependency -relativePath $dependency
}

Write-Host "All dependencies have been downloaded and placed in the local Maven repository."
