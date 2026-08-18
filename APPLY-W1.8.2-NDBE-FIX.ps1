param(
    [string]$ProjectRoot = "."
)

$relative = "src\main\java\com\k1ngtle\vsia\signality\internet\NetworkDeviceBlockEntity.java"
$path = Join-Path $ProjectRoot $relative

if (-not (Test-Path $path)) {
    throw "NetworkDeviceBlockEntity.java not found at $path"
}

$text = [System.IO.File]::ReadAllText($path)
$originalNewline = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$normalized = $text.Replace("`r`n", "`n").Replace("`r", "`n")

if ($normalized.Contains("public String wifiSecurityDiagnostic()")) {
    Write-Host "wifiSecurityDiagnostic() already exists. No change needed."
    exit 0
}

$anchor = @'
    public WifiSecurityState wifiSecurityState() {
        return wifiMac.securityState();
    }
'@

$replacement = @'
    public WifiSecurityState wifiSecurityState() {
        return wifiMac.securityState();
    }

    public String wifiSecurityDiagnostic() {
        return wifiMac.lastSecurityDiagnostic();
    }
'@

if (-not $normalized.Contains($anchor)) {
    throw "Patch anchor not found: wifiSecurityState()"
}

$backup = "$path.w1.8.2-ndbe.bak"
if (-not (Test-Path $backup)) {
    [System.IO.File]::Copy($path, $backup)
}

$normalized = $normalized.Replace($anchor, $replacement)

if ($originalNewline -eq "`r`n") {
    $normalized = $normalized.Replace("`n", "`r`n")
}

$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText(
    $path,
    $normalized,
    $utf8
)

Write-Host "W1.8.2 NDBE accessor patched: $path"
Write-Host "Backup: $backup"
