package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.engineering.host.w120.W120HostStack;
import java.util.HashMap;
import java.util.Map;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
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
    private final Map<String, ServerRackDnsRecord> detailedDnsRecords = new LinkedHashMap<>();
    private final Map<String, String> dnsCache = new HashMap<>();
    private final Map<String, Long> dnsCacheExpiry = new HashMap<>();
    private final Map<String, String> leasedIps = new HashMap<>();
    private final Map<String, ListTag> mailboxes = new HashMap<>();
    private final Map<String,ServerRackDhcpPool> dhcpPools=new LinkedHashMap<>();
    private final Map<String,ServerRackDhcpLease> dhcpLeases=new LinkedHashMap<>();
    private final Map<String,ServerRackHostedFile> hostedFiles=new LinkedHashMap<>();
    private final Map<String,String> fileUsers=new HashMap<>();
    private final Map<String,ServerRackMailAccount> mailAccounts=new LinkedHashMap<>();
    private String mailDomain="vsia-net.com";
    private boolean httpsEnabled=true; private int httpPort=80,httpsPort=443,ftpPort=21,tftpPort=69;
    private String displayName = "Server0", subnetMask = "255.255.255.0", gatewayIp = "192.168.1.1", dnsServer = "192.168.1.2";
    private boolean usesDhcp, httpEnabled = true, dnsEnabled = true, dhcpEnabled = true, mailEnabled = true, doorOpen;
    private long lastInteractionTick = Long.MIN_VALUE;
    private int nextIpSuffix = 100;
    private String indexHtml = "<html><body><h1>VSIA Server Rack</h1></body></html>";
    private String programSource = "hostname Server0; show config";
    private String programOutput = "Ready.";
    private ServerRackProfile profile = ServerRackProfile.INTRA_DATA_CENTER;
    private boolean wiredBackboneConnected;
    private final Set<BlockPos> cableLinks = new LinkedHashSet<>();

    // W1.20 REAL WIRED HOST STACK
    private final W120HostStack w120Host = new W120HostStack();
    private long w120LastTickGameTime = Long.MIN_VALUE;
    private String ipv6Address="fd00::2", ipv6Gateway="fd00::1", ipv6DnsServer="fd00::2";
    private int ipv6PrefixLength=64;
    private boolean ipv6Automatic;
    private long clockOffsetMillis, lastPtpSyncMillis;
    private ServerRackPtpMode ptpMode=ServerRackPtpMode.DISABLED;
    private ServerRackPtpProfile ptpProfile=ServerRackPtpProfile.POWER;
    private boolean ntpServerEnabled,ntpClientEnabled;
    private int ntpStratum=2,ntpPollSeconds=64,clockDriftPpm;
    private String ntpSourceIp=""; private long lastNtpSyncMillis; private String clockSyncStatus="Free running";
    private final java.util.List<ServerRackSyslogEntry> syslogEntries=new java.util.ArrayList<>();
    private int syslogMinimumSeverity=7;
    private boolean syslogAcceptRemote=true;
    private final Map<String,ServerRackAaaUser> aaaUsers=new LinkedHashMap<>();
    private final java.util.List<ServerRackAaaRecord> aaaRecords=new java.util.ArrayList<>();
    private final Map<String,ServerRackRadiusClient> radiusClients=new LinkedHashMap<>();
    private final java.util.List<String> radiusEvents=new java.util.ArrayList<>();
    private final EnumSet<ServerRackService> enabledServices = EnumSet.noneOf(ServerRackService.class);
    private final Map<String,Long> prpSeenFrames = new LinkedHashMap<>();

    public ServerRackBlockEntity(BlockPos pos, BlockState state) {
        super(SignalityBlocks.SERVER_RACK_BE.get(), pos, state);
        ipAddress = "192.168.1.2";
        dnsRecords.put("www.vsia-net.com", ipAddress); dnsRecords.put("mail.vsia-net.com", ipAddress);
        putDefaultDnsRecord("www.vsia-net.com","A",ipAddress);putDefaultDnsRecord("mail.vsia-net.com","A",ipAddress);
        enabledServices.add(ServerRackService.HTTP);enabledServices.add(ServerRackService.DHCP);
        enabledServices.add(ServerRackService.DNS);enabledServices.add(ServerRackService.EMAIL);
        dhcpPools.put("4:lan",new ServerRackDhcpPool("LAN",false,"192.168.1.100","192.168.1.254","255.255.255.0",gatewayIp,dnsServer,3600,""));
        dhcpPools.put("6:lan6",new ServerRackDhcpPool("LAN6",true,"fd00::100","fd00::ffff","64",ipv6Gateway,ipv6DnsServer,3600,""));
        hostedFiles.put("index.html",new ServerRackHostedFile("index.html",indexHtml,true,true));hostedFiles.put("styles.css",new ServerRackHostedFile("styles.css","body { color: white; background: #202020; }",true,true));fileUsers.put("admin","admin");
        mailAccounts.put("admin@vsia-net.com",new ServerRackMailAccount("admin","vsia-net.com","admin",100));
        aaaUsers.put("admin",new ServerRackAaaUser("admin","admin",15,true));
        radiusClients.put("127.0.0.1",new ServerRackRadiusClient("Local NAS","127.0.0.1","vsia-radius",true));
    }

    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel serverLevel&&!getPersistentData().contains("RackId"))assignAutomaticId(ServerRackIdSavedData.allocate(serverLevel));ServerRackDirectory.register(this);}
    @Override public void setRemoved(){ServerRackDirectory.unregister(this);super.setRemoved();}
    public String displayName(){return displayName;} public String ipAddress(){return ipAddress;}
    public int rackId(){return getPersistentData().contains("RackId")?getPersistentData().getInt("RackId"):-1;}
    public void assignAutomaticId(int id){if(getPersistentData().contains("RackId"))return;getPersistentData().putInt("RackId",id);displayName="Server"+id;setChanged();}
    public String subnetMask(){return subnetMask;} public String gatewayIp(){return gatewayIp;}
    public String dnsServer(){return dnsServer;} public boolean usesDhcp(){return usesDhcp;}
    public boolean httpEnabled(){return httpEnabled;} public boolean dnsEnabled(){return dnsEnabled;}
    public boolean dhcpEnabled(){return dhcpEnabled;} public boolean mailEnabled(){return mailEnabled;}
    public boolean isDoorOpen(){return doorOpen;}
    public ServerRackProfile profile(){return profile;}
    public boolean wiredBackboneConnected(){return wiredBackboneConnected;}
    public String ipv6Address(){return ipv6Address;} public int ipv6PrefixLength(){return ipv6PrefixLength;}
    public String ipv6Gateway(){return ipv6Gateway;} public String ipv6DnsServer(){return ipv6DnsServer;}
    public boolean ipv6Automatic(){return ipv6Automatic;} public long clockOffsetMillis(){return clockOffsetMillis;}
    public long lastPtpSyncMillis(){return lastPtpSyncMillis;} public ServerRackPtpMode ptpMode(){return ptpMode;}
    public ServerRackPtpProfile ptpProfile(){return ptpProfile;}
    public boolean ntpServerEnabled(){return ntpServerEnabled;} public boolean ntpClientEnabled(){return ntpClientEnabled;} public int ntpStratum(){return ntpStratum;} public int ntpPollSeconds(){return ntpPollSeconds;} public String ntpSourceIp(){return ntpSourceIp;} public int clockDriftPpm(){return clockDriftPpm;} public long lastNtpSyncMillis(){return lastNtpSyncMillis;} public String clockSyncStatus(){return clockSyncStatus;}
    public String configureNtp(boolean server,boolean client,int stratum,int poll,String source,int drift){if(stratum<1||stratum>15)return "NTP stratum must be 1-15.";if(poll<16||poll>4096)return "NTP poll interval must be 16-4096 seconds.";if(drift< -500||drift>500)return "Clock drift must be -500 to 500 ppm.";ntpServerEnabled=server;ntpClientEnabled=client;ntpStratum=stratum;ntpPollSeconds=poll;ntpSourceIp=source.trim();clockDriftPpm=drift;clockSyncStatus=client?"Waiting for NTP source":server?"NTP server":"Free running";setChanged();return "NTP configuration saved.";}
    public int syslogMinimumSeverity(){return syslogMinimumSeverity;}
    public boolean syslogAcceptRemote(){return syslogAcceptRemote;}
    public String syslogData(){StringBuilder out=new StringBuilder();for(int i=Math.max(0,syslogEntries.size()-100);i<syslogEntries.size();i++){ServerRackSyslogEntry e=syslogEntries.get(i);out.append(e.timestamp()).append('\t').append(e.source().replace('\t',' ')).append('\t').append(e.facility().replace('\t',' ')).append('\t').append(e.severity()).append('\t').append(e.message().replace('\t',' ').replace('\n',' ')).append('\n');}return out.toString();}
    public String manageSyslog(String action,int minimumSeverity,boolean acceptRemote,String facility,int severity,String message,String source){if(action.equals("CLEAR")){syslogEntries.clear();setChanged();return "Syslog entries cleared.";}if(action.equals("CONFIG")){if(minimumSeverity<0||minimumSeverity>7)return "Minimum severity must be 0-7.";syslogMinimumSeverity=minimumSeverity;syslogAcceptRemote=acceptRemote;setChanged();return "Syslog configuration saved.";}if(severity<0||severity>7)return "Severity must be 0-7.";addSyslog(source,facility,severity,message);return "Test message recorded.";}
    private void addSyslog(String source,String facility,int severity,String message){if(!serviceEnabled(ServerRackService.SYSLOG)||severity>syslogMinimumSeverity||message==null||message.isBlank())return;if(syslogEntries.size()>=256)syslogEntries.remove(0);syslogEntries.add(new ServerRackSyslogEntry(deviceTimeMillis(),source==null||source.isBlank()?displayName:source,facility==null||facility.isBlank()?"LOCAL0":facility.toUpperCase(),severity,message.substring(0,Math.min(message.length(),512))));setChanged();}
    public String aaaUserData(){StringBuilder out=new StringBuilder();aaaUsers.values().forEach(u->out.append(u.username()).append('\t').append(u.privilege()).append('\t').append(u.enabled()).append('\n'));return out.toString();}
    public String aaaAccountingData(){StringBuilder out=new StringBuilder();for(int i=Math.max(0,aaaRecords.size()-100);i<aaaRecords.size();i++){ServerRackAaaRecord r=aaaRecords.get(i);out.append(r.timestamp()).append('\t').append(r.username()).append('\t').append(r.service()).append('\t').append(r.action()).append('\t').append(r.success()).append('\t').append(r.source()).append('\n');}return out.toString();}
    public String manageAaa(String action,String originalUsername,String password,int privilege,boolean enabled,String service,String source){String username=originalUsername.trim().toLowerCase();if(action.equals("CLEAR_ACCOUNTING")){aaaRecords.clear();setChanged();return "AAA accounting records cleared.";}if(action.equals("DELETE")){if(username.equals("admin"))return "The default admin account cannot be deleted.";return aaaUsers.remove(username)==null?"AAA user not found.":changed("AAA user removed.");}if(action.equals("SAVE")){if(!username.matches("[a-z0-9._-]{1,32}"))return "Username must be 1-32 valid characters.";if(password.length()<4||password.length()>64)return "Password must be 4-64 characters.";if(privilege<0||privilege>15)return "Privilege must be 0-15.";aaaUsers.put(username,new ServerRackAaaUser(username,password,privilege,enabled));setChanged();return "AAA user saved.";}boolean success=authenticateAaa(username,password,privilege,service,source);return success?"Authentication and authorization succeeded.":"Authentication or authorization failed.";}
    private boolean authenticateAaa(String username,String password,int requiredPrivilege,String service,String source){ServerRackAaaUser user=aaaUsers.get(username.toLowerCase());boolean success=user!=null&&user.enabled()&&java.util.Objects.equals(user.password(),password)&&user.privilege()>=Math.max(0,requiredPrivilege);if(aaaRecords.size()>=256)aaaRecords.remove(0);aaaRecords.add(new ServerRackAaaRecord(deviceTimeMillis(),username,service==null?"LOGIN":service,"AUTH",success,source==null?"":source));addSyslog(source,"AUTH",success?5:4,"AAA "+(success?"accepted":"rejected")+" user "+username);setChanged();return success;}
    public String radiusClientData(){StringBuilder out=new StringBuilder();radiusClients.values().forEach(c->out.append(c.name()).append('\t').append(c.address()).append('\t').append(c.enabled()).append('\n'));return out.toString();}
    public String radiusEventData(){return String.join("\n",radiusEvents);}
    private Map<String,ServerRackIotDevice> iotDevices(){Map<String,ServerRackIotDevice> devices=new LinkedHashMap<>();ListTag list=getPersistentData().getList("IotDevices",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){ServerRackIotDevice device=ServerRackIotDevice.load(list.getCompound(i));devices.put(device.id(),device);}return devices;}
    private void saveIotDevices(Map<String,ServerRackIotDevice> devices){ListTag list=new ListTag();devices.values().forEach(d->list.add(d.save()));getPersistentData().put("IotDevices",list);setChanged();}
    public String iotDeviceData(){StringBuilder out=new StringBuilder();iotDevices().values().forEach(d->out.append(d.id()).append('\t').append(d.name()).append('\t').append(d.type()).append('\t').append(d.online()).append('\t').append(d.state()).append('\t').append(d.telemetry()).append('\t').append(d.lastSeen()).append('\n'));return out.toString();}
    public String manageIot(String action,String id,String name,String type,String value){Map<String,ServerRackIotDevice> devices=iotDevices();if(action.equals("QUERY"))return "IoT devices refreshed.";id=id.trim().toLowerCase();if(action.equals("CLEAR_OFFLINE")){devices.values().removeIf(d->!d.online());saveIotDevices(devices);return "Offline IoT devices removed.";}if(id.isEmpty()||!id.matches("[a-z0-9._-]{1,32}"))return "Device ID must use 1-32 letters, numbers, dots, dashes, or underscores.";if(action.equals("REMOVE")){if(devices.remove(id)==null)return "IoT device not found.";saveIotDevices(devices);return "IoT device removed.";}ServerRackIotDevice current=devices.get(id);if(action.equals("REGISTER")){if(name.isBlank()||name.length()>32)return "Device name must be 1-32 characters.";if(type.isBlank()||type.length()>24)return "Device type must be 1-24 characters.";devices.put(id,new ServerRackIotDevice(id,name.trim(),type.trim(),true,"ON","Awaiting telemetry",deviceTimeMillis()));saveIotDevices(devices);return "IoT device registered.";}if(current==null)return "IoT device not found.";if(action.equals("CONTROL")){String state=value.trim().toUpperCase();if(state.isEmpty()||state.length()>32)return "Control state must be 1-32 characters.";devices.put(id,new ServerRackIotDevice(id,current.name(),current.type(),true,state,current.telemetry(),deviceTimeMillis()));saveIotDevices(devices);return "Control command applied: "+state;}if(action.equals("TELEMETRY")){String telemetry=value.trim();if(telemetry.isEmpty()||telemetry.length()>128)return "Telemetry must be 1-128 characters.";devices.put(id,new ServerRackIotDevice(id,current.name(),current.type(),true,current.state(),telemetry,deviceTimeMillis()));saveIotDevices(devices);return "Telemetry updated.";}if(action.equals("OFFLINE")){devices.put(id,new ServerRackIotDevice(id,current.name(),current.type(),false,current.state(),current.telemetry(),current.lastSeen()));saveIotDevices(devices);return "IoT device marked offline.";}return "IoT devices refreshed.";}
    private Map<String,ServerRackVirtualMachine> virtualMachines(){Map<String,ServerRackVirtualMachine> machines=new LinkedHashMap<>();ListTag list=getPersistentData().getList("VirtualMachines",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){ServerRackVirtualMachine vm=ServerRackVirtualMachine.load(list.getCompound(i));machines.put(vm.name().toLowerCase(),vm);}return machines;}
    private void saveVirtualMachines(Map<String,ServerRackVirtualMachine> machines){ListTag list=new ListTag();machines.values().forEach(vm->list.add(vm.save()));getPersistentData().put("VirtualMachines",list);setChanged();}
    public String virtualMachineData(){StringBuilder out=new StringBuilder();virtualMachines().values().forEach(vm->out.append(vm.name()).append('\t').append(vm.operatingSystem()).append('\t').append(vm.cpuCores()).append('\t').append(vm.memoryMb()).append('\t').append(vm.storageGb()).append('\t').append(vm.state()).append('\t').append(vm.console().replace('\t',' ').replace('\n',' ')).append('\n'));return out.toString();}
    public String manageVirtualMachine(String action,String name,String os,int cpu,int memory,int storage){Map<String,ServerRackVirtualMachine> machines=virtualMachines();if(action.equals("QUERY"))return "Virtual machines refreshed.";String key=name.trim().toLowerCase();if(!key.matches("[a-z0-9._-]{1,32}"))return "VM name must use 1-32 valid characters.";ServerRackVirtualMachine current=machines.get(key);if(action.equals("DELETE")){if(current==null)return "Virtual machine not found.";if(!current.state().equals("STOPPED"))return "Stop the virtual machine before deleting it.";machines.remove(key);saveVirtualMachines(machines);return "Virtual machine deleted.";}if(action.equals("CREATE")){if(os.isBlank()||os.length()>32)return "Operating system must be 1-32 characters.";if(cpu<1||cpu>16)return "CPU cores must be 1-16.";if(memory<256||memory>32768)return "Memory must be 256-32768 MB.";if(storage<1||storage>1024)return "Storage must be 1-1024 GB.";int usedCpu=machines.values().stream().filter(v->!v.name().equalsIgnoreCase(name)).mapToInt(ServerRackVirtualMachine::cpuCores).sum();int usedMemory=machines.values().stream().filter(v->!v.name().equalsIgnoreCase(name)).mapToInt(ServerRackVirtualMachine::memoryMb).sum();int usedStorage=machines.values().stream().filter(v->!v.name().equalsIgnoreCase(name)).mapToInt(ServerRackVirtualMachine::storageGb).sum();if(usedCpu+cpu>16||usedMemory+memory>32768||usedStorage+storage>1024)return "Rack capacity exceeded (16 CPU / 32768 MB / 1024 GB).";String state=current==null?"STOPPED":current.state();machines.put(key,new ServerRackVirtualMachine(name.trim(),os.trim(),cpu,memory,storage,state,deviceTimeMillis(),"Configuration saved."));saveVirtualMachines(machines);return current==null?"Virtual machine created.":"Virtual machine updated.";}if(current==null)return "Virtual machine not found.";String state=switch(action){case "START"->"RUNNING";case "STOP"->"STOPPED";case "RESTART"->"RUNNING";default->current.state();};String console=switch(action){case "START"->"Boot completed: "+current.operatingSystem();case "STOP"->"Guest shutdown completed.";case "RESTART"->"Guest restarted successfully.";default->current.console();};machines.put(key,new ServerRackVirtualMachine(current.name(),current.operatingSystem(),current.cpuCores(),current.memoryMb(),current.storageGb(),state,deviceTimeMillis(),console));saveVirtualMachines(machines);return "VM "+current.name()+" is now "+state+".";}
    public boolean prpEnabled(){return getPersistentData().getBoolean("PrpEnabled");}
    public boolean prpLaneAUp(){return !getPersistentData().contains("PrpLaneA")||getPersistentData().getBoolean("PrpLaneA");}
    public boolean prpLaneBUp(){return !getPersistentData().contains("PrpLaneB")||getPersistentData().getBoolean("PrpLaneB");}
    public String prpPeerIp(){return getPersistentData().getString("PrpPeerIp");}
    public String prpStatusData(){return (prpEnabled()?"ENABLED":"DISABLED")+'\t'+(prpLaneAUp()?"UP":"DOWN")+'\t'+(prpLaneBUp()?"UP":"DOWN")+'\t'+prpPeerIp()+'\t'+getPersistentData().getLong("PrpTxFrames")+'\t'+getPersistentData().getLong("PrpDuplicatesDiscarded");}
    public String managePrp(String action,boolean enabled,boolean laneA,boolean laneB,String peerIp){if(action.equals("QUERY"))return "PRP supervision refreshed.";if(action.equals("CLEAR_COUNTERS")){getPersistentData().putLong("PrpTxFrames",0);getPersistentData().putLong("PrpDuplicatesDiscarded",0);setChanged();return "PRP counters cleared.";}peerIp=peerIp.trim();if(enabled&&!validIp(peerIp))return "A valid redundancy peer IPv4 address is required.";if(peerIp.equals(ipAddress))return "The PRP peer must be another rack.";getPersistentData().putBoolean("PrpEnabled",enabled);getPersistentData().putBoolean("PrpLaneA",laneA);getPersistentData().putBoolean("PrpLaneB",laneB);getPersistentData().putString("PrpPeerIp",peerIp);setChanged();return enabled?(laneA||laneB?"PRP configuration saved.":"Warning: both PRP lanes are down."):"PRP disabled.";}
    public String manageRadius(String action,String name,String address,String secret,boolean enabled,String username,String password,int privilege){name=name.trim();address=address.trim();if(action.equals("CLEAR")){radiusEvents.clear();setChanged();return "RADIUS events cleared.";}if(action.equals("DELETE"))return radiusClients.remove(address)==null?"RADIUS client not found.":changed("RADIUS client removed.");if(action.equals("SAVE")){if(name.isEmpty()||name.length()>32)return "Client name must be 1-32 characters.";if(!validIp(address))return "Invalid NAS IPv4 address.";if(secret.length()<6||secret.length()>64)return "Shared secret must be 6-64 characters.";radiusClients.put(address,new ServerRackRadiusClient(name,address,secret,enabled));setChanged();return "RADIUS client saved.";}boolean accepted=radiusAuthenticate(address,secret,username,password,privilege,"EAP");return accepted?"RADIUS Access-Accept.":"RADIUS Access-Reject.";}
    private boolean radiusAuthenticate(String address,String secret,String username,String password,int privilege,String service){ServerRackRadiusClient client=radiusClients.get(address);boolean clientOk=client!=null&&client.enabled()&&java.util.Objects.equals(client.sharedSecret(),secret);boolean accepted=clientOk&&authenticateAaa(username,password,privilege,service,address);if(radiusEvents.size()>=128)radiusEvents.remove(0);radiusEvents.add(deviceTimeMillis()+"\t"+address+"\t"+username+"\t"+(accepted?"Access-Accept":"Access-Reject"));addSyslog(address,"AUTH",accepted?5:4,"RADIUS "+(accepted?"accepted":"rejected")+" user "+username);setChanged();return accepted;}
    public boolean serviceEnabled(ServerRackService service){return switch(service){case HTTP->httpEnabled;case DHCP->dhcpEnabled;case DNS->dnsEnabled;case EMAIL->mailEnabled;default->enabledServices.contains(service);};}
    public long serviceMask(){long mask=0;for(ServerRackService service:ServerRackService.values())if(serviceEnabled(service))mask|=1L<<service.ordinal();return mask;}
    public void applyServiceMask(long mask){enabledServices.clear();for(ServerRackService service:ServerRackService.values())if((mask&(1L<<service.ordinal()))!=0)enabledServices.add(service);httpEnabled=enabledServices.contains(ServerRackService.HTTP);dhcpEnabled=enabledServices.contains(ServerRackService.DHCP);dnsEnabled=enabledServices.contains(ServerRackService.DNS);mailEnabled=enabledServices.contains(ServerRackService.EMAIL);setChanged();}
    private long rawDeviceTimeMillis(){long elapsed=System.currentTimeMillis()-lastNtpSyncMillis;long drift=lastNtpSyncMillis==0?0:elapsed*clockDriftPpm/1_000_000L;return System.currentTimeMillis()+clockOffsetMillis+drift;}
    public long deviceTimeMillis(){if(ptpMode==ServerRackPtpMode.CLIENT){ServerRackBlockEntity m=ServerRackDirectory.nearestPtpGrandmaster(this);if(m!=null){clockOffsetMillis=m.rawDeviceTimeMillis()-System.currentTimeMillis();lastPtpSyncMillis=System.currentTimeMillis();clockSyncStatus="PTP synchronized: "+m.displayName();setChanged();return rawDeviceTimeMillis();}}if(ntpClientEnabled&&(lastNtpSyncMillis==0||System.currentTimeMillis()-lastNtpSyncMillis>=ntpPollSeconds*1000L)){ServerRackBlockEntity source=ServerRackDirectory.ntpSource(this,ntpSourceIp);if(source!=null){clockOffsetMillis=source.rawDeviceTimeMillis()-System.currentTimeMillis();lastNtpSyncMillis=System.currentTimeMillis();clockSyncStatus="NTP synchronized: "+source.displayName()+" (stratum "+source.ntpStratum()+")";setChanged();}}return rawDeviceTimeMillis();}
    public void setProfile(ServerRackProfile value){profile=value==null?ServerRackProfile.INTRA_DATA_CENTER:value;setChanged();}
    public void setWiredBackboneConnected(boolean connected){wiredBackboneConnected=connected;setChanged();}
    public void setOwner(net.minecraft.server.level.ServerPlayer player){getPersistentData().putUUID("RackOwner",player.getUUID());getPersistentData().putString("RackOwnerName",player.getGameProfile().getName());setChanged();}
    public String ownerName(){String value=getPersistentData().getString("RackOwnerName");return value.isBlank()?"Unclaimed":value;}
    public boolean canConfigure(net.minecraft.server.level.ServerPlayer player){return !getPersistentData().hasUUID("RackOwner")||getPersistentData().getUUID("RackOwner").equals(player.getUUID())||player.hasPermissions(2);}
    public Set<BlockPos> cableLinks(){return Set.copyOf(cableLinks);}
    public boolean connectCable(BlockPos target){if(target==null||target.equals(worldPosition))return false;boolean added=cableLinks.add(target.immutable());wiredBackboneConnected=!cableLinks.isEmpty();if(added)setChanged();return added;}
    public boolean disconnectCable(BlockPos target){boolean removed=cableLinks.remove(target);wiredBackboneConnected=!cableLinks.isEmpty();if(removed)setChanged();return removed;}
    public void clearCableLinks(){cableLinks.clear();wiredBackboneConnected=false;setChanged();}
    public boolean hasCableLinkTo(BlockPos target){return cableLinks.contains(target);}

    public String requestDynamicIp(String clientId, boolean ipv6) {
        ServerRackDhcpLease lease = allocateLease(clientId, ipv6);
        if (lease != null) {
            setChanged();
            return lease.address();
        }
        return null;
    }

    // W1.20 REAL HOST METHODS
    private void w120ConfigureHost(){
        w120Host.configure(displayName,ipAddress,subnetMask,gatewayIp,macAddress);
    }

    private boolean w120EmitFrames(java.util.List<OSINetworkPacket> frames){
        if(frames==null||frames.isEmpty())return true;
        if(!(level instanceof ServerLevel))return false;
        boolean emitted=false;
        for(BlockPos peerPos:new java.util.ArrayList<>(cableLinks)){
            BlockEntity peer=level.getBlockEntity(peerPos);
            if(peer instanceof NetworkSwitchBlockEntity sw
                    && sw.getConnectedDevices().contains(worldPosition)){
                for(OSINetworkPacket frame:frames){
                    if(frame==null)continue;
                    sw.receiveWiredPacket(
                            OSINetworkPacket.deserializeNBT(frame.serializeNBT().copy()),
                            worldPosition
                    );
                }
                emitted=true;
            }
        }
        if(!emitted)w120Host.noteNoPhysicalLink(frames.size());
        return emitted;
    }

    private String w120Ping(String input,ServerLevel serverLevel){
        String[] args=input==null?new String[0]:input.trim().split("\\s+");
        String target=args.length==0?"":args[0];
        if(!validIp(target)){
            String resolved=ServerRackDirectory.resolve(serverLevel,target.toLowerCase());
            if(resolved==null)return "Ping could not resolve host "+target+".";
            target=resolved;
        }
        w120ConfigureHost();
        java.util.List<OSINetworkPacket> frames=w120Host.ping(target,System.currentTimeMillis());
        boolean emitted=w120EmitFrames(frames);
        return "Pinging "+target+" with 32 bytes of data:\n"
                +(emitted?"W1.20 real Ethernet/IP probe injected. ARP/ICMP processing is asynchronous."
                :"Request could not enter the wired data plane: no operational switch cable link.")
                +"\n"+w120Host.status(System.currentTimeMillis());
    }

    public String w120HostStatus(){
        w120ConfigureHost();
        return w120Host.status(System.currentTimeMillis())+" | cables="+cableLinkData();
    }

    public void w120Tick(){
        if(!(level instanceof ServerLevel serverLevel))return;
        long gameTime=serverLevel.getGameTime();
        if(w120LastTickGameTime!=Long.MIN_VALUE&&gameTime-w120LastTickGameTime<20L)return;
        w120LastTickGameTime=gameTime;
        w120ConfigureHost();
        w120EmitFrames(w120Host.tick(System.currentTimeMillis()));
    }

    private boolean w120AcceptWiredFrame(
            OSINetworkPacket packet
    ) {
        if (packet == null) {
            return false;
        }

        w120ConfigureHost();

        boolean arp =
                "ARP".equalsIgnoreCase(
                        packet.applicationProtocol
                );

        boolean addressedToIp =
                ipAddress.equals(
                        packet.targetIp
                );

        boolean addressedToMac =
                macAddress != null
                        && !macAddress.isBlank()
                        && macAddress.equalsIgnoreCase(
                        packet.targetMac
                );

        boolean broadcast =
                "FF:FF:FF:FF:FF:FF".equalsIgnoreCase(
                        packet.targetMac
                );

        boolean dhcpClientResponse =
                "DHCP".equalsIgnoreCase(
                        packet.applicationProtocol
                )
                        && packet.isResponse;

        boolean dnsClientResponse =
                "DNS".equalsIgnoreCase(
                        packet.applicationProtocol
                )
                        && packet.isResponse;

        boolean w117Echo =
                packet.payload != null
                        && (
                        packet.payload.getBoolean(
                                "w117_echo_request"
                        )
                                || packet.payload.getBoolean(
                                "w117_echo_reply"
                        )
                );

        boolean ownedByW120Host =
                arp
                        || dhcpClientResponse
                        || dnsClientResponse
                        || w117Echo;

        if (!ownedByW120Host) {
            return false;
        }

        if (!arp
                && !addressedToIp
                && !addressedToMac
                && !broadcast) {
            return false;
        }

        w120EmitFrames(
                w120Host.receive(
                        packet,
                        System.currentTimeMillis()
                )
        );

        return true;
    }
    public String cableLinkData(){if(cableLinks.isEmpty())return "No physical links";return cableLinks.stream().map(p->p.getX()+", "+p.getY()+", "+p.getZ()).collect(java.util.stream.Collectors.joining("; "));}
    public double configuredMaximumRangeBlocks(){return profile.maximumRangeBlocks();}
    public double effectiveMaximumRangeBlocks(){return profile.wiredBeyondCampus()&&!wiredBackboneConnected?10_000.0:profile.maximumRangeBlocks();}
    @Override public double maximumReceptionRangeBlocks(){return effectiveMaximumRangeBlocks();}
    @Override protected void transmitPacket(OSINetworkPacket packet){
        if(packet != null
                && packet.isResponse
                && packet.sessionId != null
                && !packet.sessionId.isBlank()
                && !"TCP".equalsIgnoreCase(packet.applicationProtocol)){
            super.transmitPacket(packet);
            return;
        }
        if(level instanceof ServerLevel serverLevel&&!packet.targetIp.isBlank()&&!packet.targetIp.equals("255.255.255.255")){
            ServerRackBlockEntity target=ServerRackDirectory.byIp(serverLevel,packet.targetIp);
            if(target!=null){
                if(prpEnabled()&&target.prpEnabled()&&ServerRackDirectory.arePhysicallyLinked(this,target)&&(prpPeerIp().isBlank()||prpPeerIp().equals(target.ipAddress()))){
                    if(!prpLaneAUp()&&!prpLaneBUp()){addSyslog(displayName,"PRP",3,"Both PRP lanes are down; packet dropped");return;}
                    long sequence=getPersistentData().getLong("PrpSequence")+1;getPersistentData().putLong("PrpSequence",sequence);getPersistentData().putLong("PrpTxFrames",getPersistentData().getLong("PrpTxFrames")+1);setChanged();
                    if(prpLaneAUp())target.receivePrpPacket(copyPrpPacket(packet,sequence,"A"));
                    if(prpLaneBUp())target.receivePrpPacket(copyPrpPacket(packet,sequence,"B"));
                    return;
                }
                double distance=Math.sqrt(worldPosition.distSqr(target.getBlockPos()));
                double maximum=Math.min(configuredMaximumRangeBlocks(),target.configuredMaximumRangeBlocks());
                if(distance>maximum){addSyslog(displayName,"NETWORK",4,"Dropped packet to "+packet.targetIp+": profile range exceeded");return;}
                if(distance>10_000.0){
                    if(ServerRackDirectory.arePhysicallyLinked(this,target)){target.receiveWiredPacket(packet);return;}
                    addSyslog(displayName,"NETWORK",4,"Dropped packet to "+packet.targetIp+": physical cable required beyond 10 km");return;
                }
            }
        }
        if(w120EmitFrames(java.util.List.of(packet))){
            return;
        }
        super.transmitPacket(packet);
    }
    public void receiveWiredPacket(OSINetworkPacket packet){
        if (w119ReceiveFromDistributionSystem(
                packet
        )) {
            return;
        }

        // W1.20 REAL HOST RX
        if (w120AcceptWiredFrame(packet)) {
            return;
        }
processLayer2(packet);}
    private OSINetworkPacket copyPrpPacket(OSINetworkPacket packet,long sequence,String lane){OSINetworkPacket copy=OSINetworkPacket.deserializeNBT(packet.serializeNBT().copy());copy.payload=copy.payload.copy();copy.payload.putLong("_prp_sequence",sequence);copy.payload.putString("_prp_source",ipAddress);copy.payload.putString("_prp_lane",lane);return copy;}
    public void receivePrpPacket(OSINetworkPacket packet){String key=packet.payload.getString("_prp_source")+":"+packet.payload.getLong("_prp_sequence");long now=System.currentTimeMillis();prpSeenFrames.entrySet().removeIf(e->now-e.getValue()>30_000L);if(prpSeenFrames.putIfAbsent(key,now)!=null){getPersistentData().putLong("PrpDuplicatesDiscarded",getPersistentData().getLong("PrpDuplicatesDiscarded")+1);setChanged();return;}processLayer2(packet);}
    public boolean beginInteraction(long gameTime){
        if(lastInteractionTick==gameTime)return false;
        lastInteractionTick=gameTime;
        return true;
    }
    public void openDoor(){doorOpen=true; triggerAnim("rack_controller","open"); setChanged();}
    public void closeDoor(){doorOpen=false; triggerAnim("rack_controller","close"); setChanged();}

    public String applyGuiConfiguration(String name, String ip, String subnet, String gateway, String dns,
                                        String ipv6, int prefix, String gateway6, String dns6, boolean automatic6,
                                        long offsetMillis, String mode, String profileName,
                                        boolean dhcp, boolean http, boolean dnsOn, boolean dhcpOn, boolean mailOn) {
        name = name.trim(); ip = ip.trim(); subnet = subnet.trim(); gateway = gateway.trim(); dns = dns.trim();
        if (name.isEmpty() || name.length() > 32) return "Display name must be 1-32 characters.";
        if (!validIp(ip) || !validIp(subnet) || !validIp(gateway) || !validIp(dns)) return "One or more IPv4 addresses are invalid.";
        if(!validIpv6(ipv6)||!validIpv6(gateway6)||!validIpv6(dns6)||prefix<0||prefix>128)return "One or more IPv6 settings are invalid.";
        displayName=name; ipAddress=ip; subnetMask=subnet; gatewayIp=gateway; dnsServer=dns; usesDhcp=dhcp;
        ipv6Address=ipv6.trim();ipv6PrefixLength=prefix;ipv6Gateway=gateway6.trim();ipv6DnsServer=dns6.trim();ipv6Automatic=automatic6;
        clockOffsetMillis=Math.max(-86400000L,Math.min(86400000L,offsetMillis));ptpMode=ServerRackPtpMode.byName(mode);ptpProfile=ServerRackPtpProfile.byName(profileName);
        httpEnabled=http; dnsEnabled=dnsOn; dhcpEnabled=dhcpOn; mailEnabled=mailOn; setChanged(); return null;
    }
    private static boolean validIp(String value){return IPV4.matcher(value).matches();}
    private static boolean validIpv6(String value){try{return value.contains(":")&&InetAddress.getByName(value) instanceof Inet6Address;}catch(Exception ignored){return false;}}
    public String resolveDomain(String domain){return resolveDns(domain,"A");}
    public String resolveDns(String name,String type){return resolveDns(name,type,0);}
    private String resolveDns(String name,String type,int depth){
        if(depth>8)return null;
        if(!dnsEnabled)return null;String key=type.toUpperCase()+"|"+name.toLowerCase();long now=System.currentTimeMillis();
        if(dnsCacheExpiry.getOrDefault(key,0L)>now)return dnsCache.get(key);
        ServerRackDnsRecord record=detailedDnsRecords.get(key);String result=null;int ttl=300;
        if(record!=null){result=record.detail();ttl=record.ttl();}
        else{ServerRackDnsRecord alias=detailedDnsRecords.get("CNAME|"+name.toLowerCase());if(alias!=null){result=resolveDns(alias.detail(),type,depth+1);ttl=alias.ttl();}}
        if(result!=null){dnsCache.put(key,result);dnsCacheExpiry.put(key,now+ttl*1000L);}return result;
    }
    private void putDefaultDnsRecord(String name,String type,String detail){ServerRackDnsRecord r=new ServerRackDnsRecord(name,type,detail,300);detailedDnsRecords.put(r.key(),r);}
    public String dnsRecordData(){StringBuilder b=new StringBuilder();for(ServerRackDnsRecord r:detailedDnsRecords.values())b.append(r.name()).append('\t').append(r.type()).append('\t').append(r.detail()).append('\t').append(r.ttl()).append('\n');return b.toString();}
    public String manageDnsRecord(String action,String name,String type,String detail,int ttl){
        name=name.trim().toLowerCase();type=type.trim().toUpperCase();detail=detail.trim();
        if(action.equals("CLEAR_CACHE")){dnsCache.clear();dnsCacheExpiry.clear();return changed("DNS cache cleared.");}
        if(name.isEmpty()||name.length()>253||!name.matches("[a-z0-9._-]+"))return "Invalid DNS name.";
        if(!java.util.Set.of("A","AAAA","CNAME","MX","PTR").contains(type))return "Unsupported DNS record type.";
        String key=type+"|"+name;
        if(action.equals("REMOVE")){ServerRackDnsRecord old=detailedDnsRecords.remove(key);if(type.equals("A"))dnsRecords.remove(name);dnsCache.clear();dnsCacheExpiry.clear();return old==null?"DNS record not found.":changed("DNS record removed.");}
        if(ttl<30||ttl>86400)return "TTL must be between 30 and 86400 seconds.";
        if(type.equals("A")&&!validIp(detail))return "A records require a valid IPv4 address.";
        if(type.equals("AAAA")&&!validIpv6(detail))return "AAAA records require a valid IPv6 address.";
        if((type.equals("CNAME")||type.equals("MX")||type.equals("PTR"))&&(detail.isEmpty()||detail.length()>253))return type+" records require a valid target name.";
        if(detailedDnsRecords.size()>=256&&!detailedDnsRecords.containsKey(key))return "DNS record limit reached.";
        detailedDnsRecords.put(key,new ServerRackDnsRecord(name,type,detail,ttl));if(type.equals("A"))dnsRecords.put(name,detail);dnsCache.clear();dnsCacheExpiry.clear();setChanged();return "DNS record saved.";
    }
    public String dhcpData(boolean ipv6){StringBuilder b=new StringBuilder("POOLS\n");for(ServerRackDhcpPool p:dhcpPools.values())if(p.ipv6()==ipv6)b.append(p.name()).append('\t').append(p.start()).append('\t').append(p.end()).append('\t').append(p.prefixOrMask()).append('\t').append(p.gateway()).append('\t').append(p.dns()).append('\t').append(p.leaseSeconds()).append('\t').append(p.exclusions()).append('\n');b.append("LEASES\n");long now=System.currentTimeMillis();for(ServerRackDhcpLease l:dhcpLeases.values())if(l.ipv6()==ipv6&&l.expiresAt()>now)b.append(l.clientId()).append('\t').append(l.address()).append('\t').append(l.pool()).append('\t').append(l.expiresAt()).append('\n');return b.toString();}
    public String manageDhcpPool(String action,String originalName,boolean ipv6,String start,String end,String prefix,String gateway,String dns,int lease,String exclusions){String name=originalName.trim();if(action.equals("CLEAR_LEASES")){dhcpLeases.entrySet().removeIf(e->e.getValue().ipv6()==ipv6);setChanged();return "Active leases cleared.";}if(name.isEmpty()||name.length()>32)return "Pool name must be 1-32 characters.";if(action.equals("REMOVE")){dhcpLeases.entrySet().removeIf(e->e.getValue().pool().equalsIgnoreCase(name));return dhcpPools.remove((ipv6?"6:":"4:")+name.toLowerCase())==null?"Pool not found.":changed("Pool removed.");}if(lease<60||lease>604800)return "Lease time must be 60-604800 seconds.";if(ipv6){if(!validIpv6(start)||!validIpv6(end)||!validIpv6(gateway)||!validIpv6(dns))return "Invalid DHCPv6 address.";try{int p=Integer.parseInt(prefix);if(p<0||p>128)return "IPv6 prefix must be 0-128.";}catch(Exception e){return "IPv6 prefix must be 0-128.";}}else if(!validIp(start)||!validIp(end)||!validIp(prefix)||!validIp(gateway)||!validIp(dns))return "Invalid DHCPv4 pool setting.";dhcpPools.put((ipv6?"6:":"4:")+name.toLowerCase(),new ServerRackDhcpPool(name,ipv6,start,end,prefix,gateway,dns,lease,exclusions));setChanged();return "DHCP pool saved.";}
    public String programSource(){return programSource;}
    public String mailData(){StringBuilder b=new StringBuilder("DOMAIN\t").append(mailDomain).append('\n');for(ServerRackMailAccount a:mailAccounts.values())b.append(a.address()).append('\t').append(a.quota()).append('\t').append(mailboxes.getOrDefault(a.address(),new ListTag()).size()).append('\n');return b.toString();}
    public String manageMailAccount(String action,String originalUser,String domain,String password,int quota){String user=originalUser.trim().toLowerCase();domain=domain.trim().toLowerCase();if(!user.matches("[a-z0-9._-]{1,32}")||!domain.matches("[a-z0-9.-]{1,253}"))return "Invalid mailbox name or domain.";String address=user+"@"+domain;if(action.equals("DELETE")){mailboxes.remove(address);return mailAccounts.remove(address)==null?"Mailbox not found.":changed("Mailbox deleted.");}if(password.length()<4||password.length()>64)return "Password must be 4-64 characters.";if(quota<10||quota>1000)return "Quota must be 10-1000 messages.";mailDomain=domain;mailAccounts.put(address,new ServerRackMailAccount(user,domain,password,quota));setChanged();return "Mailbox saved.";}
    public CompoundTag manageMailClient(String action,String originalAddress,String password,String folder,String messageId,String originalTo,String subject,String body){
        CompoundTag result=new CompoundTag();String address=originalAddress.trim().toLowerCase();String selectedFolder=folder.equalsIgnoreCase("SENT")?"SENT":"INBOX";result.putString("Address",address);result.putString("Folder",selectedFolder);
        if(!mailEnabled){result.putString("Message","Email service is disabled.");return result;}ServerRackMailAccount account=mailAccounts.get(address);if(account==null||!java.util.Objects.equals(account.password(),password)){result.putString("Message","Login failed: invalid mailbox or password.");return result;}result.putBoolean("Authenticated",true);
        String boxKey=selectedFolder.equals("SENT")?"sent:"+address:address;ListTag box=mailboxes.computeIfAbsent(boxKey,k->new ListTag());
        if(action.equalsIgnoreCase("SEND")){String to=originalTo.trim().toLowerCase();ServerRackMailAccount recipient=mailAccounts.get(to);if(to.equals(address)){recipient=account;}if(recipient==null){result.putString("Message","Send failed: recipient mailbox does not exist.");}else{ListTag inbox=mailboxes.computeIfAbsent(to,k->new ListTag());if(inbox.size()>=recipient.quota()){result.putString("Message","Send failed: recipient mailbox is full.");}else{CompoundTag message=new CompoundTag();message.putString("id",java.util.UUID.randomUUID().toString());message.putString("from",address);message.putString("to",to);message.putString("subject",subject.isBlank()?"(No subject)":subject.substring(0,Math.min(128,subject.length())));message.putString("body",body.substring(0,Math.min(8192,body.length())));message.putLong("sentAt",deviceTimeMillis());message.putBoolean("read",false);inbox.add(message.copy());CompoundTag sent=message.copy();sent.putBoolean("read",true);mailboxes.computeIfAbsent("sent:"+address,k->new ListTag()).add(sent);selectedFolder="SENT";boxKey="sent:"+address;box=mailboxes.get(boxKey);result.putString("Folder",selectedFolder);result.putString("Message","Message delivered to "+to+".");setChanged();}}}
        else if(action.equalsIgnoreCase("DELETE")){boolean removed=box.removeIf(tag->((CompoundTag)tag).getString("id").equals(messageId));result.putString("Message",removed?"Message deleted.":"Message not found.");if(removed)setChanged();}
        else if(action.equalsIgnoreCase("OPEN")){for(int i=0;i<box.size();i++){CompoundTag message=box.getCompound(i);if(message.getString("id").equals(messageId)){message.putBoolean("read",true);result.putString("Id",message.getString("id"));result.putString("From",message.getString("from"));result.putString("To",message.getString("to"));result.putString("Subject",message.getString("subject"));result.putString("Body",message.getString("body"));result.putLong("SentAt",message.getLong("sentAt"));result.putString("Message","Message opened.");setChanged();break;}}if(!result.contains("Id"))result.putString("Message","Message not found.");}
        else result.putString("Message",action.equalsIgnoreCase("LOGIN")?"Signed in as "+address+".":selectedFolder+" refreshed.");
        StringBuilder data=new StringBuilder();for(int i=box.size()-1;i>=0;i--){CompoundTag message=box.getCompound(i);data.append(message.getString("id")).append('\t').append(message.getBoolean("read")).append('\t').append(message.getString("from")).append('\t').append(message.getString("to")).append('\t').append(message.getString("subject").replace('\t',' ')).append('\t').append(message.getLong("sentAt")).append('\n');}result.putString("Data",data.toString());return result;
    }
    public String hostedFileData(){StringBuilder b=new StringBuilder();hostedFiles.values().forEach(f->b.append(f.name()).append('\t').append(f.readable()).append('\t').append(f.writable()).append('\t').append(f.content().length()).append('\n'));return b.toString();}
    private Map<String,ServerRackHostedFile> desktopTextFiles(){Map<String,ServerRackHostedFile> files=new LinkedHashMap<>();ListTag list=getPersistentData().getList("DesktopTextFiles",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){ServerRackHostedFile file=ServerRackHostedFile.load(list.getCompound(i));files.put(file.name(),file);}return files;}
    private void saveDesktopTextFiles(Map<String,ServerRackHostedFile> files){ListTag list=new ListTag();files.values().forEach(file->list.add(file.save()));getPersistentData().put("DesktopTextFiles",list);setChanged();}
    public String desktopTextFileData(){StringBuilder data=new StringBuilder();desktopTextFiles().values().forEach(file->data.append(file.name()).append('\t').append(file.content().length()).append('\n'));return data.toString();}
    public CompoundTag manageDesktopTextFile(String action,String originalName,String content){
        CompoundTag result=new CompoundTag();Map<String,ServerRackHostedFile> files=desktopTextFiles();String name=originalName.trim().toLowerCase();result.putString("Name",name);
        if(action.equals("QUERY")){result.putString("Message","Documents refreshed.");}
        else if(action.equals("NEW")){result.putString("Name","untitled.txt");result.putString("Content","");result.putString("Message","New document ready.");}
        else if(name.isEmpty()||name.length()>64||name.contains("..")||name.contains("/")||!name.matches("[a-z0-9._-]+")){result.putString("Message","File name must use 1-64 letters, numbers, dots, dashes, or underscores.");}
        else if(action.equals("OPEN")){ServerRackHostedFile file=files.get(name);if(file==null)result.putString("Message","Document not found.");else{result.putString("Content",file.content());result.putString("Message","Document opened.");}}
        else if(action.equals("DELETE")){if(files.remove(name)==null)result.putString("Message","Document not found.");else{saveDesktopTextFiles(files);result.putString("Name","");result.putString("Message","Document deleted.");}}
        else if(action.equals("SAVE")){if(content.length()>32768)result.putString("Message","Document exceeds the 32 KB limit.");else if(files.size()>=128&&!files.containsKey(name))result.putString("Message","Document limit reached.");else{files.put(name,new ServerRackHostedFile(name,content,true,true));saveDesktopTextFiles(files);result.putString("Content",content);result.putString("Message","Document saved.");}}
        else result.putString("Message","Unknown document action.");
        result.putString("Files",desktopTextFileData());return result;
    }
    public ServerRackHostedFile hostedFile(String originalName){return hostedFiles.get(originalName.trim().toLowerCase());}
    public boolean httpsEnabled(){return httpsEnabled;}
    public int httpPort(){return httpPort;}
    public int httpsPort(){return httpsPort;}
    public String configureWebServices(boolean https,int http,int securePort){if(http<1||http>65535||securePort<1||securePort>65535)return "Ports must be between 1 and 65535.";if(http==securePort)return "HTTP and HTTPS must use different ports.";httpsEnabled=https;httpPort=http;httpsPort=securePort;setChanged();return "Web service settings saved.";}
    public String manageHostedFile(String action,String protocol,String originalName,String content,boolean readable,boolean writable){String name=originalName.trim().toLowerCase();if(name.isEmpty()||name.length()>64||name.contains("..")||name.startsWith("/")||!name.matches("[a-z0-9._/-]+"))return "Invalid file name.";if(content.length()>32768)return "File exceeds 32 KB.";if(action.equals("DELETE"))return hostedFiles.remove(name)==null?"File not found.":changed("File deleted.");ServerRackHostedFile old=hostedFiles.get(name);if(old!=null&&!old.writable())return "File is read-only.";if(hostedFiles.size()>=128&&old==null)return "File limit reached.";hostedFiles.put(name,new ServerRackHostedFile(name,content,readable,writable));if(name.equals("index.html"))indexHtml=content;setChanged();return "File saved for "+protocol+".";}
    public String manageFileUser(String action,String originalUsername,String password){String username=originalUsername.trim().toLowerCase();if(username.isEmpty()||username.length()>24||!username.matches("[a-z0-9._-]+"))return "Invalid username.";if(action.equals("DELETE"))return fileUsers.remove(username)==null?"User not found.":changed("User deleted.");if(password.length()<4||password.length()>64)return "Password must be 4-64 characters.";fileUsers.put(username,password);setChanged();return "FTP user saved.";}
    public String fileUserData(){StringBuilder data=new StringBuilder();fileUsers.keySet().forEach(user->data.append(user).append('\n'));return data.toString();}
    public int ftpPort(){return ftpPort;}
    public int tftpPort(){return tftpPort;}
    public String configureTransferService(String protocol,int port){if(port<1||port>65535)return "Port must be 1-65535.";if(protocol.equalsIgnoreCase("FTP"))ftpPort=port;else if(protocol.equalsIgnoreCase("TFTP"))tftpPort=port;else return "Unknown transfer protocol.";setChanged();return protocol.toUpperCase()+" configuration saved.";}
    private ServerRackDhcpLease allocateLease(String client,boolean ipv6){long now=System.currentTimeMillis();dhcpLeases.entrySet().removeIf(e->e.getValue().expiresAt()<=now);String key=(ipv6?"6:":"4:")+client;ServerRackDhcpLease existing=dhcpLeases.get(key);if(existing!=null)return existing;for(ServerRackDhcpPool p:dhcpPools.values()){if(p.ipv6()!=ipv6)continue;if(ipv6){for(int i=0;i<4096;i++){String address=p.start().replaceAll("[0-9a-fA-F]+$",Integer.toHexString(0x100+i));if(addressExcluded(address,p.exclusions())||leaseUses(address))continue;ServerRackDhcpLease l=new ServerRackDhcpLease(client,address,p.name(),true,now+p.leaseSeconds()*1000L);dhcpLeases.put(key,l);return l;}}else{long a=ipv4Number(p.start()),z=ipv4Number(p.end());for(long n=a;n<=z&&n-a<65536;n++){String address=ipv4Text(n);if(addressExcluded(address,p.exclusions())||leaseUses(address))continue;ServerRackDhcpLease l=new ServerRackDhcpLease(client,address,p.name(),false,now+p.leaseSeconds()*1000L);dhcpLeases.put(key,l);return l;}}}return null;}
    private boolean leaseUses(String address){return dhcpLeases.values().stream().anyMatch(l->l.address().equalsIgnoreCase(address));}
    private static boolean addressExcluded(String address,String exclusions){for(String e:exclusions.split(","))if(address.equalsIgnoreCase(e.trim()))return true;return false;}
    private static long ipv4Number(String ip){String[] p=ip.split("\\.");long n=0;for(String s:p)n=(n<<8)|Integer.parseInt(s);return n;}
    private static String ipv4Text(long n){return ((n>>24)&255)+"."+((n>>16)&255)+"."+((n>>8)&255)+"."+(n&255);}
    public String programOutput(){return programOutput;}
    public String runProgram(String source, ServerLevel level){
        if(source.length()>16384)return "ERROR: Program source exceeds 16 KB.";
        programSource=source;
        programOutput=ServerRackScriptEngine.execute(this,level,source);
        if(programOutput.length()>16384)programOutput=programOutput.substring(0,16384)+"\nOutput truncated.";
        setChanged();
        return programOutput;
    }
    public String saveProgram(String source){
        if(source.length()>16384)return "ERROR: Program source exceeds 16 KB.";
        programSource=source;setChanged();return "Program saved.";
    }
    String executeScriptCommand(String command,ServerLevel level){
        String lower=command.toLowerCase();
        if(lower.startsWith("hostname ")){String value=command.substring(9).trim();if(value.isEmpty()||value.length()>32)return "ERROR: Hostname must be 1-32 characters.";displayName=value;setChanged();return "OK: Hostname set to "+value;}
        if(lower.startsWith("ip address "))return setScriptIp("IP address",command.substring(11).trim(),v->ipAddress=v);
        if(lower.startsWith("subnet "))return setScriptIp("Subnet mask",command.substring(7).trim(),v->subnetMask=v);
        if(lower.startsWith("gateway "))return setScriptIp("Default gateway",command.substring(8).trim(),v->gatewayIp=v);
        if(lower.startsWith("dns server "))return setScriptIp("DNS server",command.substring(11).trim(),v->dnsServer=v);
        if(lower.startsWith("addressing ")){String mode=lower.substring(11).trim();if(!mode.equals("static")&&!mode.equals("dhcp"))return "ERROR: Use addressing static or addressing dhcp.";usesDhcp=mode.equals("dhcp");setChanged();return "OK: Addressing mode is "+mode.toUpperCase();}
        if(lower.startsWith("service "))return scriptService(lower.substring(8).trim());
        if(lower.startsWith("dns add ")){String[] p=command.substring(8).trim().split("\\s+",2);if(p.length!=2||!validIp(p[1]))return "ERROR: Use dns add <domain> <IPv4>.";if(dnsRecords.size()>=128&&!dnsRecords.containsKey(p[0].toLowerCase()))return "ERROR: DNS record limit reached.";dnsRecords.put(p[0].toLowerCase(),p[1]);setChanged();return "OK: DNS record added.";}
        if(lower.startsWith("dns remove ")){String domain=command.substring(11).trim().toLowerCase();return dnsRecords.remove(domain)==null?"ERROR: DNS record not found.":changed("OK: DNS record removed.");}
        if(lower.startsWith("http index ")){String html=command.substring(11);if(html.length()>32768)return "ERROR: HTML exceeds 32 KB.";indexHtml=html;setChanged();return "OK: index.html updated.";}
        if(lower.equals("show config"))return ipConfiguration();
        if(lower.equals("show services"))return "HTTP: "+onOff(httpEnabled)+"\nDNS: "+onOff(dnsEnabled)+"\nDHCP: "+onOff(dhcpEnabled)+"\nSMTP: "+onOff(mailEnabled);
        if(lower.equals("show dns")){if(dnsRecords.isEmpty())return "No DNS records.";StringBuilder b=new StringBuilder();dnsRecords.forEach((d,i)->b.append(d).append(" -> ").append(i).append('\n'));return b.toString();}
        if(lower.startsWith("ping "))return ping(command.substring(5).trim(),level);
        if(lower.startsWith("nslookup "))return dnsLookup(command.substring(9).trim(),level);
        if(lower.equals("help"))return "hostname, ip address, subnet, gateway, dns server, addressing, service, dns add/remove, http index, show config/services/dns, ping, nslookup";
        return "ERROR: Unknown command: "+command;
    }
    private interface StringSetter{void set(String value);}
    private String setScriptIp(String label,String value,StringSetter setter){if(!validIp(value))return "ERROR: Invalid IPv4 value for "+label+".";setter.set(value);setChanged();return "OK: "+label+" set to "+value;}
    private String scriptService(String args){String[] p=args.split("\\s+");if(p.length!=2||(!p[1].equals("on")&&!p[1].equals("off")))return "ERROR: Use service <http|dns|dhcp|smtp> <on|off>.";boolean value=p[1].equals("on");switch(p[0]){case "http"->httpEnabled=value;case "dns"->dnsEnabled=value;case "dhcp"->dhcpEnabled=value;case "smtp","mail"->mailEnabled=value;default->{return "ERROR: Unknown service: "+p[0];}}setChanged();return "OK: "+p[0].toUpperCase()+" is "+onOff(value);}
    private String changed(String result){setChanged();return result;}
    private static String onOff(boolean value){return value?"ON":"OFF";}

    public String executeDesktopTool(String tool,String input,ServerLevel level){
        tool=tool.trim();input=input.trim();
        if(tool.equalsIgnoreCase("IP Configuration"))return ipConfiguration();
        if(tool.equalsIgnoreCase("Ping"))return ping(input,level);
        if(tool.equalsIgnoreCase("DNS Lookup"))return dnsLookup(input,level);
        if(tool.equalsIgnoreCase("Web Browser"))return browse(input,level);
        if(tool.equalsIgnoreCase("Email"))return mailbox(input);
        if(tool.equalsIgnoreCase("FTP Client"))return desktopTransfer(input,level,true);
        if(tool.equalsIgnoreCase("TFTP Client"))return desktopTransfer(input,level,false);
        if(tool.equalsIgnoreCase("Terminal")||tool.equalsIgnoreCase("Command Prompt"))return command(input,level);
        if(tool.equalsIgnoreCase("Text Editor"))return "Use the Text Editor window to create, open, edit, and save documents.";
        return "Unknown desktop tool: "+tool;
    }
    private String desktopTransfer(String input,ServerLevel level,boolean ftp){String[] p=input.split("\\s+",ftp?6:4);int minimum=ftp?5:3;if(p.length<minimum)return ftp?"Usage: <server-ip> <LIST|GET|PUT> <file> <username> <password> [content]":"Usage: <server-ip> <LIST|GET|PUT> <file> [content]";String target=p[0],action=p[1].toUpperCase(),filename=p[2].toLowerCase();if(!validIp(target))return "Invalid server IPv4 address.";ServerRackBlockEntity rack=ServerRackDirectory.byIp(level,target);if(rack==null)return "Transfer failed: server is unavailable.";ServerRackService service=ftp?ServerRackService.FTP:ServerRackService.TFTP;if(!rack.serviceEnabled(service))return service.displayName()+" service is disabled.";double distance=Math.sqrt(getBlockPos().distSqr(rack.getBlockPos()));double allowed=Math.min(effectiveMaximumRangeBlocks(),rack.effectiveMaximumRangeBlocks());if(distance>allowed)return "Transfer timed out: server is outside the allowed network range.";if(ftp&&!java.util.Objects.equals(rack.fileUsers.get(p[3].toLowerCase()),p[4]))return "FTP authentication failed.";if(action.equals("LIST"))return rack.hostedFileData().isBlank()?"No hosted files.":rack.hostedFileData().replace('\t',' ');if(action.equals("GET")){ServerRackHostedFile file=rack.hostedFiles.get(filename);return file==null||!file.readable()?"File not found or is not readable.":"Downloaded "+filename+" ("+file.content().length()+" bytes)\n"+file.content();}if(action.equals("PUT")){String content=ftp?(p.length>=6?p[5]:""):(p.length>=4?p[3]:"");return rack.manageHostedFile("SAVE",service.displayName(),filename,content,true,true);}return "Action must be LIST, GET, or PUT.";}
    public CompoundTag manageDesktopFtp(String action,String serverIp,String username,String password,String remoteName,String localName,ServerLevel level){
        CompoundTag result=new CompoundTag();Map<String,ServerRackHostedFile> local=desktopTextFiles();result.putString("LocalFiles",desktopTextFileData());action=action.trim().toUpperCase();serverIp=serverIp.trim();username=username.trim().toLowerCase();remoteName=remoteName.trim().toLowerCase();localName=localName.trim();
        ServerRackBlockEntity remote=validIp(serverIp)?ServerRackDirectory.byIp(level,serverIp):null;
        if(remote==null){result.putString("Message","Connection failed: FTP server is unavailable.");return result;}
        if(!remote.serviceEnabled(ServerRackService.FTP)){result.putString("Message","Connection refused: FTP service is disabled.");return result;}
        double distance=Math.sqrt(getBlockPos().distSqr(remote.getBlockPos()));double allowed=Math.min(effectiveMaximumRangeBlocks(),remote.effectiveMaximumRangeBlocks());
        if(distance>allowed){result.putString("Message","Connection timed out: server is outside the allowed network range.");return result;}
        if(!java.util.Objects.equals(remote.fileUsers.get(username),password)){result.putString("Message","530 Login incorrect.");return result;}
        result.putBoolean("Connected",true);result.putString("Server",serverIp);result.putString("Username",username);result.putString("RemoteFiles",remote.hostedFileData());
        if(action.equals("CONNECT")||action.equals("LIST")){result.putString("Message","230 User logged in. Remote directory loaded.");return result;}
        if(action.equals("GET")){ServerRackHostedFile file=remote.hostedFiles.get(remoteName);if(file==null||!file.readable())result.putString("Message","550 File unavailable or not readable.");else if(local.size()>=128&&!local.containsKey(localName.isBlank()?remoteName:localName))result.putString("Message","Local document limit reached.");else{String destination=localName.isBlank()?remoteName:localName;if(destination.length()>64||!destination.matches("[A-Za-z0-9._-]+"))result.putString("Message","Invalid local filename.");else{local.put(destination,new ServerRackHostedFile(destination,file.content(),true,true));saveDesktopTextFiles(local);result.putString("Message","226 Download complete: "+remoteName+" -> "+destination+".");}}}
        else if(action.equals("PUT")){ServerRackHostedFile file=local.get(localName);if(file==null)result.putString("Message","Local document not found.");else{String destination=remoteName.isBlank()?localName.toLowerCase():remoteName;result.putString("Message",remote.manageHostedFile("SAVE","FTP",destination,file.content(),true,true));}}
        else result.putString("Message","Unsupported FTP action.");
        result.putString("LocalFiles",desktopTextFileData());result.putString("RemoteFiles",remote.hostedFileData());return result;
    }
    public CompoundTag manageDesktopTftp(String action,String serverIp,String remoteName,String localName,ServerLevel level){
        CompoundTag result=new CompoundTag();Map<String,ServerRackHostedFile> local=desktopTextFiles();result.putString("LocalFiles",desktopTextFileData());action=action.trim().toUpperCase();serverIp=serverIp.trim();remoteName=remoteName.trim().toLowerCase();localName=localName.trim();
        ServerRackBlockEntity remote=validIp(serverIp)?ServerRackDirectory.byIp(level,serverIp):null;
        if(remote==null){result.putString("Message","TFTP timeout: server is unavailable.");return result;}
        if(!remote.serviceEnabled(ServerRackService.TFTP)){result.putString("Message","TFTP service is disabled on the target rack.");return result;}
        double distance=Math.sqrt(getBlockPos().distSqr(remote.getBlockPos()));double allowed=Math.min(effectiveMaximumRangeBlocks(),remote.effectiveMaximumRangeBlocks());
        if(distance>allowed){result.putString("Message","TFTP timeout: server is outside the allowed network range.");return result;}
        result.putBoolean("Connected",true);result.putString("Server",serverIp);result.putString("RemoteFiles",remote.hostedFileData());
        if(action.equals("CONNECT")||action.equals("LIST")){result.putString("Message","TFTP server ready. Directory loaded.");return result;}
        if(action.equals("GET")){ServerRackHostedFile file=remote.hostedFiles.get(remoteName);if(file==null||!file.readable())result.putString("Message","Error 1: File not found or not readable.");else{String destination=localName.isBlank()?remoteName:localName;if(destination.length()>64||!destination.matches("[A-Za-z0-9._-]+"))result.putString("Message","Invalid local filename.");else if(local.size()>=128&&!local.containsKey(destination))result.putString("Message","Local document limit reached.");else{local.put(destination,new ServerRackHostedFile(destination,file.content(),true,true));saveDesktopTextFiles(local);result.putString("Message","Read complete: "+remoteName+" -> "+destination+" (octet mode).");}}}
        else if(action.equals("PUT")){ServerRackHostedFile file=local.get(localName);if(file==null)result.putString("Message","Local document not found.");else{String destination=remoteName.isBlank()?localName.toLowerCase():remoteName;result.putString("Message",remote.manageHostedFile("SAVE","TFTP",destination,file.content(),true,true));}}
        else result.putString("Message","Unsupported TFTP action.");
        result.putString("LocalFiles",desktopTextFileData());result.putString("RemoteFiles",remote.hostedFileData());return result;
    }
    private String ipConfiguration(){return "Device: "+displayName+"\nIPv4 Address: "+ipAddress+"\nSubnet Mask: "+subnetMask+"\nIPv4 Gateway: "+gatewayIp+"\nIPv4 DNS: "+dnsServer+"\nIPv6 Address: "+ipv6Address+"/"+ipv6PrefixLength+"\nIPv6 Gateway: "+ipv6Gateway+"\nIPv6 DNS: "+ipv6DnsServer+"\nDevice Clock (UTC ms): "+deviceTimeMillis()+"\nPTP: "+ptpMode+" / "+ptpProfile+"\nNetwork Profile: "+profile.displayName()+"\nConfigured Range: "+profile.rangeText()+"\nEffective Range: "+(long)effectiveMaximumRangeBlocks()+" blocks"+(profile.wiredBeyondCampus()&&!wiredBackboneConnected?" (wire required beyond 10 km)":"");}
    public String configureDesktopNetwork(boolean automatic,String address,String mask,String router,String resolver,String address6,int prefix,String router6,String resolver6){
        if(!validIp(address)||!validIp(mask)||!validIp(router)||!validIp(resolver))return "Invalid IPv4 address, subnet mask, gateway, or DNS server.";
        if(!validIpv6(address6)||prefix<0||prefix>128||!validIpv6(router6)||!validIpv6(resolver6))return "Invalid IPv6 address, prefix, gateway, or DNS server.";
        usesDhcp=automatic;ipAddress=address;subnetMask=mask;gatewayIp=router;dnsServer=resolver;ipv6Address=address6;ipv6PrefixLength=prefix;ipv6Gateway=router6;ipv6DnsServer=resolver6;ipv6Automatic=automatic;setChanged();return automatic?"DHCP/automatic addressing enabled. Current addresses retained until a lease is received.":"Static IPv4 and IPv6 configuration saved.";
    }
    private String ping(String input,ServerLevel level){return w120Ping(input,level);}
    private String pingLegacyW120(String input,ServerLevel level){String[] args=input.trim().split("\\s+");String target=args.length==0?"":args[0];int count=4;if(args.length>1)try{count=Math.max(1,Math.min(10,Integer.parseInt(args[1])));}catch(Exception ignored){}if(!validIp(target)&&!validIpv6(target)){String resolved=ServerRackDirectory.resolve(level,target.toLowerCase());if(resolved!=null)target=resolved;else return "Ping could not resolve host "+target+".";}ServerRackBlockEntity r=ServerRackDirectory.byIp(level,target);if(r==null)return "Pinging "+target+" with 32 bytes of data:\nRequest timed out.\n\nPackets: Sent = "+count+", Received = 0, Lost = "+count+" (100% loss)";double distance=Math.sqrt(getBlockPos().distSqr(r.getBlockPos()));double allowed=Math.min(effectiveMaximumRangeBlocks(),r.effectiveMaximumRangeBlocks());if(distance>allowed)return "Pinging "+target+":\nRequest timed out. Host is "+String.format("%.1f",distance)+" blocks away; allowed range is "+(long)allowed+" blocks."+(profile.wiredBeyondCampus()&&!wiredBackboneConnected?"\nConnect a wired backbone for this network profile.":"")+"\n\nPackets: Sent = "+count+", Received = 0, Lost = "+count+" (100% loss)";long latency=Math.max(1,Math.round(distance/250.0));StringBuilder out=new StringBuilder("Pinging ").append(target).append(" [").append(r.displayName()).append("] with 32 bytes of data:\n");for(int i=0;i<count;i++)out.append("Reply from ").append(target).append(": bytes=32 time=").append(latency).append("ms TTL=64\n");return out.append("\nPackets: Sent = ").append(count).append(", Received = ").append(count).append(", Lost = 0 (0% loss)\nApproximate round trip: ").append(latency).append("ms; distance: ").append(String.format("%.1f",distance)).append(" blocks").toString();}
    private String dnsLookup(String input,ServerLevel level){
        String[] arguments=input.trim().split("\\s+");
        if(arguments.length==0||arguments[0].isBlank())return "Usage: nslookup <name> [A|AAAA|CNAME|MX|PTR] [server-ip]";
        String name=arguments[0].toLowerCase();
        String type=arguments.length>1?arguments[1].toUpperCase():"A";
        String requestedServer=arguments.length>2?arguments[2]:"";
        if(!java.util.Set.of("A","AAAA","CNAME","MX","PTR").contains(type))return "Unsupported query type: "+type+"\nUse A, AAAA, CNAME, MX, or PTR.";

        String resolverAddress=requestedServer.isBlank()?(type.equals("AAAA")?ipv6DnsServer:dnsServer):requestedServer;
        ServerRackBlockEntity resolver=null;
        if(resolverAddress.equalsIgnoreCase(ipAddress)||resolverAddress.equalsIgnoreCase(ipv6Address))resolver=this;
        if(resolver==null)resolver=ServerRackDirectory.byIp(level,resolverAddress);
        if(resolver==null&&requestedServer.isBlank()&&dnsEnabled){resolver=this;resolverAddress=type.equals("AAAA")?ipv6Address:ipAddress;}
        if(resolver==null)return "DNS request timed out.\nServer "+resolverAddress+" is unavailable.";
        if(!resolver.dnsEnabled)return "DNS request refused by "+resolver.displayName()+".\nThe DNS service is disabled.";

        double distance=Math.sqrt(getBlockPos().distSqr(resolver.getBlockPos()));
        double allowed=Math.min(effectiveMaximumRangeBlocks(),resolver.effectiveMaximumRangeBlocks());
        if(distance>allowed)return "DNS request timed out.\nServer: "+resolver.displayName()+"\nAddress: "+resolverAddress+"\nDistance: "+String.format("%.1f",distance)+" blocks\nAllowed range: "+(long)allowed+" blocks";

        String key=type+'|'+name;
        long now=System.currentTimeMillis();
        boolean cached=resolver.dnsCacheExpiry.getOrDefault(key,0L)>now&&resolver.dnsCache.containsKey(key);
        ServerRackDnsRecord record=resolver.detailedDnsRecords.get(key);
        long started=System.nanoTime();
        String answer=resolver.resolveDns(name,type);
        long responseMillis=Math.max(1L,(System.nanoTime()-started)/1_000_000L+Math.round(distance/500.0));
        if(answer==null)return "Server: "+resolver.displayName()+"\nAddress: "+resolverAddress+":53\n\n*** "+resolver.displayName()+" can't find "+name+": NXDOMAIN\nQuery type: "+type+"\nResponse time: "+responseMillis+" ms";
        int ttl=record==null?300:record.ttl();
        return "Server: "+resolver.displayName()+"\nAddress: "+resolverAddress+":53\n\nName: "+name+"\nType: "+type+"\nAnswer: "+answer+"\nTTL: "+ttl+" seconds\nSource: "+(cached?"Resolver cache":"Authoritative records")+"\nResponse time: "+responseMillis+" ms";
    }
    private String browse(String originalAddress,ServerLevel level){
        String address=originalAddress.trim();if(address.isEmpty())return browserResult(400,address,"Bad Request","Enter a URL or IPv4 address.");
        boolean secure=address.regionMatches(true,0,"https://",0,8);String remainder=address.replaceFirst("(?i)^https?://","");int slash=remainder.indexOf('/');String authority=slash<0?remainder:remainder.substring(0,slash);String path=slash<0?"index.html":remainder.substring(slash+1);if(path.isBlank())path="index.html";
        String host=authority;int port=secure?443:80;int colon=authority.lastIndexOf(':');if(colon>0&&authority.indexOf(':')==colon){host=authority.substring(0,colon);try{port=Integer.parseInt(authority.substring(colon+1));}catch(Exception e){return browserResult(400,address,"Bad Request","Invalid port number.");}}
        String target=validIp(host)?host:ServerRackDirectory.resolve(level,host.toLowerCase());if(target==null)return browserResult(404,address,"Host Not Found","DNS could not resolve "+host+".");ServerRackBlockEntity rack=ServerRackDirectory.byIp(level,target);if(rack==null)return browserResult(503,address,"Connection Failed","No loaded server rack owns "+target+".");
        if(!rack.httpEnabled)return browserResult(503,address,"Service Unavailable","HTTP service is disabled on "+target+".");if(secure&&!rack.httpsEnabled)return browserResult(503,address,"Service Unavailable","HTTPS service is disabled on "+target+".");int expected=secure?rack.httpsPort:rack.httpPort;if(port!=expected)return browserResult(404,address,"Port Closed","Nothing is listening on port "+port+". Expected "+expected+".");
        ServerRackHostedFile file=rack.hostedFile(path);if(file==null||!file.readable())return browserResult(404,address,"Not Found","The requested file /"+path+" does not exist or is not readable.");String content=file.content();java.util.regex.Matcher sheets=Pattern.compile("(?is)<link[^>]+href\\s*=\\s*['\"]([^'\"]+\\.css)['\"][^>]*>").matcher(content);StringBuilder embeddedCss=new StringBuilder();while(sheets.find()&&embeddedCss.length()<8192){String cssPath=sheets.group(1).replaceFirst("^/","");ServerRackHostedFile css=rack.hostedFile(cssPath);if(css!=null&&css.readable())embeddedCss.append("<style>").append(css.content()).append("</style>");}content=embeddedCss+content;String canonical=(secure?"https://":"http://")+host+(port==(secure?443:80)?"":":"+port)+"/"+path;return browserResult(200,canonical,rack.displayName(),content);
    }
    private static String browserResult(int status,String url,String title,String content){return "VSIA_BROWSER\t"+status+"\t"+url.replace('\t',' ')+"\t"+title.replace('\t',' ')+'\n'+content;}
    private String mailbox(String address){if(!mailEnabled)return "SMTP service is disabled.";if(address.isBlank())return "Enter a mailbox address.";ListTag messages=mailboxes.get(address.toLowerCase());if(messages==null||messages.isEmpty())return "Mailbox "+address+" is empty.";StringBuilder out=new StringBuilder("Mailbox: ").append(address).append('\n');for(int i=0;i<messages.size();i++){CompoundTag m=messages.getCompound(i);out.append(i+1).append(". ").append(m.getString("subject")).append(" - from ").append(m.getString("from")).append('\n');}return out.toString();}
    private String command(String command,ServerLevel level){String[] p=command.trim().split("\\s+",2);String name=p.length==0?"":p[0].toLowerCase();String arg=p.length>1?p[1]:"";return switch(name){case "ipconfig"->ipConfiguration();case "arp","netstat","hostnet"->w120HostStatus();case "ping"->ping(arg,level);case "nslookup"->dnsLookup(arg,level);case "curl"->browse(arg,level);case "mail"->mailbox(arg);case "ftp"->desktopTransfer(arg,level,true);case "tftp"->desktopTransfer(arg,level,false);case "help"->"Commands: ipconfig, hostnet, arp, netstat, ping <ip>, nslookup <domain>, curl <url>, mail <address>, ftp <server> <action> <file> <user> <password> [content], tftp <server> <action> <file> [content], help";case ""->"Enter a command.";default->"Unknown command: "+name+"\nType help for available commands.";};}
    private OSINetworkPacket response(OSINetworkPacket q,int port,String protocol){
        OSINetworkPacket r=new OSINetworkPacket(); r.sourceMac=macAddress;r.targetMac=q.sourceMac;r.sourceIp=ipAddress;
        r.targetIp=q.sourceIp;r.sourcePort=port;r.targetPort=q.sourcePort;r.applicationProtocol=protocol;r.isResponse=true;r.sessionId=q.sessionId;return r;
    }
    @Override protected void handleWebRequest(OSINetworkPacket q){if(httpEnabled&&!q.isResponse&&"HTTP".equalsIgnoreCase(q.applicationProtocol))serveFile(q,"HTTP",httpPort,false);}
    @Override protected void handleIncomingData(OSINetworkPacket q){if(q.isResponse)return;String protocol=q.applicationProtocol.toUpperCase();if(protocol.equals("RADIUS_EAP")&&serviceEnabled(ServerRackService.RADIUS_EAP)){boolean ok=radiusAuthenticate(q.sourceIp,q.payload.getString("shared_secret"),q.payload.getString("username"),q.payload.getString("password"),q.payload.getInt("required_privilege"),q.payload.getString("eap_method"));OSINetworkPacket r=response(q,1812,"RADIUS_EAP");r.payload.putString("result",ok?"Access-Accept":"Access-Reject");r.payload.putBoolean("accepted",ok);transmitPacket(r);return;}if(protocol.equals("AAA")&&serviceEnabled(ServerRackService.AAA)){boolean ok=authenticateAaa(q.payload.getString("username"),q.payload.getString("password"),q.payload.getInt("required_privilege"),q.payload.getString("service"),q.sourceIp);OSINetworkPacket r=response(q,0,"AAA");r.payload.putBoolean("accepted",ok);ServerRackAaaUser u=aaaUsers.get(q.payload.getString("username").toLowerCase());r.payload.putInt("privilege",ok&&u!=null?u.privilege():-1);transmitPacket(r);return;}if(protocol.equals("SYSLOG")&&serviceEnabled(ServerRackService.SYSLOG)&&syslogAcceptRemote){addSyslog(q.sourceIp,q.payload.getString("facility"),q.payload.getInt("severity"),q.payload.getString("message"));return;}if(protocol.equals("HTTPS")&&httpsEnabled){serveFile(q,"HTTPS",httpsPort,true);return;}if(protocol.equals("FTP")&&serviceEnabled(ServerRackService.FTP)){fileTransfer(q,"FTP",ftpPort,true);return;}if(protocol.equals("TFTP")&&serviceEnabled(ServerRackService.TFTP)){fileTransfer(q,"TFTP",tftpPort,false);}}
    private void serveFile(OSINetworkPacket q,String protocol,int port,boolean secure){OSINetworkPacket r=response(q,port,protocol);String path=q.payload.getString("path");if(path.isBlank()||path.equals("/"))path="index.html";path=path.replaceFirst("^/","").toLowerCase();ServerRackHostedFile file=hostedFiles.get(path);if(file==null||!file.readable()){r.payload.putInt("status",404);r.payload.putString("content","Not Found");}else{r.payload.putInt("status",200);r.payload.putString("content",file.content());r.payload.putBoolean("secure",secure);}transmitPacket(r);}
    private void fileTransfer(OSINetworkPacket q,String protocol,int port,boolean requiresLogin){OSINetworkPacket r=response(q,port,protocol);String action=q.payload.getString("action");String user=q.payload.getString("username").toLowerCase();if(requiresLogin&&!java.util.Objects.equals(fileUsers.get(user),q.payload.getString("password"))){r.payload.putString("status","AUTH_FAILED");transmitPacket(r);return;}String name=q.payload.getString("filename").toLowerCase();if(action.equalsIgnoreCase("GET")){ServerRackHostedFile f=hostedFiles.get(name);if(f==null||!f.readable())r.payload.putString("status","NOT_FOUND");else{r.payload.putString("status","OK");r.payload.putString("content",f.content());}}else if(action.equalsIgnoreCase("PUT")){String result=manageHostedFile("SAVE",protocol,name,q.payload.getString("content"),true,true);r.payload.putString("status",result.startsWith("File saved")?"OK":result);}else if(action.equalsIgnoreCase("LIST")){r.payload.putString("status","OK");r.payload.putString("files",hostedFileData());}transmitPacket(r);}
    @Override protected void handleDnsRequest(OSINetworkPacket q){if(dnsEnabled&&!q.isResponse&&"DNS".equalsIgnoreCase(q.applicationProtocol)){OSINetworkPacket r=response(q,53,"DNS");String d=q.payload.getString("domain").toLowerCase();String type=q.payload.contains("query_type")?q.payload.getString("query_type"):"A";String answer=resolveDns(d,type);ServerRackDnsRecord record=detailedDnsRecords.get(type.toUpperCase()+"|"+d);int ttl=record==null?300:record.ttl();r.payload.putInt("dns_id",q.payload.getInt("dns_id"));r.payload.putString("domain",d);r.payload.putString("query_type",type);r.payload.putString("record_type",type);r.payload.putString("answer",answer==null?"":answer);r.payload.putString("resolved_ip",answer==null||!"A".equalsIgnoreCase(type)?"0.0.0.0":answer);r.payload.putInt("ttl",answer==null?0:ttl);r.payload.putInt("rcode",answer==null?3:0);transmitPacket(r);}}
    @Override protected void handleDhcpRequest(OSINetworkPacket q){if(q.isResponse)return;boolean v6="DHCPV6".equalsIgnoreCase(q.applicationProtocol);if((v6&&!serviceEnabled(ServerRackService.DHCPV6))||(!v6&&(!dhcpEnabled||!"DHCP".equalsIgnoreCase(q.applicationProtocol))))return;String action=q.payload.getString("type");String key=(v6?"6:":"4:")+q.sourceMac;if("RELEASE".equalsIgnoreCase(action)){dhcpLeases.remove(key);setChanged();return;}if(!"DISCOVER".equalsIgnoreCase(action)&&!"REQUEST".equalsIgnoreCase(action)&&!"RENEW".equalsIgnoreCase(action))return;ServerRackDhcpLease lease=allocateLease(q.sourceMac,v6);OSINetworkPacket r=response(q,v6?547:67,v6?"DHCPV6":"DHCP");r.targetIp=v6?"ff02::1:2":"255.255.255.255";r.payload.putInt("xid",q.payload.getInt("xid"));if(lease==null){r.payload.putString("type","NAK");}else{ServerRackDhcpPool pool=dhcpPools.values().stream().filter(p->p.name().equals(lease.pool())&&p.ipv6()==v6).findFirst().orElse(null);String replyType=!v6&&"DISCOVER".equalsIgnoreCase(action)?"OFFER":"ACK";r.payload.putString("type",replyType);r.payload.putString("assigned_ip",lease.address());r.payload.putString("server_identifier",ipAddress);r.payload.putString(v6?"prefix_length":"subnet_mask",pool.prefixOrMask());r.payload.putString("router_ip",pool.gateway());r.payload.putString("dns_server",pool.dns());r.payload.putInt("lease_seconds",pool.leaseSeconds());if(!v6&&"OFFER".equals(replyType))r.payload.putString("diagnostic","DHCP DORA OFFER");}transmitPacket(r);setChanged();}
    @Override protected void handleMailRequest(OSINetworkPacket q){if(!mailEnabled||q.isResponse||!"SMTP".equalsIgnoreCase(q.applicationProtocol))return;OSINetworkPacket r=response(q,25,"SMTP");String action=q.payload.getString("action");String address=q.payload.getString("address").toLowerCase();ServerRackMailAccount account=mailAccounts.get(address);if(account==null||!java.util.Objects.equals(account.password(),q.payload.getString("password"))){r.payload.putString("status","AUTH_FAILED");transmitPacket(r);return;}if("SEND".equalsIgnoreCase(action)){String to=q.payload.getString("to").toLowerCase();ServerRackMailAccount recipient=mailAccounts.get(to);if(recipient==null){r.payload.putString("status","NO_SUCH_MAILBOX");}else{ListTag inbox=mailboxes.computeIfAbsent(to,k->new ListTag());if(inbox.size()>=recipient.quota()){r.payload.putString("status","MAILBOX_FULL");}else{CompoundTag m=new CompoundTag();m.putString("id",java.util.UUID.randomUUID().toString());m.putString("from",address);m.putString("to",to);m.putString("subject",q.payload.getString("subject"));m.putString("body",q.payload.getString("body"));m.putLong("sentAt",deviceTimeMillis());m.putBoolean("read",false);inbox.add(m);CompoundTag sent=m.copy();sent.putBoolean("sent",true);mailboxes.computeIfAbsent("sent:"+address,k->new ListTag()).add(sent);r.payload.putString("status","DELIVERED");setChanged();}}}else if("LIST".equalsIgnoreCase(action)){r.payload.put("messages",mailboxes.getOrDefault(address,new ListTag()).copy());r.payload.putString("status","OK");}else if("DELETE".equalsIgnoreCase(action)){ListTag box=mailboxes.getOrDefault(address,new ListTag());String id=q.payload.getString("id");box.removeIf(tag->((CompoundTag)tag).getString("id").equals(id));r.payload.putString("status","DELETED");setChanged();}transmitPacket(r);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"rack_controller",0,s->PlayState.STOP).triggerableAnim("open",OPEN).triggerableAnim("close",CLOSE));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);ListTag radiusClientList=new ListTag();radiusClients.values().forEach(c->radiusClientList.add(c.save()));t.put("RadiusClients",radiusClientList);ListTag radiusEventList=new ListTag();radiusEvents.forEach(e->{CompoundTag v=new CompoundTag();v.putString("Value",e);radiusEventList.add(v);});t.put("RadiusEvents",radiusEventList);ListTag aaaUserList=new ListTag();aaaUsers.values().forEach(u->aaaUserList.add(u.save()));t.put("AaaUsers",aaaUserList);ListTag aaaRecordList=new ListTag();aaaRecords.forEach(r->aaaRecordList.add(r.save()));t.put("AaaRecords",aaaRecordList);ListTag syslogList=new ListTag();syslogEntries.forEach(e->syslogList.add(e.save()));t.put("SyslogEntries",syslogList);t.putInt("SyslogMinimumSeverity",syslogMinimumSeverity);t.putBoolean("SyslogAcceptRemote",syslogAcceptRemote);t.putBoolean("NtpServerEnabled",ntpServerEnabled);t.putBoolean("NtpClientEnabled",ntpClientEnabled);t.putInt("NtpStratum",ntpStratum);t.putInt("NtpPollSeconds",ntpPollSeconds);t.putString("NtpSourceIp",ntpSourceIp);t.putInt("ClockDriftPpm",clockDriftPpm);t.putLong("LastNtpSyncMillis",lastNtpSyncMillis);t.putString("ClockSyncStatus",clockSyncStatus);ListTag accountList=new ListTag();mailAccounts.values().forEach(a->accountList.add(a.save()));t.put("MailAccounts",accountList);t.putString("MailDomain",mailDomain);ListTag fileList=new ListTag();hostedFiles.values().forEach(f->fileList.add(f.save()));t.put("HostedFiles",fileList);CompoundTag users=new CompoundTag();fileUsers.forEach(users::putString);t.put("FileUsers",users);t.putBoolean("HttpsEnabled",httpsEnabled);t.putInt("HttpPort",httpPort);t.putInt("HttpsPort",httpsPort);t.putInt("FtpPort",ftpPort);t.putInt("TftpPort",tftpPort);ListTag poolList=new ListTag();dhcpPools.values().forEach(p->poolList.add(p.save()));t.put("DhcpPools",poolList);ListTag leaseList=new ListTag();dhcpLeases.values().forEach(l->leaseList.add(l.save()));t.put("DhcpLeases",leaseList);ListTag dnsList=new ListTag();detailedDnsRecords.values().forEach(r->dnsList.add(r.save()));t.put("DetailedDnsRecords",dnsList);t.putLong("EnabledServiceMask",serviceMask());t.putString("Ipv6Address",ipv6Address);t.putInt("Ipv6PrefixLength",ipv6PrefixLength);t.putString("Ipv6Gateway",ipv6Gateway);t.putString("Ipv6DnsServer",ipv6DnsServer);t.putBoolean("Ipv6Automatic",ipv6Automatic);t.putLong("ClockOffsetMillis",clockOffsetMillis);t.putLong("LastPtpSyncMillis",lastPtpSyncMillis);t.putString("PtpMode",ptpMode.name());t.putString("PtpProfile",ptpProfile.name());t.putString("RackProfile",profile.name());t.putBoolean("WiredBackboneConnected",wiredBackboneConnected);t.putString("DisplayName",displayName);t.putString("SubnetMask",subnetMask);t.putString("GatewayIp",gatewayIp);t.putString("DnsServer",dnsServer);t.putBoolean("UsesDhcp",usesDhcp);t.putBoolean("HttpEnabled",httpEnabled);t.putBoolean("DnsEnabled",dnsEnabled);t.putBoolean("DhcpEnabled",dhcpEnabled);t.putBoolean("MailEnabled",mailEnabled);t.putBoolean("DoorOpen",doorOpen);t.putInt("NextIpSuffix",nextIpSuffix);t.putString("IndexHtml",indexHtml);t.putString("ProgramSource",programSource);t.putString("ProgramOutput",programOutput);CompoundTag d=new CompoundTag();dnsRecords.forEach(d::putString);t.put("DnsRecords",d);CompoundTag l=new CompoundTag();leasedIps.forEach(l::putString);t.put("LeasedIps",l);CompoundTag m=new CompoundTag();mailboxes.forEach(m::put);t.put("Mailboxes",m);ListTag links=new ListTag();cableLinks.forEach(p->{CompoundTag entry=new CompoundTag();entry.putLong("Position",p.asLong());links.add(entry);});t.put("CableLinks",links);}
    @Override public void load(CompoundTag t){
        super.load(t);
        cableLinks.clear();
        if(t.contains("CableLinks",Tag.TAG_LIST)){
            ListTag links=t.getList("CableLinks",Tag.TAG_COMPOUND);
            for(int i=0;i<links.size();i++)
                cableLinks.add(BlockPos.of(links.getCompound(i).getLong("Position")));
        }
        wiredBackboneConnected=!cableLinks.isEmpty();
        if(t.contains("RadiusClients",Tag.TAG_LIST)){radiusClients.clear();ListTag radiusClientList=t.getList("RadiusClients",Tag.TAG_COMPOUND);for(int i=0;i<radiusClientList.size();i++){ServerRackRadiusClient c=ServerRackRadiusClient.load(radiusClientList.getCompound(i));radiusClients.put(c.address(),c);}}if(t.contains("RadiusEvents",Tag.TAG_LIST)){radiusEvents.clear();ListTag radiusEventList=t.getList("RadiusEvents",Tag.TAG_COMPOUND);for(int i=0;i<radiusEventList.size();i++)radiusEvents.add(radiusEventList.getCompound(i).getString("Value"));}if(t.contains("AaaUsers",Tag.TAG_LIST)){aaaUsers.clear();ListTag aaaUserList=t.getList("AaaUsers",Tag.TAG_COMPOUND);for(int i=0;i<aaaUserList.size();i++){ServerRackAaaUser u=ServerRackAaaUser.load(aaaUserList.getCompound(i));aaaUsers.put(u.username(),u);}}if(t.contains("AaaRecords",Tag.TAG_LIST)){aaaRecords.clear();ListTag aaaRecordList=t.getList("AaaRecords",Tag.TAG_COMPOUND);for(int i=0;i<aaaRecordList.size();i++)aaaRecords.add(ServerRackAaaRecord.load(aaaRecordList.getCompound(i)));}if(t.contains("SyslogMinimumSeverity"))syslogMinimumSeverity=t.getInt("SyslogMinimumSeverity");if(t.contains("SyslogAcceptRemote"))syslogAcceptRemote=t.getBoolean("SyslogAcceptRemote");if(t.contains("SyslogEntries",Tag.TAG_LIST)){syslogEntries.clear();ListTag syslogList=t.getList("SyslogEntries",Tag.TAG_COMPOUND);for(int i=0;i<syslogList.size();i++)syslogEntries.add(ServerRackSyslogEntry.load(syslogList.getCompound(i)));}if(t.contains("NtpServerEnabled"))ntpServerEnabled=t.getBoolean("NtpServerEnabled");if(t.contains("NtpClientEnabled"))ntpClientEnabled=t.getBoolean("NtpClientEnabled");if(t.contains("NtpStratum"))ntpStratum=t.getInt("NtpStratum");if(t.contains("NtpPollSeconds"))ntpPollSeconds=t.getInt("NtpPollSeconds");if(t.contains("NtpSourceIp"))ntpSourceIp=t.getString("NtpSourceIp");if(t.contains("ClockDriftPpm"))clockDriftPpm=t.getInt("ClockDriftPpm");if(t.contains("LastNtpSyncMillis"))lastNtpSyncMillis=t.getLong("LastNtpSyncMillis");if(t.contains("ClockSyncStatus"))clockSyncStatus=t.getString("ClockSyncStatus");if(t.contains("Ipv6Address"))ipv6Address=t.getString("Ipv6Address");if(t.contains("Ipv6PrefixLength"))ipv6PrefixLength=t.getInt("Ipv6PrefixLength");if(t.contains("Ipv6Gateway"))ipv6Gateway=t.getString("Ipv6Gateway");if(t.contains("Ipv6DnsServer"))ipv6DnsServer=t.getString("Ipv6DnsServer");if(t.contains("Ipv6Automatic"))ipv6Automatic=t.getBoolean("Ipv6Automatic");if(t.contains("ClockOffsetMillis"))clockOffsetMillis=t.getLong("ClockOffsetMillis");if(t.contains("LastPtpSyncMillis"))lastPtpSyncMillis=t.getLong("LastPtpSyncMillis");if(t.contains("PtpMode"))ptpMode=ServerRackPtpMode.byName(t.getString("PtpMode"));if(t.contains("PtpProfile"))ptpProfile=ServerRackPtpProfile.byName(t.getString("PtpProfile"));if(t.contains("RackProfile"))profile=ServerRackProfile.byName(t.getString("RackProfile"));if(t.contains("WiredBackboneConnected"))wiredBackboneConnected=t.getBoolean("WiredBackboneConnected");if(t.contains("DisplayName"))displayName=t.getString("DisplayName");if(t.contains("SubnetMask"))subnetMask=t.getString("SubnetMask");if(t.contains("GatewayIp"))gatewayIp=t.getString("GatewayIp");if(t.contains("DnsServer"))dnsServer=t.getString("DnsServer");if(t.contains("UsesDhcp"))usesDhcp=t.getBoolean("UsesDhcp");if(t.contains("HttpEnabled"))httpEnabled=t.getBoolean("HttpEnabled");if(t.contains("DnsEnabled"))dnsEnabled=t.getBoolean("DnsEnabled");if(t.contains("DhcpEnabled"))dhcpEnabled=t.getBoolean("DhcpEnabled");if(t.contains("MailEnabled"))mailEnabled=t.getBoolean("MailEnabled");doorOpen=t.getBoolean("DoorOpen");if(t.contains("NextIpSuffix"))nextIpSuffix=t.getInt("NextIpSuffix");if(t.contains("IndexHtml"))indexHtml=t.getString("IndexHtml");if(t.contains("ProgramSource"))programSource=t.getString("ProgramSource");if(t.contains("ProgramOutput"))programOutput=t.getString("ProgramOutput");dnsRecords.clear();CompoundTag d=t.getCompound("DnsRecords");d.getAllKeys().forEach(k->dnsRecords.put(k,d.getString(k)));leasedIps.clear();CompoundTag l=t.getCompound("LeasedIps");l.getAllKeys().forEach(k->leasedIps.put(k,l.getString(k)));mailboxes.clear();CompoundTag m=t.getCompound("Mailboxes");m.getAllKeys().forEach(k->mailboxes.put(k,m.getList(k,Tag.TAG_COMPOUND)));if(t.contains("EnabledServiceMask"))applyServiceMask(t.getLong("EnabledServiceMask"));detailedDnsRecords.clear();if(t.contains("DetailedDnsRecords",Tag.TAG_LIST)){ListTag dnsList=t.getList("DetailedDnsRecords",Tag.TAG_COMPOUND);for(int i=0;i<dnsList.size();i++){ServerRackDnsRecord record=ServerRackDnsRecord.load(dnsList.getCompound(i));detailedDnsRecords.put(record.key(),record);}}else dnsRecords.forEach((name,address)->putDefaultDnsRecord(name,"A",address));if(t.contains("DhcpPools",Tag.TAG_LIST)){dhcpPools.clear();ListTag poolList=t.getList("DhcpPools",Tag.TAG_COMPOUND);for(int i=0;i<poolList.size();i++){ServerRackDhcpPool p=ServerRackDhcpPool.load(poolList.getCompound(i));dhcpPools.put((p.ipv6()?"6:":"4:")+p.name().toLowerCase(),p);}}if(t.contains("DhcpLeases",Tag.TAG_LIST)){dhcpLeases.clear();ListTag leaseList=t.getList("DhcpLeases",Tag.TAG_COMPOUND);for(int i=0;i<leaseList.size();i++){ServerRackDhcpLease lease=ServerRackDhcpLease.load(leaseList.getCompound(i));if(lease.expiresAt()>System.currentTimeMillis())dhcpLeases.put((lease.ipv6()?"6:":"4:")+lease.clientId(),lease);}}if(t.contains("HostedFiles",Tag.TAG_LIST)){hostedFiles.clear();ListTag fileList=t.getList("HostedFiles",Tag.TAG_COMPOUND);for(int i=0;i<fileList.size();i++){ServerRackHostedFile f=ServerRackHostedFile.load(fileList.getCompound(i));hostedFiles.put(f.name(),f);}}if(t.contains("FileUsers")){fileUsers.clear();CompoundTag users=t.getCompound("FileUsers");users.getAllKeys().forEach(k->fileUsers.put(k,users.getString(k)));}if(t.contains("HttpsEnabled"))httpsEnabled=t.getBoolean("HttpsEnabled");if(t.contains("HttpPort"))httpPort=t.getInt("HttpPort");if(t.contains("HttpsPort"))httpsPort=t.getInt("HttpsPort");if(t.contains("FtpPort"))ftpPort=t.getInt("FtpPort");if(t.contains("TftpPort"))tftpPort=t.getInt("TftpPort");if(t.contains("MailAccounts",Tag.TAG_LIST)){mailAccounts.clear();ListTag accountList=t.getList("MailAccounts",Tag.TAG_COMPOUND);for(int i=0;i<accountList.size();i++){ServerRackMailAccount a=ServerRackMailAccount.load(accountList.getCompound(i));mailAccounts.put(a.address(),a);}}if(t.contains("MailDomain"))mailDomain=t.getString("MailDomain");}
}