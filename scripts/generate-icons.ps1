# Generate simple, code-native OpenCity artwork. Run on Windows PowerShell.
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'
$directory = Join-Path $PSScriptRoot '..\assets\images'
$green = [System.Drawing.ColorTranslator]::FromHtml('#166A53')
$white = [System.Drawing.Brushes]::White
$greenBrush = New-Object System.Drawing.SolidBrush($green)

function Save-Icon([string]$file, [bool]$background, [bool]$mark, [bool]$monochrome = $false) {
  $bitmap = New-Object System.Drawing.Bitmap(1024, 1024)
  $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
  $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $graphics.Clear($(if ($background) { $green } else { [System.Drawing.Color]::Transparent }))
  if ($mark) {
    $points = [System.Drawing.Point[]]@(
      [System.Drawing.Point]::new(359, 554),
      [System.Drawing.Point]::new(512, 864),
      [System.Drawing.Point]::new(665, 554)
    )
    $graphics.FillPolygon($white, $points)
    $graphics.FillEllipse($white, 240, 130, 544, 544)
    if ($monochrome) {
      $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
      $clearBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::Transparent)
      $graphics.FillEllipse($clearBrush, 338, 228, 348, 348)
      $clearBrush.Dispose()
      $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
    } else { $graphics.FillEllipse($greenBrush, 338, 228, 348, 348) }
    $graphics.FillRectangle($white, 397, 418, 59, 102)
    $graphics.FillRectangle($white, 475, 363, 73, 157)
    $graphics.FillRectangle($white, 568, 395, 59, 125)
  }
  $bitmap.Save((Join-Path $directory $file), [System.Drawing.Imaging.ImageFormat]::Png)
  $graphics.Dispose()
  $bitmap.Dispose()
}

Save-Icon 'icon.png' $true $true
Save-Icon 'android-icon-foreground.png' $false $true
Save-Icon 'android-icon-monochrome.png' $false $true $true
Save-Icon 'android-icon-background.png' $true $false
Save-Icon 'splash-icon.png' $false $true
$source = [System.Drawing.Image]::FromFile((Join-Path $directory 'icon.png'))
$favicon = New-Object System.Drawing.Bitmap(64, 64)
$graphics = [System.Drawing.Graphics]::FromImage($favicon)
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$graphics.DrawImage($source, 0, 0, 64, 64)
$favicon.Save((Join-Path $directory 'favicon.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$favicon.Dispose()
$source.Dispose()
$greenBrush.Dispose()
