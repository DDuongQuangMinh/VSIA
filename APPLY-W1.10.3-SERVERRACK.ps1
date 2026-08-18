param(
    [string]$ProjectRoot = "."
)

$path = Join-Path $ProjectRoot "src\main\java\com\k1ngtle\vsia\signality\internet\server\ServerRackBlockEntity.java"

if (-not (Test-Path $path)) {
    throw "ServerRackBlockEntity.java not found at $path"
}

$text = [System.IO.File]::ReadAllText($path)
$nl = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$text = $text.Replace("`r`n","`n").Replace("`r","`n")

if ($text.Contains('r.payload.putInt("dns_id"')) {
    Write-Host "W1.10.3 ServerRack DNS integration already present."
    exit 0
}

$backup = "$path.w1.10.3.bak"
if (-not (Test-Path $backup)) {
    [System.IO.File]::Copy($path,$backup)
}

$old = '@Override protected void handleDnsRequest(OSINetworkPacket q){if(dnsEnabled&&!q.isResponse&&"DNS".equalsIgnoreCase(q.applicationProtocol)){OSINetworkPacket r=response(q,53,"DNS");String d=q.payload.getString("domain").toLowerCase();String type=q.payload.contains("query_type")?q.payload.getString("query_type"):"A";String answer=resolveDns(d,type);r.payload.putString("domain",d);r.payload.putString("record_type",type);r.payload.putString("resolved_ip",answer==null?"0.0.0.0":answer);transmitPacket(r);}}'

$new = '@Override protected void handleDnsRequest(OSINetworkPacket q){if(dnsEnabled&&!q.isResponse&&"DNS".equalsIgnoreCase(q.applicationProtocol)){OSINetworkPacket r=response(q,53,"DNS");String d=q.payload.getString("domain").toLowerCase();String type=q.payload.contains("query_type")?q.payload.getString("query_type"):"A";String answer=resolveDns(d,type);ServerRackDnsRecord record=detailedDnsRecords.get(type.toUpperCase()+"|"+d);int ttl=record==null?300:record.ttl();r.payload.putInt("dns_id",q.payload.getInt("dns_id"));r.payload.putString("domain",d);r.payload.putString("query_type",type);r.payload.putString("record_type",type);r.payload.putString("answer",answer==null?"":answer);r.payload.putString("resolved_ip",answer==null||!"A".equalsIgnoreCase(type)?"0.0.0.0":answer);r.payload.putInt("ttl",answer==null?0:ttl);r.payload.putInt("rcode",answer==null?3:0);transmitPacket(r);}}'

if (-not $text.Contains($old)) {
    throw "W1.10.3 ServerRack patch anchor not found: handleDnsRequest"
}

$text = $text.Replace($old,$new)

if ($nl -eq "`r`n") {
    $text = $text.Replace("`n","`r`n")
}

$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($path,$text,$utf8)

Write-Host "W1.10.3 ServerRack DNS patched:"
Write-Host $path
Write-Host "Backup:"
Write-Host $backup
