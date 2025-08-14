
# Zoek de nieuwste JDK en gebruik het volledige pad naar keytool.exe
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
$keystorePath = "C:\Users\arjan\AndroidStudioProjects\TrackerMobilePrivate\keystore.jks"
$keyAlias = "key0"
$keyPassword = "password"
$storePassword = "password"

Write-Host "Voer de gewenste opties in voor je keystore:"
$keyAlias = Read-Host "Alias (bijv. key0)"
$keyPassword = Read-Host "Key wachtwoord"
$storePassword = Read-Host "Keystore wachtwoord"
$validity = Read-Host "Geldigheid in dagen (bijv. 10000)"
$dname = Read-Host "Distinguished Name (bijv. CN=Arjan, OU=Dev, O=Private, L=City, S=State, C=NL)"
$keystorePath = "C:\Users\arjan\AndroidStudioProjects\TrackerMobilePrivate\keystore.jks"
$params = @(
    "-genkeypair"
    "-v"
    "-keystore", "$keystorePath"
    "-alias", "$keyAlias"
    "-keyalg", "RSA"
    "-keysize", "2048"
    "-validity", "$validity"
    "-storepass", "$storePassword"
    "-keypass", "$keyPassword"
    "-dname", "$dname"
)

$process = Start-Process -FilePath $keytool -ArgumentList $params -Wait -NoNewWindow -PassThru -RedirectStandardOutput "keytool-output.txt" -RedirectStandardError "keytool-error.txt"
Start-Sleep -Seconds 2
if (Test-Path $keystorePath) {
    Write-Host "Keystore aangemaakt: $keystorePath"
    Write-Host "Uitvoer van keytool:"
    Get-Content "keytool-output.txt"
} else {
    Write-Host "Keystore aanmaken is mislukt. Foutmelding van keytool:"
    Get-Content "keytool-error.txt"
}
