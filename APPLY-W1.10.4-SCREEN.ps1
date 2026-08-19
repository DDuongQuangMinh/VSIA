param(
    [string]$ProjectRoot = "."
)

$path = Join-Path $ProjectRoot "src\main\java\com\k1ngtle\vsia\client\screen\WifiEngineeringScreen.java"

if (-not (Test-Path $path)) {
    throw "WifiEngineeringScreen.java not found at $path"
}

$text = [System.IO.File]::ReadAllText($path)
$nl = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$text = $text.Replace("`r`n","`n").Replace("`r","`n")

if ($text.Contains('"Auto Web"')) {
    Write-Host "W1.10.4 analyzer Auto Web button already present."
    exit 0
}

$backup = "$path.w1.10.4.bak"

if (-not (Test-Path $backup)) {
    [System.IO.File]::Copy($path,$backup)
}

$old = @'
        addIpButton(
                "TCP HTTP",
                OUTER_MARGIN,
                tcpY,
                tcpWidth,
                WifiIpAction.TCP_HTTP_GET
        );

        addIpButton(
                "TCP Close",
                OUTER_MARGIN
                        + (
                        tcpWidth
                                + tcpGap
                ),
                tcpY,
                tcpWidth,
                WifiIpAction.TCP_CLOSE
        );

        addIpButton(
                "HTTP Direct",
                OUTER_MARGIN
                        + 2
                        * (
                        tcpWidth
                                + tcpGap
                ),
                tcpY,
                tcpWidth,
                WifiIpAction.HTTP_GET
        );

        addIpButton(
                "Clear IP/TCP",
                OUTER_MARGIN
                        + 3
                        * (
                        tcpWidth
                                + tcpGap
                ),
                tcpY,
                tcpWidth,
                WifiIpAction.CLEAR_METRICS
        );
'@

$new = @'
        addIpButton(
                "Auto Web",
                OUTER_MARGIN,
                tcpY,
                tcpWidth,
                WifiIpAction.RAW_HTTP_WORKFLOW
        );

        addIpButton(
                "TCP HTTP",
                OUTER_MARGIN
                        + (
                        tcpWidth
                                + tcpGap
                ),
                tcpY,
                tcpWidth,
                WifiIpAction.TCP_HTTP_GET
        );

        addIpButton(
                "TCP Close",
                OUTER_MARGIN
                        + 2
                        * (
                        tcpWidth
                                + tcpGap
                ),
                tcpY,
                tcpWidth,
                WifiIpAction.TCP_CLOSE
        );

        addIpButton(
                "Clear IP/TCP",
                OUTER_MARGIN
                        + 3
                        * (
                        tcpWidth
                                + tcpGap
                ),
                tcpY,
                tcpWidth,
                WifiIpAction.CLEAR_METRICS
        );
'@

if (-not $text.Contains($old)) {
    throw "W1.10.4 screen patch anchor not found: TCP controls"
}

$text = $text.Replace($old,$new)

if ($nl -eq "`r`n") {
    $text = $text.Replace("`n","`r`n")
}

$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($path,$text,$utf8)

Write-Host "W1.10.4 analyzer patched:"
Write-Host $path
Write-Host "Backup:"
Write-Host $backup
