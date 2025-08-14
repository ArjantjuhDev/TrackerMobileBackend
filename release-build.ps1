# Automatisch Android release build script
# 1. SDK-pad detecteren
$possibleSdkPaths = @(
    "$env:USERPROFILE\AppData\Local\Android\Sdk",
    "C:\Android\Sdk",
    "D:\Android\Sdk"
)
$foundSdk = $null
foreach ($path in $possibleSdkPaths) {
    if (Test-Path $path) {
        $foundSdk = $path
        break
    }
}
if (-not $foundSdk) {
    Write-Host "Geen Android SDK gevonden! Installeer de SDK via Android Studio."
    exit 1
}
# 2. local.properties bijwerken
$localProp = "C:\Users\arjan\AndroidStudioProjects\TrackerMobilePrivate\local.properties"
$sdkProp = "sdk.dir=" + $foundSdk.Replace("\", "/")
Set-Content -Path $localProp -Value $sdkProp
Write-Host "local.properties bijgewerkt: $sdkProp"
# 3. Keystore aanmaken indien nodig
$keystorePath = "C:\Users\arjan\AndroidStudioProjects\TrackerMobilePrivate\keystore.jks"
if (-not (Test-Path $keystorePath)) {
    $javaRoot = "C:\Program Files\Java"
    $jdkDirs = Get-ChildItem -Path $javaRoot -Directory | Where-Object { $_.Name -like "jdk*" }
    if ($jdkDirs.Count -eq 0) {
        Write-Host "Geen JDK gevonden in $javaRoot. Installeer Java JDK!"
        exit 1
    }
    $latestJdk = $jdkDirs | Sort-Object Name -Descending | Select-Object -First 1
    $keytool = Join-Path $latestJdk.FullName "bin\keytool.exe"
    if (-not (Test-Path $keytool)) {
        Write-Host "keytool.exe niet gevonden in $($latestJdk.FullName)."
        exit 1
    }
    $params = @(
        "-genkeypair",
        "-v",
        "-keystore", "$keystorePath",
        "-alias", "key0",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-storepass", "password",
        "-keypass", "password",
        "-dname", 'CN=Arjan, OU=Dev, O=Private, L=City, S=State, C=NL'
    )
    Start-Process -FilePath $keytool -ArgumentList $params -Wait
    if (Test-Path $keystorePath) {
        Write-Host "Keystore aangemaakt: $keystorePath"
    } else {
        Write-Host "Keystore aanmaken is mislukt."
        exit 1
    }
} else {
    Write-Host "Keystore bestaat al: $keystorePath"
}
# 4. Release build uitvoeren
$gradlePath = "C:\Users\arjan\AndroidStudioProjects\TrackerMobilePrivate\gradlew.bat"
if (-not (Test-Path $gradlePath)) {
    Write-Host "gradlew.bat niet gevonden!"
    exit 1
}
Write-Host "Start release build..."
$build = Start-Process -FilePath $gradlePath -ArgumentList "clean", "assembleRelease" -Wait -NoNewWindow -PassThru -RedirectStandardOutput "build-output.txt" -RedirectStandardError "build-error.txt"
Start-Sleep -Seconds 5
$apkPath = "C:\Users\arjan\AndroidStudioProjects\TrackerMobilePrivate\app\build\outputs\apk\release\app-release.apk"
if (Test-Path $apkPath) {
    Write-Host "Release APK succesvol aangemaakt: $apkPath"
} else {
    Write-Host "Build is mislukt. Zie build-error.txt voor details."
    Get-Content "build-error.txt"
}
