package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
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
    private String ipv6Address="fd00::2", ipv6Gateway="fd00::1", ipv6DnsServer="fd00::2";
    private int ipv6PrefixLength=64;
    private boolean ipv6Automatic;
    private long clockOffsetMillis, lastPtpSyncMillis;
    private ServerRackPtpMode ptpMode=ServerRackPtpMode.DISABLED;
    private ServerRackPtpProfile ptpProfile=ServerRackPtpProfile.POWER;
    private boolean ntpServerEnabled,ntpClientEnabled;
    private int ntpStratum=2,ntpPollSeconds=64,clockDriftPpm;
    private String ntpSourceIp=""; private long lastNtpSyncMillis; private String clockSyncStatus="Free running";
    private final EnumSet<ServerRackService> enabledServices = EnumSet.noneOf(ServerRackService.class);

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
    }

    @Override public void onLoad(){super.onLoad();ServerRackDirectory.register(this);}
    @Override public void setRemoved(){ServerRackDirectory.unregister(this);super.setRemoved();}
    public String displayName(){return displayName;} public String ipAddress(){return ipAddress;}
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
    public boolean serviceEnabled(ServerRackService service){return switch(service){case HTTP->httpEnabled;case DHCP->dhcpEnabled;case DNS->dnsEnabled;case EMAIL->mailEnabled;default->enabledServices.contains(service);};}
    public long serviceMask(){long mask=0;for(ServerRackService service:ServerRackService.values())if(serviceEnabled(service))mask|=1L<<service.ordinal();return mask;}
    public void applyServiceMask(long mask){enabledServices.clear();for(ServerRackService service:ServerRackService.values())if((mask&(1L<<service.ordinal()))!=0)enabledServices.add(service);httpEnabled=enabledServices.contains(ServerRackService.HTTP);dhcpEnabled=enabledServices.contains(ServerRackService.DHCP);dnsEnabled=enabledServices.contains(ServerRackService.DNS);mailEnabled=enabledServices.contains(ServerRackService.EMAIL);setChanged();}
    private long rawDeviceTimeMillis(){long elapsed=System.currentTimeMillis()-lastNtpSyncMillis;long drift=lastNtpSyncMillis==0?0:elapsed*clockDriftPpm/1_000_000L;return System.currentTimeMillis()+clockOffsetMillis+drift;}
    public long deviceTimeMillis(){if(ptpMode==ServerRackPtpMode.CLIENT){ServerRackBlockEntity m=ServerRackDirectory.nearestPtpGrandmaster(this);if(m!=null){clockOffsetMillis=m.rawDeviceTimeMillis()-System.currentTimeMillis();lastPtpSyncMillis=System.currentTimeMillis();clockSyncStatus="PTP synchronized: "+m.displayName();setChanged();return rawDeviceTimeMillis();}}if(ntpClientEnabled&&(lastNtpSyncMillis==0||System.currentTimeMillis()-lastNtpSyncMillis>=ntpPollSeconds*1000L)){ServerRackBlockEntity source=ServerRackDirectory.ntpSource(this,ntpSourceIp);if(source!=null){clockOffsetMillis=source.rawDeviceTimeMillis()-System.currentTimeMillis();lastNtpSyncMillis=System.currentTimeMillis();clockSyncStatus="NTP synchronized: "+source.displayName()+" (stratum "+source.ntpStratum()+")";setChanged();}}return rawDeviceTimeMillis();}
    public void setProfile(ServerRackProfile value){profile=value==null?ServerRackProfile.INTRA_DATA_CENTER:value;setChanged();}
    public void setWiredBackboneConnected(boolean connected){wiredBackboneConnected=connected;setChanged();}
    public double configuredMaximumRangeBlocks(){return profile.maximumRangeBlocks();}
    public double effectiveMaximumRangeBlocks(){return profile.wiredBeyondCampus()&&!wiredBackboneConnected?10_000.0:profile.maximumRangeBlocks();}
    @Override public double maximumReceptionRangeBlocks(){return effectiveMaximumRangeBlocks();}
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
    public String hostedFileData(){StringBuilder b=new StringBuilder();hostedFiles.values().forEach(f->b.append(f.name()).append('\t').append(f.readable()).append('\t').append(f.writable()).append('\t').append(f.content().length()).append('\n'));return b.toString();}
    public String manageHostedFile(String action,String protocol,String originalName,String content,boolean readable,boolean writable){String name=originalName.trim().toLowerCase();if(name.isEmpty()||name.length()>64||name.contains("..")||name.startsWith("/")||!name.matches("[a-z0-9._/-]+"))return "Invalid file name.";if(content.length()>32768)return "File exceeds 32 KB.";if(action.equals("DELETE"))return hostedFiles.remove(name)==null?"File not found.":changed("File deleted.");ServerRackHostedFile old=hostedFiles.get(name);if(old!=null&&!old.writable())return "File is read-only.";if(hostedFiles.size()>=128&&old==null)return "File limit reached.";hostedFiles.put(name,new ServerRackHostedFile(name,content,readable,writable));if(name.equals("index.html"))indexHtml=content;setChanged();return "File saved for "+protocol+".";}
    public String manageFileUser(String action,String originalUsername,String password){String username=originalUsername.trim().toLowerCase();if(username.isEmpty()||username.length()>24||!username.matches("[a-z0-9._-]+"))return "Invalid username.";if(action.equals("DELETE"))return fileUsers.remove(username)==null?"User not found.":changed("User deleted.");if(password.length()<4||password.length()>64)return "Password must be 4-64 characters.";fileUsers.put(username,password);setChanged();return "FTP user saved.";}
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
        if(tool.equalsIgnoreCase("Terminal")||tool.equalsIgnoreCase("Command Prompt"))return command(input,level);
        if(tool.equalsIgnoreCase("Text Editor"))return "Text editor storage will be added with the programming workspace.";
        return "Unknown desktop tool: "+tool;
    }
    private String ipConfiguration(){return "Device: "+displayName+"\nIPv4 Address: "+ipAddress+"\nSubnet Mask: "+subnetMask+"\nIPv4 Gateway: "+gatewayIp+"\nIPv4 DNS: "+dnsServer+"\nIPv6 Address: "+ipv6Address+"/"+ipv6PrefixLength+"\nIPv6 Gateway: "+ipv6Gateway+"\nIPv6 DNS: "+ipv6DnsServer+"\nDevice Clock (UTC ms): "+deviceTimeMillis()+"\nPTP: "+ptpMode+" / "+ptpProfile+"\nNetwork Profile: "+profile.displayName()+"\nConfigured Range: "+profile.rangeText()+"\nEffective Range: "+(long)effectiveMaximumRangeBlocks()+" blocks"+(profile.wiredBeyondCampus()&&!wiredBackboneConnected?" (wire required beyond 10 km)":"");}
    private String ping(String target,ServerLevel level){if(!validIp(target))return "Usage: ping <IPv4 address>";ServerRackBlockEntity r=ServerRackDirectory.byIp(level,target);if(r==null)return "Request timed out.\nHost "+target+" is not available.";double distance=Math.sqrt(getBlockPos().distSqr(r.getBlockPos()));double allowed=Math.min(effectiveMaximumRangeBlocks(),r.effectiveMaximumRangeBlocks());if(distance>allowed)return "Request timed out.\nDistance: "+String.format("%.1f",distance)+" blocks\nAllowed range: "+(long)allowed+" blocks"+(profile.wiredBeyondCampus()&&!wiredBackboneConnected?"\nA wired backbone is required beyond 10 km.":"");return "Reply from "+target+": bytes=32 distance="+String.format("%.1f",distance)+" blocks\nPackets: Sent = 1, Received = 1, Lost = 0";}
    private String dnsLookup(String domain,ServerLevel level){if(domain.isBlank())return "Usage: nslookup <domain>";String result=ServerRackDirectory.resolve(level,domain.toLowerCase());return result==null?"DNS name not found: "+domain:"Name: "+domain+"\nAddress: "+result;}
    private String browse(String address,ServerLevel level){if(address.startsWith("http://"))address=address.substring(7);String target=validIp(address)?address:ServerRackDirectory.resolve(level,address);if(target==null)return "Unable to resolve host: "+address;ServerRackBlockEntity rack=ServerRackDirectory.byIp(level,target);if(rack==null)return "Connection failed: "+target;if(!rack.httpEnabled)return "HTTP service is disabled on "+target;return "HTTP/1.1 200 OK\nServer: "+rack.displayName+"\n\n"+rack.indexHtml;}
    private String mailbox(String address){if(!mailEnabled)return "SMTP service is disabled.";if(address.isBlank())return "Enter a mailbox address.";ListTag messages=mailboxes.get(address.toLowerCase());if(messages==null||messages.isEmpty())return "Mailbox "+address+" is empty.";StringBuilder out=new StringBuilder("Mailbox: ").append(address).append('\n');for(int i=0;i<messages.size();i++){CompoundTag m=messages.getCompound(i);out.append(i+1).append(". ").append(m.getString("subject")).append(" - from ").append(m.getString("from")).append('\n');}return out.toString();}
    private String command(String command,ServerLevel level){String[] p=command.trim().split("\\s+",2);String name=p.length==0?"":p[0].toLowerCase();String arg=p.length>1?p[1]:"";return switch(name){case "ipconfig"->ipConfiguration();case "ping"->ping(arg,level);case "nslookup"->dnsLookup(arg,level);case "curl"->browse(arg,level);case "mail"->mailbox(arg);case "help"->"Commands: ipconfig, ping <ip>, nslookup <domain>, curl <url>, mail <address>, help";case ""->"Enter a command.";default->"Unknown command: "+name+"\nType help for available commands.";};}
    private OSINetworkPacket response(OSINetworkPacket q,int port,String protocol){
        OSINetworkPacket r=new OSINetworkPacket(); r.sourceMac=macAddress;r.targetMac=q.sourceMac;r.sourceIp=ipAddress;
        r.targetIp=q.sourceIp;r.sourcePort=port;r.targetPort=q.sourcePort;r.applicationProtocol=protocol;r.isResponse=true;r.sessionId=q.sessionId;return r;
    }
    @Override protected void handleWebRequest(OSINetworkPacket q){if(httpEnabled&&!q.isResponse&&"HTTP".equalsIgnoreCase(q.applicationProtocol)){OSINetworkPacket r=response(q,80,"HTTP");r.payload.putInt("status",200);r.payload.putString("html",indexHtml);transmitPacket(r);}}
    @Override protected void handleIncomingData(OSINetworkPacket q){if(q.isResponse)return;String protocol=q.applicationProtocol.toUpperCase();if(protocol.equals("HTTPS")&&httpsEnabled){serveFile(q,"HTTPS",httpsPort,true);return;}if(protocol.equals("FTP")&&serviceEnabled(ServerRackService.FTP)){fileTransfer(q,"FTP",ftpPort,true);return;}if(protocol.equals("TFTP")&&serviceEnabled(ServerRackService.TFTP)){fileTransfer(q,"TFTP",tftpPort,false);}}
    private void serveFile(OSINetworkPacket q,String protocol,int port,boolean secure){OSINetworkPacket r=response(q,port,protocol);String path=q.payload.getString("path");if(path.isBlank()||path.equals("/"))path="index.html";path=path.replaceFirst("^/","").toLowerCase();ServerRackHostedFile file=hostedFiles.get(path);if(file==null||!file.readable()){r.payload.putInt("status",404);r.payload.putString("content","Not Found");}else{r.payload.putInt("status",200);r.payload.putString("content",file.content());r.payload.putBoolean("secure",secure);}transmitPacket(r);}
    private void fileTransfer(OSINetworkPacket q,String protocol,int port,boolean requiresLogin){OSINetworkPacket r=response(q,port,protocol);String action=q.payload.getString("action");String user=q.payload.getString("username").toLowerCase();if(requiresLogin&&!java.util.Objects.equals(fileUsers.get(user),q.payload.getString("password"))){r.payload.putString("status","AUTH_FAILED");transmitPacket(r);return;}String name=q.payload.getString("filename").toLowerCase();if(action.equalsIgnoreCase("GET")){ServerRackHostedFile f=hostedFiles.get(name);if(f==null||!f.readable())r.payload.putString("status","NOT_FOUND");else{r.payload.putString("status","OK");r.payload.putString("content",f.content());}}else if(action.equalsIgnoreCase("PUT")){String result=manageHostedFile("SAVE",protocol,name,q.payload.getString("content"),true,true);r.payload.putString("status",result.startsWith("File saved")?"OK":result);}else if(action.equalsIgnoreCase("LIST")){r.payload.putString("status","OK");r.payload.putString("files",hostedFileData());}transmitPacket(r);}
    @Override protected void handleDnsRequest(OSINetworkPacket q){if(dnsEnabled&&!q.isResponse&&"DNS".equalsIgnoreCase(q.applicationProtocol)){OSINetworkPacket r=response(q,53,"DNS");String d=q.payload.getString("domain").toLowerCase();String type=q.payload.contains("query_type")?q.payload.getString("query_type"):"A";String answer=resolveDns(d,type);r.payload.putString("domain",d);r.payload.putString("record_type",type);r.payload.putString("resolved_ip",answer==null?"0.0.0.0":answer);transmitPacket(r);}}
    @Override protected void handleDhcpRequest(OSINetworkPacket q){if(q.isResponse)return;boolean v6="DHCPV6".equalsIgnoreCase(q.applicationProtocol);if((v6&&!serviceEnabled(ServerRackService.DHCPV6))||(!v6&&(!dhcpEnabled||!"DHCP".equalsIgnoreCase(q.applicationProtocol))))return;String action=q.payload.getString("type");String key=(v6?"6:":"4:")+q.sourceMac;if("RELEASE".equalsIgnoreCase(action)){dhcpLeases.remove(key);setChanged();return;}if(!"DISCOVER".equalsIgnoreCase(action)&&!"REQUEST".equalsIgnoreCase(action)&&!"RENEW".equalsIgnoreCase(action))return;ServerRackDhcpLease lease=allocateLease(q.sourceMac,v6);OSINetworkPacket r=response(q,v6?547:67,v6?"DHCPV6":"DHCP");r.targetIp=v6?"ff02::1:2":"255.255.255.255";if(lease==null){r.payload.putString("type","NAK");}else{ServerRackDhcpPool pool=dhcpPools.values().stream().filter(p->p.name().equals(lease.pool())&&p.ipv6()==v6).findFirst().orElse(null);r.payload.putString("type","ACK");r.payload.putString("assigned_ip",lease.address());r.payload.putString(v6?"prefix_length":"subnet_mask",pool.prefixOrMask());r.payload.putString("router_ip",pool.gateway());r.payload.putString("dns_server",pool.dns());r.payload.putInt("lease_seconds",pool.leaseSeconds());}transmitPacket(r);setChanged();}
    @Override protected void handleMailRequest(OSINetworkPacket q){if(!mailEnabled||q.isResponse||!"SMTP".equalsIgnoreCase(q.applicationProtocol))return;OSINetworkPacket r=response(q,25,"SMTP");String action=q.payload.getString("action");String address=q.payload.getString("address").toLowerCase();ServerRackMailAccount account=mailAccounts.get(address);if(account==null||!java.util.Objects.equals(account.password(),q.payload.getString("password"))){r.payload.putString("status","AUTH_FAILED");transmitPacket(r);return;}if("SEND".equalsIgnoreCase(action)){String to=q.payload.getString("to").toLowerCase();ServerRackMailAccount recipient=mailAccounts.get(to);if(recipient==null){r.payload.putString("status","NO_SUCH_MAILBOX");}else{ListTag inbox=mailboxes.computeIfAbsent(to,k->new ListTag());if(inbox.size()>=recipient.quota()){r.payload.putString("status","MAILBOX_FULL");}else{CompoundTag m=new CompoundTag();m.putString("id",java.util.UUID.randomUUID().toString());m.putString("from",address);m.putString("to",to);m.putString("subject",q.payload.getString("subject"));m.putString("body",q.payload.getString("body"));m.putLong("sentAt",deviceTimeMillis());m.putBoolean("read",false);inbox.add(m);CompoundTag sent=m.copy();sent.putBoolean("sent",true);mailboxes.computeIfAbsent("sent:"+address,k->new ListTag()).add(sent);r.payload.putString("status","DELIVERED");setChanged();}}}else if("LIST".equalsIgnoreCase(action)){r.payload.put("messages",mailboxes.getOrDefault(address,new ListTag()).copy());r.payload.putString("status","OK");}else if("DELETE".equalsIgnoreCase(action)){ListTag box=mailboxes.getOrDefault(address,new ListTag());String id=q.payload.getString("id");box.removeIf(tag->((CompoundTag)tag).getString("id").equals(id));r.payload.putString("status","DELETED");setChanged();}transmitPacket(r);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"rack_controller",0,s->PlayState.STOP).triggerableAnim("open",OPEN).triggerableAnim("close",CLOSE));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putBoolean("NtpServerEnabled",ntpServerEnabled);t.putBoolean("NtpClientEnabled",ntpClientEnabled);t.putInt("NtpStratum",ntpStratum);t.putInt("NtpPollSeconds",ntpPollSeconds);t.putString("NtpSourceIp",ntpSourceIp);t.putInt("ClockDriftPpm",clockDriftPpm);t.putLong("LastNtpSyncMillis",lastNtpSyncMillis);t.putString("ClockSyncStatus",clockSyncStatus);ListTag accountList=new ListTag();mailAccounts.values().forEach(a->accountList.add(a.save()));t.put("MailAccounts",accountList);t.putString("MailDomain",mailDomain);ListTag fileList=new ListTag();hostedFiles.values().forEach(f->fileList.add(f.save()));t.put("HostedFiles",fileList);CompoundTag users=new CompoundTag();fileUsers.forEach(users::putString);t.put("FileUsers",users);t.putBoolean("HttpsEnabled",httpsEnabled);t.putInt("HttpPort",httpPort);t.putInt("HttpsPort",httpsPort);t.putInt("FtpPort",ftpPort);t.putInt("TftpPort",tftpPort);ListTag poolList=new ListTag();dhcpPools.values().forEach(p->poolList.add(p.save()));t.put("DhcpPools",poolList);ListTag leaseList=new ListTag();dhcpLeases.values().forEach(l->leaseList.add(l.save()));t.put("DhcpLeases",leaseList);ListTag dnsList=new ListTag();detailedDnsRecords.values().forEach(r->dnsList.add(r.save()));t.put("DetailedDnsRecords",dnsList);t.putLong("EnabledServiceMask",serviceMask());t.putString("Ipv6Address",ipv6Address);t.putInt("Ipv6PrefixLength",ipv6PrefixLength);t.putString("Ipv6Gateway",ipv6Gateway);t.putString("Ipv6DnsServer",ipv6DnsServer);t.putBoolean("Ipv6Automatic",ipv6Automatic);t.putLong("ClockOffsetMillis",clockOffsetMillis);t.putLong("LastPtpSyncMillis",lastPtpSyncMillis);t.putString("PtpMode",ptpMode.name());t.putString("PtpProfile",ptpProfile.name());t.putString("RackProfile",profile.name());t.putBoolean("WiredBackboneConnected",wiredBackboneConnected);t.putString("DisplayName",displayName);t.putString("SubnetMask",subnetMask);t.putString("GatewayIp",gatewayIp);t.putString("DnsServer",dnsServer);t.putBoolean("UsesDhcp",usesDhcp);t.putBoolean("HttpEnabled",httpEnabled);t.putBoolean("DnsEnabled",dnsEnabled);t.putBoolean("DhcpEnabled",dhcpEnabled);t.putBoolean("MailEnabled",mailEnabled);t.putBoolean("DoorOpen",doorOpen);t.putInt("NextIpSuffix",nextIpSuffix);t.putString("IndexHtml",indexHtml);t.putString("ProgramSource",programSource);t.putString("ProgramOutput",programOutput);CompoundTag d=new CompoundTag();dnsRecords.forEach(d::putString);t.put("DnsRecords",d);CompoundTag l=new CompoundTag();leasedIps.forEach(l::putString);t.put("LeasedIps",l);CompoundTag m=new CompoundTag();mailboxes.forEach(m::put);t.put("Mailboxes",m);}
    @Override public void load(CompoundTag t){super.load(t);if(t.contains("NtpServerEnabled"))ntpServerEnabled=t.getBoolean("NtpServerEnabled");if(t.contains("NtpClientEnabled"))ntpClientEnabled=t.getBoolean("NtpClientEnabled");if(t.contains("NtpStratum"))ntpStratum=t.getInt("NtpStratum");if(t.contains("NtpPollSeconds"))ntpPollSeconds=t.getInt("NtpPollSeconds");if(t.contains("NtpSourceIp"))ntpSourceIp=t.getString("NtpSourceIp");if(t.contains("ClockDriftPpm"))clockDriftPpm=t.getInt("ClockDriftPpm");if(t.contains("LastNtpSyncMillis"))lastNtpSyncMillis=t.getLong("LastNtpSyncMillis");if(t.contains("ClockSyncStatus"))clockSyncStatus=t.getString("ClockSyncStatus");if(t.contains("Ipv6Address"))ipv6Address=t.getString("Ipv6Address");if(t.contains("Ipv6PrefixLength"))ipv6PrefixLength=t.getInt("Ipv6PrefixLength");if(t.contains("Ipv6Gateway"))ipv6Gateway=t.getString("Ipv6Gateway");if(t.contains("Ipv6DnsServer"))ipv6DnsServer=t.getString("Ipv6DnsServer");if(t.contains("Ipv6Automatic"))ipv6Automatic=t.getBoolean("Ipv6Automatic");if(t.contains("ClockOffsetMillis"))clockOffsetMillis=t.getLong("ClockOffsetMillis");if(t.contains("LastPtpSyncMillis"))lastPtpSyncMillis=t.getLong("LastPtpSyncMillis");if(t.contains("PtpMode"))ptpMode=ServerRackPtpMode.byName(t.getString("PtpMode"));if(t.contains("PtpProfile"))ptpProfile=ServerRackPtpProfile.byName(t.getString("PtpProfile"));if(t.contains("RackProfile"))profile=ServerRackProfile.byName(t.getString("RackProfile"));if(t.contains("WiredBackboneConnected"))wiredBackboneConnected=t.getBoolean("WiredBackboneConnected");if(t.contains("DisplayName"))displayName=t.getString("DisplayName");if(t.contains("SubnetMask"))subnetMask=t.getString("SubnetMask");if(t.contains("GatewayIp"))gatewayIp=t.getString("GatewayIp");if(t.contains("DnsServer"))dnsServer=t.getString("DnsServer");if(t.contains("UsesDhcp"))usesDhcp=t.getBoolean("UsesDhcp");if(t.contains("HttpEnabled"))httpEnabled=t.getBoolean("HttpEnabled");if(t.contains("DnsEnabled"))dnsEnabled=t.getBoolean("DnsEnabled");if(t.contains("DhcpEnabled"))dhcpEnabled=t.getBoolean("DhcpEnabled");if(t.contains("MailEnabled"))mailEnabled=t.getBoolean("MailEnabled");doorOpen=t.getBoolean("DoorOpen");if(t.contains("NextIpSuffix"))nextIpSuffix=t.getInt("NextIpSuffix");if(t.contains("IndexHtml"))indexHtml=t.getString("IndexHtml");if(t.contains("ProgramSource"))programSource=t.getString("ProgramSource");if(t.contains("ProgramOutput"))programOutput=t.getString("ProgramOutput");dnsRecords.clear();CompoundTag d=t.getCompound("DnsRecords");d.getAllKeys().forEach(k->dnsRecords.put(k,d.getString(k)));leasedIps.clear();CompoundTag l=t.getCompound("LeasedIps");l.getAllKeys().forEach(k->leasedIps.put(k,l.getString(k)));mailboxes.clear();CompoundTag m=t.getCompound("Mailboxes");m.getAllKeys().forEach(k->mailboxes.put(k,m.getList(k,Tag.TAG_COMPOUND)));if(t.contains("EnabledServiceMask"))applyServiceMask(t.getLong("EnabledServiceMask"));detailedDnsRecords.clear();if(t.contains("DetailedDnsRecords",Tag.TAG_LIST)){ListTag dnsList=t.getList("DetailedDnsRecords",Tag.TAG_COMPOUND);for(int i=0;i<dnsList.size();i++){ServerRackDnsRecord record=ServerRackDnsRecord.load(dnsList.getCompound(i));detailedDnsRecords.put(record.key(),record);}}else dnsRecords.forEach((name,address)->putDefaultDnsRecord(name,"A",address));if(t.contains("DhcpPools",Tag.TAG_LIST)){dhcpPools.clear();ListTag poolList=t.getList("DhcpPools",Tag.TAG_COMPOUND);for(int i=0;i<poolList.size();i++){ServerRackDhcpPool p=ServerRackDhcpPool.load(poolList.getCompound(i));dhcpPools.put((p.ipv6()?"6:":"4:")+p.name().toLowerCase(),p);}}if(t.contains("DhcpLeases",Tag.TAG_LIST)){dhcpLeases.clear();ListTag leaseList=t.getList("DhcpLeases",Tag.TAG_COMPOUND);for(int i=0;i<leaseList.size();i++){ServerRackDhcpLease lease=ServerRackDhcpLease.load(leaseList.getCompound(i));if(lease.expiresAt()>System.currentTimeMillis())dhcpLeases.put((lease.ipv6()?"6:":"4:")+lease.clientId(),lease);}}if(t.contains("HostedFiles",Tag.TAG_LIST)){hostedFiles.clear();ListTag fileList=t.getList("HostedFiles",Tag.TAG_COMPOUND);for(int i=0;i<fileList.size();i++){ServerRackHostedFile f=ServerRackHostedFile.load(fileList.getCompound(i));hostedFiles.put(f.name(),f);}}if(t.contains("FileUsers")){fileUsers.clear();CompoundTag users=t.getCompound("FileUsers");users.getAllKeys().forEach(k->fileUsers.put(k,users.getString(k)));}if(t.contains("HttpsEnabled"))httpsEnabled=t.getBoolean("HttpsEnabled");if(t.contains("HttpPort"))httpPort=t.getInt("HttpPort");if(t.contains("HttpsPort"))httpsPort=t.getInt("HttpsPort");if(t.contains("FtpPort"))ftpPort=t.getInt("FtpPort");if(t.contains("TftpPort"))tftpPort=t.getInt("TftpPort");if(t.contains("MailAccounts",Tag.TAG_LIST)){mailAccounts.clear();ListTag accountList=t.getList("MailAccounts",Tag.TAG_COMPOUND);for(int i=0;i<accountList.size();i++){ServerRackMailAccount a=ServerRackMailAccount.load(accountList.getCompound(i));mailAccounts.put(a.address(),a);}}if(t.contains("MailDomain"))mailDomain=t.getString("MailDomain");}
}
