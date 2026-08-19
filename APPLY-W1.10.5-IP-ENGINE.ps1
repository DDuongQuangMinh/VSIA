param([string]$ProjectRoot=".")
$path=Join-Path $ProjectRoot "src\main\java\com\k1ngtle\vsia\signality\engineering\wifi\ip\WifiIpApplicationEngine.java"
if(-not(Test-Path $path)){throw "WifiIpApplicationEngine.java not found at $path"}
$text=[IO.File]::ReadAllText($path);$nl=if($text.Contains("`r`n")){"`r`n"}else{"`n"};$text=$text.Replace("`r`n","`n").Replace("`r","`n")
if($text.Contains("public String neighborMac(")){Write-Host "W1.10.5 neighbor lookup already present.";exit 0}
$backup="$path.w1.10.5.bak";if(-not(Test-Path $backup)){[IO.File]::Copy($path,$backup)}
$old=@'
    public Map<String, WifiIpNeighbor> neighbors() {
        return Map.copyOf(
                neighbors
        );
    }

'@
$new=@'
    public Map<String, WifiIpNeighbor> neighbors() {
        return Map.copyOf(
                neighbors
        );
    }

    public String neighborMac(
            String ip
    ) {
        WifiIpNeighbor neighbor =
                neighbors.get(
                        ip
                );

        return neighbor == null
                ? ""
                : neighbor.mac();
    }

'@
if(-not $text.Contains($old)){throw "W1.10.5 app patch anchor not found: neighbors()"}
$text=$text.Replace($old,$new)
if($nl-eq"`r`n"){$text=$text.Replace("`n","`r`n")}
[IO.File]::WriteAllText($path,$text,(New-Object Text.UTF8Encoding($false)))
Write-Host "Patched WifiIpApplicationEngine neighbor lookup."
