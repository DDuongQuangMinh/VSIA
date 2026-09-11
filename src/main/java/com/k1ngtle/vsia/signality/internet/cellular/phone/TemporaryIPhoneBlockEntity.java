package com.k1ngtle.vsia.signality.internet.cellular.phone;

import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.network.NetworkKind;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

// TEMPORARY_IPHONE_V1
public final class TemporaryIPhoneBlockEntity extends NetworkDeviceBlockEntity {
    public TemporaryIPhoneBlockEntity(BlockPos pos, BlockState state) {
        super(SignalityBlocks.TEMPORARY_IPHONE_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level == null || level.isClientSide()) {
            return;
        }

        if (networkProfile().kind() != NetworkKind.CELLULAR) {
            setNetworkProfile(
                    new ResourceLocation(Signality.MODID, "cellular_5g")
            );
        }
    }
}
