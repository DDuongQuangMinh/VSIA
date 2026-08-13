package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.signality.internet.server.ServerRackNetwork;
import com.k1ngtle.vsia.signality.internet.server.ServerRackNetwork.OpenRackPacket;
import com.k1ngtle.vsia.signality.internet.server.ServerRackService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
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
    private EditBox name, ip, subnet, gateway, dns, ipv6, prefix6, gateway6, dns6, clockOffset, dnsName, dnsDetail, dnsTtl, poolName, poolStart, poolEnd, poolPrefix, poolGateway, poolDns, poolLease, poolExclusions, mailDomainField, mailUserField, mailPasswordField, mailQuotaField, ntpStratumField, ntpPollField, ntpSourceField, ntpDriftField, syslogMinField, syslogFacilityField, syslogSeverityField, syslogMessageField, aaaUserField, aaaPasswordField, aaaPrivilegeField, aaaServiceField, radiusNameField, radiusAddressField, radiusSecretField, radiusUserField, radiusPasswordField, radiusPrivilegeField, iotIdField, iotNameField, iotTypeField, iotValueField, vmNameField, vmOsField, vmCpuField, vmMemoryField, vmStorageField, prpPeerField, httpFileNameField, httpContentField, httpPortField, httpsPortField, transferFileNameField, transferContentField, transferPortField, transferUserField, transferPasswordField, mailClientAddressField, mailClientPasswordField, mailClientToField, mailClientSubjectField, mailClientBodyField, dnsClientNameField, dnsClientServerField, toolInput, programInput;
    private String desktopOutput = "Ready.";
    private String dnsClientType = "A";
    private String textFileData="",textFileStatus="Press Refresh to load documents.",textFileName="",textFileContent="";
    private int textFileScroll;
    private EditBox textFileNameField;
    private MultiLineEditBox textFileEditor;
    private EditBox ftpClientServerField,ftpClientUserField,ftpClientPasswordField,ftpClientRemoteField,ftpClientLocalField;
    private String ftpClientServer="192.168.1.2",ftpClientUser="",ftpClientPassword="",ftpClientRemoteFiles="",ftpClientLocalFiles="",ftpClientStatus="Enter the FTP server and account credentials.";
    private boolean ftpClientConnected;
    private int ftpClientRemoteScroll,ftpClientLocalScroll;
    private EditBox tftpClientServerField,tftpClientRemoteField,tftpClientLocalField;
    private String tftpClientServer="192.168.1.2",tftpClientRemoteFiles="",tftpClientLocalFiles="",tftpClientStatus="Enter the TFTP server address.";
    private boolean tftpClientConnected;
    private int tftpClientRemoteScroll,tftpClientLocalScroll;
    private final List<String> browserHistory=new ArrayList<>();
    private final List<String[]> browserLinks=new ArrayList<>();
    private final List<String[]> browserActions=new ArrayList<>();
    private final List<String[]> browserFormFields=new ArrayList<>();
    private final List<EditBox> browserFormWidgets=new ArrayList<>();
    private String browserFormAction="";
    private int browserHistoryIndex=-1,browserStatus=0;
    private String browserUrl="http://192.168.1.2/",browserTitle="VSIA Browser",browserPageText="Enter an address and press Go.";
    private String browserNotice="";
    private int browserBackground=0xFFF4F4F4,browserForeground=0xFF202020;
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
    private int configuredHttpPort=80,configuredHttpsPort=443;
    private boolean httpEditorOpen;
    private int httpFileScroll;
    private MultiLineEditBox httpMultiLineEditor;
    private String transferFiles="",transferUsers="",transferStatus="Press Refresh to load transfer data.";
    private int configuredFtpPort=21,configuredTftpPort=69,transferFileScroll;
    private boolean transferReadable=true,transferWritable=true,transferEditorOpen;
    private final Map<String,Integer> serviceScrolls=new HashMap<>();
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
    private boolean mailClientLoggedIn,mailClientCompose,mailClientReading;
    private String mailClientAddress="",mailClientPassword="",mailClientFolder="INBOX",mailClientData="",mailClientStatus="Enter your mailbox and password.",mailClientMessageId="",mailClientFrom="",mailClientTo="",mailClientSubject="",mailClientBody="";
    private long mailClientSentAt;
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
        if(selected==ServerRackService.HTTP&&httpEditorOpen){initHttpEditor(cx);}
        else if(selected==ServerRackService.HTTP){
            int right=x+w-28, gap=16, half=(right-cx-gap)/2;
            pageButton(https?"HTTPS   [ON]    Off":"HTTPS   On    [OFF]",cx+half+gap,y+64,half,()->{https=!https;httpStatus="HTTPS changed. Save web settings to apply.";});
            httpPortField=field(cx,y+126,half,Integer.toString(configuredHttpPort),5);httpsPortField=field(cx+half+gap,y+126,half,Integer.toString(configuredHttpsPort),5);
            pageButton("New File",cx,y+166,90,()->openNewHttpFile());pageButton("Refresh",cx+100,y+166,80,()->sendHttp("QUERY"));pageButton("Save Web Settings",cx+190,y+166,150,()->sendHttp("CONFIG"));
            int listWidth=right-cx,fileWidth=listWidth/2,permissionWidth=listWidth/4,sizeWidth=listWidth-fileWidth-permissionWidth;
            List<String[]> files=httpFileRows();int visible=Math.min(12,Math.max(0,files.size()-httpFileScroll));for(int i=0;i<visible;i++){String[] row=files.get(httpFileScroll+i);int rowY=y+230+i*20;String fileLabel=font.plainSubstrByWidth(row[0],fileWidth-12);pageButton(fileLabel,cx,rowY,fileWidth,()->openHttpFile(row[0]));pageButton(row[1],cx+fileWidth,rowY,permissionWidth,()->openHttpFile(row[0]));pageButton(row[2],cx+fileWidth+permissionWidth,rowY,sizeWidth,()->openHttpFile(row[0]));}
            pageButton("Ã¢â€“Â²",right-42,y+202,34,()->scrollHttpFiles(-1));pageButton("Ã¢â€“Â¼",right-42,y+476,34,()->scrollHttpFiles(1));
        }
        if(selected==ServerRackService.FTP||selected==ServerRackService.TFTP)initTransferService(cx,selected.displayName());
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

    private void initInteractiveServiceRows(ServerRackService service,int cx){
        List<String[]> rows=interactiveRows(service);if(rows.isEmpty())return;String key=service.displayName();int visible=interactiveVisibleRows(service);int top=interactiveListTop(service);int offset=Math.max(0,Math.min(serviceScrolls.getOrDefault(key,0),Math.max(0,rows.size()-visible)));serviceScrolls.put(key,offset);int right=x+w-28;pageButton("Ã¢â€“Â²",right-38,top,32,()->scrollService(service,-1,visible));pageButton("Ã¢â€“Â¼",right-38,top+Math.max(20,(visible-1)*20),32,()->scrollService(service,1,visible));
    }
    private List<String[]> interactiveRows(ServerRackService service){List<String[]> rows=new ArrayList<>();String data=switch(service){case DNS->dnsRecordData;case DHCP->dhcp4Data;case DHCPV6->dhcp6Data;case SYSLOG->syslogData;case AAA->aaaUsers;case RADIUS_EAP->radiusClients;case IOT->iotDevices;case VM_MANAGEMENT->virtualMachines;default->"";};if(service==ServerRackService.EMAIL){rows.add(new String[]{"admin@vsia-net.com    Quota 100","admin@vsia-net.com\t100"});rows.add(new String[]{"player@vsia-net.com    Quota 100","player@vsia-net.com\t100"});return rows;}if(data==null||data.isBlank())return rows;boolean pools=service==ServerRackService.DHCP||service==ServerRackService.DHCPV6;boolean inPools=!pools;for(String line:data.strip().split("\\n")){if(pools&&line.equals("POOLS")){inPools=true;continue;}if(pools&&line.equals("LEASES")){inPools=false;continue;}if(!inPools||line.isBlank())continue;String[] p=line.split("\\t",-1);String label=switch(service){case DNS->p[0]+"  ["+(p.length>1?p[1]:"")+"]    "+(p.length>2?p[2]:"");case DHCP,DHCPV6->"Pool "+p[0]+"    "+(p.length>2?p[1]+" - "+p[2]:"");case SYSLOG->(p.length>4?p[2]+" / "+severityName(parseIntText(p[3]))+"    "+p[4]:line);case AAA->p[0]+"    Privilege "+(p.length>1?p[1]:"")+"    "+(p.length>2&&Boolean.parseBoolean(p[2])?"Enabled":"Disabled");case RADIUS_EAP->p[0]+"    "+(p.length>1?p[1]:"")+"    "+(p.length>2&&Boolean.parseBoolean(p[2])?"Enabled":"Disabled");case IOT->(p.length>2?p[1]+" ["+p[2]+"]    "+p[4]:line);case VM_MANAGEMENT->(p.length>5?p[0]+" ["+p[1]+"]    "+p[5]:line);default->line;};rows.add(new String[]{label,line});}return rows;}
    private void scrollService(ServerRackService service,int delta,int visible){List<String[]> rows=interactiveRows(service);String key=service.displayName();int value=Math.max(0,Math.min(Math.max(0,rows.size()-visible),serviceScrolls.getOrDefault(key,0)+delta));serviceScrolls.put(key,value);init();}
    private void selectServiceRow(ServerRackService service,String raw){String[] p=raw.split("\\t",-1);switch(service){case DNS->{if(p.length>=4){dnsName.setValue(p[0]);dnsRecordType=p[1];dnsDetail.setValue(p[2]);dnsTtl.setValue(p[3]);dnsStatus="Record loaded for editing.";}}case DHCP,DHCPV6->{if(p.length>=8){poolName.setValue(p[0]);poolStart.setValue(p[1]);poolEnd.setValue(p[2]);poolPrefix.setValue(p[3]);poolGateway.setValue(p[4]);poolDns.setValue(p[5]);poolLease.setValue(p[6]);poolExclusions.setValue(p[7]);dhcpStatus="Pool loaded for editing.";}}case EMAIL->{String[] address=p[0].split("@",2);mailUserField.setValue(address[0]);if(address.length>1)mailDomainField.setValue(address[1]);mailQuotaField.setValue(p.length>1?p[1]:"100");mailPasswordField.setValue("");serviceStatus="Mailbox loaded. Enter its password before saving changes.";}case SYSLOG->{if(p.length>=5){syslogFacilityField.setValue(p[2]);syslogSeverityField.setValue(p[3]);syslogMessageField.setValue(p[4]);syslogStatus="Log details loaded into the test fields.";}}case AAA->{if(p.length>=3){aaaUserField.setValue(p[0]);aaaPasswordField.setValue("");aaaPrivilegeField.setValue(p[1]);aaaUserEnabled=Boolean.parseBoolean(p[2]);aaaStatus="User loaded. Enter a password before updating.";}}case RADIUS_EAP->{if(p.length>=3){radiusNameField.setValue(p[0]);radiusAddressField.setValue(p[1]);radiusSecretField.setValue("");radiusClientEnabled=Boolean.parseBoolean(p[2]);radiusStatus="NAS client loaded. Enter its shared secret before updating.";}}case IOT->{if(p.length>=6){iotIdField.setValue(p[0]);iotNameField.setValue(p[1]);iotTypeField.setValue(p[2]);iotValueField.setValue(p[4].isBlank()?p[5]:p[4]);iotStatus="IoT device loaded for editing.";}}case VM_MANAGEMENT->{if(p.length>=5){vmNameField.setValue(p[0]);vmOsField.setValue(p[1]);vmCpuField.setValue(p[2]);vmMemoryField.setValue(p[3]);vmStorageField.setValue(p[4]);vmStatus="Virtual machine loaded for editing.";}}default->{}}}

    private void syncLegacyServiceFlags(){http=serviceBit(ServerRackService.HTTP);dhcpService=serviceBit(ServerRackService.DHCP);dnsService=serviceBit(ServerRackService.DNS);mail=serviceBit(ServerRackService.EMAIL);}
    private boolean serviceBit(ServerRackService service){return(serviceMask&(1L<<service.ordinal()))!=0;}

    private interface BoolSet {void set(boolean v);}
    private void serviceToggle(String label,int bx,int by,boolean value,BoolSet set){
        pageButton(label+"   "+(value?"[ON]    Off":"On    [OFF]"),bx,by,260,()->{set.set(!value);init();});
    }

    private void initDesktop() {
        if(!desktopPage.isEmpty()){
            pageButton("< Desktop",x+14,y+35,100,()->{desktopPage="";init();});
            if(desktopPage.equals("Email")){initMailClient();return;}
            if(desktopPage.equals("IP Configuration")){initDesktopIpConfiguration();return;}
            if(desktopPage.equals("DNS Lookup")){initDesktopDnsLookup();return;}
            if(desktopPage.equals("Text Editor")){initDesktopTextEditor();return;}
            if(desktopPage.equals("FTP Client")){initDesktopFtpClient();return;}
            if(desktopPage.equals("TFTP Client")){initDesktopTftpClient();return;}
            String initial=desktopPage.equals("IP Configuration")?"ipconfig":desktopPage.equals("Web Browser")?browserUrl:"";
            if(desktopPage.equals("Web Browser")){
                pageButton("Back",x+125,y+62,55,()->browserBack());pageButton("Forward",x+185,y+62,65,()->browserForward());pageButton("Refresh",x+255,y+62,65,()->navigateBrowser(browserUrl,false));
                toolInput=field(x+125,y+86,w-225,initial,256);pageButton("Go",x+w-80,y+86,60,()->navigateBrowser(toolInput.getValue(),true));
                browserFormWidgets.clear();
                int formWidth=Math.max(100,(w-200)/Math.max(1,Math.min(3,browserFormFields.size())));
                for(int i=0;i<Math.min(browserFormFields.size(),3);i++){String[] spec=browserFormFields.get(i);EditBox input=field(x+35+i*formWidth,y+h-101,formWidth-10,spec[1],128);input.setHint(Component.literal(spec[2].isBlank()?spec[0]:spec[2]));browserFormWidgets.add(input);}
                List<String[]> controls=new ArrayList<>();
                for(String[] link:browserLinks)controls.add(new String[]{"NAV",link[0],link[1]});
                controls.addAll(browserActions);
                for(int i=0;i<Math.min(controls.size(),8);i++){String[] control=controls.get(i);pageButton(control[1],x+35+(i%4)*((w-70)/4),y+h-75+(i/4)*22,Math.max(90,(w-100)/4),()->runBrowserControl(control));}
                if(!browserFormFields.isEmpty())pageButton("Submit",x+w-115,y+h-101,80,()->submitBrowserForm());
                return;
            }
            toolInput=field(x+125,y+82,w-225,initial,256);
            pageButton("Run",x+w-80,y+82,60,()->runDesktopTool()); return;
        }
        String[] tools={"IP Configuration","Terminal","Command Prompt","Web Browser","Email","DNS Lookup","Ping","Text Editor","FTP Client","TFTP Client"};
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
        if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen&&screen.desktopPage.equals(tool)){if(tool.equals("Web Browser"))screen.acceptBrowserPage(result);else screen.desktopOutput=result;}
    }

    private void initDesktopIpConfiguration(){int left=x+40,fieldX=left+155,fieldWidth=Math.max(220,w-250);pageButton(dhcp?"[ DHCP ]    Static":"DHCP    [ Static ]",fieldX,y+72,220,()->{dhcp=!dhcp;init();});ip=field(fieldX,y+112,fieldWidth,state.ip(),15);subnet=field(fieldX,y+140,fieldWidth,state.subnet(),15);gateway=field(fieldX,y+168,fieldWidth,state.gateway(),15);dns=field(fieldX,y+196,fieldWidth,state.dns(),15);ipv6=field(fieldX,y+252,fieldWidth,state.ipv6(),45);prefix6=field(fieldX,y+280,80,Integer.toString(state.ipv6Prefix()),3);gateway6=field(fieldX,y+308,fieldWidth,state.gateway6(),45);dns6=field(fieldX,y+336,fieldWidth,state.dns6(),45);pageButton("Save Network Configuration",fieldX,y+378,190,()->saveDesktopNetwork());}
    private void saveDesktopNetwork(){desktopOutput="Saving network configuration...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DesktopNetworkConfigPacket(state.pos(),dhcp,ip.getValue(),subnet.getValue(),gateway.getValue(),dns.getValue(),ipv6.getValue(),parseInt(prefix6,state.ipv6Prefix()),gateway6.getValue(),dns6.getValue()));}

    private void initDesktopDnsLookup(){
        int left=x+40,fieldX=left+145,fieldWidth=Math.max(260,w-250);
        dnsClientNameField=field(fieldX,y+100,fieldWidth,"",253);
        dnsClientServerField=field(fieldX,y+140,fieldWidth,state.dns(),45);
        pageButton("Query Type: "+dnsClientType,left,y+184,150,()->{String[] types={"A","AAAA","CNAME","MX","PTR"};int next=(java.util.Arrays.asList(types).indexOf(dnsClientType)+1)%types.length;dnsClientType=types[next];init();});
        pageButton("Lookup",left+165,y+184,110,()->runDesktopDnsLookup());
        pageButton("Use Configured DNS",left+290,y+184,165,()->dnsClientServerField.setValue(dnsClientType.equals("AAAA")?state.dns6():state.dns()));
    }
    private void runDesktopDnsLookup(){String query=dnsClientNameField.getValue().trim()+" "+dnsClientType+" "+dnsClientServerField.getValue().trim();desktopOutput="Querying DNS server...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DesktopToolPacket(state.pos(),desktopPage,query.trim()));}

    private void initDesktopTextEditor(){
        int left=x+190,right=x+w-28;
        pageButton("New",x+25,y+72,70,()->sendTextFile("NEW"));
        pageButton("Refresh",x+100,y+72,75,()->sendTextFile("QUERY"));
        List<String[]> rows=textFileRows();int end=Math.min(rows.size(),textFileScroll+14);
        for(int i=textFileScroll;i<end;i++){String[] row=rows.get(i);pageButton(row[0]+"  ("+row[1]+" B)",x+25,y+108+(i-textFileScroll)*23,150,()->sendTextFile("OPEN",row[0]));}
        pageButton("▲",x+25,y+h-70,70,()->scrollTextFiles(-1));pageButton("▼",x+105,y+h-70,70,()->scrollTextFiles(1));
        textFileNameField=field(left,y+86,right-left,textFileName,64);
        textFileEditor=new MultiLineEditBox(font,left,y+140,right-left,h-248,Component.literal("Enter document text"),Component.literal("VSIA text document"));
        textFileEditor.setCharacterLimit(32768);textFileEditor.setValue(textFileContent);addRenderableWidget(textFileEditor);
        pageButton("Save",right-170,y+h-76,80,()->sendTextFile("SAVE"));pageButton("Delete",right-80,y+h-76,80,()->sendTextFile("DELETE"));
    }
    private List<String[]> textFileRows(){List<String[]> rows=new ArrayList<>();if(textFileData==null||textFileData.isBlank())return rows;for(String line:textFileData.strip().split("\\n")){String[] p=line.split("\\t",-1);if(p.length>=2)rows.add(p);}return rows;}
    private void scrollTextFiles(int amount){textFileScroll=Math.max(0,Math.min(Math.max(0,textFileRows().size()-14),textFileScroll+amount));init();}
    private void sendTextFile(String action){sendTextFile(action,textFileNameField==null?textFileName:textFileNameField.getValue());}
    private void sendTextFile(String action,String filename){String content=textFileEditor==null?textFileContent:textFileEditor.getValue();textFileStatus="Working...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.TextFileCommandPacket(state.pos(),action,filename,content));}
    public static void acceptTextFileResult(String message,String files,String filename,String content){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen&&screen.desktopPage.equals("Text Editor")){screen.textFileStatus=message;screen.textFileData=files;if(!filename.isEmpty()||message.equals("Document deleted.")){screen.textFileName=filename;screen.textFileContent=content;}screen.textFileScroll=Math.min(screen.textFileScroll,Math.max(0,screen.textFileRows().size()-14));screen.init();}}

    private void initDesktopFtpClient(){
        int left=x+28,right=x+w-28,fieldX=left+115;
        ftpClientServerField=field(fieldX,y+72,220,ftpClientServer,15);ftpClientUserField=field(fieldX,y+100,180,ftpClientUser,32);ftpClientPasswordField=field(fieldX+195,y+100,220,ftpClientPassword,64);
        pageButton(ftpClientConnected?"Reconnect":"Connect",fieldX+235,y+72,90,()->sendFtpClient("CONNECT"));pageButton("Disconnect",fieldX+335,y+72,90,()->{ftpClientConnected=false;ftpClientStatus="Disconnected.";init();});pageButton("Refresh",fieldX+435,y+72,80,()->sendFtpClient("LIST"));
        int mid=x+w/2,top=y+190;List<String[]> remote=ftpClientRows(ftpClientRemoteFiles),local=ftpClientRows(ftpClientLocalFiles);
        for(int i=ftpClientRemoteScroll;i<Math.min(remote.size(),ftpClientRemoteScroll+10);i++){String[] row=remote.get(i);pageButton(row[0]+"  "+row[3]+" B",left,top+(i-ftpClientRemoteScroll)*22,mid-left-18,()->{ftpClientRemoteField.setValue(row[0]);if(ftpClientLocalField.getValue().isBlank())ftpClientLocalField.setValue(row[0]);});}
        for(int i=ftpClientLocalScroll;i<Math.min(local.size(),ftpClientLocalScroll+10);i++){String[] row=local.get(i);pageButton(row[0]+"  "+row[1]+" B",mid+10,top+(i-ftpClientLocalScroll)*22,right-mid-10,()->{ftpClientLocalField.setValue(row[0]);if(ftpClientRemoteField.getValue().isBlank())ftpClientRemoteField.setValue(row[0]);});}
        ftpClientRemoteField=field(left+105,y+h-104,mid-left-125,"",64);ftpClientLocalField=field(mid+105,y+h-104,right-mid-105,"",64);
        pageButton("Download ->",left,y+h-72,120,()->sendFtpClient("GET"));pageButton("<- Upload",mid+10,y+h-72,120,()->sendFtpClient("PUT"));
    }
    private List<String[]> ftpClientRows(String data){List<String[]> rows=new ArrayList<>();if(data==null||data.isBlank())return rows;for(String line:data.strip().split("\\n")){String[] p=line.split("\\t",-1);if(p.length>=2)rows.add(p);}return rows;}
    private void sendFtpClient(String action){if(ftpClientServerField!=null)ftpClientServer=ftpClientServerField.getValue();if(ftpClientUserField!=null)ftpClientUser=ftpClientUserField.getValue();if(ftpClientPasswordField!=null)ftpClientPassword=ftpClientPasswordField.getValue();ftpClientStatus="Working...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.FtpClientCommandPacket(state.pos(),action,ftpClientServer,ftpClientUser,ftpClientPassword,ftpClientRemoteField==null?"":ftpClientRemoteField.getValue(),ftpClientLocalField==null?"":ftpClientLocalField.getValue()));}
    public static void acceptFtpClientResult(String message,boolean connected,String server,String username,String remoteFiles,String localFiles){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen&&screen.desktopPage.equals("FTP Client")){screen.ftpClientStatus=message;screen.ftpClientConnected=connected;screen.ftpClientServer=server.isBlank()?screen.ftpClientServer:server;screen.ftpClientUser=username.isBlank()?screen.ftpClientUser:username;screen.ftpClientRemoteFiles=remoteFiles;screen.ftpClientLocalFiles=localFiles;screen.init();}}
    private void initDesktopTftpClient(){
        int left=x+28,right=x+w-28,fieldX=left+115;
        tftpClientServerField=field(fieldX,y+78,220,tftpClientServer,15);pageButton(tftpClientConnected?"Reconnect":"Connect",fieldX+235,y+78,90,()->sendTftpClient("CONNECT"));pageButton("Disconnect",fieldX+335,y+78,90,()->{tftpClientConnected=false;tftpClientStatus="Disconnected.";init();});pageButton("Refresh",fieldX+435,y+78,80,()->sendTftpClient("LIST"));
        int mid=x+w/2,top=y+180;List<String[]> remote=ftpClientRows(tftpClientRemoteFiles),local=ftpClientRows(tftpClientLocalFiles);
        for(int i=tftpClientRemoteScroll;i<Math.min(remote.size(),tftpClientRemoteScroll+11);i++){String[] row=remote.get(i);pageButton(row[0]+"  "+row[3]+" B",left,top+(i-tftpClientRemoteScroll)*22,mid-left-18,()->{tftpClientRemoteField.setValue(row[0]);if(tftpClientLocalField.getValue().isBlank())tftpClientLocalField.setValue(row[0]);});}
        for(int i=tftpClientLocalScroll;i<Math.min(local.size(),tftpClientLocalScroll+11);i++){String[] row=local.get(i);pageButton(row[0]+"  "+row[1]+" B",mid+10,top+(i-tftpClientLocalScroll)*22,right-mid-10,()->{tftpClientLocalField.setValue(row[0]);if(tftpClientRemoteField.getValue().isBlank())tftpClientRemoteField.setValue(row[0]);});}
        tftpClientRemoteField=field(left+105,y+h-104,mid-left-125,"",64);tftpClientLocalField=field(mid+105,y+h-104,right-mid-105,"",64);pageButton("Read / Download ->",left,y+h-72,145,()->sendTftpClient("GET"));pageButton("<- Write / Upload",mid+10,y+h-72,145,()->sendTftpClient("PUT"));
    }
    private void sendTftpClient(String action){if(tftpClientServerField!=null)tftpClientServer=tftpClientServerField.getValue();tftpClientStatus="Working...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.TftpClientCommandPacket(state.pos(),action,tftpClientServer,tftpClientRemoteField==null?"":tftpClientRemoteField.getValue(),tftpClientLocalField==null?"":tftpClientLocalField.getValue()));}
    public static void acceptTftpClientResult(String message,boolean connected,String server,String remoteFiles,String localFiles){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen&&screen.desktopPage.equals("TFTP Client")){screen.tftpClientStatus=message;screen.tftpClientConnected=connected;screen.tftpClientServer=server.isBlank()?screen.tftpClientServer:server;screen.tftpClientRemoteFiles=remoteFiles;screen.tftpClientLocalFiles=localFiles;screen.init();}}

    private void initMailClient(){
        int left=x+30,right=x+w-30;
        if(!mailClientLoggedIn){mailClientAddressField=field(left+150,y+100,right-left-170,mailClientAddress,286);mailClientPasswordField=field(left+150,y+132,right-left-170,"",64);mailClientPasswordField.setResponder(v->mailClientPassword=v);pageButton("Sign In",left+150,y+170,120,()->sendMailClient("LOGIN",""));return;}
        pageButton("Inbox",left,y+70,85,()->openMailFolder("INBOX"));pageButton("Sent",left+92,y+70,85,()->openMailFolder("SENT"));pageButton("Compose",left+184,y+70,100,()->{mailClientCompose=true;mailClientReading=false;mailClientTo="";mailClientSubject="";mailClientBody="";init();});pageButton("Refresh",left+291,y+70,90,()->sendMailClient("LIST",""));pageButton("Sign Out",right-90,y+70,90,()->{mailClientLoggedIn=false;mailClientPassword="";mailClientData="";init();});
        if(mailClientCompose){mailClientToField=field(left+90,y+126,right-left-110,mailClientTo,286);mailClientSubjectField=field(left+90,y+158,right-left-110,mailClientSubject,128);mailClientBodyField=field(left+90,y+205,right-left-110,mailClientBody,8192);pageButton("Send",left+90,y+245,100,()->sendMailClient("SEND",""));pageButton("Cancel",left+200,y+245,100,()->{mailClientCompose=false;init();});return;}
        if(mailClientReading){pageButton("Back",left,y+115,80,()->{mailClientReading=false;init();});pageButton("Reply",left+90,y+115,80,()->{mailClientCompose=true;mailClientReading=false;mailClientTo=mailClientFrom;mailClientSubject=mailClientSubject.startsWith("Re:")?mailClientSubject:"Re: "+mailClientSubject;mailClientBody="\n\n--- Original message ---\n"+mailClientBody;init();});pageButton("Delete",left+180,y+115,80,()->sendMailClient("DELETE",mailClientMessageId));return;}
        List<String[]> rows=mailClientRows();for(int i=0;i<Math.min(rows.size(),12);i++){String[] row=rows.get(i);String marker=Boolean.parseBoolean(row[1])?"  ":"* ";String correspondent=mailClientFolder.equals("SENT")?row[3]:row[2];pageButton(marker+row[4]+"   -   "+correspondent,left,y+122+i*22,right-left,()->sendMailClient("OPEN",row[0]));}
    }
    private List<String[]> mailClientRows(){List<String[]> rows=new ArrayList<>();if(mailClientData==null||mailClientData.isBlank())return rows;for(String line:mailClientData.strip().split("\\n")){String[] values=line.split("\\t",-1);if(values.length>=6)rows.add(values);}return rows;}
    private void openMailFolder(String folder){mailClientFolder=folder;mailClientCompose=false;mailClientReading=false;sendMailClient("LIST","");}
    private void sendMailClient(String action,String id){
        if(mailClientAddressField!=null)mailClientAddress=mailClientAddressField.getValue().trim().toLowerCase();if(mailClientPasswordField!=null&&!mailClientPasswordField.getValue().isEmpty())mailClientPassword=mailClientPasswordField.getValue();
        String to=mailClientToField==null?mailClientTo:mailClientToField.getValue();String subject=mailClientSubjectField==null?mailClientSubject:mailClientSubjectField.getValue();String body=mailClientBodyField==null?mailClientBody:mailClientBodyField.getValue();mailClientStatus="Working...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.MailClientCommandPacket(state.pos(),action,mailClientAddress,mailClientPassword,mailClientFolder,id,to,subject,body));
    }
    public static void acceptMailClientResult(String message,boolean authenticated,String address,String folder,String data,String id,String from,String to,String subject,String body,long sentAt){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.mailClientStatus=message;if(authenticated){screen.mailClientLoggedIn=true;screen.mailClientAddress=address;screen.mailClientFolder=folder;screen.mailClientData=data;}if(!id.isEmpty()){screen.mailClientReading=true;screen.mailClientCompose=false;screen.mailClientMessageId=id;screen.mailClientFrom=from;screen.mailClientTo=to;screen.mailClientSubject=subject;screen.mailClientBody=body;screen.mailClientSentAt=sentAt;}else if(message.startsWith("Message delivered")||message.startsWith("Message deleted")){screen.mailClientReading=false;screen.mailClientCompose=false;}screen.init();}}
    private void navigateBrowser(String url,boolean addHistory){url=url.trim();if(url.isEmpty())return;if(!url.matches("(?i)^https?://.*"))url="http://"+url;if(addHistory){while(browserHistory.size()>browserHistoryIndex+1)browserHistory.remove(browserHistory.size()-1);browserHistory.add(url);browserHistoryIndex=browserHistory.size()-1;}browserUrl=url;browserTitle="Loading...";browserPageText="Contacting server...";browserLinks.clear();browserActions.clear();browserFormFields.clear();browserFormWidgets.clear();browserFormAction="";browserNotice="";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DesktopToolPacket(state.pos(),"Web Browser",url));}
    private void browserBack(){if(browserHistoryIndex>0){browserHistoryIndex--;navigateBrowser(browserHistory.get(browserHistoryIndex),false);}}
    private void browserForward(){if(browserHistoryIndex+1<browserHistory.size()){browserHistoryIndex++;navigateBrowser(browserHistory.get(browserHistoryIndex),false);}}
    private String resolveBrowserLink(String href){if(href.matches("(?i)^https?://.*"))return href;String base=browserUrl;int scheme=base.indexOf("://")+3;int slash=base.indexOf('/',scheme);String root=slash<0?base:base.substring(0,slash);if(href.startsWith("/"))return root+href;int last=base.lastIndexOf('/');return (last>=scheme?base.substring(0,last+1):base+"/")+href;}
    private void acceptBrowserPage(String result){browserLinks.clear();browserActions.clear();browserFormFields.clear();browserFormWidgets.clear();browserFormAction="";browserNotice="";browserBackground=0xFFF4F4F4;browserForeground=0xFF202020;if(!result.startsWith("VSIA_BROWSER\t")){browserStatus=500;browserTitle="Invalid Response";browserPageText=result;init();return;}int newline=result.indexOf('\n');String header=newline<0?result:result.substring(0,newline);String html=newline<0?"":result.substring(newline+1);String[] parts=header.split("\t",4);try{browserStatus=Integer.parseInt(parts[1]);}catch(Exception e){browserStatus=500;}if(parts.length>2&&!parts[2].isBlank())browserUrl=parts[2];browserTitle=parts.length>3?parts[3]:"VSIA Browser";Matcher titleMatcher=Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(html);if(titleMatcher.find())browserTitle=cleanHtml(titleMatcher.group(1));Matcher colors=Pattern.compile("(?is)(?:body\\s*\\{|<body[^>]*style=['\"])(.*?)(?:\\}|['\"])").matcher(html);if(colors.find()){Matcher bg=Pattern.compile("(?i)background(?:-color)?\\s*:\\s*(#[0-9a-f]{6}|black|white|navy)").matcher(colors.group(1));Matcher fg=Pattern.compile("(?i)(?:^|;)\\s*color\\s*:\\s*(#[0-9a-f]{6}|black|white|navy)").matcher(colors.group(1));if(bg.find())browserBackground=cssColor(bg.group(1),browserBackground);if(fg.find())browserForeground=cssColor(fg.group(1),browserForeground);}Matcher links=Pattern.compile("(?is)<a\\s+[^>]*href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>(.*?)</a>").matcher(html);while(links.find()&&browserLinks.size()<8){String label=cleanHtml(links.group(2));browserLinks.add(new String[]{label.isBlank()?links.group(1):label,links.group(1)});}Map<String,String> functions=new HashMap<>();Matcher functionMatcher=Pattern.compile("(?is)function\\s+([A-Za-z_$][\\w$]*)\\s*\\([^)]*\\)\\s*\\{(.*?)\\}").matcher(html);while(functionMatcher.find())functions.put(functionMatcher.group(1),functionMatcher.group(2));Matcher buttons=Pattern.compile("(?is)<button\\b([^>]*)>(.*?)</button>").matcher(html);while(buttons.find()&&browserLinks.size()+browserActions.size()<8){Matcher click=Pattern.compile("(?is)onclick\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')").matcher(buttons.group(1));if(!click.find())continue;String code=click.group(1)!=null?click.group(1):click.group(2);Matcher call=Pattern.compile("^\\s*([A-Za-z_$][\\w$]*)\\s*\\(\\s*\\)\\s*;?\\s*$").matcher(code);if(call.matches()&&functions.containsKey(call.group(1)))code=functions.get(call.group(1));String label=cleanHtml(buttons.group(2));browserActions.add(new String[]{"SCRIPT",label.isBlank()?"Run action":label,decodeEntities(code)});}Matcher form=Pattern.compile("(?is)<form\\b([^>]*)>(.*?)</form>").matcher(html);if(form.find()){Matcher action=Pattern.compile("(?is)action\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')").matcher(form.group(1));browserFormAction=action.find()?(action.group(1)!=null?action.group(1):action.group(2)):browserUrl;Matcher inputs=Pattern.compile("(?is)<input\\b([^>]*)>").matcher(form.group(2));while(inputs.find()&&browserFormFields.size()<3){String attrs=inputs.group(1);String type=htmlAttribute(attrs,"type","text");if(!type.equalsIgnoreCase("text")&&!type.equalsIgnoreCase("password")&&!type.equalsIgnoreCase("search"))continue;String fieldName=htmlAttribute(attrs,"name","");if(fieldName.isBlank())continue;browserFormFields.add(new String[]{fieldName,htmlAttribute(attrs,"value",""),htmlAttribute(attrs,"placeholder",fieldName)});}}String text=html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>","").replaceAll("(?i)<br\\s*/?>|</p>|</h[1-6]>|</li>|</div>","\n").replaceAll("(?s)<[^>]+>","");browserPageText=decodeEntities(text).replaceAll("[ \\t]+"," ").replaceAll("\\n\\s*\\n+","\n\n").trim();if(browserPageText.isBlank())browserPageText="This page has no visible text.";init();}
    private void runBrowserControl(String[] control){if(control.length<3)return;if("NAV".equals(control[0]))navigateBrowser(resolveBrowserLink(control[2]),true);else runBrowserScript(control[2]);}
    private void submitBrowserForm(){if(browserFormFields.isEmpty())return;StringBuilder query=new StringBuilder();for(int i=0;i<browserFormFields.size()&&i<browserFormWidgets.size();i++){if(query.length()>0)query.append('&');query.append(urlEncode(browserFormFields.get(i)[0])).append('=').append(urlEncode(browserFormWidgets.get(i).getValue()));}String target=browserFormAction.isBlank()?browserUrl:resolveBrowserLink(browserFormAction);target+=(target.contains("?")?"&":"?")+query;navigateBrowser(target,true);}
    private static String htmlAttribute(String attributes,String name,String fallback){Matcher matcher=Pattern.compile("(?is)\\b"+Pattern.quote(name)+"\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))").matcher(attributes);if(!matcher.find())return fallback;String value=matcher.group(1)!=null?matcher.group(1):matcher.group(2)!=null?matcher.group(2):matcher.group(3);return decodeEntities(value);}
    private static String urlEncode(String value){return java.net.URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8).replace("+","%20");}
    private void runBrowserScript(String script){if(script==null||script.length()>1024){browserNotice="Blocked: the website action is too large.";init();return;}Matcher navigation=Pattern.compile("(?is)(?:window\\.)?location(?:\\.href)?\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(script);if(navigation.find()){navigateBrowser(resolveBrowserLink(navigation.group(1)),true);return;}Matcher message=Pattern.compile("(?is)(?:alert|console\\.log)\\s*\\(\\s*['\"]([^'\"]*)['\"]\\s*\\)").matcher(script);if(message.find()){browserNotice=decodeEntities(message.group(1));init();return;}Matcher textUpdate=Pattern.compile("(?is)document\\.getElementById\\s*\\(\\s*['\"][^'\"]+['\"]\\s*\\)\\s*\\.(?:innerText|textContent)\\s*=\\s*['\"]([^'\"]*)['\"]").matcher(script);if(textUpdate.find()){browserNotice=decodeEntities(textUpdate.group(1));init();return;}browserNotice="Blocked unsupported website action. Allowed: navigation, alert, console.log, and text updates.";init();}
    private static int cssColor(String value,int fallback){return switch(value.toLowerCase()){case "black"->0xFF000000;case "white"->0xFFFFFFFF;case "navy"->0xFF000080;default->{try{yield 0xFF000000|Integer.parseInt(value.substring(1),16);}catch(Exception e){yield fallback;}}};}
    private static String cleanHtml(String value){return decodeEntities(value.replaceAll("(?s)<[^>]+>","")).trim();}
    private static String decodeEntities(String value){return value.replace("&lt;","<").replace("&gt;",">").replace("&amp;","&").replace("&quot;","\"").replace("&#39;", "'").replace("&nbsp;"," ");}
    private void sendDns(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DnsRecordPacket(state.pos(),action,dnsName==null?"":dnsName.getValue(),dnsRecordType,dnsDetail==null?"":dnsDetail.getValue(),parseInt(dnsTtl,300)));}
    public static void acceptDnsResult(String message,String records){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.dnsStatus=message;screen.dnsRecordData=records;screen.init();}}
    private void sendDhcp(String action,boolean v6){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DhcpPoolPacket(state.pos(),action,poolName.getValue(),v6,poolStart.getValue(),poolEnd.getValue(),poolPrefix.getValue(),poolGateway.getValue(),poolDns.getValue(),parseInt(poolLease,3600),poolExclusions.getValue()));}
    public static void acceptDhcpResult(String message,String data,boolean ipv6){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.dhcpStatus=message;if(ipv6)screen.dhcp6Data=data;else screen.dhcp4Data=data;screen.init();}}
    private void sendNtp(){serviceStatus="Saving NTP configuration...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.NtpConfigPacket(state.pos(),ntpServer,ntpClient,parseInt(ntpStratumField,state.ntpStratum()),parseInt(ntpPollField,state.ntpPoll()),ntpSourceField.getValue(),parseInt(ntpDriftField,state.clockDrift())));}
    public static void acceptNtpResult(String message,String status,long deviceTime,long lastSync){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.serviceStatus=message;screen.ntpStatus=status;screen.ntpDeviceTime=deviceTime;screen.lastNtpSync=lastSync;}}
    private void sendSyslog(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.SyslogCommandPacket(state.pos(),action,parseInt(syslogMinField,7),syslogAcceptRemote,syslogFacilityField==null?"LOCAL0":syslogFacilityField.getValue(),parseInt(syslogSeverityField,6),syslogMessageField==null?"":syslogMessageField.getValue()));}
    public static void acceptSyslogResult(String message,String data,int minimumSeverity,boolean acceptRemote){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.syslogStatus=message;screen.syslogData=data;screen.syslogAcceptRemote=acceptRemote;screen.init();if(screen.syslogMinField!=null)screen.syslogMinField.setValue(Integer.toString(minimumSeverity));}}
    private void sendAaa(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.AaaCommandPacket(state.pos(),action,aaaUserField==null?"":aaaUserField.getValue(),aaaPasswordField==null?"":aaaPasswordField.getValue(),parseInt(aaaPrivilegeField,1),aaaUserEnabled,aaaServiceField==null?"LOGIN":aaaServiceField.getValue()));}
    public static void acceptAaaResult(String message,String users,String accounting){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.aaaStatus=message;screen.aaaUsers=users;screen.aaaAccounting=accounting;screen.init();}}
    private void sendRadius(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.RadiusCommandPacket(state.pos(),action,radiusNameField==null?"":radiusNameField.getValue(),radiusAddressField==null?"":radiusAddressField.getValue(),radiusSecretField==null?"":radiusSecretField.getValue(),radiusClientEnabled,radiusUserField==null?"":radiusUserField.getValue(),radiusPasswordField==null?"":radiusPasswordField.getValue(),parseInt(radiusPrivilegeField,1)));}
    public static void acceptRadiusResult(String message,String clients,String events){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.radiusStatus=message;screen.radiusClients=clients;screen.radiusEvents=events;screen.init();}}
    private void sendIot(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.IotCommandPacket(state.pos(),action,iotIdField==null?"":iotIdField.getValue(),iotNameField==null?"":iotNameField.getValue(),iotTypeField==null?"":iotTypeField.getValue(),iotValueField==null?"":iotValueField.getValue()));}
    public static void acceptIotResult(String message,String devices){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.iotStatus=message;screen.iotDevices=devices;screen.init();}}
    private void sendVm(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.VmCommandPacket(state.pos(),action,vmNameField==null?"":vmNameField.getValue(),vmOsField==null?"":vmOsField.getValue(),parseInt(vmCpuField,2),parseInt(vmMemoryField,4096),parseInt(vmStorageField,64)));}
    public static void acceptVmResult(String message,String machines){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.vmStatus=message;screen.virtualMachines=machines;screen.init();}}
    private void sendPrp(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.PrpCommandPacket(state.pos(),action,prpEnabled,prpLaneA,prpLaneB,prpPeerField==null?"":prpPeerField.getValue()));}
    public static void acceptPrpResult(String message,String status){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.prpStatus=message;screen.prpData=status;String[] p=status.split("\\t",-1);if(p.length>=4){screen.prpEnabled=p[0].equals("ENABLED");screen.prpLaneA=p[1].equals("UP");screen.prpLaneB=p[2].equals("UP");if(screen.prpPeerField!=null)screen.prpPeerField.setValue(p[3]);}}}
    private void sendHttp(String action){String content=httpMultiLineEditor!=null?httpMultiLineEditor.getValue():httpContentField==null?"":httpContentField.getValue();ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.HttpFileCommandPacket(state.pos(),action,httpFileNameField==null?"":httpFileNameField.getValue(),content,httpReadable,httpWritable,https,parseInt(httpPortField,80),parseInt(httpsPortField,443)));}
    private void initHttpEditor(int cx){int right=x+w-28;String filename=httpFileNameField==null?"":httpFileNameField.getValue();String content=httpContentField==null?"":httpContentField.getValue();httpFileNameField=field(cx,y+130,right-cx,filename,64);httpMultiLineEditor=new MultiLineEditBox(font,cx,y+190,right-cx,h-294,Component.literal("Enter HTML, CSS, or JavaScript"),Component.literal("Hosted file editor"));httpMultiLineEditor.setCharacterLimit(32768);httpMultiLineEditor.setValue(content);addRenderableWidget(httpMultiLineEditor);pageButton("< File Manager",cx,y+h-88,110,()->{httpContentField=hiddenHttpContent(httpMultiLineEditor.getValue());httpEditorOpen=false;init();});pageButton("Import Clipboard",cx+120,y+h-88,115,()->importHttpClipboard());pageButton("Auto Format",cx+245,y+h-88,95,()->formatHttpEditor());pageButton(httpReadable?"Readable [YES]":"Readable [NO]",cx+350,y+h-88,105,()->toggleHttpPermission(true));pageButton(httpWritable?"Writable [YES]":"Writable [NO]",cx+465,y+h-88,105,()->toggleHttpPermission(false));pageButton("Save",right-150,y+h-58,70,()->sendHttp("SAVE"));pageButton("Delete",right-70,y+h-58,70,()->sendHttp("DELETE"));}
    private void toggleHttpPermission(boolean readable){httpContentField=hiddenHttpContent(httpMultiLineEditor.getValue());if(readable)httpReadable=!httpReadable;else httpWritable=!httpWritable;init();}
    private EditBox hiddenHttpContent(String value){EditBox box=new EditBox(font,-1000,-1000,60,18,Component.empty());box.setMaxLength(32768);box.setValue(value);return box;}
    private List<String[]> httpFileRows(){List<String[]> rows=new ArrayList<>();if(httpFiles==null||httpFiles.isBlank())return rows;for(String line:httpFiles.strip().split("\\n")){String[] p=line.split("\\t",-1);if(p.length>=4)rows.add(new String[]{p[0],(p[1].equals("true")?"R":"-")+(p[2].equals("true")?"W":"-"),p[3]+" B"});}return rows;}
    private void scrollHttpFiles(int delta){int max=Math.max(0,httpFileRows().size()-12);httpFileScroll=Math.max(0,Math.min(max,httpFileScroll+delta));init();}
    private void openHttpFile(String filename){httpFileNameField=hiddenHttpContent(filename);httpStatus="Loading "+filename+"...";sendHttp("OPEN");}
    private void openNewHttpFile(){httpEditorOpen=true;httpFileNameField=hiddenHttpContent("new-page.html");httpContentField=hiddenHttpContent("<!DOCTYPE html>\n<html>\n<head>\n<title>New Page</title>\n</head>\n<body>\n<h1>New Page</h1>\n</body>\n</html>");httpReadable=true;httpWritable=true;init();httpFileNameField.setValue("new-page.html");httpMultiLineEditor.setValue(httpContentField.getValue());}
    private void importHttpClipboard(){String value=net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();if(value.length()>32768)value=value.substring(0,32768);if(httpMultiLineEditor!=null)httpMultiLineEditor.setValue(value);else if(httpContentField!=null)httpContentField.setValue(value);httpStatus="Clipboard imported. Press Save to store it on the rack.";}
    private void formatHttpEditor(){if(httpMultiLineEditor==null)return;String name=httpFileNameField.getValue().toLowerCase();String source=httpMultiLineEditor.getValue();String formatted=name.endsWith(".html")||name.endsWith(".htm")?formatMarkup(source):formatBraces(source);httpMultiLineEditor.setValue(formatted);httpStatus="Formatting complete. Review the result, then press Save.";}
    private static String formatMarkup(String source){String prepared=source.replaceAll(">\\s*<",">\n<");StringBuilder out=new StringBuilder();int depth=0;for(String raw:prepared.split("\\n")){String line=raw.trim();if(line.isEmpty())continue;boolean closing=line.matches("(?i)^</.*");boolean singleton=line.matches("(?i).*?/?>$")&&(line.matches("(?i)^<(br|hr|img|input|link|meta|!doctype).*"));if(closing)depth=Math.max(0,depth-1);out.append("  ".repeat(depth)).append(line).append('\n');if(line.matches("(?i)^<[^/!][^>]*>.*")&&!line.matches("(?is).*?</[^>]+>.*")&&!singleton)depth++;}return out.toString().stripTrailing();}
    private static String formatBraces(String source){String prepared=source.replace("{","{\n").replace("}","\n}\n").replace(";",";\n");StringBuilder out=new StringBuilder();int depth=0;for(String raw:prepared.split("\\n")){String line=raw.trim();if(line.isEmpty())continue;if(line.startsWith("}"))depth=Math.max(0,depth-1);out.append("  ".repeat(depth)).append(line).append('\n');if(line.endsWith("{"))depth++;}return out.toString().stripTrailing();}
    public static void acceptHttpFileResult(String message,String files,String filename,String content,boolean readable,boolean writable,boolean secure,int httpPort,int httpsPort){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.httpStatus=message;screen.httpFiles=files;screen.httpReadable=readable;screen.httpWritable=writable;screen.https=secure;screen.configuredHttpPort=httpPort;screen.configuredHttpsPort=httpsPort;if(!filename.isEmpty()){screen.httpEditorOpen=true;screen.httpFileNameField=screen.hiddenHttpContent(filename);screen.httpContentField=screen.hiddenHttpContent(content);}screen.init();if(!filename.isEmpty()){screen.httpFileNameField.setValue(filename);screen.httpMultiLineEditor.setValue(content);}}}
    private void initTransferService(int cx,String protocol){int right=x+w-28;boolean ftp=protocol.equals("FTP");if(transferEditorOpen){transferFileNameField=field(cx,y+126,right-cx,transferFileNameField==null?"new-file.txt":transferFileNameField.getValue(),64);transferContentField=field(cx,y+174,right-cx,transferContentField==null?"":transferContentField.getValue(),32768);pageButton(transferReadable?"Readable [YES]":"Readable [NO]",cx,y+214,130,()->{transferReadable=!transferReadable;init();});pageButton(transferWritable?"Writable [YES]":"Writable [NO]",cx+140,y+214,130,()->{transferWritable=!transferWritable;init();});pageButton("< File Manager",cx,y+254,120,()->{transferEditorOpen=false;init();});pageButton("Save",cx+130,y+254,80,()->sendTransfer(protocol,"SAVE"));pageButton("Delete",cx+220,y+254,80,()->sendTransfer(protocol,"DELETE"));return;}transferPortField=field(cx,y+126,150,Integer.toString(ftp?configuredFtpPort:configuredTftpPort),5);pageButton("Save Port",cx+160,y+126,100,()->sendTransfer(protocol,"CONFIG"));pageButton("Refresh",cx+270,y+126,90,()->sendTransfer(protocol,"QUERY"));pageButton("New File",cx+370,y+126,90,()->openNewTransferFile());int listTop=ftp?y+270:y+210;if(ftp){transferUserField=field(cx,y+184,180,"",32);transferPasswordField=field(cx+190,y+184,220,"",64);pageButton("Add / Update User",cx+420,y+184,140,()->sendTransfer(protocol,"SAVE_USER"));pageButton("Delete User",cx+570,y+184,110,()->sendTransfer(protocol,"DELETE_USER"));}List<String[]> files=transferFileRows();int visible=Math.min(10,Math.max(0,files.size()-transferFileScroll));int total=right-cx,nameWidth=total/2,permissionWidth=total/4;for(int i=0;i<visible;i++){String[] row=files.get(transferFileScroll+i);int rowY=listTop+22+i*20;pageButton(font.plainSubstrByWidth(row[0],nameWidth-12),cx,rowY,nameWidth,()->openTransferFile(protocol,row[0]));pageButton(row[1],cx+nameWidth,rowY,permissionWidth,()->openTransferFile(protocol,row[0]));pageButton(row[2],cx+nameWidth+permissionWidth,rowY,total-nameWidth-permissionWidth,()->openTransferFile(protocol,row[0]));}}
    private void sendTransfer(String protocol,String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.TransferFileCommandPacket(state.pos(),protocol,action,transferFileNameField==null?"":transferFileNameField.getValue(),transferContentField==null?"":transferContentField.getValue(),transferReadable,transferWritable,transferUserField==null?"":transferUserField.getValue(),transferPasswordField==null?"":transferPasswordField.getValue(),parseInt(transferPortField,protocol.equals("FTP")?21:69)));}
    private List<String[]> transferFileRows(){List<String[]> rows=new ArrayList<>();if(transferFiles==null||transferFiles.isBlank())return rows;for(String line:transferFiles.strip().split("\\n")){String[] p=line.split("\\t",-1);if(p.length>=4)rows.add(new String[]{p[0],(p[1].equals("true")?"R":"-")+(p[2].equals("true")?"W":"-"),p[3]+" B"});}return rows;}
    private void openTransferFile(String protocol,String filename){transferFileNameField=hiddenHttpContent(filename);transferStatus="Loading "+filename+"...";sendTransfer(protocol,"OPEN");}
    private void openNewTransferFile(){transferEditorOpen=true;transferFileNameField=hiddenHttpContent("new-file.txt");transferContentField=hiddenHttpContent("");transferReadable=true;transferWritable=true;init();}
    private void scrollTransferFiles(int delta){transferFileScroll=Math.max(0,Math.min(Math.max(0,transferFileRows().size()-10),transferFileScroll+delta));init();}
    public static void acceptTransferFileResult(String protocol,String message,String files,String users,String filename,String content,boolean readable,boolean writable,int port){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.transferStatus=message;screen.transferFiles=files;screen.transferUsers=users;screen.transferReadable=readable;screen.transferWritable=writable;if(protocol.equals("FTP"))screen.configuredFtpPort=port;else screen.configuredTftpPort=port;if(!filename.isEmpty()){screen.transferEditorOpen=true;screen.transferFileNameField=screen.hiddenHttpContent(filename);screen.transferContentField=screen.hiddenHttpContent(content);}screen.init();}}
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
    private void renderInteractiveListSurface(GuiGraphics g,int mouseX,int mouseY){
        ServerRackService service=ServerRackService.byDisplayName(servicePage);List<String[]> rows=interactiveRows(service);if(service==ServerRackService.HTTP||rows.isEmpty())return;
        int cx=x+side+24,right=x+w-28,rowRight=right-44,visible=interactiveVisibleRows(service),top=interactiveListTop(service),bottom=Math.min(y+h-34,top+visible*20),width=rowRight-cx;
        int firstX=cx+8,secondX=cx+width/3,thirdX=cx+(width*2)/3;
        g.fill(cx,top-23,right,bottom+1,0xFF202020);g.fill(cx,top-22,right,top-2,0xFF464646);
        String[] headers=interactiveHeaders(service);drawClipped(g,headers[0],firstX,top-16,width/3-12,0xFFFFFF);drawClipped(g,headers[1],secondX,top-16,width/3-12,0xFFFFFF);drawClipped(g,headers[2],thirdX,top-16,width/3-12,0xFFFFFF);
        int offset=serviceScrolls.getOrDefault(service.displayName(),0),shown=Math.min(visible,rows.size()-offset);
        for(int i=0;i<shown;i++){int ry=top+i*20;boolean hovered=mouseX>=cx&&mouseX<rowRight&&mouseY>=ry&&mouseY<ry+19;g.fill(cx,ry,rowRight,ry+19,hovered?0xFF555555:((i&1)==0?0xFF303030:0xFF292929));String[] columns=interactiveColumns(service,rows.get(offset+i)[1]);drawClipped(g,columns[0],firstX,ry+5,width/3-12,0xFFF0F0F0);drawClipped(g,columns[1],secondX,ry+5,width/3-12,0xFFE0E0E0);drawClipped(g,columns[2],thirdX,ry+5,width/3-12,0xFFD0D0D0);}
    }

    private int interactiveVisibleRows(ServerRackService service){return service==ServerRackService.SYSLOG?6:service==ServerRackService.AAA||service==ServerRackService.RADIUS_EAP?4:9;}
    private String[] interactiveHeaders(ServerRackService service){return switch(service){case DNS->new String[]{"Name","Record Type","Address / TTL"};case DHCP,DHCPV6->new String[]{"Pool","Address Range","Network Settings"};case EMAIL->new String[]{"Mailbox","Quota","Messages"};case SYSLOG->new String[]{"Facility","Severity","Message"};case AAA->new String[]{"Username","Privilege","Status"};case RADIUS_EAP->new String[]{"NAS Client","Address","Status"};case IOT->new String[]{"Device","Type","State / Telemetry"};case VM_MANAGEMENT->new String[]{"Virtual Machine","OS / Resources","State"};default->new String[]{"Name","Details","Status"};};}
    private String[] interactiveColumns(ServerRackService service,String raw){String[] p=raw.split("\\t",-1);return switch(service){case DNS->new String[]{valueAt(p,0),valueAt(p,1),valueAt(p,2)+(p.length>3?"  TTL "+p[3]:"")};case DHCP,DHCPV6->new String[]{valueAt(p,0),valueAt(p,1)+" - "+valueAt(p,2),valueAt(p,3)+" / "+valueAt(p,4)};case EMAIL->new String[]{valueAt(p,0),valueAt(p,1),"Open mailbox"};case SYSLOG->new String[]{valueAt(p,2),severityName(parseIntText(valueAt(p,3))),valueAt(p,4)};case AAA->new String[]{valueAt(p,0),"Level "+valueAt(p,1),Boolean.parseBoolean(valueAt(p,2))?"Enabled":"Disabled"};case RADIUS_EAP->new String[]{valueAt(p,0),valueAt(p,1),Boolean.parseBoolean(valueAt(p,2))?"Enabled":"Disabled"};case IOT->new String[]{valueAt(p,1),valueAt(p,2),valueAt(p,4).isBlank()?valueAt(p,5):valueAt(p,4)};case VM_MANAGEMENT->new String[]{valueAt(p,0),valueAt(p,1)+" / "+valueAt(p,2)+" CPU",valueAt(p,5)};default->new String[]{valueAt(p,0),valueAt(p,1),valueAt(p,2)};};}
    private String valueAt(String[] values,int index){return index<values.length?values[index]:"";}
    private void drawClipped(GuiGraphics g,String value,int drawX,int drawY,int maxWidth,int color){g.drawString(font,font.plainSubstrByWidth(value,Math.max(1,maxWidth)),drawX,drawY,color,false);}

    private int interactiveListTop(ServerRackService service){return switch(service){case DNS,DHCP,DHCPV6->y+266;case EMAIL->y+320;case SYSLOG->y+377;case AAA,RADIUS_EAP,IOT->y+333;case VM_MANAGEMENT->y+345;default->y+330;};}
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(button==0&&tab==Tab.SERVICES&&!servicePage.equals("HTTP")){
            ServerRackService service=ServerRackService.byDisplayName(servicePage);List<String[]> rows=interactiveRows(service);int visible=interactiveVisibleRows(service),top=interactiveListTop(service),left=x+side+24,right=x+w-72;
            if(mouseX>=left&&mouseX<right&&mouseY>=top&&mouseY<top+visible*20){int offset=serviceScrolls.getOrDefault(service.displayName(),0),index=offset+(int)((mouseY-top)/20);if(index>=0&&index<rows.size()){selectServiceRow(service,rows.get(index)[1]);return true;}}
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double delta){if(tab==Tab.DESKTOP&&desktopPage.equals("Text Editor")&&mouseX<x+185){scrollTextFiles(delta>0?-1:1);return true;}if(tab==Tab.DESKTOP&&(desktopPage.equals("FTP Client")||desktopPage.equals("TFTP Client"))){int amount=delta>0?-1:1;boolean ftp=desktopPage.equals("FTP Client");if(mouseX<x+w/2){if(ftp)ftpClientRemoteScroll=Math.max(0,Math.min(Math.max(0,ftpClientRows(ftpClientRemoteFiles).size()-10),ftpClientRemoteScroll+amount));else tftpClientRemoteScroll=Math.max(0,Math.min(Math.max(0,ftpClientRows(tftpClientRemoteFiles).size()-11),tftpClientRemoteScroll+amount));}else{if(ftp)ftpClientLocalScroll=Math.max(0,Math.min(Math.max(0,ftpClientRows(ftpClientLocalFiles).size()-10),ftpClientLocalScroll+amount));else tftpClientLocalScroll=Math.max(0,Math.min(Math.max(0,ftpClientRows(tftpClientLocalFiles).size()-11),tftpClientLocalScroll+amount));}init();return true;}if(tab==Tab.SERVICES&&mouseX>x+side&&mouseY>y+190){if(servicePage.equals("HTTP")&&!httpEditorOpen)scrollHttpFiles(delta>0?-1:1);else if((servicePage.equals("FTP")||servicePage.equals("TFTP"))&&!transferEditorOpen)scrollTransferFiles(delta>0?-1:1);else{ServerRackService service=ServerRackService.byDisplayName(servicePage);int visible=service==ServerRackService.SYSLOG?6:service==ServerRackService.AAA||service==ServerRackService.RADIUS_EAP?4:9;scrollService(service,delta>0?-1:1,visible);}return true;}return super.mouseScrolled(mouseX,mouseY,delta);}

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
        if(servicePage.equals("FTP")||servicePage.equals("TFTP")){renderTransferService(g,cx,servicePage);return;}
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
    private void desktop(GuiGraphics g){if(desktopPage.isEmpty())return;if(desktopPage.equals("Web Browser")){renderBrowser(g);return;}if(desktopPage.equals("Email")){renderMailClient(g);return;}if(desktopPage.equals("IP Configuration")){renderDesktopIpConfiguration(g);return;}if(desktopPage.equals("DNS Lookup")){renderDesktopDnsLookup(g);return;}if(desktopPage.equals("Text Editor")){renderDesktopTextEditor(g);return;}if(desktopPage.equals("FTP Client")){renderDesktopFtpClient(g);return;}if(desktopPage.equals("TFTP Client")){renderDesktopTftpClient(g);return;}g.drawCenteredString(font,desktopPage,x+w/2,y+45,0xFFFFFF);g.drawString(font,inputLabel(),x+125,y+70,0xCCCCCC);g.drawString(font,"Output",x+25,y+120,0xFFFFFF);g.fill(x+25,y+136,x+w-25,y+h-25,0xFF080808);int lineY=y+145;for(String raw:desktopOutput.split("\\n",-1)){for(FormattedCharSequence line:font.split(Component.literal(raw),w-70)){g.drawString(font,line,x+35,lineY,0xB8FFB8);lineY+=12;if(lineY>y+h-38)return;}}}
    private void renderDesktopIpConfiguration(GuiGraphics g){int left=x+40;g.drawCenteredString(font,"IP Configuration",x+w/2,y+45,0xFFFFFF);g.drawString(font,"Addressing Mode",left,y+77,0xCCCCCC);sectionLine(g,left,y+99,"IPv4 Configuration");g.drawString(font,"IPv4 Address",left,y+117,0xCCCCCC);g.drawString(font,"Subnet Mask",left,y+145,0xCCCCCC);g.drawString(font,"Default Gateway",left,y+173,0xCCCCCC);g.drawString(font,"DNS Server",left,y+201,0xCCCCCC);sectionLine(g,left,y+231,"IPv6 Configuration");g.drawString(font,"IPv6 Address",left,y+257,0xCCCCCC);g.drawString(font,"Prefix Length",left,y+285,0xCCCCCC);g.drawString(font,"Default Gateway",left,y+313,0xCCCCCC);g.drawString(font,"DNS Server",left,y+341,0xCCCCCC);g.drawString(font,"DHCP keeps the current address until a server grants a new lease.",left,y+412,0x999999);g.drawString(font,desktopOutput,left,y+434,0x88CCFF);}
    private void renderDesktopDnsLookup(GuiGraphics g){int left=x+40;g.drawCenteredString(font,"DNS Lookup",x+w/2,y+45,0xFFFFFF);sectionLine(g,left,y+72,"Resolver Query");g.drawString(font,"Name or reverse record",left,y+105,0xCCCCCC);g.drawString(font,"DNS Server",left,y+145,0xCCCCCC);g.drawString(font,"Supported record types: A, AAAA, CNAME, MX, PTR",left,y+225,0x999999);sectionLine(g,left,y+252,"Lookup Result");g.fill(left,y+274,x+w-40,y+h-28,0xFF080808);int lineY=y+286;for(String raw:desktopOutput.split("\\n",-1)){for(FormattedCharSequence line:font.split(Component.literal(raw),w-100)){g.drawString(font,line,left+12,lineY,0xB8FFB8);lineY+=13;if(lineY>y+h-42)return;}}}
    private void renderDesktopTextEditor(GuiGraphics g){int left=x+190,right=x+w-28;g.drawCenteredString(font,"Text Editor",x+w/2,y+45,0xFFFFFF);g.fill(x+16,y+62,x+184,y+h-24,0xFF202020);g.drawCenteredString(font,"Documents",x+100,y+61,0xFFFFFF);g.drawString(font,"Showing "+(textFileRows().isEmpty()?0:textFileScroll+1)+"-"+Math.min(textFileRows().size(),textFileScroll+14)+" of "+textFileRows().size(),x+25,y+h-94,0x999999);g.drawString(font,"File Name",left,y+72,0xCCCCCC);g.drawString(font,"Scrollable document editor",left,y+126,0xCCCCCC);g.drawString(font,textFileStatus,left,y+h-44,0x88CCFF);g.drawString(font,(textFileEditor==null?textFileContent.length():textFileEditor.getValue().length())+" / 32768 characters",right-130,y+h-44,0x999999);}
    private void renderDesktopFtpClient(GuiGraphics g){int left=x+28,mid=x+w/2;g.drawCenteredString(font,"FTP Client",x+w/2,y+45,0xFFFFFF);g.drawString(font,"Server IPv4",left,y+77,0xCCCCCC);g.drawString(font,"Username",left,y+105,0xCCCCCC);g.drawString(font,"Password",left+310,y+105,0xCCCCCC);g.drawString(font,ftpClientConnected?"Connected to "+ftpClientServer:"Not connected",left,y+137,ftpClientConnected?0x88FF88:0xFFAAAA);g.drawString(font,ftpClientStatus,left,y+153,0x88CCFF);sectionLine(g,left,y+174,"Remote FTP Files");sectionLine(g,mid+10,y+174,"Local Text Editor Documents");g.drawString(font,"Remote name",left,y+h-99,0xCCCCCC);g.drawString(font,"Local name",mid+10,y+h-99,0xCCCCCC);}
    private void renderDesktopTftpClient(GuiGraphics g){int left=x+28,mid=x+w/2;g.drawCenteredString(font,"TFTP Client",x+w/2,y+45,0xFFFFFF);g.drawString(font,"Server IPv4",left,y+83,0xCCCCCC);g.drawString(font,"TFTP uses UDP port 69 without user authentication.",left,y+115,0x999999);g.drawString(font,tftpClientConnected?"Server ready: "+tftpClientServer:"Not connected",left,y+135,tftpClientConnected?0x88FF88:0xFFAAAA);g.drawString(font,tftpClientStatus,left,y+151,0x88CCFF);sectionLine(g,left,y+164,"Remote TFTP Files");sectionLine(g,mid+10,y+164,"Local Text Editor Documents");g.drawString(font,"Remote name",left,y+h-99,0xCCCCCC);g.drawString(font,"Local name",mid+10,y+h-99,0xCCCCCC);}
    private void renderMailClient(GuiGraphics g){int left=x+30,right=x+w-30;g.drawCenteredString(font,"VSIA Mail Client",x+w/2,y+45,0xFFFFFF);if(!mailClientLoggedIn){g.drawString(font,"Mailbox Address",left,y+105,0xCCCCCC);g.drawString(font,"Password",left,y+137,0xCCCCCC);g.drawString(font,"Sign in with an account configured in Services > EMAIL.",left,y+205,0x999999);g.drawString(font,mailClientStatus,left,y+235,0x88CCFF);return;}g.drawString(font,mailClientAddress,left,y+99,0xBBBBBB);g.drawString(font,mailClientStatus,left+400,y+99,0x88CCFF);if(mailClientCompose){g.drawString(font,"New Message",left,y+110,0xFFFFFF);g.drawString(font,"To",left,y+131,0xCCCCCC);g.drawString(font,"Subject",left,y+163,0xCCCCCC);g.drawString(font,"Message",left,y+210,0xCCCCCC);return;}if(mailClientReading){g.drawString(font,mailClientSubject,left,y+155,0xFFFFFF);g.drawString(font,"From: "+mailClientFrom,left,y+177,0xCCCCCC);g.drawString(font,"To: "+mailClientTo,left,y+193,0xCCCCCC);g.drawString(font,"Sent: "+java.time.Instant.ofEpochMilli(mailClientSentAt),left,y+209,0x999999);g.fill(left,y+228,right,y+h-25,0xFF080808);int lineY=y+240;for(FormattedCharSequence line:font.split(Component.literal(mailClientBody),right-left-20)){g.drawString(font,line,left+10,lineY,0xE8E8E8);lineY+=12;if(lineY>y+h-38)break;}return;}g.fill(left,y+105,right,y+123,0xFF464646);g.drawString(font,mailClientFolder.equals("SENT")?"Sent Messages":"Inbox",left+8,y+110,0xFFFFFF);g.drawString(font,"Subject / Correspondent",left+170,y+110,0xDDDDDD);if(mailClientRows().isEmpty())g.drawCenteredString(font,"No messages in this folder.",x+w/2,y+165,0x999999);}
    private void renderBrowser(GuiGraphics g){int left=x+20,right=x+w-20,top=y+116,bottom=y+h-82;g.fill(left,top,right,bottom,browserBackground);g.hLine(left,right,top,0xFF888888);g.drawCenteredString(font,browserTitle,x+w/2,top+12,browserForeground);g.drawString(font,"HTTP "+(browserStatus==0?"Ready":browserStatus),left+10,top+12,browserStatus>=400?0xFFAA2222:0xFF228844);int contentBottom=browserNotice.isBlank()?bottom-14:bottom-38;int lineY=top+34;outer:for(String raw:browserPageText.split("\\n",-1)){for(FormattedCharSequence line:font.split(Component.literal(raw),right-left-24)){if(lineY>contentBottom)break outer;g.drawString(font,line,left+12,lineY,browserForeground);lineY+=12;}if(raw.isBlank())lineY+=5;}if(!browserNotice.isBlank()){g.fill(left+6,bottom-30,right-6,bottom-6,0xFFE2F0FA);drawClipped(g,browserNotice,left+14,bottom-22,right-left-28,0xFF245A78);}}
    private String inputLabel(){return switch(desktopPage){case "Ping"->"IPv4, IPv6, or host name (optional count: host 4)";case "DNS Lookup"->"Domain name";case "Web Browser"->"URL or IPv4 address";case "Email"->"Mailbox address";case "FTP Client"->"Server Action File Username Password Content";case "TFTP Client"->"Server Action File Content";case "Terminal","Command Prompt"->"Command";default->"Input";};}
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
    private void table(GuiGraphics g,int tx,int ty,String firstHeader,String secondHeader,String[][] rows){
        int tw=w-(tx-x)-25,thirdWidth=105;
        boolean threeColumns=false;for(String[] row:rows)if(row.length>2&&!row[2].isBlank()){threeColumns=true;break;}
        int firstWidth=threeColumns?(tw-thirdWidth)/2:tw/2;
        int secondX=tx+firstWidth,secondWidth=threeColumns?tw-firstWidth-thirdWidth:tw-firstWidth;
        int thirdX=tx+tw-thirdWidth;
        g.fill(tx,ty,tx+tw,ty+20,0xFF464646);
        drawClipped(g,firstHeader,tx+8,ty+6,firstWidth-16,0xFFFFFF);
        drawClipped(g,secondHeader,secondX+8,ty+6,secondWidth-16,0xFFFFFF);
        for(int i=0;i<rows.length;i++){
            int ry=ty+21+i*20;g.fill(tx,ry,tx+tw,ry+19,(i&1)==0?0xFF292929:0xFF252525);
            drawClipped(g,rows[i][0],tx+8,ry+5,firstWidth-16,0xEEEEEE);
            if(rows[i].length>1)drawClipped(g,rows[i][1],secondX+8,ry+5,secondWidth-16,0xDDDDDD);
            if(threeColumns&&rows[i].length>2)drawClipped(g,rows[i][2],thirdX+8,ry+5,thirdWidth-16,0xDDDDDD);
        }
    }
    private String[][] dhcpRows(String data){if(data==null||data.isBlank())return new String[][]{{"No data","",""}};String[] lines=data.strip().split("\\n");List<String[]> rows=new ArrayList<>();String section="";for(String line:lines){if(line.equals("POOLS")||line.equals("LEASES")){section=line;continue;}String[] p=line.split("\\t",-1);if(section.equals("POOLS")&&p.length>=7)rows.add(new String[]{"Pool "+p[0],p[1]+" - "+p[2],"Lease "+p[6]+"s"});else if(section.equals("LEASES")&&p.length>=4)rows.add(new String[]{"Lease "+p[0],p[1],p[2]});if(rows.size()>=12)break;}return rows.isEmpty()?new String[][]{{"No active entries","",""}}:rows.toArray(new String[0][]);}
    private void renderHttpService(GuiGraphics g,int cx){int right=x+w-28,half=(right-cx-16)/2;if(httpEditorOpen){sectionLine(g,cx,y+98,"Hosted File Editor");g.drawString(font,"File Name",cx,y+117,0xCCCCCC);sectionLine(g,cx,y+158,"Source Code");g.drawString(font,"Scrollable multi-line editor",cx,y+174,0xCCCCCC);g.drawString(font,"Use Auto Format for HTML, CSS, or JavaScript indentation.",cx+210,y+174,0x999999);g.drawString(font,httpStatus,cx,y+h-44,0x88CCFF);return;}sectionLine(g,cx,y+98,"Web Service Settings");g.drawString(font,"HTTP Port",cx,y+115,0xCCCCCC);g.drawString(font,"HTTPS Port",cx+half+16,y+115,0xCCCCCC);sectionLine(g,cx,y+199,"Hosted Files - click a row to open it");int top=y+216,listWidth=right-cx,fileWidth=listWidth/2,permissionWidth=listWidth/4,sizeWidth=listWidth-fileWidth-permissionWidth;g.fill(cx,top,right,top+18,0xFF464646);g.drawCenteredString(font,"File Name",cx+fileWidth/2,top+5,0xFFFFFF);g.drawCenteredString(font,"Permissions",cx+fileWidth+permissionWidth/2,top+5,0xFFFFFF);g.drawCenteredString(font,"Size",cx+fileWidth+permissionWidth+sizeWidth/2,top+5,0xFFFFFF);g.vLine(cx+fileWidth,top,top+18,0xFF777777);g.vLine(cx+fileWidth+permissionWidth,top,top+18,0xFF777777);g.drawString(font,"Showing "+(httpFileRows().isEmpty()?0:httpFileScroll+1)+"-"+Math.min(httpFileRows().size(),httpFileScroll+12)+" of "+httpFileRows().size(),cx,y+490,0xAAAAAA);g.drawString(font,"Mouse wheel or arrow buttons scroll the list.",cx+250,y+490,0x999999);g.drawString(font,httpStatus,cx,y+h-48,0x88CCFF);}
    private void renderTransferService(GuiGraphics g,int cx,String protocol){int right=x+w-28;boolean ftp=protocol.equals("FTP");if(transferEditorOpen){sectionLine(g,cx,y+98,protocol+" File Editor");g.drawString(font,"File Name",cx,y+115,0xCCCCCC);g.drawString(font,"File Content",cx,y+163,0xCCCCCC);g.drawString(font,"Shared storage: files saved here are also available to HTTP and the other transfer service.",cx,y+304,0x999999);g.drawString(font,transferStatus,cx,y+h-48,0x88CCFF);return;}sectionLine(g,cx,y+98,protocol+" Service Configuration");g.drawString(font,protocol+" Port",cx,y+115,0xCCCCCC);if(ftp){sectionLine(g,cx,y+158,"FTP User Accounts");g.drawString(font,"Username",cx,y+173,0xCCCCCC);g.drawString(font,"Password",cx+190,y+173,0xCCCCCC);g.drawString(font,"Registered users: "+(transferUsers==null||transferUsers.isBlank()?"None":transferUsers.replace("\n",", ")),cx,y+215,0xAAAAAA);}int top=ftp?y+252:y+192;sectionLine(g,cx,top,"Hosted Files - click a row to edit");int header=top+18,total=right-cx,nameWidth=total/2,permissionWidth=total/4;g.fill(cx,header,right,header+18,0xFF464646);g.drawCenteredString(font,"File Name",cx+nameWidth/2,header+5,0xFFFFFF);g.drawCenteredString(font,"Permissions",cx+nameWidth+permissionWidth/2,header+5,0xFFFFFF);g.drawCenteredString(font,"Size",cx+nameWidth+permissionWidth+(total-nameWidth-permissionWidth)/2,header+5,0xFFFFFF);g.drawString(font,"Showing "+(transferFileRows().isEmpty()?0:transferFileScroll+1)+"-"+Math.min(transferFileRows().size(),transferFileScroll+10)+" of "+transferFileRows().size(),cx,y+h-70,0xAAAAAA);g.drawString(font,transferStatus,cx,y+h-48,0x88CCFF);}
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
            drawClipped(g,users[i][0],cx+10,ry+6,quotaColumn-cx-18,0xEEEEEE);
            drawClipped(g,users[i][1],quotaColumn,ry+6,messageColumn-quotaColumn-10,0xDDDDDD);
            drawClipped(g,users[i][2],messageColumn,ry+6,right-messageColumn-10,0xDDDDDD);
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
        else for(String line:aaaUsers.strip().split("\\n")){String[] p=line.split("\\t",3);g.fill(cx,row,right,row+19,0xFF292929);drawClipped(g,p[0],cx+8,row+5,200,0xEEEEEE);drawClipped(g,p.length>1?p[1]:"",cx+220,row+5,100,0xDDDDDD);drawClipped(g,p.length>2&&Boolean.parseBoolean(p[2])?"Enabled":"Disabled",cx+330,row+5,right-(cx+338),0xDDDDDD);row+=20;if(row>y+h-190)break;}
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
            drawClipped(g,p.length>2?p[1]+" ["+p[2]+"]":p[0],cx+8,row+5,215,0xEEEEEE);
            drawClipped(g,p.length>3&&Boolean.parseBoolean(p[3])?"Online":"Offline",cx+235,row+5,128,p.length>3&&Boolean.parseBoolean(p[3])?0x88FF88:0xFF8888);
            drawClipped(g,p.length>5?p[4]+" / "+p[5]:"",cx+375,row+5,right-(cx+383),0xDDDDDD);
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
            drawClipped(g,p.length>1?p[0]+" ["+p[1]+"]":p[0],cx+8,row+5,235,0xEEEEEE);
            drawClipped(g,p.length>4?p[2]+" CPU / "+p[3]+" MB / "+p[4]+" GB":"",cx+255,row+5,163,0xDDDDDD);
            drawClipped(g,p.length>6?p[5]+" / "+p[6]:"",cx+430,row+5,right-(cx+438),p.length>5&&p[5].equals("RUNNING")?0x88FF88:0xCCCCCC);
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
