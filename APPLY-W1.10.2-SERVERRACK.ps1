param(
    [string]$ProjectRoot = "."
)

$path = Join-Path $ProjectRoot "src\main\java\com\k1ngtle\vsia\signality\internet\server\ServerRackBlockEntity.java"

if (-not (Test-Path $path)) {
    throw "ServerRackBlockEntity.java not found at $path"
}

$text=[System.IO.File]::ReadAllText($path)
$nl=if($text.Contains("`r`n")){"`r`n"}else{"`n"}
$text=$text.Replace("`r`n","`n").Replace("`r","`n")

if($text.Contains("DHCP DORA OFFER")){
    Write-Host "W1.10.2 ServerRack DHCP integration already present."
    exit 0
}

$backup="$path.w1.10.2.bak"
if(-not(Test-Path $backup)){
    [System.IO.File]::Copy($path,$backup)
}

$old='@Override protected void handleDhcpRequest(OSINetworkPacket q){if(q.isResponse)return;boolean v6="DHCPV6".equalsIgnoreCase(q.applicationProtocol);if((v6&&!serviceEnabled(ServerRackService.DHCPV6))||(!v6&&(!dhcpEnabled||!"DHCP".equalsIgnoreCase(q.applicationProtocol))))return;String action=q.payload.getString("type");String key=(v6?"6:":"4:")+q.sourceMac;if("RELEASE".equalsIgnoreCase(action)){dhcpLeases.remove(key);setChanged();return;}if(!"DISCOVER".equalsIgnoreCase(action)&&!"REQUEST".equalsIgnoreCase(action)&&!"RENEW".equalsIgnoreCase(action))return;ServerRackDhcpLease lease=allocateLease(q.sourceMac,v6);OSINetworkPacket r=response(q,v6?547:67,v6?"DHCPV6":"DHCP");r.targetIp=v6?"ff02::1:2":"255.255.255.255";if(lease==null){r.payload.putString("type","NAK");}else{ServerRackDhcpPool pool=dhcpPools.values().stream().filter(p->p.name().equals(lease.pool())&&p.ipv6()==v6).findFirst().orElse(null);r.payload.putString("type","ACK");r.payload.putString("assigned_ip",lease.address());r.payload.putString(v6?"prefix_length":"subnet_mask",pool.prefixOrMask());r.payload.putString("router_ip",pool.gateway());r.payload.putString("dns_server",pool.dns());r.payload.putInt("lease_seconds",pool.leaseSeconds());}transmitPacket(r);setChanged();}'

$new='@Override protected void handleDhcpRequest(OSINetworkPacket q){if(q.isResponse)return;boolean v6="DHCPV6".equalsIgnoreCase(q.applicationProtocol);if((v6&&!serviceEnabled(ServerRackService.DHCPV6))||(!v6&&(!dhcpEnabled||!"DHCP".equalsIgnoreCase(q.applicationProtocol))))return;String action=q.payload.getString("type");String key=(v6?"6:":"4:")+q.sourceMac;if("RELEASE".equalsIgnoreCase(action)){dhcpLeases.remove(key);setChanged();return;}if(!"DISCOVER".equalsIgnoreCase(action)&&!"REQUEST".equalsIgnoreCase(action)&&!"RENEW".equalsIgnoreCase(action))return;ServerRackDhcpLease lease=allocateLease(q.sourceMac,v6);OSINetworkPacket r=response(q,v6?547:67,v6?"DHCPV6":"DHCP");r.targetIp=v6?"ff02::1:2":"255.255.255.255";r.payload.putInt("xid",q.payload.getInt("xid"));if(lease==null){r.payload.putString("type","NAK");}else{ServerRackDhcpPool pool=dhcpPools.values().stream().filter(p->p.name().equals(lease.pool())&&p.ipv6()==v6).findFirst().orElse(null);String replyType=!v6&&"DISCOVER".equalsIgnoreCase(action)?"OFFER":"ACK";r.payload.putString("type",replyType);r.payload.putString("assigned_ip",lease.address());r.payload.putString("server_identifier",ipAddress);r.payload.putString(v6?"prefix_length":"subnet_mask",pool.prefixOrMask());r.payload.putString("router_ip",pool.gateway());r.payload.putString("dns_server",pool.dns());r.payload.putInt("lease_seconds",pool.leaseSeconds());if(!v6&&"OFFER".equals(replyType))r.payload.putString("diagnostic","DHCP DORA OFFER");}transmitPacket(r);setChanged();}'

if(-not $text.Contains($old)){
    throw "W1.10.2 ServerRack patch anchor not found: handleDhcpRequest"
}

$text=$text.Replace($old,$new)

if($nl -eq "`r`n"){
    $text=$text.Replace("`n","`r`n")
}

$utf8=New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($path,$text,$utf8)

Write-Host "W1.10.2 ServerRack DHCP patched:"
Write-Host $path
Write-Host "Backup:"
Write-Host $backup
