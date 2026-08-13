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
    private EditBox name, ip, subnet, gateway, dns, ipv6, prefix6, gateway6, dns6, clockOffset, dnsName, dnsDetail, dnsTtl, poolName, poolStart, poolEnd, poolPrefix, poolGateway, poolDns, poolLease, poolExclusions, mailDomainField, mailUserField, mailPasswordField, mailQuotaField, ntpStratumField, ntpPollField, ntpSourceField, ntpDriftField, toolInput, programInput;
    private String desktopOutput = "Ready.";
    private String programOutput = "Ready. Separate commands with semicolons. Type help for commands.";
    private boolean dhcp, http, dnsService, dhcpService, mail;
    private boolean automatic6;
    private String ptpMode, ptpProfile;
    private long serviceMask;
    private String dnsRecordData, dnsRecordType="A", dnsStatus="Ready.";
    private String dhcp4Data,dhcp6Data,dhcpStatus="Ready.";
    private boolean https=true,pop3=true;
    private String serviceStatus="Ready.";
    private boolean ntpServer,ntpClient;
    private String ntpStatus;
    private long ntpDeviceTime,lastNtpSync;
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
        if(selected==ServerRackService.HTTP){pageButton(https?"HTTPS   [ON]    Off":"HTTPS   On    [OFF]",cx+330,y+64,260,()->{https=!https;init();});pageButton("New File",x+w-220,y+h-55,85,()->serviceStatus="New file editor will open here.");pageButton("Import",x+w-129,y+h-55,85,()->serviceStatus="Import file selector will open here.");}
        if(selected==ServerRackService.EMAIL){int right=x+w-30;mailDomainField=field(cx+110,y+122,right-cx-190,"vsia-net.com",253);pageButton("Set",right-70,y+122,70,()->serviceStatus="Mail domain updated.");mailUserField=field(cx+55,y+168,180,"",32);mailPasswordField=field(cx+315,y+168,260,"",64);mailQuotaField=field(cx+655,y+168,90,"100",4);pageButton(pop3?"POP3   [ON]    Off":"POP3   On    [OFF]",cx+330,y+64,260,()->{pop3=!pop3;init();});pageButton("+",right-80,y+225,70,()->serviceStatus="Mailbox account added.");pageButton("-",right-80,y+255,70,()->serviceStatus="Selected account removed.");pageButton("Change Password",right-110,y+285,100,()->serviceStatus="Password changed.");}
        if(selected==ServerRackService.NTP){
            pageButton(ntpServer?"Server Role   [ON]    Off":"Server Role   On    [OFF]",cx,y+106,260,()->{ntpServer=!ntpServer;init();});
            pageButton(ntpClient?"Client Role   [ON]    Off":"Client Role   On    [OFF]",cx+280,y+106,260,()->{ntpClient=!ntpClient;init();});
            ntpStratumField=field(cx+145,y+154,90,Integer.toString(state.ntpStratum()),2);
            ntpPollField=field(cx+425,y+154,110,Integer.toString(state.ntpPoll()),4);
            ntpSourceField=field(cx+145,y+188,220,state.ntpSource(),45);
            ntpDriftField=field(cx+425,y+188,110,Integer.toString(state.clockDrift()),5);
            pageButton("Apply NTP Configuration",cx,y+232,190,()->sendNtp());
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
        programInput=field(x+side,y+82,w-side-20,"hostname Server0; show config",16384);
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
        sectionLine(g,cx,y+103,"Service Configuration");
        if(servicePage.equals("HTTP")){g.drawString(font,"Hosted Files",cx,y+130,0xCCCCCC);table(g,cx,y+148,"File Name","Action",new String[][]{{"index.html","Edit","Delete"},{"styles.css","Edit","Delete"},{"script.js","Edit","Delete"}});}
        else if(servicePage.equals("DNS")){g.drawString(font,"Name",cx,y+117,0xCCCCCC);g.drawString(font,"Detail",cx,y+145,0xCCCCCC);g.drawString(font,"TTL",cx,y+173,0xCCCCCC);g.drawString(font,dnsStatus,cx+305,y+201,0xAADDFF);g.drawString(font,"Resource Records",cx,y+231,0xCCCCCC);table(g,cx,y+245,"Name / Type","Detail / TTL",dnsRows());}
        else if(servicePage.equals("DHCP")||servicePage.equals("DHCPv6")){boolean v6=servicePage.equals("DHCPv6");g.drawString(font,"Pool / Start / End",cx,y+117,0xCCCCCC);g.drawString(font,(v6?"Prefix":"Mask")+" / Gateway / DNS",cx,y+145,0xCCCCCC);g.drawString(font,"Lease Seconds / Exclusions",cx,y+173,0xCCCCCC);g.drawString(font,dhcpStatus,cx+320,y+201,0xAADDFF);table(g,cx,y+245,"Pools and Leases","Details",dhcpRows(v6?dhcp6Data:dhcp4Data));}
        else if(servicePage.equals("EMAIL")){g.drawString(font,"Mail Domain: vsia-net.com",cx,y+130,0xCCCCCC);table(g,cx,y+148,"User","Mailbox",new String[][]{{"admin","Open","Delete"},{"player","Open","Delete"}});}
        else{ServerRackService service=ServerRackService.byDisplayName(servicePage);g.drawString(font,"Default Port: "+(service.defaultPort()==0?"Protocol-managed":service.defaultPort()),cx,y+130,0xCCCCCC);g.drawString(font,"Configuration module: ready for the "+service.displayName()+" service milestone",cx,y+150,0xBBBBBB);g.drawString(font,"This switch is persistent. Protocol rules, records, accounts, and logs are added in the dedicated milestone.",cx,y+170,0x999999);}}
    private void desktop(GuiGraphics g){if(desktopPage.isEmpty())return;g.drawCenteredString(font,desktopPage,x+w/2,y+45,0xFFFFFF);g.drawString(font,inputLabel(),x+125,y+70,0xCCCCCC);g.drawString(font,"Output",x+25,y+120,0xFFFFFF);g.fill(x+25,y+136,x+w-25,y+h-25,0xFF080808);int lineY=y+145;for(String raw:desktopOutput.split("\\n",-1)){for(FormattedCharSequence line:font.split(Component.literal(raw),w-70)){g.drawString(font,line,x+35,lineY,0xB8FFB8);lineY+=12;if(lineY>y+h-38)return;}}}
    private String inputLabel(){return switch(desktopPage){case "Ping"->"Target IPv4 address";case "DNS Lookup"->"Domain name";case "Web Browser"->"URL or IPv4 address";case "Email"->"Mailbox address";case "Terminal","Command Prompt"->"Command";default->"Input";};}
    private void programming(GuiGraphics g){g.drawString(font,"Project",x+12,y+67,0x222222);g.drawString(font,"server-config",x+12,y+83,0x444444);g.drawString(font,"Configuration Script (separate commands with semicolons)",x+side,y+67,0x222222);g.drawString(font,"Console Output",x+14,y+h-108,0xCCCCCC);int lineY=y+h-92;for(String raw:programOutput.split("\\n",-1)){for(FormattedCharSequence line:font.split(Component.literal(raw),w-38)){g.drawString(font,line,x+16,lineY,0xB8FFB8);lineY+=11;if(lineY>y+h-18)return;}}}
    private void attributes(GuiGraphics g){g.drawString(font,"Device Attributes",x+16,y+39,0xFFFFFF);String[][] a={{"Mean Time Between Failures","61,320 hours"},{"Cost","2,000"},{"Power Source","Internal"},{"Rack Units","3U"},{"Power Consumption","200 W"},{"Device Model","VSIA Server Rack"},{"IPv4 Address",state.ip()},{"IPv6 Address",state.ipv6()+"/"+state.ipv6Prefix()},{"PTP",state.ptpMode()+" / "+state.ptpProfile()},{"World Position",state.pos().toShortString()}};table(g,x+20,y+62,"Attribute","Value",a);}
    private void sectionLine(GuiGraphics g,int sx,int sy,String text){g.drawString(font,text,sx,sy,0xDDDDDD);int start=sx+font.width(text)+8;g.hLine(start,x+w-24,sy+4,0xFF555555);}
    private String[][] dnsRows(){if(dnsRecordData==null||dnsRecordData.isBlank())return new String[][]{{"No records","",""}};String[] lines=dnsRecordData.strip().split("\\n");int count=Math.min(lines.length,12);String[][] rows=new String[count][3];for(int i=0;i<count;i++){String[] p=lines[i].split("\\t",4);rows[i][0]=p.length>1?p[0]+"  ["+p[1]+"]":lines[i];rows[i][1]=p.length>2?p[2]:"";rows[i][2]=p.length>3?"TTL "+p[3]:"";}return rows;}
    private void table(GuiGraphics g,int tx,int ty,String firstHeader,String secondHeader,String[][] rows){int tw=w-(tx-x)-25;g.fill(tx,ty,tx+tw,ty+20,0xFF464646);g.drawString(font,firstHeader,tx+8,ty+6,0xFFFFFF);g.drawString(font,secondHeader,tx+tw/2,ty+6,0xFFFFFF);for(int i=0;i<rows.length;i++){int ry=ty+21+i*20;g.fill(tx,ry,tx+tw,ry+19,(i&1)==0?0xFF292929:0xFF252525);g.drawString(font,rows[i][0],tx+8,ry+5,0xEEEEEE);if(rows[i].length>1)g.drawString(font,rows[i][1],tx+tw/2,ry+5,0xDDDDDD);if(rows[i].length>2)g.drawString(font,rows[i][2],tx+tw-75,ry+5,0xDDDDDD);}}
    private String[][] dhcpRows(String data){if(data==null||data.isBlank())return new String[][]{{"No data","",""}};String[] lines=data.strip().split("\\n");List<String[]> rows=new ArrayList<>();String section="";for(String line:lines){if(line.equals("POOLS")||line.equals("LEASES")){section=line;continue;}String[] p=line.split("\\t",-1);if(section.equals("POOLS")&&p.length>=7)rows.add(new String[]{"Pool "+p[0],p[1]+" - "+p[2],"Lease "+p[6]+"s"});else if(section.equals("LEASES")&&p.length>=4)rows.add(new String[]{"Lease "+p[0],p[1],p[2]});if(rows.size()>=12)break;}return rows.isEmpty()?new String[][]{{"No active entries","",""}}:rows.toArray(new String[0][]);}
    private void renderHttpService(GuiGraphics g,int cx){int right=x+w-24;sectionLine(g,cx,y+98,"Web Services");g.drawString(font,"HTTP",cx,y+113,0xFFFFFF);g.drawString(font,"HTTPS",cx+330,y+113,0xFFFFFF);g.drawString(font,"File Manager",cx,y+145,0xDDDDDD);int top=y+162;g.fill(cx,top,right,top+20,0xFF464646);g.drawString(font,"File Name",cx+10,top+6,0xFFFFFF);g.drawString(font,"Permissions",cx+(right-cx)/2,top+6,0xFFFFFF);g.drawString(font,"Actions",right-145,top+6,0xFFFFFF);String[][] files={{"index.html","Read / Write","Edit     Delete"},{"styles.css","Read / Write","Edit     Delete"},{"script.js","Read / Write","Edit     Delete"}};for(int i=0;i<files.length;i++){int ry=top+21+i*22;g.fill(cx,ry,right,ry+21,(i&1)==0?0xFF292929:0xFF252525);g.drawString(font,files[i][0],cx+10,ry+6,0xEEEEEE);g.drawString(font,files[i][1],cx+(right-cx)/2,ry+6,0xDDDDDD);g.drawString(font,files[i][2],right-145,ry+6,0xDDDDDD);}g.drawString(font,serviceStatus,cx,y+h-62,0x88CCFF);}
    private void renderEmailService(GuiGraphics g,int cx){int right=x+w-24;sectionLine(g,cx,y+98,"Mail Services");g.drawString(font,"SMTP Service",cx,y+113,0xFFFFFF);g.drawString(font,"POP3 Service",cx+330,y+113,0xFFFFFF);g.drawString(font,"Domain Name",cx,y+127,0xDDDDDD);g.drawString(font,"User Setup",cx,y+157,0xDDDDDD);g.drawString(font,"User",cx,y+173,0xBBBBBB);g.drawString(font,"Password",cx+245,y+173,0xBBBBBB);g.drawString(font,"Quota",cx+600,y+173,0xBBBBBB);int top=y+205;g.fill(cx,top,right-120,top+20,0xFF464646);g.drawString(font,"Mailbox Account",cx+10,top+6,0xFFFFFF);g.drawString(font,"Quota",cx+(right-cx)/2,top+6,0xFFFFFF);g.drawString(font,"Messages",right-270,top+6,0xFFFFFF);String[][] users={{"admin@vsia-net.com","100","0"},{"player@vsia-net.com","100","0"}};for(int i=0;i<users.length;i++){int ry=top+21+i*22;g.fill(cx,ry,right-120,ry+21,(i&1)==0?0xFF292929:0xFF252525);g.drawString(font,users[i][0],cx+10,ry+6,0xEEEEEE);g.drawString(font,users[i][1],cx+(right-cx)/2,ry+6,0xDDDDDD);g.drawString(font,users[i][2],right-270,ry+6,0xDDDDDD);}g.drawString(font,serviceStatus,cx,y+h-62,0x88CCFF);}
    private void renderNtpService(GuiGraphics g,int cx){sectionLine(g,cx,y+98,"Network Time Protocol");g.drawString(font,"NTP Roles",cx,y+120,0xDDDDDD);g.drawString(font,"Stratum (1-15)",cx,y+159,0xCCCCCC);g.drawString(font,"Poll Interval (16-4096 seconds)",cx+280,y+159,0xCCCCCC);g.drawString(font,"Preferred Source IPv4",cx,y+193,0xCCCCCC);g.drawString(font,"Clock Drift (ppm)",cx+280,y+193,0xCCCCCC);sectionLine(g,cx,y+270,"Clock Status");g.drawString(font,"Device Time (UTC): "+java.time.Instant.ofEpochMilli(ntpDeviceTime+(System.currentTimeMillis()-openedAtMillis)),cx,y+292,0xFFFFFF);g.drawString(font,"Synchronization: "+ntpStatus,cx,y+312,0xB8FFB8);g.drawString(font,"Last Sync: "+(lastNtpSync==0?"Never":java.time.Instant.ofEpochMilli(lastNtpSync).toString()),cx,y+332,0xCCCCCC);g.drawString(font,"PTP mode: "+ptpMode+" / "+ptpProfile+" (PTP has priority over NTP)",cx,y+352,0xCCCCCC);g.drawString(font,serviceStatus,cx,y+h-62,0x88CCFF);}
    @Override public boolean isPauseScreen(){return false;}
}
