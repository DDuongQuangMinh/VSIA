param(
    [string]$ProjectRoot = "."
)

$path = Join-Path $ProjectRoot "src\main\java\com\k1ngtle\vsia\signality\internet\NetworkDeviceBlockEntity.java"

if (-not (Test-Path $path)) {
    throw "NetworkDeviceBlockEntity.java not found at $path"
}

$text = [System.IO.File]::ReadAllText($path)
$nl = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$text = $text.Replace("`r`n", "`n").Replace("`r", "`n")

if ($text.Contains("DHCP RAW RX ")) {
    Write-Host "W1.10.2.1 DHCP diagnostics already present."
    exit 0
}

$backup = "$path.w1.10.2.1.bak"

if (-not (Test-Path $backup)) {
    [System.IO.File]::Copy($path, $backup)
}

$old = @'
                if (rawDhcp.sourceMac != null
                        && !rawDhcp.sourceMac.isBlank()) {
                    wifiRawDhcpPeers.add(
                            rawDhcp.sourceMac
                    );
                }

                processLayer2(
                        rawDhcp
                );
'@

$new = @'
                if (rawDhcp.sourceMac != null
                        && !rawDhcp.sourceMac.isBlank()) {
                    wifiRawDhcpPeers.add(
                            rawDhcp.sourceMac
                    );
                }

                wifiIpApplication.setStatus(
                        "DHCP RAW RX "
                                + rawDhcp.payload.getString(
                                "type"
                        )
                                + " | xid "
                                + Integer.toUnsignedString(
                                rawDhcp.payload.getInt(
                                        "xid"
                                )
                        )
                                + " | link "
                                + rawDhcp.sourceMac
                                + " -> "
                                + rawDhcp.targetMac
                );

                processLayer2(
                        rawDhcp
                );
'@

if (-not $text.Contains($old)) {
    throw "W1.10.2.1 NDBE patch anchor not found: raw DHCP receive block"
}

$text = $text.Replace($old, $new)

if ($nl -eq "`r`n") {
    $text = $text.Replace("`n", "`r`n")
}

$utf8 = New-Object System.Text.UTF8Encoding($false)

[System.IO.File]::WriteAllText(
    $path,
    $text,
    $utf8
)

Write-Host "W1.10.2.1 DHCP diagnostics patched:"
Write-Host $path
Write-Host "Backup:"
Write-Host $backup
