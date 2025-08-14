# PowerShell script to download all dependencies for com.android.application plugin version 8.9.1

# Define the base URL for Google's Maven repository
$baseUrl = "https://dl.google.com/dl/android/maven2"

# Define the dependencies to download
$dependencies = @(
    "com/android/tools/build/gradle/8.9.1/gradle-8.9.1.pom",
    "com/android/tools/build/gradle/8.9.1/gradle-8.9.1.jar",
    "com/android/tools/build/builder/8.9.1/builder-8.9.1.jar",
    "com/android/tools/build/builder-model/8.9.1/builder-model-8.9.1.jar",
    "com/android/tools/build/gradle-api/8.9.1/gradle-api-8.9.1.jar",
    "com/android/tools/build/gradle-settings-api/8.9.1/gradle-settings-api-8.9.1.jar",
    "com/android/tools/sdk-common/31.9.1/sdk-common-31.9.1.jar",
    "com/android/tools/sdklib/31.9.1/sdklib-31.9.1.jar",
    "com/android/tools/repository/31.9.1/repository-31.9.1.jar",
    "com/android/tools/ddms/31.9.1/ddmlib-31.9.1.jar",
    "com/android/tools/build/aapt2-proto/8.9.1-12782657/aapt2-proto-8.9.1-12782657.jar",
    "com/android/tools/build/aaptcompiler/8.9.1/aaptcompiler-8.9.1.jar",
    "com/android/tools/analytics-library/crash/31.9.1/crash-31.9.1.jar",
    "com/android/tools/analytics-library/shared/31.9.1/shared-31.9.1.jar",
    "com/android/tools/lint/lint-model/31.9.1/lint-model-31.9.1.jar",
    "com/android/tools/lint/lint-typedef-remover/31.9.1/lint-typedef-remover-31.9.1.jar",
    "androidx/databinding/databinding-compiler-common/8.9.1/databinding-compiler-common-8.9.1.jar",
    "androidx/databinding/databinding-common/8.9.1/databinding-common-8.9.1.jar",
    "com/android/databinding/baseLibrary/8.9.1/baseLibrary-8.9.1.jar",
    "com/android/tools/build/builder-test-api/8.9.1/builder-test-api-8.9.1.jar",
    "com/android/tools/layoutlib/layoutlib-api/31.9.1/layoutlib-api-31.9.1.jar",
    "com/android/tools/utp/android-device-provider-ddmlib-proto/31.9.1/android-device-provider-ddmlib-proto-31.9.1.jar",
    "com/android/tools/utp/android-device-provider-gradle-proto/31.9.1/android-device-provider-gradle-proto-31.9.1.jar",
    "com/android/tools/utp/android-device-provider-profile-proto/31.9.1/android-device-provider-profile-proto-31.9.1.jar",
    "com/android/tools/utp/android-test-plugin-host-additional-test-output-proto/31.9.1/android-test-plugin-host-additional-test-output-proto-31.9.1.jar",
    "com/android/tools/utp/android-test-plugin-host-coverage-proto/31.9.1/android-test-plugin-host-coverage-proto-31.9.1.jar",
    "com/android/tools/utp/android-test-plugin-host-emulator-control-proto/31.9.1/android-test-plugin-host-emulator-control-proto-31.9.1.jar",
    "com/android/tools/utp/android-test-plugin-host-logcat-proto/31.9.1/android-test-plugin-host-logcat-proto-31.9.1.jar",
    "com/android/tools/utp/android-test-plugin-host-apk-installer-proto/31.9.1/android-test-plugin-host-apk-installer-proto-31.9.1.jar",
    "com/android/tools/utp/android-test-plugin-host-retention-proto/31.9.1/android-test-plugin-host-retention-proto-31.9.1.jar",
    "com/android/tools/utp/android-test-plugin-result-listener-gradle-proto/31.9.1/android-test-plugin-result-listener-gradle-proto-31.9.1.jar",
    "org/jetbrains/kotlin/kotlin-stdlib-jdk8/2.1.0/kotlin-stdlib-jdk8-2.1.0.jar",
    "com/android/tools/build/transform-api/2.0.0-deprecated-use-gradle-api/transform-api-2.0.0-deprecated-use-gradle-api.jar",
    "org/apache/httpcomponents/httpmime/4.5.6/httpmime-4.5.6.jar",
    "commons-io/commons-io/2.16.1/commons-io-2.16.1.jar",
    "org/ow2/asm/asm/9.7/asm-9.7.jar",
    "org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar",
    "org/ow2/asm/asm-commons/9.7/asm-commons-9.7.jar",
    "org/ow2/asm/asm-util/9.7/asm-util-9.7.jar",
    "org/bouncycastle/bcpkix-jdk18on/1.79/bcpkix-jdk18on-1.79.jar",
    "org/glassfish/jaxb/jaxb-runtime/2.3.2/jaxb-runtime-2.3.2.jar",
    "net/sf/jopt-simple/jopt-simple/4.9/jopt-simple-4.9.jar",
    "com/android/tools/build/bundletool/1.17.2/bundletool-1.17.2.jar",
    "com/android/tools/build/jetifier/jetifier-core/1.0.0-beta10/jetifier-core-1.0.0-beta10.jar",
    "com/android/tools/build/jetifier/jetifier-processor/1.0.0-beta10/jetifier-processor-1.0.0-beta10.jar",
    "com/squareup/javapoet/1.10.0/javapoet-1.10.0.jar",
    "com/google/protobuf/protobuf-java/3.22.3/protobuf-java-3.22.3.jar",
    "com/google/protobuf/protobuf-java-util/3.22.3/protobuf-java-util-3.22.3.jar",
    "com/google/code/gson/gson/2.10.1/gson-2.10.1.jar",
    "io/grpc/grpc-core/1.57.0/grpc-core-1.57.0.jar",
    "io/grpc/grpc-netty/1.57.0/grpc-netty-1.57.0.jar",
    "io/grpc/grpc-protobuf/1.57.0/grpc-protobuf-1.57.0.jar",
    "io/grpc/grpc-stub/1.57.0/grpc-stub-1.57.0.jar",
    "com/google/crypto/tink/tink/1.7.0/tink-1.7.0.jar",
    "com/google/testing/platform/core-proto/0.0.9-alpha03/core-proto-0.0.9-alpha03.jar",
    "com/google/flatbuffers/flatbuffers-java/1.12.0/flatbuffers-java-1.12.0.jar",
    "org/tensorflow/tensorflow-lite-metadata/0.2.0/tensorflow-lite-metadata-0.2.0.jar"
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
