package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.iff.F35IffClientState;
import com.k1ngtle.vsia.cockpit.network.C2SF35IffActionPacket;
import com.k1ngtle.vsia.cockpit.network.C2SF35IffRequestPacket;
import com.k1ngtle.vsia.network.VsiaNetwork;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

/** Each instance is bound to one cockpit; only IDs/names, never key material, enter this UI. */
public final class F35IffKeyScreen extends Screen {
    private final Screen parent;private final UUID cockpitId;private EditBox name,mission,pilot,codes;private int left,top;
    public F35IffKeyScreen(Screen parent,UUID cockpitId){super(Component.literal("IFF MISSION KEY DISTRIBUTION"));this.parent=parent;this.cockpitId=cockpitId;}
    @Override protected void init(){
        left=Math.max(8,(width-340)/2);top=Math.max(18,(height-252)/2);F35IffClientState.Snapshot s=F35IffClientState.get(cockpitId);
        name=field(left,top+35,215,16,"Mission name","ALPHA");button(left+220,top+35,120,"Create mission",()->send(70,name.getValue()));
        mission=field(left,top+80,340,36,"Public mission UUID",s.missionId());
        button(left,top+104,105,"Load into A",()->send(72,mission.getValue()+":A"));button(left+110,top+104,105,"Load into B",()->send(72,mission.getValue()+":B"));button(left+220,top+104,120,"List in chat",()->send(75,""));
        pilot=field(left,top+150,150,16,"Online pilot name","");button(left+155,top+150,90,"Invite",()->send(71,pilot.getValue()));button(left+250,top+150,90,"Revoke",()->send(74,pilot.getValue()));
        codes=field(left,top+194,150,12,"M1:M2:M3/A",s.mode1()+":"+s.mode2()+":"+s.mode3a());button(left+155,top+194,90,"Set codes",()->send(40,codes.getValue()));button(left+250,top+194,90,"Rotate key",()->confirm(73,"Rotate mission? Old key expires in 120 seconds."));
        button(left,top+225,105,"Retire mission",()->confirm(76,"Retire this mission for EVERY aircraft?"));button(left+110,top+225,105,"Zeroize cockpit",()->confirm(31,"Erase BOTH keys from THIS cockpit and set OFF?"));button(left+220,top+225,120,"Back",this::onClose);
        request();
    }
    private EditBox field(int x,int y,int w,int max,String hint,String value){EditBox b=new EditBox(font,x,y,w,20,Component.literal(hint));b.setMaxLength(max);b.setValue(value);addRenderableWidget(b);return b;}
    private void button(int x,int y,int w,String label,Runnable action){addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    private void send(int action,String value){if(F35CockpitClientContext.bindSeat(cockpitId))VsiaNetwork.sendToServer(new C2SF35IffActionPacket(cockpitId,action,value));}
    private void confirm(int action,String text){minecraft.setScreen(new ConfirmScreen(yes->{if(yes)send(action,"");minecraft.setScreen(this);},Component.literal(text),Component.literal("This is a server-authoritative operation.")));}
    private void request(){if(F35CockpitClientContext.bindSeat(cockpitId))VsiaNetwork.sendToServer(new C2SF35IffRequestPacket(cockpitId));}
    @Override public void tick(){if(!F35CockpitClientContext.bindSeat(cockpitId)){minecraft.setScreen(null);return;}if(minecraft.player.tickCount%20==0)request();name.tick();mission.tick();pilot.tick();codes.tick();}
    @Override public void onClose(){minecraft.setScreen(F35CockpitClientContext.bindSeat(cockpitId)?parent:null);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);F35IffClientState.Snapshot s=F35IffClientState.get(cockpitId);
        g.drawString(font,"IFF - COCKPIT "+F35CockpitClientContext.label(cockpitId)+"  ACTIVE "+(s.activeSlot()==0?"A":"B")+" "+s.keyId(),left,top,0x78C8C8);
        g.drawString(font,"New mission name (creates a new alliance)",left,top+23,0xDDDDDD);
        g.drawString(font,"Mission UUID (invitation required; not a secret)",left,top+66,0xDDDDDD);
        g.drawString(font,"Invite/revoke uses the ACTIVE slot's mission",left,top+137,0xDDDDDD);
        g.drawString(font,"Codes M1:M2:M3/A; rotation uses active mission",left,top+181,0xDDDDDD);
        super.render(g,mx,my,partial);
    }
}
