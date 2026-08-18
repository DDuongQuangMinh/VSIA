param(
    [string]$ProjectRoot = "."
)

$relative = "src\main\java\com\k1ngtle\vsia\signality\Signality.java"
$path = Join-Path $ProjectRoot $relative

if (-not (Test-Path $path)) {
    throw "Signality.java not found at $path"
}

$text = [System.IO.File]::ReadAllText($path)
$originalNewline = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$text = $text.Replace("`r`n", "`n").Replace("`r", "`n")

if ($text.Contains("TcpLiveScheduler.tickAll()")) {
    Write-Host "W1.9.2 Signality scheduler integration already present."
    exit 0
}

$backup = "$path.w1.9.2.bak"
if (-not (Test-Path $backup)) {
    [System.IO.File]::Copy($path, $backup)
}

function Replace-Exact {
    param(
        [string]$Label,
        [string]$Old,
        [string]$New
    )

    if (-not $script:text.Contains($Old)) {
        throw "W1.9.2 Signality patch anchor not found: $Label"
    }

    $script:text = $script:text.Replace($Old, $New)
}

Replace-Exact "TCP scheduler import" @'
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMacTimingScheduler;
'@ @'
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMacTimingScheduler;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveScheduler;
'@

Replace-Exact "TCP scheduler clear" @'
      WifiMacTimingScheduler.clear();
'@ @'
      WifiMacTimingScheduler.clear();
      TcpLiveScheduler.clear();
'@

Replace-Exact "TCP scheduler tick" @'
            ProtocolVmScheduler.tickAll();
'@ @'
            ProtocolVmScheduler.tickAll();
            TcpLiveScheduler.tickAll();
'@

if ($originalNewline -eq "`r`n") {
    $text = $text.Replace("`n", "`r`n")
}

$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText(
    $path,
    $text,
    $utf8
)

Write-Host "W1.9.2 Signality scheduler patched: $path"
Write-Host "Backup: $backup"
