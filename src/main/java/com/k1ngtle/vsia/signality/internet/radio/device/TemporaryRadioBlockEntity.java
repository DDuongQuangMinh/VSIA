package com.k1ngtle.vsia.signality.internet.radio.device;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.engineering.radio.RadioMode;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.network.NetworkKind;
import com.k1ngtle.vsia.signality.internet.radio.RadioBandPreset;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class TemporaryRadioBlockEntity extends NetworkDeviceBlockEntity {
    public TemporaryRadioBlockEntity(BlockPos pos, BlockState state) {
        super(SignalityBlocks.TEMPORARY_RADIO_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level == null || level.isClientSide()) {
            return;
        }

        if (networkProfile().kind() != NetworkKind.RADIO) {
            RadioBandPreset.VHF.apply(this);
            return;
        }

        if (radioMode() == RadioMode.LEGACY_DIRECT) {
            RadioBandPreset preset = presetForProfile();
            preset.apply(this);
        }
    }

    public RadioBandPreset presetForProfile() {
        String path = networkProfileId().getPath();

        if ("radio_hf".equals(path)) {
            return RadioBandPreset.HF;
        }

        if ("radio_uhf".equals(path)) {
            return RadioBandPreset.UHF;
        }

        return RadioBandPreset.VHF;
    }
}
