$mjgaJad = Join-Path $PSScriptRoot "dist\MJGA.jad"
if (-not (Test-Path -LiteralPath $mjgaJad -PathType Leaf)) {
    Write-Error "Missing $mjgaJad; run: ant clean test dist"
    exit 1
}

$mjgaEmulator = Join-Path $PSScriptRoot "emulator\microemulator.jar"
$mjgaJsr75 = Join-Path $PSScriptRoot "emulator\microemu-jsr-75.jar"
$mjgaClasspath = $mjgaEmulator + ";" + $mjgaJsr75
& java -cp $mjgaClasspath org.microemu.app.Main $mjgaJad
exit $LASTEXITCODE
