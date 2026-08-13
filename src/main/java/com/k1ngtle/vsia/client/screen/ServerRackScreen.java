package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.signality.internet.server.ServerRackNetwork;
import com.k1ngtle.vsia.signality.internet.server.ServerRackNetwork.OpenRackPacket;
import com.k1ngtle.vsia.signality.internet.server.ServerRackService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class ServerRackScreen extends Screen {
    private enum Tab { PHYSICAL, CONFIG, SERVICES, DESKTOP, PROGRAMMING, ATTRIBUTES }
    private final OpenRackPacket state;
    private final long openedAtMillis=System.currentTimeMillis();
    private final List<Button> pageButtons = new ArrayList<>();
    private Tab tab = Tab.PHYSICAL;
    private String configPage = "Settings", servicePage = "HTTP", desktopPage = "";
    private EditBox name, ip, subnet, gateway, dns, ipv6, prefix6, gateway6, dns6, clockOffset, dnsName, dnsDetail, dnsTtl, poolName, poolStart, poolEnd, poolPrefix, poolGateway, poolDns, poolLease, poolExclusions, mailDomainField, mailUserField, mailPasswordField, mailQuotaField, ntpStratumField, ntpPollField, ntpSourceField, ntpDriftField, syslogMinField, syslogFacilityField, syslogSeverityField, syslogMessageField, aaaUserField, aaaPasswordField, aaaPrivilegeField, aaaServiceField, radiusNameField, radiusAddressField, radiusSecretField, radiusUserField, radiusPasswordField, radiusPrivilegeField, iotIdField, iotNameField, iotTypeField, iotValueField, vmNameField, vmOsField, vmCpuField, vmMemoryField, vmStorageField, prpPeerField, httpFileNameField, httpContentField, httpPortField, httpsPortField, toolInput, programInput;
    private String desktopOutput = "Ready.";
    private String programOutput = "Ready. Separate commands with semicolons. Type help for commands.";
    private boolean dhcp, http, dnsService, dhcpService, mail;
    private boolean automatic6;
    private String ptpMode, ptpProfile;
    private long serviceMask;
    private String dnsRecordData, dnsRecordType="A", dnsStatus="Ready.";
    private String dhcp4Data,dhcp6Data,dhcpStatus="Ready.";
    private boolean https=true,pop3=true;
    private boolean httpReadable=true,httpWritable=true;
    private String httpFiles="",httpStatus="Press Refresh to load hosted files.";
    private String serviceStatus="Ready.";
    private boolean ntpServer,ntpClient;
    private String ntpStatus;
    private long ntpDeviceTime,lastNtpSync;
    private String syslogData="",syslogStatus="Press Refresh to load entries.";
    private boolean syslogAcceptRemote=true;
    private String aaaUsers="",aaaAccounting="",aaaStatus="Press Refresh to load AAA data.";
    private boolean aaaUserEnabled=true;
    private String radiusClients="",radiusEvents="",radiusStatus="Press Refresh to load RADIUS data.";
    private boolean radiusClientEnabled=true;
    private String iotDevices="",iotStatus="Press Refresh to load IoT devices.";
    private String virtualMachines="",vmStatus="Press Refresh to load virtual machines.";
    private String prpStatus="Press Refresh to load PRP supervision.",prpData="";
    private boolean prpEnabled,prpLaneA=true,prpLaneB=true;
    private int x, y, w, h, side;

    public ServerRackScreen(OpenRackPacket state) {
        super(Component.literal("VSIA Server Rack"));
        this.state = state;
        dhcp = state.dhcp(); http = state.http(); dnsService = state.dnsService();
        dhcpService = state.dhcpService(); mail = state.mail();
        automatic6=state.automatic6();ptpMode=state.ptpMode();ptpProfile=state.ptpProfile();
        serviceMask=state.serviceMask();
        dnsRecordData=state.dnsRecordData();
        dhcp4Data=state.dhcp4Data();dhcp6Data=state.dhcp6Data();
        ntpServer=state.ntpServer();ntpClient=state.ntpClient();ntpStatus=state.clockStatus();ntpDeviceTime=state.deviceTime();lastNtpSync=state.lastNtpSync();
    }

    @Override protected void init() {
        clearWidgets(); pageButtons.clear();
        x = 6; y = 6; w = width - 12; h = height - 12; side = 145;
        String[] tabs = {"Physical", "Config", "Services", "Desktop", "Programming", "Attributes"};
        for (int i=0;i<tabs.length;i++) {
            final Tab target=Tab.values()[i];
            addRenderableWidget(Button.builder(Component.literal(tabs[i]), b->{tab=target;desktopPage="";init();})
                    .bounds(x+4+i*82,y+4,78,18).build());
        }
        switch(tab) {
            case CONFIG -> initConfig(); case SERVICES -> initServices(); case DESKTOP -> initDesktop();
            case PROGRAMMING -> initProgramming(); default -> {}
        }
    }

    private Button pageButton(String text,int bx,int by,int bw,Runnable action) {
        Button b=Button.builder(Component.literal(text), ignored->action.run()).bounds(bx,by,bw,18).build();
        pageButtons.add(b); return addRenderableWidget(b);
    }

    private void initConfig() {
        pageButton("GLOBAL",x+7,y+34,side-14,()->{});
        pageButton("Settings",x+7,y+54,side-14,()->{configPage="Settings";init();});
        pageButton("Algorithm Settings",x+7,y+74,side-14,()->{configPage="Algorithm Settings";init();});
        pageButton("INTERFACE",x+7,y+98,side-14,()->{});
        pageButton("FastEthernet0",x+7,y+118,side-14,()->{configPage="FastEthernet0";init();});
        int cx=x+side+36;
        if (configPage.equals("Settings")) {
            name=field(cx+150,y+65,w-side-225,state.displayName(),32);
            gateway=field(cx+150,y+151,w-side-225,state.gateway(),15);
            dns=field(cx+150,y+179,w-side-225,state.dns(),15);
            pageButton(dhcp?"[ DHCP ]     Static":"DHCP     [ Static ]",cx+150,y+111,190,()->{dhcp=!dhcp;init();});
            gateway6=field(cx+150,y+245,w-side-225,state.gateway6(),45);
            dns6=field(cx+150,y+273,w-side-225,state.dns6(),45);
            pageButton(automatic6?"[ Automatic ]     Static":"Automatic     [ Static ]",cx+150,y+217,190,()->{automatic6=!automatic6;init();});
        } else if(configPage.equals("FastEthernet0")) {
            ip=field(cx+150,y+112,w-side-225,state.ip(),15);
            subnet=field(cx+150,y+140,w-side-225,state.subnet(),15);
            ipv6=field(cx+150,y+196,w-side-225,state.ipv6(),45);
            prefix6=field(cx+150,y+224,80,Integer.toString(state.ipv6Prefix()),3);
            pageButton("Port Status     [ ON ]",cx+150,y+70,190,()->{});
        } else {
            clockOffset=field(cx+170,y+104,120,Long.toString(state.clockOffset()/1000L),8);
            pageButton("PTP Mode: "+ptpMode,cx+170,y+145,220,()->{ptpMode=ptpMode.equals("DISABLED")?"GRANDMASTER":ptpMode.equals("GRANDMASTER")?"CLIENT":"DISABLED";init();});
            pageButton("PTP Profile: "+ptpProfile,cx+170,y+173,220,()->{ptpProfile=ptpProfile.equals("POWER")?"TELECOM":ptpProfile.equals("TELECOM")?"DEFAULT":"POWER";init();});
        }
        pageButton("Save Configuration",x+w-157,y+h-29,145,()->save());
    }

    private void initServices() {
        pageButton("SERVICES",x+7,y+34,side-14,()->{});
        ServerRackService[] services=ServerRackService.values();
        for(int i=0;i<services.length;i++){final ServerRackService service=services[i];pageButton(service.displayName(),x+7,y+54+i*20,side-14,()->{servicePage=service.displayName();init();});}
        int cx=x+side+24;
        ServerRackService selected=ServerRackService.byDisplayName(servicePage);
        boolean enabled=(serviceMask&(1L<<selected.ordinal()))!=0;
        serviceToggle(selected.displayName(),cx,y+64,enabled,v->{if(v)serviceMask|=1L<<selected.ordinal();else serviceMask&=~(1L<<selected.ordinal());syncLegacyServiceFlags();});
        if(selected==ServerRackService.DNS){
            dnsName=field(cx+75,y+112,230,"",253);dnsDetail=field(cx+75,y+140,Math.max(180,w-side-260),"",253);dnsTtl=field(cx+75,y+168,70,"300",5);
            pageButton("Type: "+dnsRecordType,cx+315,y+112,120,()->{dnsRecordType=switch(dnsRecordType){case "A"->"AAAA";case "AAAA"->"CNAME";case "CNAME"->"MX";case "MX"->"PTR";default->"A";};init();});
            pageButton("Add / Update",cx,y+196,110,()->sendDns("SAVE"));pageButton("Remove",cx+116,y+196,80,()->sendDns("REMOVE"));pageButton("Clear Cache",cx+202,y+196,90,()->sendDns("CLEAR_CACHE"));
        }
        if(selected==ServerRackService.DHCP||selected==ServerRackService.DHCPV6){boolean v6=selected==ServerRackService.DHCPV6;poolName=field(cx+75,y+112,120,"LAN"+(v6?"6":""),32);poolStart=field(cx+230,y+112,170,v6?"fd00::100":"192.168.1.100",45);poolEnd=field(cx+435,y+112,170,v6?"fd00::ffff":"192.168.1.254",45);poolPrefix=field(cx+75,y+140,120,v6?"64":state.subnet(),15);poolGateway=field(cx+230,y+140,170,v6?state.gateway6():state.gateway(),45);poolDns=field(cx+435,y+140,170,v6?state.dns6():state.dns(),45);poolLease=field(cx+75,y+168,120,"3600",7);poolExclusions=field(cx+230,y+168,375,"",1024);pageButton("Add / Update Pool",cx,y+196,120,()->sendDhcp("SAVE",v6));pageButton("Remove Pool",cx+126,y+196,90,()->sendDhcp("REMOVE",v6));pageButton("Clear Leases",cx+222,y+196,90,()->sendDhcp("CLEAR_LEASES",v6));}
        if (selected == ServerRackService.DHCP || selected == ServerRackService.DHCPV6) {
            int inputStart = cx + 220;
            // Keep the form inside 82% of the visible GUI width. The remaining
            // right-side space protects the last column at large GUI scales.
            int formRight = x + (int) (w * 0.82F);
            int remaining = Math.max(420, formRight - inputStart);
            int gap = 18;
            int columnWidth = Math.max(120, (remaining - gap * 2) / 3);

            poolName.setX(inputStart);
            poolName.setWidth(columnWidth);
            poolStart.setX(inputStart + columnWidth + gap);
            poolStart.setWidth(columnWidth);
            poolEnd.setX(inputStart + (columnWidth + gap) * 2);
            poolEnd.setWidth(columnWidth);

            poolPrefix.setX(inputStart);
            poolPrefix.setWidth(columnWidth);
            poolGateway.setX(inputStart + columnWidth + gap);
            poolGateway.setWidth(columnWidth);
            poolDns.setX(inputStart + (columnWidth + gap) * 2);
            poolDns.setWidth(columnWidth);

            poolLease.setX(inputStart);
            poolLease.setWidth(columnWidth);
            poolExclusions.setX(inputStart + columnWidth + gap);
            poolExclusions.setWidth(columnWidth * 2 + gap);
        }
        if(selected==ServerRackService.HTTP){
            int right=x+w-28, gap=16, half=(right-cx-gap)/2;
            pageButton(https?"HTTPS   [ON]    Off":"HTTPS   On    [OFF]",cx+half+gap,y+64,half,()->{https=!https;httpStatus="HTTPS changed. Save web settings to apply.";});
            httpPortField=field(cx,y+126,half,"80",5);httpsPortField=field(cx+half+gap,y+126,half,"443",5);
            httpFileNameField=field(cx,y+180,right-cx,"index.html",64);
            httpContentField=field(cx,y+234,right-cx,"",32768);
            pageButton(httpReadable?"Readable   [YES]    No":"Readable   Yes    [NO]",cx,y+268,half,()->{httpReadable=!httpReadable;httpStatus="Readable permission changed. Press Save to apply.";});
            pageButton(httpWritable?"Writable   [YES]    No":"Writable   Yes    [NO]",cx+half+gap,y+268,half,()->{httpWritable=!httpWritable;httpStatus="Writable permission changed. Press Save to apply.";});
            pageButton("New",cx,y+302,70,()->{httpFileNameField.setValue("");httpContentField.setValue("");httpReadable=true;httpWritable=true;httpStatus="New file ready.";});
            pageButton("Load",cx+80,y+302,70,()->sendHttp("OPEN"));
            pageButton("Save",cx+160,y+302,70,()->sendHttp("SAVE"));
            pageButton("Delete",cx+240,y+302,70,()->sendHttp("DELETE"));
            pageButton("Import Clipboard",cx+320,y+302,120,()->importHttpClipboard());
            pageButton("Refresh",cx+450,y+302,80,()->sendHttp("QUERY"));
            pageButton("Save Web Settings",cx,y+336,150,()->sendHttp("CONFIG"));
        }
        if(selected==ServerRackService.EMAIL){
            int right=x+w-28;
            int contentWidth=right-cx;
            int gap=16;
            int serviceWidth=(contentWidth-gap)/2;
            int setWidth=76;
            int domainWidth=contentWidth-setWidth-gap;
            int quotaWidth=Math.max(90,contentWidth/8);
            int userWidth=Math.max(170,contentWidth/4);
            int passwordWidth=contentWidth-userWidth-quotaWidth-gap*2;
            pageButton(pop3?"POP3 Service   [ON]    Off":"POP3 Service   On    [OFF]",cx+serviceWidth+gap,y+64,serviceWidth,()->{pop3=!pop3;init();});
            mailDomainField=field(cx,y+151,domainWidth,"vsia-net.com",253);
            pageButton("Set Domain",cx+domainWidth+gap,y+151,setWidth,()->serviceStatus="Mail domain updated.");
            mailUserField=field(cx,y+211,userWidth,"",32);
            mailPasswordField=field(cx+userWidth+gap,y+211,passwordWidth,"",64);
            mailQuotaField=field(cx+userWidth+gap+passwordWidth+gap,y+211,quotaWidth,"100",4);
            pageButton("Add Account",cx,y+245,110,()->serviceStatus="Mailbox account added.");
            pageButton("Remove Account",cx+120,y+245,120,()->serviceStatus="Selected account removed.");
            pageButton("Change Password",cx+250,y+245,125,()->serviceStatus="Password changed.");
        }
        if(selected==ServerRackService.NTP){
            int right=x+w-28;
            int gap=24;
            int columnWidth=Math.max(180,(right-cx-gap)/2);
            int secondColumn=cx+columnWidth+gap;
            pageButton(ntpServer?"Server Role   [ON]    Off":"Server Role   On    [OFF]",cx,y+126,columnWidth,()->{ntpServer=!ntpServer;init();});
            pageButton(ntpClient?"Client Role   [ON]    Off":"Client Role   On    [OFF]",secondColumn,y+126,columnWidth,()->{ntpClient=!ntpClient;init();});
            ntpStratumField=field(cx,y+181,columnWidth,Integer.toString(state.ntpStratum()),2);
            ntpPollField=field(secondColumn,y+181,columnWidth,Integer.toString(state.ntpPoll()),4);
            ntpSourceField=field(cx,y+235,columnWidth,state.ntpSource(),45);
            ntpDriftField=field(secondColumn,y+235,columnWidth,Integer.toString(state.clockDrift()),5);
            pageButton("Apply NTP Configuration",cx,y+269,190,()->sendNtp());
        }
        if(selected==ServerRackService.SYSLOG){
            int right=x+w-28;
            int gap=20;
            int columnWidth=Math.max(180,(right-cx-gap)/2);
            int secondColumn=cx+columnWidth+gap;
            syslogMinField=field(cx,y+145,columnWidth,"7",1);
            pageButton(syslogAcceptRemote?"Remote Messages   [ON]    Off":"Remote Messages   On    [OFF]",secondColumn,y+145,columnWidth,()->{syslogAcceptRemote=!syslogAcceptRemote;init();});
            syslogFacilityField=field(cx,y+199,columnWidth,"LOCAL0",16);
            syslogSeverityField=field(secondColumn,y+199,columnWidth,"6",1);
            syslogMessageField=field(cx,y+253,right-cx,"Server rack syslog test message",512);
            pageButton("Save Configuration",cx,y+287,150,()->sendSyslog("CONFIG"));
            pageButton("Send Test",cx+160,y+287,100,()->sendSyslog("TEST"));
            pageButton("Refresh",cx+270,y+287,90,()->sendSyslog("QUERY"));
            pageButton("Clear Log",cx+370,y+287,90,()->sendSyslog("CLEAR"));
        }
        if(selected==ServerRackService.AAA){
            int right=x+w-28;
            int gap=20;
            int columnWidth=Math.max(180,(right-cx-gap)/2);
            int secondColumn=cx+columnWidth+gap;
            aaaUserField=field(cx,y+139,columnWidth,"",32);
            aaaPasswordField=field(secondColumn,y+139,columnWidth,"",64);
            aaaPrivilegeField=field(cx,y+193,columnWidth,"1",2);
            aaaServiceField=field(secondColumn,y+193,columnWidth,"LOGIN",32);
            pageButton(aaaUserEnabled?"Account   [ENABLED]    Disabled":"Account   Enabled    [DISABLED]",cx,y+227,columnWidth,()->{aaaUserEnabled=!aaaUserEnabled;init();});
            pageButton("Add / Update User",secondColumn,y+227,columnWidth,()->sendAaa("SAVE"));
            pageButton("Remove User",cx,y+261,110,()->sendAaa("DELETE"));
            pageButton("Test Login",cx+120,y+261,100,()->sendAaa("TEST"));
            pageButton("Refresh",cx+230,y+261,90,()->sendAaa("QUERY"));
            pageButton("Clear Accounting",cx+330,y+261,130,()->sendAaa("CLEAR_ACCOUNTING"));
        }
        if(selected==ServerRackService.RADIUS_EAP){
            int right=x+w-28, gap=16, total=right-cx, third=(total-gap*2)/3;
            radiusNameField=field(cx,y+139,third,"Campus NAS",32);
            radiusAddressField=field(cx+third+gap,y+139,third,"192.168.1.10",15);
            radiusSecretField=field(cx+(third+gap)*2,y+139,third,"vsia-radius",64);
            radiusUserField=field(cx,y+193,third,"admin",32);
            radiusPasswordField=field(cx+third+gap,y+193,third,"admin",64);
            radiusPrivilegeField=field(cx+(third+gap)*2,y+193,third,"1",2);
            pageButton(radiusClientEnabled?"NAS Client   [ENABLED]    Disabled":"NAS Client   Enabled    [DISABLED]",cx,y+227,third,()->{radiusClientEnabled=!radiusClientEnabled;init();});
            pageButton("Add / Update NAS",cx+third+gap,y+227,third,()->sendRadius("SAVE"));
            pageButton("Test EAP Login",cx+(third+gap)*2,y+227,third,()->sendRadius("TEST"));
            pageButton("Remove NAS",cx,y+261,110,()->sendRadius("DELETE"));
            pageButton("Refresh",cx+120,y+261,90,()->sendRadius("QUERY"));
            pageButton("Clear Events",cx+220,y+261,110,()->sendRadius("CLEAR"));
        }
        if(selected==ServerRackService.IOT){
            int right=x+w-28,gap=16,total=right-cx,third=(total-gap*2)/3;
            iotIdField=field(cx,y+139,third,"sensor-01",32);
            iotNameField=field(cx+third+gap,y+139,third,"Temperature Sensor",32);
            iotTypeField=field(cx+(third+gap)*2,y+139,third,"SENSOR",24);
            iotValueField=field(cx,y+193,total,"temperature=22.5C",128);
            pageButton("Register / Update",cx,y+227,140,()->sendIot("REGISTER"));
            pageButton("Apply Control",cx+150,y+227,120,()->sendIot("CONTROL"));
            pageButton("Update Telemetry",cx+280,y+227,130,()->sendIot("TELEMETRY"));
            pageButton("Mark Offline",cx+420,y+227,110,()->sendIot("OFFLINE"));
            pageButton("Remove Device",cx,y+261,120,()->sendIot("REMOVE"));
            pageButton("Refresh",cx+130,y+261,90,()->sendIot("QUERY"));
            pageButton("Clear Offline",cx+230,y+261,110,()->sendIot("CLEAR_OFFLINE"));
        }
        if(selected==ServerRackService.VM_MANAGEMENT){
            int right=x+w-28,gap=16,total=right-cx,third=(total-gap*2)/3;
            vmNameField=field(cx,y+139,third,"web-vm-01",32);
            vmOsField=field(cx+third+gap,y+139,third*2+gap,"VSIA Linux",32);
            vmCpuField=field(cx,y+193,third,"2",2);
            vmMemoryField=field(cx+third+gap,y+193,third,"4096",5);
            vmStorageField=field(cx+(third+gap)*2,y+193,third,"64",4);
            pageButton("Create / Update",cx,y+227,130,()->sendVm("CREATE"));
            pageButton("Start",cx+140,y+227,80,()->sendVm("START"));
            pageButton("Stop",cx+230,y+227,80,()->sendVm("STOP"));
            pageButton("Restart",cx+320,y+227,90,()->sendVm("RESTART"));
            pageButton("Delete",cx+420,y+227,80,()->sendVm("DELETE"));
            pageButton("Refresh",cx,y+261,90,()->sendVm("QUERY"));
        }
        if(selected==ServerRackService.PRP){
            int right=x+w-28,total=right-cx;
            prpPeerField=field(cx,y+181,total,"192.168.1.3",15);
            pageButton(prpEnabled?"PRP   [ENABLED]    Disabled":"PRP   Enabled    [DISABLED]",cx,y+126,total,()->{prpEnabled=!prpEnabled;init();});
            pageButton(prpLaneA?"LAN A   [UP]    Down":"LAN A   Up    [DOWN]",cx,y+227,(total-16)/2,()->{prpLaneA=!prpLaneA;init();});
            pageButton(prpLaneB?"LAN B   [UP]    Down":"LAN B   Up    [DOWN]",cx+(total-16)/2+16,y+227,(total-16)/2,()->{prpLaneB=!prpLaneB;init();});
            pageButton("Apply PRP Configuration",cx,y+261,180,()->sendPrp("CONFIG"));
            pageButton("Refresh Supervision",cx+190,y+261,150,()->sendPrp("QUERY"));
            pageButton("Clear Counters",cx+350,y+261,120,()->sendPrp("CLEAR_COUNTERS"));
        }
        pageButton("Save Services",x+w-137,y+h-29,125,()->save());
    }

    private void syncLegacyServiceFlags(){http=serviceBit(ServerRackService.HTTP);dhcpService=serviceBit(ServerRackService.DHCP);dnsService=serviceBit(ServerRackService.DNS);mail=serviceBit(ServerRackService.EMAIL);}
    private boolean serviceBit(ServerRackService service){return(serviceMask&(1L<<service.ordinal()))!=0;}

    private interface BoolSet {void set(boolean v);}
    private void serviceToggle(String label,int bx,int by,boolean value,BoolSet set){
        pageButton(label+"   "+(value?"[ON]    Off":"On    [OFF]"),bx,by,260,()->{set.set(!value);init();});
    }

    private void initDesktop() {
        if(!desktopPage.isEmpty()){
            pageButton("< Desktop",x+14,y+35,100,()->{desktopPage="";init();});
            String initial=desktopPage.equals("IP Configuration")?"ipconfig":"";
            toolInput=field(x+125,y+82,w-225,initial,256);
            pageButton("Run",x+w-80,y+82,60,()->runDesktopTool()); return;
        }
        String[] tools={"IP Configuration","Terminal","Command Prompt","Web Browser","Email","DNS Lookup","Ping","Text Editor"};
        for(int i=0;i<tools.length;i++){int col=i%4,row=i/4;String tool=tools[i];pageButton(tool,x+35+col*((w-70)/4),y+75+row*95,145,()->{desktopPage=tool;init();});}
    }

    private void initProgramming() {
        pageButton("New",x+10,y+34,50,()->{programInput.setValue("");programOutput="New program.";});
        pageButton("Open",x+64,y+34,50,()->sendProgram("open"));
        pageButton("Save",x+118,y+34,50,()->sendProgram("save"));
        pageButton("Run",x+w-118,y+34,50,()->sendProgram("run"));
        int editorX=x+180;
        programInput=field(editorX,y+92,x+w-editorX-22,"hostname Server0; show config",16384);
    }

    private EditBox field(int bx,int by,int bw,String value,int max){EditBox e=new EditBox(font,bx,by,Math.max(60,bw),18,Component.empty());e.setMaxLength(max);e.setValue(value);addRenderableWidget(e);return e;}

    private void save(){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.SaveConfigPacket(state.pos(),
            name==null?state.displayName():name.getValue(),ip==null?state.ip():ip.getValue(),subnet==null?state.subnet():subnet.getValue(),
            gateway==null?state.gateway():gateway.getValue(),dns==null?state.dns():dns.getValue(),dhcp,http,dnsService,dhcpService,mail,
            ipv6==null?state.ipv6():ipv6.getValue(),parseInt(prefix6,state.ipv6Prefix()),gateway6==null?state.gateway6():gateway6.getValue(),dns6==null?state.dns6():dns6.getValue(),automatic6,parseLong(clockOffset,state.clockOffset()/1000L)*1000L,ptpMode,ptpProfile,serviceMask));}
    private int parseInt(EditBox box,int fallback){try{return box==null?fallback:Integer.parseInt(box.getValue());}catch(Exception e){return fallback;}}
    private long parseLong(EditBox box,long fallback){try{return box==null?fallback:Long.parseLong(box.getValue());}catch(Exception e){return fallback;}}
    private void runDesktopTool(){desktopOutput="Running...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DesktopToolPacket(state.pos(),desktopPage,toolInput==null?"":toolInput.getValue()));}
    public static void acceptDesktopResult(String tool,String result){
        if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen&&screen.desktopPage.equals(tool))screen.desktopOutput=result;
    }
    private void sendDns(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DnsRecordPacket(state.pos(),action,dnsName==null?"":dnsName.getValue(),dnsRecordType,dnsDetail==null?"":dnsDetail.getValue(),parseInt(dnsTtl,300)));}
    public static void acceptDnsResult(String message,String records){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.dnsStatus=message;screen.dnsRecordData=records;}}
    private void sendDhcp(String action,boolean v6){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DhcpPoolPacket(state.pos(),action,poolName.getValue(),v6,poolStart.getValue(),poolEnd.getValue(),poolPrefix.getValue(),poolGateway.getValue(),poolDns.getValue(),parseInt(poolLease,3600),poolExclusions.getValue()));}
    public static void acceptDhcpResult(String message,String data,boolean ipv6){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.dhcpStatus=message;if(ipv6)screen.dhcp6Data=data;else screen.dhcp4Data=data;}}
    private void sendNtp(){serviceStatus="Saving NTP configuration...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.NtpConfigPacket(state.pos(),ntpServer,ntpClient,parseInt(ntpStratumField,state.ntpStratum()),parseInt(ntpPollField,state.ntpPoll()),ntpSourceField.getValue(),parseInt(ntpDriftField,state.clockDrift())));}
    public static void acceptNtpResult(String message,String status,long deviceTime,long lastSync){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.serviceStatus=message;screen.ntpStatus=status;screen.ntpDeviceTime=deviceTime;screen.lastNtpSync=lastSync;}}
    private void sendSyslog(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.SyslogCommandPacket(state.pos(),action,parseInt(syslogMinField,7),syslogAcceptRemote,syslogFacilityField==null?"LOCAL0":syslogFacilityField.getValue(),parseInt(syslogSeverityField,6),syslogMessageField==null?"":syslogMessageField.getValue()));}
    public static void acceptSyslogResult(String message,String data,int minimumSeverity,boolean acceptRemote){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.syslogStatus=message;screen.syslogData=data;screen.syslogAcceptRemote=acceptRemote;if(screen.syslogMinField!=null)screen.syslogMinField.setValue(Integer.toString(minimumSeverity));}}
    private void sendAaa(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.AaaCommandPacket(state.pos(),action,aaaUserField==null?"":aaaUserField.getValue(),aaaPasswordField==null?"":aaaPasswordField.getValue(),parseInt(aaaPrivilegeField,1),aaaUserEnabled,aaaServiceField==null?"LOGIN":aaaServiceField.getValue()));}
    public static void acceptAaaResult(String message,String users,String accounting){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.aaaStatus=message;screen.aaaUsers=users;screen.aaaAccounting=accounting;}}
    private void sendRadius(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.RadiusCommandPacket(state.pos(),action,radiusNameField==null?"":radiusNameField.getValue(),radiusAddressField==null?"":radiusAddressField.getValue(),radiusSecretField==null?"":radiusSecretField.getValue(),radiusClientEnabled,radiusUserField==null?"":radiusUserField.getValue(),radiusPasswordField==null?"":radiusPasswordField.getValue(),parseInt(radiusPrivilegeField,1)));}
    public static void acceptRadiusResult(String message,String clients,String events){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.radiusStatus=message;screen.radiusClients=clients;screen.radiusEvents=events;}}
    private void sendIot(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.IotCommandPacket(state.pos(),action,iotIdField==null?"":iotIdField.getValue(),iotNameField==null?"":iotNameField.getValue(),iotTypeField==null?"":iotTypeField.getValue(),iotValueField==null?"":iotValueField.getValue()));}
    public static void acceptIotResult(String message,String devices){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.iotStatus=message;screen.iotDevices=devices;}}
    private void sendVm(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.VmCommandPacket(state.pos(),action,vmNameField==null?"":vmNameField.getValue(),vmOsField==null?"":vmOsField.getValue(),parseInt(vmCpuField,2),parseInt(vmMemoryField,4096),parseInt(vmStorageField,64)));}
    public static void acceptVmResult(String message,String machines){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.vmStatus=message;screen.virtualMachines=machines;}}
    private void sendPrp(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.PrpCommandPacket(state.pos(),action,prpEnabled,prpLaneA,prpLaneB,prpPeerField==null?"":prpPeerField.getValue()));}
    public static void acceptPrpResult(String message,String status){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.prpStatus=message;screen.prpData=status;String[] p=status.split("\\t",-1);if(p.length>=4){screen.prpEnabled=p[0].equals("ENABLED");screen.prpLaneA=p[1].equals("UP");screen.prpLaneB=p[2].equals("UP");if(screen.prpPeerField!=null)screen.prpPeerField.setValue(p[3]);}}}
    private void sendHttp(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.HttpFileCommandPacket(state.pos(),action,httpFileNameField==null?"":httpFileNameField.getValue(),httpContentField==null?"":httpContentField.getValue(),httpReadable,httpWritable,https,parseInt(httpPortField,80),parseInt(httpsPortField,443)));}
    private void importHttpClipboard(){String value=net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();if(value.length()>32768)value=value.substring(0,32768);httpContentField.setValue(value);httpStatus="Clipboard imported. Press Save to store it on the rack.";}
    public static void acceptHttpFileResult(String message,String files,String filename,String content,boolean readable,boolean writable,boolean secure,int httpPort,int httpsPort){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.httpStatus=message;screen.httpFiles=files;screen.httpReadable=readable;screen.httpWritable=writable;screen.https=secure;if(screen.httpPortField!=null)screen.httpPortField.setValue(Integer.toString(httpPort));if(screen.httpsPortField!=null)screen.httpsPortField.setValue(Integer.toString(httpsPort));if(!filename.isEmpty()&&screen.httpFileNameField!=null){screen.httpFileNameField.setValue(filename);screen.httpContentField.setValue(content);}}}
    private void sendProgram(String action){programOutput=action.equals("run")?"Running...":"Working...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.ProgramPacket(state.pos(),action,programInput==null?"":programInput.getValue()));}
    public static void acceptProgramResult(String source,String result){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){if(screen.programInput!=null)screen.programInput.setValue(source);screen.programOutput=result;}}

    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderBackground(g); g.fill(x,y,x+w,y+h,0xFF181818); g.fill(x+2,y+28,x+w-2,y+h-2,0xFF202020);
        g.hLine(x+2,x+w-2,y+27,0xFF777777);
        if(tab==Tab.CONFIG||tab==Tab.SERVICES||tab==Tab.PHYSICAL) {g.fill(x+5,y+31,x+side,y+h-5,0xFF292929);g.vLine(x+side,y+31,y+h-5,0xFF888888);}
        if(tab==Tab.DESKTOP&&!desktopPage.isEmpty()) g.fill(x+8,y+61,x+w-8,y+h-8,0xFF101820);
        if(tab==Tab.DESKTOP&&desktopPage.isEmpty()) g.fill(x+5,y+31,x+w-5,y+h-5,0xFF50B7CE);
        if(tab==Tab.PROGRAMMING) {g.fill(x+7,y+58,x+w-7,y+h-118,0xFFF3F3F3);g.fill(x+7,y+h-112,x+w-7,y+h-8,0xFF080808);}
        switch(tab){case PHYSICAL->physical(g);case CONFIG->config(g);case SERVICES->services(g);case DESKTOP->desktop(g);case PROGRAMMING->programming(g);case ATTRIBUTES->attributes(g);}
        super.render(g,mx,my,pt);
    }

    private void title(GuiGraphics g,String t){g.drawCenteredString(font,t,x+side+(w-side)/2,y+38,0xFFFFFF);}
    private void physical(GuiGraphics g){
        title(g,"Physical Device View");g.drawCenteredString(font,"MODULES",x+side/2,y+38,0xFFFFFF);
        String[] mods={"WMP300N","PT-HOST-NM-1CE","PT-HOST-NM-1CFE","PT-HOST-NM-1CGE","PT-HOST-NM-1FFE","PT-HOST-NM-1W"};
        for(int i=0;i<mods.length;i++)g.drawCenteredString(font,mods[i],x+side/2,y+62+i*18,0xDDDDDD);
        int px=x+side+45,py=y+75,pw=Math.min(500,w-side-80),ph=Math.min(250,h-115);
        g.fill(px,py,px+pw,py+ph,0xFF454545);g.fill(px+25,py+25,px+pw-25,py+ph-25,0xFF111111);
        g.drawCenteredString(font,"VSIA SERVER RACK - 3D device preview",px+pw/2,py+ph/2-5,0x80FF80);
        g.drawString(font,"Power: ON",px+18,py+ph+12,0x80FF80);g.drawString(font,"Door: OPEN",px+110,py+ph+12,0xFFFFFF);
    }
    private void config(GuiGraphics g){title(g,configPage);int cx=x+side+36;
        if(configPage.equals("Settings")){
            g.drawString(font,"Display Name",cx,y+70,0xFFFFFF);
            sectionLine(g,cx,y+99,"IPv4 Gateway and DNS");
            g.drawString(font,"Addressing Mode",cx,y+116,0xFFFFFF);
            g.drawString(font,"Default Gateway",cx,y+156,0xFFFFFF);
            g.drawString(font,"DNS Server",cx,y+184,0xFFFFFF);
            sectionLine(g,cx,y+205,"IPv6 Gateway and DNS");g.drawString(font,"Addressing Mode",cx,y+222,0xFFFFFF);g.drawString(font,"Default Gateway",cx,y+250,0xFFFFFF);g.drawString(font,"DNS Server",cx,y+278,0xFFFFFF);
            g.drawString(font,"Device Clock: "+java.time.Instant.ofEpochMilli(state.deviceTime()+(System.currentTimeMillis()-openedAtMillis)).toString(),cx,y+312,0xBBBBBB);
        } else if(configPage.equals("FastEthernet0")) {
            sectionLine(g,cx,y+61,"Interface Settings");
            g.drawString(font,"Interface Status",cx,y+75,0xFFFFFF);
            g.drawString(font,"IPv4 Address",cx,y+117,0xFFFFFF);
            g.drawString(font,"Subnet Mask",cx,y+145,0xFFFFFF);
            g.drawString(font,"IPv6 Address",cx,y+201,0xFFFFFF);g.drawString(font,"Prefix Length",cx,y+229,0xFFFFFF);
            g.drawString(font,"MAC Address",cx,y+180,0xFFFFFF);
            g.drawString(font,"Automatically managed by the device",cx+150,y+180,0xBBBBBB);
        } else {sectionLine(g,cx,y+65,"Device Clock and Precision Time Protocol");g.drawString(font,"Clock Offset (seconds)",cx,y+109,0xFFFFFF);g.drawString(font,"PTP clients synchronize to the nearest loaded grandmaster using the same profile.",cx,y+211,0xBBBBBB);}}
    private void services(GuiGraphics g){title(g,servicePage);int cx=x+side+24;
        if(servicePage.equals("HTTP")){renderHttpService(g,cx);return;}
        if(servicePage.equals("EMAIL")){renderEmailService(g,cx);return;}
        if(servicePage.equals("NTP")){renderNtpService(g,cx);return;}
        if(servicePage.equals("SYSLOG")){renderSyslogService(g,cx);return;}
        if(servicePage.equals("AAA")){renderAaaService(g,cx);return;}
        if(servicePage.equals("Radius EAP")){renderRadiusService(g,cx);return;}
        if(servicePage.equals("IoT")){renderIotService(g,cx);return;}
        if(servicePage.equals("VM Management")){renderVmService(g,cx);return;}
        if(servicePage.equals("PRP")){renderPrpService(g,cx);return;}
        sectionLine(g,cx,y+103,"Service Configuration");
        if(servicePage.equals("HTTP")){g.drawString(font,"Hosted Files",cx,y+130,0xCCCCCC);table(g,cx,y+148,"File Name","Action",new String[][]{{"index.html","Edit","Delete"},{"styles.css","Edit","Delete"},{"script.js","Edit","Delete"}});}
        else if(servicePage.equals("DNS")){g.drawString(font,"Name",cx,y+117,0xCCCCCC);g.drawString(font,"Detail",cx,y+145,0xCCCCCC);g.drawString(font,"TTL",cx,y+173,0xCCCCCC);g.drawString(font,dnsStatus,cx+305,y+201,0xAADDFF);g.drawString(font,"Resource Records",cx,y+231,0xCCCCCC);table(g,cx,y+245,"Name / Type","Detail / TTL",dnsRows());}
        else if(servicePage.equals("DHCP")||servicePage.equals("DHCPv6")){boolean v6=servicePage.equals("DHCPv6");g.drawString(font,"Pool / Start / End",cx,y+117,0xCCCCCC);g.drawString(font,(v6?"Prefix":"Mask")+" / Gateway / DNS",cx,y+145,0xCCCCCC);g.drawString(font,"Lease Seconds / Exclusions",cx,y+173,0xCCCCCC);g.drawString(font,dhcpStatus,cx+320,y+201,0xAADDFF);table(g,cx,y+245,"Pools and Leases","Details",dhcpRows(v6?dhcp6Data:dhcp4Data));}
        else if(servicePage.equals("EMAIL")){g.drawString(font,"Mail Domain: vsia-net.com",cx,y+130,0xCCCCCC);table(g,cx,y+148,"User","Mailbox",new String[][]{{"admin","Open","Delete"},{"player","Open","Delete"}});}
        else{ServerRackService service=ServerRackService.byDisplayName(servicePage);g.drawString(font,"Default Port: "+(service.defaultPort()==0?"Protocol-managed":service.defaultPort()),cx,y+130,0xCCCCCC);g.drawString(font,"Configuration module: ready for the "+service.displayName()+" service milestone",cx,y+150,0xBBBBBB);g.drawString(font,"This switch is persistent. Protocol rules, records, accounts, and logs are added in the dedicated milestone.",cx,y+170,0x999999);}}
    private void desktop(GuiGraphics g){if(desktopPage.isEmpty())return;g.drawCenteredString(font,desktopPage,x+w/2,y+45,0xFFFFFF);g.drawString(font,inputLabel(),x+125,y+70,0xCCCCCC);g.drawString(font,"Output",x+25,y+120,0xFFFFFF);g.fill(x+25,y+136,x+w-25,y+h-25,0xFF080808);int lineY=y+145;for(String raw:desktopOutput.split("\\n",-1)){for(FormattedCharSequence line:font.split(Component.literal(raw),w-70)){g.drawString(font,line,x+35,lineY,0xB8FFB8);lineY+=12;if(lineY>y+h-38)return;}}}
    private String inputLabel(){return switch(desktopPage){case "Ping"->"Target IPv4 address";case "DNS Lookup"->"Domain name";case "Web Browser"->"URL or IPv4 address";case "Email"->"Mailbox address";case "Terminal","Command Prompt"->"Command";default->"Input";};}
    private void programming(GuiGraphics g){
        int editorX=x+180;
        g.fill(x+8,y+59,editorX-10,y+h-119,0xFFE2E2E2);
        g.vLine(editorX-9,y+59,y+h-119,0xFF999999);
        g.drawString(font,"Project",x+18,y+70,0xFF222222);
        g.drawString(font,"server-config",x+18,y+94,0xFF444444);
        g.drawString(font,"Configuration Script",editorX,y+70,0xFF222222);
        g.drawString(font,"Separate commands with semicolons",editorX,y+81,0xFF666666);
        g.drawString(font,"Console Output",x+18,y+h-105,0xFFCCCCCC);
        int lineY=y+h-86;
        for(String raw:programOutput.split("\\n",-1)){
            for(FormattedCharSequence line:font.split(Component.literal(raw),w-48)){
                g.drawString(font,line,x+20,lineY,0xFFB8FFB8);
                lineY+=12;
                if(lineY>y+h-18)return;
            }
        }
    }
    private void attributes(GuiGraphics g){g.drawString(font,"Device Attributes",x+16,y+39,0xFFFFFF);String[][] a={{"Mean Time Between Failures","61,320 hours"},{"Cost","2,000"},{"Power Source","Internal"},{"Rack Units","3U"},{"Power Consumption","200 W"},{"Device Model","VSIA Server Rack"},{"IPv4 Address",state.ip()},{"IPv6 Address",state.ipv6()+"/"+state.ipv6Prefix()},{"PTP",state.ptpMode()+" / "+state.ptpProfile()},{"World Position",state.pos().toShortString()}};table(g,x+20,y+62,"Attribute","Value",a);}
    private void sectionLine(GuiGraphics g,int sx,int sy,String text){g.drawString(font,text,sx,sy,0xDDDDDD);int start=sx+font.width(text)+8;g.hLine(start,x+w-24,sy+4,0xFF555555);}
    private String[][] dnsRows(){if(dnsRecordData==null||dnsRecordData.isBlank())return new String[][]{{"No records","",""}};String[] lines=dnsRecordData.strip().split("\\n");int count=Math.min(lines.length,12);String[][] rows=new String[count][3];for(int i=0;i<count;i++){String[] p=lines[i].split("\\t",4);rows[i][0]=p.length>1?p[0]+"  ["+p[1]+"]":lines[i];rows[i][1]=p.length>2?p[2]:"";rows[i][2]=p.length>3?"TTL "+p[3]:"";}return rows;}
    private void table(GuiGraphics g,int tx,int ty,String firstHeader,String secondHeader,String[][] rows){int tw=w-(tx-x)-25;g.fill(tx,ty,tx+tw,ty+20,0xFF464646);g.drawString(font,firstHeader,tx+8,ty+6,0xFFFFFF);g.drawString(font,secondHeader,tx+tw/2,ty+6,0xFFFFFF);for(int i=0;i<rows.length;i++){int ry=ty+21+i*20;g.fill(tx,ry,tx+tw,ry+19,(i&1)==0?0xFF292929:0xFF252525);g.drawString(font,rows[i][0],tx+8,ry+5,0xEEEEEE);if(rows[i].length>1)g.drawString(font,rows[i][1],tx+tw/2,ry+5,0xDDDDDD);if(rows[i].length>2)g.drawString(font,rows[i][2],tx+tw-75,ry+5,0xDDDDDD);}}
    private String[][] dhcpRows(String data){if(data==null||data.isBlank())return new String[][]{{"No data","",""}};String[] lines=data.strip().split("\\n");List<String[]> rows=new ArrayList<>();String section="";for(String line:lines){if(line.equals("POOLS")||line.equals("LEASES")){section=line;continue;}String[] p=line.split("\\t",-1);if(section.equals("POOLS")&&p.length>=7)rows.add(new String[]{"Pool "+p[0],p[1]+" - "+p[2],"Lease "+p[6]+"s"});else if(section.equals("LEASES")&&p.length>=4)rows.add(new String[]{"Lease "+p[0],p[1],p[2]});if(rows.size()>=12)break;}return rows.isEmpty()?new String[][]{{"No active entries","",""}}:rows.toArray(new String[0][]);}
    private void renderHttpService(GuiGraphics g,int cx){int right=x+w-28,half=(right-cx-16)/2;sectionLine(g,cx,y+98,"Web Service Settings");g.drawString(font,"HTTP Port",cx,y+115,0xCCCCCC);g.drawString(font,"HTTPS Port",cx+half+16,y+115,0xCCCCCC);sectionLine(g,cx,y+153,"Interactive File Editor");g.drawString(font,"File Name",cx,y+169,0xCCCCCC);g.drawString(font,"Content (paste text, then Save)",cx,y+223,0xCCCCCC);sectionLine(g,cx,y+370,"Hosted Files");int top=y+386;g.fill(cx,top,right,top+20,0xFF464646);g.drawString(font,"File Name",cx+8,top+6,0xFFFFFF);g.drawString(font,"Permissions",cx+half,top+6,0xFFFFFF);g.drawString(font,"Size",right-75,top+6,0xFFFFFF);String[] lines=httpFiles==null?new String[0]:httpFiles.strip().split("\\n");for(int i=0;i<Math.min(lines.length,7);i++){String[] p=lines[i].split("\\t",-1);int ry=top+21+i*19;g.fill(cx,ry,right,ry+18,(i&1)==0?0xFF292929:0xFF252525);g.drawString(font,p[0],cx+8,ry+5,0xEEEEEE);g.drawString(font,p.length>2?((p[1].equals("true")?"R":"-")+(p[2].equals("true")?"W":"-")):"--",cx+half,ry+5,0xDDDDDD);g.drawString(font,p.length>3?p[3]+" B":"",right-75,ry+5,0xDDDDDD);}g.drawString(font,httpStatus,cx,y+h-48,0x88CCFF);}
    private void renderEmailService(GuiGraphics g,int cx){
        int right=x+w-28;
        int contentWidth=right-cx;
        int gap=16;
        int quotaWidth=Math.max(90,contentWidth/8);
        int userWidth=Math.max(170,contentWidth/4);
        int passwordWidth=contentWidth-userWidth-quotaWidth-gap*2;
        int passwordX=cx+userWidth+gap;
        int quotaX=passwordX+passwordWidth+gap;
        sectionLine(g,cx,y+98,"Mail Services");
        g.drawString(font,"SMTP Service",cx,y+113,0xFFFFFF);
        g.drawString(font,"POP3 Service",cx+(contentWidth+gap)/2,y+113,0xFFFFFF);
        sectionLine(g,cx,y+130,"Domain Configuration");
        g.drawString(font,"Domain Name",cx,y+140,0xBBBBBB);
        sectionLine(g,cx,y+184,"User Setup");
        g.drawString(font,"Username",cx,y+200,0xBBBBBB);
        g.drawString(font,"Password",passwordX,y+200,0xBBBBBB);
        g.drawString(font,"Quota",quotaX,y+200,0xBBBBBB);
        sectionLine(g,cx,y+282,"Mailbox Accounts");
        int top=y+299;
        g.fill(cx,top,right,top+20,0xFF464646);
        int quotaColumn=cx+(int)(contentWidth*0.62F);
        int messageColumn=cx+(int)(contentWidth*0.78F);
        g.drawString(font,"Mailbox Account",cx+10,top+6,0xFFFFFF);
        g.drawString(font,"Quota",quotaColumn,top+6,0xFFFFFF);
        g.drawString(font,"Messages",messageColumn,top+6,0xFFFFFF);
        String[][] users={{"admin@vsia-net.com","100","0"},{"player@vsia-net.com","100","0"}};
        for(int i=0;i<users.length;i++){
            int ry=top+21+i*22;
            g.fill(cx,ry,right,ry+21,(i&1)==0?0xFF292929:0xFF252525);
            g.drawString(font,users[i][0],cx+10,ry+6,0xEEEEEE);
            g.drawString(font,users[i][1],quotaColumn,ry+6,0xDDDDDD);
            g.drawString(font,users[i][2],messageColumn,ry+6,0xDDDDDD);
        }
        g.drawString(font,serviceStatus,cx,y+h-62,0x88CCFF);
    }
    private void renderNtpService(GuiGraphics g,int cx){
        int right=x+w-28;
        int gap=24;
        int columnWidth=Math.max(180,(right-cx-gap)/2);
        int secondColumn=cx+columnWidth+gap;
        sectionLine(g,cx,y+98,"Network Time Protocol");
        g.drawString(font,"NTP Roles",cx,y+113,0xDDDDDD);
        g.drawString(font,"Stratum",cx,y+160,0xFFFFFF);
        g.drawString(font,"Valid range: 1-15",cx,y+170,0x999999);
        g.drawString(font,"Poll Interval",secondColumn,y+160,0xFFFFFF);
        g.drawString(font,"Valid range: 16-4096 seconds",secondColumn,y+170,0x999999);
        g.drawString(font,"Preferred Source IPv4",cx,y+214,0xFFFFFF);
        g.drawString(font,"Leave blank to select the best available source",cx,y+224,0x999999);
        g.drawString(font,"Clock Drift",secondColumn,y+214,0xFFFFFF);
        g.drawString(font,"Range: -500 to 500 ppm",secondColumn,y+224,0x999999);
        sectionLine(g,cx,y+307,"Clock Status");
        g.drawString(font,"Device Time (UTC)",cx,y+326,0xBBBBBB);
        g.drawString(font,java.time.Instant.ofEpochMilli(ntpDeviceTime+(System.currentTimeMillis()-openedAtMillis)).toString(),cx+130,y+326,0xFFFFFF);
        g.drawString(font,"Synchronization",cx,y+346,0xBBBBBB);
        g.drawString(font,ntpStatus,cx+130,y+346,0xB8FFB8);
        g.drawString(font,"Last Sync",cx,y+366,0xBBBBBB);
        g.drawString(font,lastNtpSync==0?"Never":java.time.Instant.ofEpochMilli(lastNtpSync).toString(),cx+130,y+366,0xCCCCCC);
        g.drawString(font,"PTP Priority",cx,y+386,0xBBBBBB);
        g.drawString(font,ptpMode+" / "+ptpProfile,cx+130,y+386,0xCCCCCC);
        g.drawString(font,serviceStatus,cx,y+h-62,0x88CCFF);
    }
    private void renderSyslogService(GuiGraphics g,int cx){
        int right=x+w-28;
        int gap=20;
        int columnWidth=Math.max(180,(right-cx-gap)/2);
        int secondColumn=cx+columnWidth+gap;
        sectionLine(g,cx,y+98,"Syslog Service");
        g.drawString(font,"Minimum Severity",cx,y+124,0xFFFFFF);
        g.drawString(font,"0 Emergency - 7 Debug",cx,y+134,0x999999);
        g.drawString(font,"Remote Message Reception",secondColumn,y+124,0xFFFFFF);
        g.drawString(font,"Facility",cx,y+178,0xFFFFFF);
        g.drawString(font,"Example: LOCAL0, AUTH, DAEMON",cx,y+188,0x999999);
        g.drawString(font,"Test Severity",secondColumn,y+178,0xFFFFFF);
        g.drawString(font,"Test Message",cx,y+232,0xFFFFFF);
        sectionLine(g,cx,y+325,"Stored Messages");
        g.drawString(font,syslogStatus,cx,y+340,0x88CCFF);
        int top=y+356;
        g.fill(cx,top,right,top+20,0xFF464646);
        g.drawString(font,"Time",cx+6,top+6,0xFFFFFF);
        g.drawString(font,"Source / Facility",cx+145,top+6,0xFFFFFF);
        g.drawString(font,"Severity / Message",cx+330,top+6,0xFFFFFF);
        if(syslogData==null||syslogData.isBlank()){g.drawString(font,"No syslog entries",cx+8,top+27,0xAAAAAA);return;}
        String[] lines=syslogData.strip().split("\\n");
        int count=Math.min(lines.length,Math.max(1,(y+h-top-78)/20));
        for(int i=0;i<count;i++){
            String[] p=lines[lines.length-count+i].split("\\t",5);
            int row=top+21+i*20;
            g.fill(cx,row,right,row+19,(i&1)==0?0xFF292929:0xFF252525);
            String time=p.length>0?java.time.Instant.ofEpochMilli(parseLongText(p[0])).toString():"";
            g.drawString(font,time.length()>19?time.substring(0,19):time,cx+6,row+5,0xDDDDDD);
            g.drawString(font,(p.length>2?p[1]+" / "+p[2]:""),cx+145,row+5,0xDDDDDD);
            g.drawString(font,(p.length>4?severityName(parseIntText(p[3]))+" / "+p[4]:""),cx+330,row+5,0xEEEEEE);
        }
    }
    private long parseLongText(String value){try{return Long.parseLong(value);}catch(Exception e){return 0;}}
    private int parseIntText(String value){try{return Integer.parseInt(value);}catch(Exception e){return 7;}}
    private String severityName(int value){return switch(value){case 0->"Emergency";case 1->"Alert";case 2->"Critical";case 3->"Error";case 4->"Warning";case 5->"Notice";case 6->"Informational";default->"Debug";};}
    private void renderAaaService(GuiGraphics g,int cx){
        int right=x+w-28;
        int gap=20;
        int columnWidth=Math.max(180,(right-cx-gap)/2);
        int secondColumn=cx+columnWidth+gap;
        sectionLine(g,cx,y+98,"Authentication, Authorization and Accounting");
        g.drawString(font,"Username",cx,y+118,0xFFFFFF);
        g.drawString(font,"Password",secondColumn,y+118,0xFFFFFF);
        g.drawString(font,"Required / Assigned Privilege",cx,y+172,0xFFFFFF);
        g.drawString(font,"Range: 0-15",cx,y+182,0x999999);
        g.drawString(font,"Service",secondColumn,y+172,0xFFFFFF);
        g.drawString(font,"Examples: LOGIN, CONSOLE, CONFIG",secondColumn,y+182,0x999999);
        g.drawString(font,aaaStatus,cx,y+294,0x88CCFF);
        int usersTop=y+312;
        g.fill(cx,usersTop,right,usersTop+20,0xFF464646);
        g.drawString(font,"AAA Users",cx+8,usersTop+6,0xFFFFFF);
        g.drawString(font,"Privilege",cx+220,usersTop+6,0xFFFFFF);
        g.drawString(font,"Status",cx+330,usersTop+6,0xFFFFFF);
        int row=usersTop+21;
        if(aaaUsers==null||aaaUsers.isBlank()){
            g.fill(cx,row,right,row+19,0xFF292929);
            g.drawString(font,"No AAA users loaded",cx+8,row+5,0xAAAAAA);
            row+=20;
        }
        else for(String line:aaaUsers.strip().split("\\n")){String[] p=line.split("\\t",3);g.fill(cx,row,right,row+19,0xFF292929);g.drawString(font,p[0],cx+8,row+5,0xEEEEEE);g.drawString(font,p.length>1?p[1]:"",cx+220,row+5,0xDDDDDD);g.drawString(font,p.length>2&&Boolean.parseBoolean(p[2])?"Enabled":"Disabled",cx+330,row+5,0xDDDDDD);row+=20;if(row>y+h-190)break;}
        int accountingTop=row+14;
        g.fill(cx,accountingTop,right,accountingTop+20,0xFF464646);
        g.drawString(font,"Recent Accounting",cx+8,accountingTop+6,0xFFFFFF);
        g.drawString(font,"User / Service",cx+170,accountingTop+6,0xFFFFFF);
        g.drawString(font,"Result / Source",cx+365,accountingTop+6,0xFFFFFF);
        row=accountingTop+21;
        if(aaaAccounting==null||aaaAccounting.isBlank()){
            g.fill(cx,row,right,row+19,0xFF292929);
            g.drawString(font,"No AAA accounting records",cx+8,row+5,0xAAAAAA);
        }
        else {String[] lines=aaaAccounting.strip().split("\\n");int count=Math.min(lines.length,5);for(int i=lines.length-count;i<lines.length;i++){String[] p=lines[i].split("\\t",6);g.fill(cx,row,right,row+19,(i&1)==0?0xFF292929:0xFF252525);String time=p.length>0?java.time.Instant.ofEpochMilli(parseLongText(p[0])).toString():"";g.drawString(font,time.length()>19?time.substring(0,19):time,cx+8,row+5,0xDDDDDD);g.drawString(font,p.length>2?p[1]+" / "+p[2]:"",cx+170,row+5,0xDDDDDD);g.drawString(font,p.length>5?(Boolean.parseBoolean(p[4])?"Accepted":"Rejected")+" / "+p[5]:"",cx+365,row+5,0xEEEEEE);row+=20;if(row>y+h-36)break;}}
    }
    private void renderRadiusService(GuiGraphics g,int cx){
        int right=x+w-28,gap=16,total=right-cx,third=(total-gap*2)/3;
        sectionLine(g,cx,y+98,"RADIUS EAP Authentication");
        g.drawString(font,"NAS Client Name",cx,y+118,0xFFFFFF);
        g.drawString(font,"NAS IPv4 Address",cx+third+gap,y+118,0xFFFFFF);
        g.drawString(font,"Shared Secret",cx+(third+gap)*2,y+118,0xFFFFFF);
        g.drawString(font,"AAA Username",cx,y+172,0xFFFFFF);
        g.drawString(font,"AAA Password",cx+third+gap,y+172,0xFFFFFF);
        g.drawString(font,"Required Privilege (0-15)",cx+(third+gap)*2,y+172,0xFFFFFF);
        g.drawString(font,radiusStatus,cx,y+294,0x88CCFF);
        int top=y+312;
        g.fill(cx,top,right,top+20,0xFF464646);
        g.drawString(font,"Registered NAS Clients",cx+8,top+6,0xFFFFFF);
        g.drawString(font,"Address",cx+230,top+6,0xFFFFFF);
        g.drawString(font,"Status",cx+390,top+6,0xFFFFFF);
        int row=top+21;
        if(radiusClients==null||radiusClients.isBlank()){
            g.fill(cx,row,right,row+19,0xFF292929);
            g.drawString(font,"No registered NAS clients",cx+8,row+5,0xAAAAAA);
            row+=20;
        } else for(String line:radiusClients.strip().split("\\n")){String[] p=line.split("\\t",3);g.fill(cx,row,right,row+19,0xFF292929);g.drawString(font,p[0],cx+8,row+5,0xEEEEEE);g.drawString(font,p.length>1?p[1]:"",cx+230,row+5,0xDDDDDD);g.drawString(font,p.length>2&&Boolean.parseBoolean(p[2])?"Enabled":"Disabled",cx+390,row+5,0xDDDDDD);row+=20;if(row>y+h-175)break;}
        int eventTop=row+14;
        g.fill(cx,eventTop,right,eventTop+20,0xFF464646);
        g.drawString(font,"Recent RADIUS Events",cx+8,eventTop+6,0xFFFFFF);
        row=eventTop+21;
        if(radiusEvents==null||radiusEvents.isBlank()){
            g.fill(cx,row,right,row+19,0xFF292929);
            g.drawString(font,"No RADIUS authentication events",cx+8,row+5,0xAAAAAA);
        }
        else {String[] lines=radiusEvents.strip().split("\\n");int count=Math.min(lines.length,5);for(int i=lines.length-count;i<lines.length;i++){String[] p=lines[i].split("\\t",4);g.fill(cx,row,right,row+19,(i&1)==0?0xFF292929:0xFF252525);String time=p.length>0?java.time.Instant.ofEpochMilli(parseLongText(p[0])).toString():"";g.drawString(font,time.length()>19?time.substring(0,19):time,cx+8,row+5,0xDDDDDD);g.drawString(font,p.length>2?p[1]+" / "+p[2]:"",cx+170,row+5,0xDDDDDD);g.drawString(font,p.length>3?p[3]:"",cx+390,row+5,0xEEEEEE);row+=20;if(row>y+h-36)break;}}
    }
    private void renderIotService(GuiGraphics g,int cx){
        int right=x+w-28;
        int gap=16;
        int third=(right-cx-gap*2)/3;
        sectionLine(g,cx,y+98,"IoT Device Registration and Control");
        g.drawString(font,"Device ID",cx,y+118,0xFFFFFF);
        g.drawString(font,"Display Name",cx+third+gap,y+118,0xFFFFFF);
        g.drawString(font,"Device Type",cx+(third+gap)*2,y+118,0xFFFFFF);
        g.drawString(font,"Control State or Telemetry",cx,y+172,0xFFFFFF);
        g.drawString(font,iotStatus,cx,y+294,0x88CCFF);
        int top=y+312;
        g.fill(cx,top,right,top+20,0xFF464646);
        g.drawString(font,"Device / Type",cx+8,top+6,0xFFFFFF);
        g.drawString(font,"Connectivity",cx+235,top+6,0xFFFFFF);
        g.drawString(font,"State / Telemetry",cx+375,top+6,0xFFFFFF);
        int row=top+21;
        if(iotDevices==null||iotDevices.isBlank()){
            g.fill(cx,row,right,row+19,0xFF292929);
            g.drawString(font,"No IoT devices registered",cx+8,row+5,0xAAAAAA);
            return;
        }
        for(String line:iotDevices.strip().split("\\n")){
            String[] p=line.split("\\t",7);
            g.fill(cx,row,right,row+19,(row/20&1)==0?0xFF292929:0xFF252525);
            g.drawString(font,p.length>2?p[1]+" ["+p[2]+"]":p[0],cx+8,row+5,0xEEEEEE);
            g.drawString(font,p.length>3&&Boolean.parseBoolean(p[3])?"Online":"Offline",cx+235,row+5,p.length>3&&Boolean.parseBoolean(p[3])?0x88FF88:0xFF8888);
            g.drawString(font,p.length>5?p[4]+" / "+p[5]:"",cx+375,row+5,0xDDDDDD);
            row+=20;if(row>y+h-42)break;
        }
    }
    private void renderVmService(GuiGraphics g,int cx){
        int right=x+w-28,gap=16,third=(right-cx-gap*2)/3;
        sectionLine(g,cx,y+98,"Virtual Machine Management");
        g.drawString(font,"VM Name",cx,y+118,0xFFFFFF);
        g.drawString(font,"Operating System",cx+third+gap,y+118,0xFFFFFF);
        g.drawString(font,"CPU Cores (1-16)",cx,y+172,0xFFFFFF);
        g.drawString(font,"Memory MB (256-32768)",cx+third+gap,y+172,0xFFFFFF);
        g.drawString(font,"Storage GB (1-1024)",cx+(third+gap)*2,y+172,0xFFFFFF);
        g.drawString(font,"Rack Capacity: 16 CPU cores / 32768 MB RAM / 1024 GB storage",cx,y+294,0xAAAAAA);
        g.drawString(font,vmStatus,cx,y+306,0x88CCFF);
        int top=y+324;
        g.fill(cx,top,right,top+20,0xFF464646);
        g.drawString(font,"Virtual Machine / OS",cx+8,top+6,0xFFFFFF);
        g.drawString(font,"Resources",cx+255,top+6,0xFFFFFF);
        g.drawString(font,"State / Console",cx+430,top+6,0xFFFFFF);
        int row=top+21;
        if(virtualMachines==null||virtualMachines.isBlank()){
            g.fill(cx,row,right,row+19,0xFF292929);
            g.drawString(font,"No virtual machines configured",cx+8,row+5,0xAAAAAA);
            return;
        }
        for(String line:virtualMachines.strip().split("\\n")){
            String[] p=line.split("\\t",7);
            g.fill(cx,row,right,row+19,(row/20&1)==0?0xFF292929:0xFF252525);
            g.drawString(font,p.length>1?p[0]+" ["+p[1]+"]":p[0],cx+8,row+5,0xEEEEEE);
            g.drawString(font,p.length>4?p[2]+" CPU / "+p[3]+" MB / "+p[4]+" GB":"",cx+255,row+5,0xDDDDDD);
            g.drawString(font,p.length>6?p[5]+" / "+p[6]:"",cx+430,row+5,p.length>5&&p[5].equals("RUNNING")?0x88FF88:0xCCCCCC);
            row+=20;if(row>y+h-42)break;
        }
    }
    private void renderPrpService(GuiGraphics g,int cx){
        int right=x+w-28;
        sectionLine(g,cx,y+98,"Parallel Redundancy Protocol");
        g.drawString(font,"PRP sends identical frames over LAN A and LAN B with zero-time recovery.",cx,y+112,0xAAAAAA);
        g.drawString(font,"Redundancy Peer IPv4 Address",cx,y+160,0xFFFFFF);
        g.drawString(font,prpStatus,cx,y+294,0x88CCFF);
        int top=y+322;
        g.fill(cx,top,right,top+20,0xFF464646);
        g.drawString(font,"Supervision",cx+8,top+6,0xFFFFFF);
        g.drawString(font,"Value",cx+260,top+6,0xFFFFFF);
        String[] p=prpData.split("\\t",-1);
        String[][] rows={{"PRP Service",p.length>0?p[0]:"Unknown"},{"LAN A",p.length>1?p[1]:"Unknown"},{"LAN B",p.length>2?p[2]:"Unknown"},{"Redundancy Peer",p.length>3&&!p[3].isBlank()?p[3]:"Not configured"},{"Frames Transmitted",p.length>4?p[4]:"0"},{"Duplicate Frames Discarded",p.length>5?p[5]:"0"}};
        for(int i=0;i<rows.length;i++){int row=top+21+i*20;g.fill(cx,row,right,row+19,(i&1)==0?0xFF292929:0xFF252525);g.drawString(font,rows[i][0],cx+8,row+5,0xEEEEEE);int color=(rows[i][1].equals("UP")||rows[i][1].equals("ENABLED"))?0x88FF88:rows[i][1].equals("DOWN")?0xFF8888:0xDDDDDD;g.drawString(font,rows[i][1],cx+260,row+5,color);}
    }
    @Override public boolean isPauseScreen(){return false;}
}
