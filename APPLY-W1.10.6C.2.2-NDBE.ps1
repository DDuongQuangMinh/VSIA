param(
    [string]$ProjectRoot = "."
)

$path = Join-Path $ProjectRoot "src\main\java\com\k1ngtle\vsia\signality\internet\NetworkDeviceBlockEntity.java"

if (-not (Test-Path $path)) {
    throw "NetworkDeviceBlockEntity.java not found at $path"
}

$text = [System.IO.File]::ReadAllText($path)

$usesCrlf = $text.Contains("`r`n")
$text = $text.Replace("`r`n", "`n")
$text = $text.Replace("`r", "`n")

if ($text.Contains("configureWifiActiveFrequency(")) {
    Write-Host "W1.10.6C.2.2 Wi-Fi channel API already present."
    exit 0
}

$backup = "$path.w1.10.6C.2.2.bak"

if (-not (Test-Path $backup)) {
    [System.IO.File]::Copy(
        $path,
        $backup
    )
}

$anchor = @'
    public double activeFrequencyHz() {
        return activeFrequencyHz;
    }

'@

$replacement = @'
    public double activeFrequencyHz() {
        return activeFrequencyHz;
    }

    public boolean configureWifiActiveFrequency(
            double frequencyHz
    ) {
        if (!isWifiProfile()
                || !Double.isFinite(
                frequencyHz
        )
                || !networkProfile()
                .supportsFrequency(
                        frequencyHz
                )) {
            return false;
        }

        activeFrequencyHz =
                frequencyHz;

        setChanged();

        return true;
    }

'@

if (-not $text.Contains($anchor)) {
    throw "W1.10.6C.2.2 anchor not found: activeFrequencyHz()"
}

$text = $text.Replace(
    $anchor,
    $replacement
)

if (-not $text.Contains("configureWifiActiveFrequency(")) {
    throw "Verification failed: configureWifiActiveFrequency() missing."
}

if ($usesCrlf) {
    $text = $text.Replace("`n", "`r`n")
}

$utf8 = New-Object System.Text.UTF8Encoding($false)

[System.IO.File]::WriteAllText(
    $path,
    $text,
    $utf8
)

Write-Host ""
Write-Host "W1.10.6C.2.2 Wi-Fi channel API applied."
Write-Host "Backup: $backup"
