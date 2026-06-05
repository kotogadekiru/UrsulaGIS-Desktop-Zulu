# Refreshes libs/voyager2/ from a local CN1SDK build.
# Usage: .\scripts\refresh-voyager2-bundle.ps1 [-Cn1SdkRoot "D:\path\to\CN1SDK_4.1.2"]

param(
    [string]$Cn1SdkRoot = "D:\worskpaces\CN1SDK_4.1.2"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$destRoot = Join-Path $repoRoot "libs\voyager2"
$sampleApp = Join-Path $Cn1SdkRoot "Voyager2SampleApp\Voyager2SampleApp"
$nativeSrc = Join-Path $sampleApp "JavaWrapper\build\Release"
$sdkSrc = Join-Path $sampleApp "bin\Release\net8.0-windows7.0"

foreach ($path in @($nativeSrc, $sdkSrc)) {
    if (-not (Test-Path $path)) {
        Write-Error "CN1SDK path not found: $path. Build JavaWrapper and Voyager2SampleApp first."
    }
}

$nativeDest = Join-Path $destRoot "native"
$sdkDest = Join-Path $destRoot "sdk"
New-Item -ItemType Directory -Force -Path $nativeDest, $sdkDest | Out-Null

Copy-Item (Join-Path $nativeSrc "CNHVoyager2JNI.dll") $nativeDest -Force
Copy-Item (Join-Path $nativeSrc "CNHVoyager2Bridge.dll") $nativeDest -Force
Copy-Item (Join-Path $nativeSrc "nethost.dll") $nativeDest -Force
Copy-Item (Join-Path $sdkSrc "*") $sdkDest -Force

Write-Host "Voyager 2 legacy bundle updated at $destRoot"
Get-ChildItem -Recurse $destRoot | Select-Object FullName, Length
