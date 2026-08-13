package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class ServerRackBlockEntity extends NetworkDeviceBlockEntity implements GeoBlockEntity {
    private static final Pattern IPV4 = Pattern.compile("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$");
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlay("open_rack");
    private static final RawAnimation CLOSE = RawAnimation.begin().thenPlay("close_rack");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, String> dnsRecords = new HashMap<>();
    private final Map<String, String> leasedIps = new HashMap<>();
    private final Map<String, ListTag> mailboxes = new HashMap<>();
    private String displayName = "Server0", subnetMask = "255.255.255.0", gatewayIp = "192.168.1.1", dnsServer = "192.168.1.2";
    private boolean usesDhcp, httpEnabled = true, dnsEnabled = true, dhcpEnabled = true, mailEnabled = true, doorOpen;
    private int nextIpSuffix = 100;
    private String indexHtml = "<html><body><h1>VSIA Server Rack</h1></body></html>";

    public ServerRackBlockEntity(BlockPos pos, BlockState state) {
        super(SignalityBlocks.SERVER_RACK_BE.get(), pos, state);
        ipAddress = "192.168.1.2";
        dnsRecords.put("www.vsia-net.com", ipAddress); dnsRecords.put("mail.vsia-net.com", ipAddress);
    }
    public String displayName(){return displayName;} public String ipAddress(){return ipAddress;}
    public String subnetMask(){return subnetMask;} public String gatewayIp(){return gatewayIp;}
    public String dnsServer(){return dnsServer;} public boolean usesDhcp(){return usesDhcp;}
    public boolean httpEnabled(){return httpEnabled;} public boolean dnsEnabled(){return dnsEnabled;}
    public boolean dhcpEnabled(){return dhcpEnabled;} public boolean mailEnabled(){return mailEnabled;}
    public boolean isDoorOpen(){return doorOpen;}
    public void openDoor(){doorOpen=true; triggerAnim("rack_controller","open"); setChanged();}
    public void closeDoor(){doorOpen=false; triggerAnim("rack_controller","close"); setChanged();}

    public String applyGuiConfiguration(String name, String ip, String subnet, String gateway, String dns,
                                        boolean dhcp, boolean http, boolean dnsOn, boolean dhcpOn, boolean mailOn) {
        name = name.trim(); ip = ip.trim(); subnet = subnet.trim(); gateway = gateway.trim(); dns = dns.trim();
        if (name.isEmpty() || name.length() > 32) return "Display name must be 1-32 characters.";
        if (!validIp(ip) || !validIp(subnet) || !validIp(gateway) || !validIp(dns)) return "One or more IPv4 addresses are invalid.";
        displayName=name; ipAddress=ip; subnetMask=subnet; gatewayIp=gateway; dnsServer=dns; usesDhcp=dhcp;
        httpEnabled=http; dnsEnabled=dnsOn; dhcpEnabled=dhcpOn; mailEnabled=mailOn; setChanged(); return null;
    }
    private static boolean validIp(String value){return IPV4.matcher(value).matches();}
    private OSINetworkPacket response(OSINetworkPacket q,int port,String protocol){
        OSINetworkPacket r=new OSINetworkPacket(); r.sourceMac=macAddress;r.targetMac=q.sourceMac;r.sourceIp=ipAddress;
        r.targetIp=q.sourceIp;r.sourcePort=port;r.targetPort=q.sourcePort;r.applicationProtocol=protocol;r.isResponse=true;r.sessionId=q.sessionId;return r;
    }
    @Override protected void handleWebRequest(OSINetworkPacket q){if(httpEnabled&&!q.isResponse&&"HTTP".equalsIgnoreCase(q.applicationProtocol)){OSINetworkPacket r=response(q,80,"HTTP");r.payload.putInt("status",200);r.payload.putString("html",indexHtml);transmitPacket(r);}}
    @Override protected void handleDnsRequest(OSINetworkPacket q){if(dnsEnabled&&!q.isResponse&&"DNS".equalsIgnoreCase(q.applicationProtocol)){OSINetworkPacket r=response(q,53,"DNS");String d=q.payload.getString("domain").toLowerCase();r.payload.putString("domain",d);r.payload.putString("resolved_ip",dnsRecords.getOrDefault(d,"0.0.0.0"));transmitPacket(r);}}
    @Override protected void handleDhcpRequest(OSINetworkPacket q){if(dhcpEnabled&&!q.isResponse&&"DHCP".equalsIgnoreCase(q.applicationProtocol)&&"DISCOVER".equalsIgnoreCase(q.payload.getString("type"))){String assigned=leasedIps.computeIfAbsent(q.sourceMac,k->"192.168.1."+nextIpSuffix++);OSINetworkPacket r=response(q,67,"DHCP");r.targetIp="255.255.255.255";r.payload.putString("type","ACK");r.payload.putString("assigned_ip",assigned);r.payload.putString("subnet_mask",subnetMask);r.payload.putString("router_ip",gatewayIp);transmitPacket(r);setChanged();}}
    @Override protected void handleMailRequest(OSINetworkPacket q){if(!mailEnabled||q.isResponse||!"SMTP".equalsIgnoreCase(q.applicationProtocol))return;OSINetworkPacket r=response(q,25,"SMTP");String a=q.payload.getString("action");if("SEND".equalsIgnoreCase(a)){String to=q.payload.getString("to").toLowerCase();CompoundTag m=new CompoundTag();m.putString("from",q.payload.getString("from"));m.putString("to",to);m.putString("subject",q.payload.getString("subject"));m.putString("body",q.payload.getString("body"));mailboxes.computeIfAbsent(to,k->new ListTag()).add(m);r.payload.putString("status","DELIVERED");setChanged();}else if("LIST".equalsIgnoreCase(a)){r.payload.put("messages",mailboxes.getOrDefault(q.payload.getString("address").toLowerCase(),new ListTag()).copy());r.payload.putString("status","OK");}transmitPacket(r);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"rack_controller",0,s->PlayState.STOP).triggerableAnim("open",OPEN).triggerableAnim("close",CLOSE));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putString("DisplayName",displayName);t.putString("SubnetMask",subnetMask);t.putString("GatewayIp",gatewayIp);t.putString("DnsServer",dnsServer);t.putBoolean("UsesDhcp",usesDhcp);t.putBoolean("HttpEnabled",httpEnabled);t.putBoolean("DnsEnabled",dnsEnabled);t.putBoolean("DhcpEnabled",dhcpEnabled);t.putBoolean("MailEnabled",mailEnabled);t.putBoolean("DoorOpen",doorOpen);t.putInt("NextIpSuffix",nextIpSuffix);t.putString("IndexHtml",indexHtml);CompoundTag d=new CompoundTag();dnsRecords.forEach(d::putString);t.put("DnsRecords",d);CompoundTag l=new CompoundTag();leasedIps.forEach(l::putString);t.put("LeasedIps",l);CompoundTag m=new CompoundTag();mailboxes.forEach(m::put);t.put("Mailboxes",m);}
    @Override public void load(CompoundTag t){super.load(t);if(t.contains("DisplayName"))displayName=t.getString("DisplayName");if(t.contains("SubnetMask"))subnetMask=t.getString("SubnetMask");if(t.contains("GatewayIp"))gatewayIp=t.getString("GatewayIp");if(t.contains("DnsServer"))dnsServer=t.getString("DnsServer");if(t.contains("UsesDhcp"))usesDhcp=t.getBoolean("UsesDhcp");if(t.contains("HttpEnabled"))httpEnabled=t.getBoolean("HttpEnabled");if(t.contains("DnsEnabled"))dnsEnabled=t.getBoolean("DnsEnabled");if(t.contains("DhcpEnabled"))dhcpEnabled=t.getBoolean("DhcpEnabled");if(t.contains("MailEnabled"))mailEnabled=t.getBoolean("MailEnabled");doorOpen=t.getBoolean("DoorOpen");if(t.contains("NextIpSuffix"))nextIpSuffix=t.getInt("NextIpSuffix");if(t.contains("IndexHtml"))indexHtml=t.getString("IndexHtml");dnsRecords.clear();CompoundTag d=t.getCompound("DnsRecords");d.getAllKeys().forEach(k->dnsRecords.put(k,d.getString(k)));leasedIps.clear();CompoundTag l=t.getCompound("LeasedIps");l.getAllKeys().forEach(k->leasedIps.put(k,l.getString(k)));mailboxes.clear();CompoundTag m=t.getCompound("Mailboxes");m.getAllKeys().forEach(k->mailboxes.put(k,m.getList(k,Tag.TAG_COMPOUND)));}
}
