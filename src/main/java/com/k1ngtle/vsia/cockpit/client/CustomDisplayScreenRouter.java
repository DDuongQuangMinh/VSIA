package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.item.DisplayHardDriveItem;
import net.minecraft.client.Minecraft;

public final class CustomDisplayScreenRouter {
    private CustomDisplayScreenRouter(){}
    public static boolean customInstalled(){var cockpit=F35CockpitClientContext.seated();return cockpit!=null&&DisplayHardDriveItem.PROGRAM_CUSTOM.equals(DisplayHardDriveItem.programId(cockpit.displayDrive()));}
    public static void open(int section){var cockpit=F35CockpitClientContext.seated();if(cockpit==null||cockpit.cockpitId()==null)return;if(customInstalled())Minecraft.getInstance().setScreen(new CustomDisplaySectionScreen(cockpit,section));else if(section>=1&&section<=4)Minecraft.getInstance().setScreen(new F35DisplaySectionScreen(section));}
}
