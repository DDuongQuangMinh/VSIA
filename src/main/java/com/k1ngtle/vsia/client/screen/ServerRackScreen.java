package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.signality.internet.server.ServerRackNetwork;
import com.k1ngtle.vsia.signality.internet.server.ServerRackNetwork.OpenRackPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ServerRackScreen extends Screen {
    private enum Tab { PHYSICAL, CONFIG, SERVICES, DESKTOP, PROGRAMMING, ATTRIBUTES }
    private final OpenRackPacket state;
    private final List<Button> pageButtons = new ArrayList<>();
    private Tab tab = Tab.PHYSICAL;
    private String configPage = "Settings", servicePage = "HTTP", desktopPage = "";
    private EditBox name, ip, subnet, gateway, dns, toolInput, programInput;
    private boolean dhcp, http, dnsService, dhcpService, mail;
    private int x, y, w, h, side;

    public ServerRackScreen(OpenRackPacket state) {
        super(Component.literal("VSIA Server Rack"));
        this.state = state;
        dhcp = state.dhcp(); http = state.http(); dnsService = state.dnsService();
        dhcpService = state.dhcpService(); mail = state.mail();
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
        pageButton("INTERFACE",x+7,y+78,side-14,()->{});
        pageButton("FastEthernet0",x+7,y+98,side-14,()->{configPage="FastEthernet0";init();});
        int cx=x+side+36;
        if (configPage.equals("Settings")) {
            name=field(cx+150,y+65,w-side-225,state.displayName(),32);
            gateway=field(cx+150,y+151,w-side-225,state.gateway(),15);
            dns=field(cx+150,y+179,w-side-225,state.dns(),15);
            pageButton(dhcp?"[ DHCP ]     Static":"DHCP     [ Static ]",cx+150,y+111,190,()->{dhcp=!dhcp;init();});
        } else {
            ip=field(cx+150,y+112,w-side-225,state.ip(),15);
            subnet=field(cx+150,y+140,w-side-225,state.subnet(),15);
            pageButton("Port Status     [ ON ]",cx+150,y+70,190,()->{});
        }
        pageButton("Save Configuration",x+w-157,y+h-29,145,()->save());
    }

    private void initServices() {
        String[] services={"SERVICES","HTTP","DHCP","DNS","EMAIL"};
        for(int i=0;i<services.length;i++){final String s=services[i];pageButton(s,x+7,y+34+i*20,side-14,()->{if(!s.equals("SERVICES")){servicePage=s;init();}});}
        int cx=x+side+24;
        if(servicePage.equals("HTTP")) serviceToggle("HTTP",cx,y+64,http,v->http=v);
        else if(servicePage.equals("DHCP")) serviceToggle("DHCP",cx,y+64,dhcpService,v->dhcpService=v);
        else if(servicePage.equals("DNS")) serviceToggle("DNS",cx,y+64,dnsService,v->dnsService=v);
        else serviceToggle("EMAIL / SMTP",cx,y+64,mail,v->mail=v);
        pageButton("Save Services",x+w-137,y+h-29,125,()->save());
    }

    private interface BoolSet {void set(boolean v);}
    private void serviceToggle(String label,int bx,int by,boolean value,BoolSet set){
        pageButton(label+"   "+(value?"[ON]    Off":"On    [OFF]"),bx,by,260,()->{set.set(!value);init();});
    }

    private void initDesktop() {
        if(!desktopPage.isEmpty()){
            pageButton("← Desktop",x+14,y+35,100,()->{desktopPage="";init();});
            toolInput=field(x+125,y+82,w-155,"",128);
            pageButton("Run",x+w-80,y+82,60,()->{}); return;
        }
        String[] tools={"IP Configuration","Terminal","Command Prompt","Web Browser","Email","DNS Lookup","Ping","Text Editor"};
        for(int i=0;i<tools.length;i++){int col=i%4,row=i/4;String tool=tools[i];pageButton(tool,x+35+col*((w-70)/4),y+75+row*95,145,()->{desktopPage=tool;init();});}
    }

    private void initProgramming() {
        pageButton("New",x+10,y+34,50,()->{}); pageButton("Open",x+64,y+34,50,()->{});
        pageButton("Save",x+118,y+34,50,()->{}); pageButton("Run",x+w-118,y+34,50,()->{});
        programInput=field(x+side,y+82,w-side-20,"hostname Server0",4096);
    }

    private EditBox field(int bx,int by,int bw,String value,int max){EditBox e=new EditBox(font,bx,by,Math.max(60,bw),18,Component.empty());e.setMaxLength(max);e.setValue(value);addRenderableWidget(e);return e;}

    private void save(){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.SaveConfigPacket(state.pos(),
            name==null?state.displayName():name.getValue(),ip==null?state.ip():ip.getValue(),subnet==null?state.subnet():subnet.getValue(),
            gateway==null?state.gateway():gateway.getValue(),dns==null?state.dns():dns.getValue(),dhcp,http,dnsService,dhcpService,mail));}

    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderBackground(g); g.fill(x,y,x+w,y+h,0xFF181818); g.fill(x+2,y+28,x+w-2,y+h-2,0xFF202020);
        g.hLine(x+2,x+w-2,y+27,0xFF777777);
        if(tab==Tab.CONFIG||tab==Tab.SERVICES||tab==Tab.PHYSICAL) {g.fill(x+5,y+31,x+side,y+h-5,0xFF292929);g.vLine(x+side,y+31,y+h-5,0xFF888888);}
        if(tab==Tab.DESKTOP&&!desktopPage.isEmpty()) g.fill(x+8,y+61,x+w-8,y+h-8,0xFF101820);
        if(tab==Tab.DESKTOP&&desktopPage.isEmpty()) g.fill(x+5,y+31,x+w-5,y+h-5,0xFF50B7CE);
        if(tab==Tab.PROGRAMMING) g.fill(x+7,y+58,x+w-7,y+h-35,0xFFF3F3F3);
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
        g.drawCenteredString(font,"VSIA SERVER RACK — 3D device preview",px+pw/2,py+ph/2-5,0x80FF80);
        g.drawString(font,"Power: ON",px+18,py+ph+12,0x80FF80);g.drawString(font,"Door: OPEN",px+110,py+ph+12,0xFFFFFF);
    }
    private void config(GuiGraphics g){title(g,configPage.equals("Settings")?"Global Settings":"FastEthernet0");int cx=x+side+36;
        if(configPage.equals("Settings")){
            g.drawString(font,"Display Name",cx,y+70,0xFFFFFF);
            sectionLine(g,cx,y+99,"IPv4 Gateway and DNS");
            g.drawString(font,"Addressing Mode",cx,y+116,0xFFFFFF);
            g.drawString(font,"Default Gateway",cx,y+156,0xFFFFFF);
            g.drawString(font,"DNS Server",cx,y+184,0xFFFFFF);
            sectionLine(g,cx,y+216,"Device Information");
            g.drawString(font,"Clock source: Minecraft server time",cx,y+233,0xBBBBBB);
        } else {
            sectionLine(g,cx,y+61,"Interface Settings");
            g.drawString(font,"Interface Status",cx,y+75,0xFFFFFF);
            g.drawString(font,"IPv4 Address",cx,y+117,0xFFFFFF);
            g.drawString(font,"Subnet Mask",cx,y+145,0xFFFFFF);
            g.drawString(font,"MAC Address",cx,y+180,0xFFFFFF);
            g.drawString(font,"Automatically managed by the device",cx+150,y+180,0xBBBBBB);
        }}
    private void services(GuiGraphics g){title(g,servicePage);int cx=x+side+24;
        sectionLine(g,cx,y+103,"Service Configuration");
        if(servicePage.equals("HTTP")){g.drawString(font,"Hosted Files",cx,y+130,0xCCCCCC);table(g,cx,y+148,"File Name","Action",new String[][]{{"index.html","Edit","Delete"},{"styles.css","Edit","Delete"},{"script.js","Edit","Delete"}});}
        else if(servicePage.equals("DNS")){g.drawString(font,"DNS Resource Records",cx,y+130,0xCCCCCC);table(g,cx,y+148,"Name","Address",new String[][]{{"www.vsia-net.com",state.ip(),"A"},{"mail.vsia-net.com",state.ip(),"A"}});}
        else if(servicePage.equals("DHCP")){g.drawString(font,"Address Pool: 192.168.1.100 - 192.168.1.254",cx,y+130,0xCCCCCC);table(g,cx,y+148,"Setting","Value",new String[][]{{"Default Gateway",state.gateway(),""},{"DNS Server",state.dns(),""},{"Subnet Mask",state.subnet(),""}});}
        else{g.drawString(font,"Mail Domain: vsia-net.com",cx,y+130,0xCCCCCC);table(g,cx,y+148,"User","Mailbox",new String[][]{{"admin","Open","Delete"},{"player","Open","Delete"}});}}
    private void desktop(GuiGraphics g){if(desktopPage.isEmpty())return;g.drawCenteredString(font,desktopPage,x+w/2,y+45,0xFFFFFF);g.drawString(font,"Input / address / command",x+125,y+70,0xCCCCCC);g.drawString(font,"Output",x+25,y+120,0xFFFFFF);g.fill(x+25,y+136,x+w-25,y+h-25,0xFF080808);}
    private void programming(GuiGraphics g){g.drawString(font,"Project",x+12,y+67,0x222222);g.drawString(font,"server-config",x+12,y+83,0x444444);g.drawString(font,"Configuration Script",x+side,y+67,0x222222);g.drawString(font,"Console Output",x+12,y+h-29,0xCCCCCC);}
    private void attributes(GuiGraphics g){g.drawString(font,"Device Attributes",x+16,y+39,0xFFFFFF);String[][] a={{"Mean Time Between Failures","61,320 hours"},{"Cost","2,000"},{"Power Source","Internal"},{"Rack Units","3U"},{"Power Consumption","200 W"},{"Device Model","VSIA Server Rack"},{"IPv4 Address",state.ip()},{"World Position",state.pos().toShortString()}};table(g,x+20,y+62,"Attribute","Value",a);}
    private void sectionLine(GuiGraphics g,int sx,int sy,String text){g.drawString(font,text,sx,sy,0xDDDDDD);int start=sx+font.width(text)+8;g.hLine(start,x+w-24,sy+4,0xFF555555);}
    private void table(GuiGraphics g,int tx,int ty,String firstHeader,String secondHeader,String[][] rows){int tw=w-(tx-x)-25;g.fill(tx,ty,tx+tw,ty+20,0xFF464646);g.drawString(font,firstHeader,tx+8,ty+6,0xFFFFFF);g.drawString(font,secondHeader,tx+tw/2,ty+6,0xFFFFFF);for(int i=0;i<rows.length;i++){int ry=ty+21+i*20;g.fill(tx,ry,tx+tw,ry+19,(i&1)==0?0xFF292929:0xFF252525);g.drawString(font,rows[i][0],tx+8,ry+5,0xEEEEEE);if(rows[i].length>1)g.drawString(font,rows[i][1],tx+tw/2,ry+5,0xDDDDDD);if(rows[i].length>2)g.drawString(font,rows[i][2],tx+tw-75,ry+5,0xDDDDDD);}}
    @Override public boolean isPauseScreen(){return false;}
}
