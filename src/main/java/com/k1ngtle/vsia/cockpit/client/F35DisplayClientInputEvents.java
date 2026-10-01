package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.iff.F35IffClientState;
import com.k1ngtle.vsia.cockpit.display.F35ClientDetectionCache;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=Vsia.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public final class F35DisplayClientInputEvents {
    private static Object connection;
    private static UUID previousCockpit;
    private F35DisplayClientInputEvents(){ }
    @SubscribeEvent public static void onClientTick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(connection!=mc.getConnection()){
            connection=mc.getConnection();previousCockpit=null;
            F35TargetLockClient.clearAll();F35IffClientState.clearAll();F35ClientDetectionCache.clearAll();F35DisplayClientConfig.clearContext();
        }
        if(mc.player==null||connection==null)return;
        F35CockpitSeatBlockEntity cockpit=F35CockpitClientContext.seated();
        UUID current=cockpit==null?null:cockpit.cockpitId();
        if(current!=null&&!current.equals(previousCockpit)){
            mc.player.displayClientMessage(Component.literal("F-35 "+F35CockpitClientContext.label(current)+": [1] IFF  [2] SENSOR  [3] TSD  [4] HSI  [\\] CONFIG"),true);
        }
        previousCockpit=current;
        while(F35DisplayKeyMappings.CONFIGURE_DISPLAY.consumeClick()){
            if(mc.screen instanceof F35DisplayConfigScreen)mc.setScreen(null);
            else if(current!=null&&(mc.screen==null||mc.screen instanceof F35DisplaySectionScreen))mc.setScreen(new F35DisplayConfigScreen());
        }
    }
    @SubscribeEvent public static void onKeyInput(InputEvent.Key event){
        if(event.getAction()!=GLFW.GLFW_PRESS)return;
        int section=F35DisplaySectionScreen.sectionForKey(event.getKey());if(section==0)return;
        Minecraft mc=Minecraft.getInstance();F35CockpitSeatBlockEntity cockpit=F35CockpitClientContext.seated();
        if(mc.screen==null&&cockpit!=null&&cockpit.cockpitId()!=null)mc.setScreen(new F35DisplaySectionScreen(section));
    }
}
