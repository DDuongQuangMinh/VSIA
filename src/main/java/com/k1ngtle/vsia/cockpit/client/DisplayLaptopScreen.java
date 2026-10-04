package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.program.*;
import com.k1ngtle.vsia.cockpit.network.*;
import com.k1ngtle.vsia.item.*;
import com.k1ngtle.vsia.network.VsiaNetwork;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;

/** A drive-bound screen, never a global cockpit config. Server checks every write independently. */
public final class DisplayLaptopScreen extends Screen {
    private S2CDisplayProgramPacket snapshot;
    private final long openedAt=System.nanoTime();
    private List<DisplayDesign.Widget> widgets=new ArrayList<>();private String theme="CYAN",status="",programName,source;private DisplayCodeLanguage language;
    private EditBox name,label;private MultiLineEditBox code;private int selected=-1,preset,palettePage;private boolean coding,dragging,resizing,busy,stale;private float canvasX,canvasY,canvasScale,offsetX,offsetY;
    private final Map<DisplayCodeLanguage,String> drafts=new EnumMap<>(DisplayCodeLanguage.class);
    private UUID localJob;
    private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> editorDimension;
    public DisplayLaptopScreen(S2CDisplayProgramPacket p){super(Component.literal("DISPLAY LAPTOP"));snapshot=p;editorDimension=Minecraft.getInstance().level==null?null:Minecraft.getInstance().level.dimension();coding=p.placed();programName=p.name().equals("Empty")?"My display":p.name();source=p.source();language=DisplayCodeLanguage.valueOf(p.language());status=p.status();loadLayout(p.layout());}
    public static void receive(S2CDisplayProgramPacket p){Minecraft mc=Minecraft.getInstance();if(p.open())mc.setScreen(new DisplayLaptopScreen(p));else if(mc.screen instanceof DisplayLaptopScreen s&&s.snapshot.session().equals(p.session())&&s.snapshot.driveId().equals(p.driveId()))s.accept(p);}
    private void accept(S2CDisplayProgramPacket p){remember();snapshot=p;status=p.status();busy=localJob!=null||status.startsWith("BUSY:");stale|=status.startsWith("STALE:");if(status.startsWith("OK:")){loadLayout(p.layout());if(p.layout().isEmpty()){source=p.source();drafts.clear();}}rebuildWidgets();}
    private void loadLayout(String json){try{DisplayDesign d=json.isBlank()?DisplayDesign.preset(0):DisplayDesign.parse(json);theme=d.theme();widgets=new ArrayList<>(d.widgets());selected=-1;}catch(Exception e){widgets=new ArrayList<>(DisplayDesign.preset(0).widgets());status="Stored layout invalid; previewing Tactical preset";}}
    private DisplayDesign design(){return new DisplayDesign(theme,widgets);}
    private boolean compact(){return width<380||height<260;}
    private void remember(){if(name!=null)programName=name.getValue();if(code!=null&&coding)source=code.getValue();drafts.put(language,source);}
    private void switchMode(){remember();coding=!coding;rebuildWidgets();}
    @Override protected void init(){
        name=null;code=null;label=null;
        if(compact()){button(8,48,100,"Close",this::onClose,true);return;}
        canvasX=112;canvasY=86;canvasScale=Math.max(.01f,Math.min((width-224f)/860f,(height-143f)/343f));
        name=new EditBox(font,8,24,Math.max(80,width-190),18,Component.literal("Program name"));name.setMaxLength(48);name.setValue(programName);name.setResponder(v->programName=v);name.setEditable(canWrite());addRenderableWidget(name);
        button(width-172,24,78,coding?"Designer":"Code",()->switchMode(),true);button(width-88,24,80,"Close",this::onClose,true);
        if(coding){
            button(8,48,116,language.label,()->{remember();language=DisplayCodeLanguage.values()[(language.ordinal()+1)%9];source=drafts.getOrDefault(language,"");rebuildWidgets();},canWrite());
            button(130,48,100,"Example",()->confirm("Replace this code draft with an example?",()->{source=language.example(design());rebuildWidgets();}),canWrite());
            button(236,48,Math.max(78,width-244),"Compile + write (local)",()->send(1),canWrite());
            code=new MultiLineEditBox(font,8,86,width-16,Math.max(32,height-143),Component.literal("Native display code. Compilation runs on YOUR client in isolated Docker."),Component.literal("Source"));code.setCharacterLimit(DisplayProgramLimits.SOURCE_CHARS);code.setValue(source);code.setValueListener(v->source=v);code.active=canWrite();addRenderableWidget(code);
        }else{
            code=null;
            button(8,48,102,DisplayDesign.PRESETS.get(preset),()->confirm("Replace the unsaved layout with the next preset?",()->{preset=(preset+1)%8;DisplayDesign d=DisplayDesign.preset(preset);widgets=new ArrayList<>(d.widgets());theme=d.theme();selected=-1;rebuildWidgets();}),!snapshot.readOnly());
            button(116,48,76,"Write drive",()->send(0),canWrite());button(198,48,72,"F-35 stock",()->confirm("Write the stock F-35 program to this drive?",()->send(2)),canWrite());button(276,48,Math.max(36,Math.min(68,width-284)),"Erase",()->confirm("Erase THIS drive's program and code?",()->send(3)),canWrite());
            int rows=Math.max(1,(height-166)/20),start=palettePage*rows;
            for(int i=0;i<rows&&start+i<DisplayDesign.TYPES.size();i++){String type=DisplayDesign.TYPES.get(start+i);button(8,86+i*20,98,type,()->{if(widgets.size()>=48){status="Limit: 48 widgets";return;}widgets.add(new DisplayDesign.Widget(type,300,80,200,160,0,"","SPEED"));selected=widgets.size()-1;dragging=true;offsetX=100;offsetY=80;rebuildWidgets();},canWrite());}
            button(8,height-76,46,"<",()->{palettePage=Math.max(0,palettePage-1);rebuildWidgets();},true);button(60,height-76,46,">",()->{palettePage=(palettePage+1)%((DisplayDesign.TYPES.size()+rows-1)/rows);rebuildWidgets();},true);
            int rx=width-104;
            button(rx,86,96,"Theme "+theme,()->{theme=DisplayDesign.THEMES.get((DisplayDesign.THEMES.indexOf(theme)+1)%4);rebuildWidgets();},canWrite());
            if(selected>=0&&selected<widgets.size()){
                var w=widgets.get(selected);label=new EditBox(font,rx,110,96,18,Component.literal("Widget label"));label.setMaxLength(48);label.setValue(w.text());label.setEditable(canWrite());label.setResponder(v->{if(selected>=0&&selected<widgets.size()&&!v.chars().anyMatch(c->c<32||c==167)){var old=widgets.get(selected);widgets.set(selected,old.style(old.variant(),v,old.binding()));}});addRenderableWidget(label);
                button(rx,132,96,"Style "+w.variant(),()->{var old=widgets.get(selected);widgets.set(selected,old.style((old.variant()+1)%5,old.text(),old.binding()));rebuildWidgets();},canWrite());
                button(rx,154,96,w.binding(),()->{var old=widgets.get(selected);widgets.set(selected,old.style(old.variant(),old.text(),DisplayDesign.BINDINGS.get((DisplayDesign.BINDINGS.indexOf(old.binding())+1)%11)));rebuildWidgets();},canWrite());
                button(rx,176,96,w.section()==0?"Section AUTO":"Section "+w.section(),()->{var old=widgets.get(selected);widgets.set(selected,old.inSection((old.section()+1)%7));rebuildWidgets();},canWrite());
                if(height>=304){button(rx,198,96,"Duplicate",()->{if(widgets.size()<48){widgets.add(widgets.get(selected).move(w.x()+16,w.y()+16));selected=widgets.size()-1;rebuildWidgets();}},canWrite());button(rx,220,96,"Remove",()->{widgets.remove(selected);selected=-1;rebuildWidgets();},canWrite());}
                else button(rx,198,96,"Remove",()->{widgets.remove(selected);selected=-1;rebuildWidgets();},canWrite());
            }else label=null;
        }
    }
    private boolean canWrite(){return !snapshot.readOnly()&&!busy&&!stale;}
    private void button(int x,int y,int w,String text,Runnable action,boolean enabled){Button b=Button.builder(Component.literal(text),v->action.run()).bounds(x,y,w,18).build();b.active=enabled;addRenderableWidget(b);}
    private void confirm(String text,Runnable action){remember();minecraft.setScreen(new ConfirmScreen(yes->{minecraft.setScreen(this);if(yes)action.run();},Component.literal(text),Component.literal("Only the selected drive will be changed.")));}
    private void send(int action){if(!canWrite())return;remember();if(action==1){confirm("Run this code on YOUR computer in isolated Docker? Never run code you do not trust.",this::compileLocal);return;}submit(action,programName,language.name(),design().json(),source);}
    private void submit(int action,String name,String lang,String layout,String text){
        UUID upload=null;
        if(action==DisplayProgramSubmission.DESIGNER||action==DisplayProgramSubmission.CLIENT_COMPILED){
            List<String> parts;try{parts=DisplayProgramLimits.uploadParts(text);}catch(IllegalArgumentException e){status="ERROR: "+e.getMessage();return;}
            upload=UUID.randomUUID();for(int i=0;i<parts.size();i++)VsiaNetwork.sendToServer(new C2SDisplaySourceChunkPacket(snapshot.session(),snapshot.driveId(),snapshot.revision(),upload,i,parts.size(),parts.get(i)));
        }
        VsiaNetwork.sendToServer(new C2SDisplayProgramPacket(snapshot.session(),snapshot.driveId(),snapshot.revision(),action,name,lang,layout,"",upload));busy=true;status="BUSY: waiting for server validation/write...";rebuildWidgets();
    }
    private void compileLocal(){
        if(!canWrite())return;remember();final String text=source,title=programName,lang=language.name();
        try{DisplayProgramLimits.validate(text);}catch(IllegalArgumentException e){status="ERROR: "+e.getMessage();return;}
        final var before=snapshot;final var connection=minecraft.getConnection();final UUID job=UUID.randomUUID();localJob=job;busy=true;status="BUSY: compiling on YOUR client in isolated Docker...";rebuildWidgets();
        DisplayLocalCompiler.compile(lang,text).thenAccept(result->minecraft.execute(()->{
            if(!job.equals(localJob))return;localJob=null;busy=false;
            if(minecraft.screen!=this||minecraft.getConnection()!=connection||minecraft.player==null)return;
            var held=editorDrive(before);
            if(!DisplayLocalCompiler.sameEditor(before.session(),before.driveId(),before.revision(),snapshot.session(),snapshot.driveId(),snapshot.revision())||!before.driveId().equals(DisplayHardDriveItem.driveId(held))||before.revision()!=DisplayHardDriveItem.revision(held)||stale){stale=true;status="STALE: local compile result discarded; laptop/drive/session changed. Reopen laptop.";rebuildWidgets();return;}
            if(!result.ok()){status="ERROR: "+result.error();rebuildWidgets();return;}
            submit(DisplayProgramSubmission.CLIENT_COMPILED,title,lang,result.design().json(),text);
        }));
    }
    @Override public void onClose(){localJob=null;super.onClose();}
    private net.minecraft.world.item.ItemStack editorDrive(S2CDisplayProgramPacket p){
        if(minecraft.player==null||minecraft.level==null||editorDimension==null||!editorDimension.equals(minecraft.level.dimension()))return net.minecraft.world.item.ItemStack.EMPTY;
        if(p.placed()){
            if(!minecraft.level.hasChunkAt(p.laptopPos()))return net.minecraft.world.item.ItemStack.EMPTY;
            if(!(minecraft.level.getBlockEntity(p.laptopPos()) instanceof DisplayLaptopBlockEntity laptop)||laptop.isRemoved())return net.minecraft.world.item.ItemStack.EMPTY;
            if(!DisplayLaptopSessionRules.accessible(p.laptopId(),laptop.laptopId(),true,true,!minecraft.player.isSpectator(),minecraft.player.position().distanceToSqr(com.k1ngtle.vsia.cockpit.F35VsShipHelper.blockWorldPosition(laptop))))return net.minecraft.world.item.ItemStack.EMPTY;
            return laptop.displayDrive();
        }
        var other=minecraft.player.getItemInHand(p.hand()==net.minecraft.world.InteractionHand.MAIN_HAND?net.minecraft.world.InteractionHand.OFF_HAND:net.minecraft.world.InteractionHand.MAIN_HAND);
        return other.getItem() instanceof DisplayLaptopItem?minecraft.player.getItemInHand(p.hand()):net.minecraft.world.item.ItemStack.EMPTY;
    }
    @Override public void tick(){if(minecraft.player==null)return;if(System.nanoTime()-openedAt>=java.util.concurrent.TimeUnit.MINUTES.toNanos(5)){minecraft.player.displayClientMessage(Component.literal("Laptop session expired; reopen to continue"),true);minecraft.setScreen(null);return;}var drive=editorDrive(snapshot);
        if(!snapshot.driveId().equals(DisplayHardDriveItem.driveId(drive))){onClose();return;}if(name!=null)name.tick();if(code!=null)code.tick();}
    @Override public boolean isPauseScreen(){return false;}
    private boolean canvasContains(double x,double y){return x>=canvasX&&y>=canvasY&&x<=canvasX+860*canvasScale&&y<=canvasY+343*canvasScale;}
    @Override public boolean mouseClicked(double mx,double my,int button){if(!compact()&&!coding&&button==0&&canvasContains(mx,my)){
        float x=(float)(mx-canvasX)/canvasScale,y=(float)(my-canvasY)/canvasScale;selected=-1;
        for(int i=widgets.size()-1;i>=0;i--){var w=widgets.get(i);if(x>=w.x()&&x<=w.x()+w.w()&&y>=w.y()&&y<=w.y()+w.h()){selected=i;offsetX=x-w.x();offsetY=y-w.y();resizing=x>w.x()+w.w()-18&&y>w.y()+w.h()-18;dragging=canWrite();break;}}
        remember();rebuildWidgets();return true;
    }return super.mouseClicked(mx,my,button);}
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(!compact()&&!coding&&button==0&&dragging&&selected>=0&&canWrite()){
        var w=widgets.get(selected);int x=Math.round((float)(mx-canvasX)/canvasScale),y=Math.round((float)(my-canvasY)/canvasScale);
        widgets.set(selected,resizing?w.resize(x-w.x(),y-w.y()):w.move(Math.round(x-offsetX),Math.round(y-offsetY)));return true;
    }return super.mouseDragged(mx,my,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){dragging=false;resizing=false;return super.mouseReleased(x,y,button);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(!compact()&&!coding&&key==261&&selected>=0&&canWrite()&&(label==null||!label.isFocused())&&!name.isFocused()){widgets.remove(selected);selected=-1;rebuildWidgets();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);g.drawString(font,(snapshot.placed()?"LAPTOP "+snapshot.laptopId().toString().substring(0,8).toUpperCase()+" / ":"")+"DISPLAY DRIVE "+snapshot.driveId().toString().substring(0,8).toUpperCase()+" / REV "+snapshot.revision()+(snapshot.readOnly()?" / READ ONLY":""),8,8,0x88dddd);
        if(compact()){int row=0;for(var line:font.split(Component.literal("Editor needs at least 380 x 260 GUI pixels. Reduce Minecraft's GUI Scale in Video Settings, then reopen the laptop. Unsaved drafts are retained while resizing this screen."),width-16))g.drawString(font,line,8,76+row++*11,0xdddddd);super.render(g,mx,my,partial);return;}
        if(!coding){g.fill((int)canvasX,(int)canvasY,(int)(canvasX+860*canvasScale),(int)(canvasY+343*canvasScale),0xff050a0a);
            // GuiGraphics scissors form a stack. Own one entry and release it before widgets render.
            try(DisplayGuiClipScope clips=new DisplayGuiClipScope(new DisplayGuiClipScope.Backend(){
                public void push(int left,int top,int right,int bottom){g.enableScissor(left,top,right,bottom);}
                public void pop(){g.disableScissor();}
            })){
            DisplayDesignPainter.Draw draw=new DisplayDesignPainter.Draw(){
                public void line(float x1,float y1,float x2,float y2,int color){int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(x2-x1),Math.abs(y2-y1))*canvasScale));for(int i=0;i<=steps;i++){float t=i/(float)steps;int x=Math.round(canvasX+(x1+(x2-x1)*t)*canvasScale),y=Math.round(canvasY+(y1+(y2-y1)*t)*canvasScale);g.fill(x,y,x+1,y+1,color);}}
                public void text(String text,float x,float y,float scale,int color){g.pose().pushPose();try{g.pose().translate(canvasX+x*canvasScale,canvasY+y*canvasScale,0);float s=scale*canvasScale;g.pose().scale(s,s,s);g.drawString(font,text,0,0,color,false);}finally{g.pose().popPose();}}
                public void clip(float x,float y,float w,float h){clips.replace((int)(canvasX+x*canvasScale),(int)(canvasY+y*canvasScale),(int)(canvasX+(x+w)*canvasScale),(int)(canvasY+(y+h)*canvasScale));}
            };DisplayDesignPainter.paint(draw,design(),null);
            if(selected>=0&&selected<widgets.size()){var w=widgets.get(selected);draw.rect(w.x(),w.y(),w.w(),w.h(),0xffffffff);draw.rect(w.x()+w.w()-16,w.y()+w.h()-16,16,16,0xffffffff);}
            }
        }
        g.drawString(font,coding?"CLIENT-LOCAL Docker / 131072 characters. No server compiler service required.":"PREVIEW / SAMPLE DATA - drag widgets; drag lower-right corner to resize.",8,height-52,0x999999);
        var lines=font.split(Component.literal(status),Math.max(20,width-16));for(int i=0;i<Math.min(3,lines.size());i++)g.drawString(font,lines.get(i),8,height-38+i*10,status.startsWith("ERROR")||stale?0xff7777:0x88cccc);
        super.render(g,mx,my,partial);
    }
}
