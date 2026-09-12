package com.k1ngtle.vsia.phone.client;

import com.k1ngtle.vsia.phone.client.screen.IPhoneHomeScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientPhoneHooks {
    private ClientPhoneHooks() {
    }

    public static void openPhone() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new IPhoneHomeScreen());
        }
    }
}
