# Zoek het standaard Android SDK pad en update local.properties
$defaultSdkPath = "$env:USERPROFILE\AppData\Local\Android\Sdk"
$localPropertiesPath = "C:\Users\arjan\AndroidStudioProjects\TrackerMobilePrivate\local.properties"

if (Test-Path $defaultSdkPath) {
    $content = "sdk.dir=" + $defaultSdkPath.Replace("\", "/")
    Set-Content -Path $localPropertiesPath -Value $content
    Write-Host "local.properties is bijgewerkt met: $defaultSdkPath"
} else {
    Write-Host "Android SDK pad niet gevonden: $defaultSdkPath"
    Write-Host "Installeer de Android SDK of geef het juiste pad op."
}
