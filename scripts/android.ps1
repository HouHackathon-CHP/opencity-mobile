param([switch]$Build)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$shortRoot = Join-Path ([System.IO.Path]::GetPathRoot($root)) 'oc'
if (Test-Path -LiteralPath $shortRoot) {
  $link = Get-Item -LiteralPath $shortRoot
  if ($link.LinkType -ne 'Junction' -or $link.Target -ne $root) { throw "Đường dẫn $shortRoot đã được sử dụng." }
} else {
  New-Item -ItemType Junction -Path $shortRoot -Target $root | Out-Null
}
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adb)) { throw 'Android SDK/adb chưa được cài.' }
if (-not ((& $adb devices) -match 'emulator-\d+\s+device')) { throw 'Hãy mở Android emulator trước.' }
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
& $adb reverse tcp:8081 tcp:8081 | Out-Null
& $adb reverse tcp:8000 tcp:8000 | Out-Null
& $adb reverse tcp:19000 tcp:19000 | Out-Null
$node = (Get-Command node -ErrorAction SilentlyContinue).Source
$bundled = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
if (-not $node -or -not ((& $node --version) -match '^v24\.')) { $node = $bundled }
if (-not (Test-Path -LiteralPath $node)) { throw 'Cần Node.js 24 để chạy Expo SDK 58.' }
$expo = Join-Path $shortRoot 'node_modules\expo\bin\cli'
if (-not (Test-Path -LiteralPath $expo)) { throw 'Hãy chạy npm ci trong repo trước.' }
Push-Location $shortRoot
try {
  if ($Build) {
    & $node --dns-result-order=ipv4first $expo run:android --port 19000
    if ($LASTEXITCODE -ne 0) { throw 'Android build thất bại.' }
  } else {
    Write-Host 'Mở OpenCity trên emulator và chọn server http://127.0.0.1:19000 trong Development Build.'
    & $node --dns-result-order=ipv4first $expo start --dev-client --port 19000 --localhost
    if ($LASTEXITCODE -ne 0) { throw 'Metro không khởi động được.' }
  }
} finally {
  Pop-Location
}
