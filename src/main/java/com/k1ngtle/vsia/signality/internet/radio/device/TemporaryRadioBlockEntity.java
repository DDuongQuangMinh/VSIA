package com.k1ngtle.vsia.signality.internet.radio.device;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.engineering.radio.RadioEmission;
import com.k1ngtle.vsia.signality.engineering.radio.RadioMode;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.network.NetworkKind;
import com.k1ngtle.vsia.signality.internet.radio.RadioBandPreset;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioState;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class TemporaryRadioBlockEntity
        extends NetworkDeviceBlockEntity {

    private boolean guiInitialized;
    private RadioEmission guiEmission =
            RadioEmission.FM;

    private String guiAccessCode =
            "100.0";

    private double guiSquelchDb =
            3.0;

    private boolean guiMesh;
    private boolean guiFhss;

    private String guiStatus =
            "Radio ready";

    public TemporaryRadioBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                SignalityBlocks
                        .TEMPORARY_RADIO_BE
                        .get(),
                pos,
                state
        );
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level == null
                || level.isClientSide()) {
            return;
        }

        if (!guiInitialized) {
            RadioBandPreset preset =
                    presetForProfile();

            if (networkProfile().kind()
                    != NetworkKind.RADIO
                    || radioMode()
                    == RadioMode.LEGACY_DIRECT) {
                applyPreset(
                        preset
                );
            } else {
                guiEmission =
                        preset.emission();

                guiAccessCode =
                        "100.0";

                guiSquelchDb =
                        3.0;

                setRadioSquelchSnrThresholdDb(
                        guiSquelchDb
                );
            }

            guiInitialized = true;
            setChanged();
        } else {
            setRadioSquelchSnrThresholdDb(
                    guiSquelchDb
            );

            enableRadioMesh(
                    guiMesh
            );

            if (guiFhss) {
                applyFhssPlan();
            }
        }
    }

    public void applyPortableState(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return;
        }

        RadioBandPreset preset =
                PortableRadioState.band(
                        stack
                );

        if (!applyPreset(
                preset
        )) {
            return;
        }

        guiEmission =
                PortableRadioState.emission(
                        stack
                );

        guiAccessCode =
                PortableRadioState.accessCode(
                        stack
                );

        guiSquelchDb =
                PortableRadioState.squelchDb(
                        stack
                );

        guiMesh =
                PortableRadioState.mesh(
                        stack
                );

        guiFhss =
                PortableRadioState.fhss(
                        stack
                );

        configureRadioTransceiver(
                preset.channelId(),
                PortableRadioState.frequencyHz(
                        stack
                ),
                PortableRadioState.bandwidthHz(
                        stack
                ),
                guiEmission,
                guiAccessCode
        );

        setRadioSquelchSnrThresholdDb(
                guiSquelchDb
        );

        enableRadioMesh(
                guiMesh
        );

        if (guiFhss) {
            applyFhssPlan();
        }

        guiStatus =
                "Portable radio configuration loaded";

        guiInitialized =
                true;

        setChanged();
    }

    public RadioBandPreset presetForProfile() {
        String path =
                networkProfileId()
                        .getPath();

        if ("radio_hf".equals(path)) {
            return RadioBandPreset.HF;
        }

        if ("radio_uhf".equals(path)) {
            return RadioBandPreset.UHF;
        }

        return RadioBandPreset.VHF;
    }

    public RadioEmission guiEmission() {
        return guiEmission;
    }

    public double guiSquelchDb() {
        return guiSquelchDb;
    }

    public boolean guiMeshEnabled() {
        return guiMesh;
    }

    public boolean guiFhssEnabled() {
        return guiFhss;
    }

    public String guiStatus() {
        return guiStatus;
    }

    public boolean applyPreset(
            RadioBandPreset preset
    ) {
        if (preset == null
                || !preset.apply(this)) {
            guiStatus =
                    "Unable to apply radio preset";
            return false;
        }

        guiEmission =
                preset.emission();

        guiAccessCode =
                "100.0";

        guiSquelchDb =
                3.0;

        guiMesh =
                false;

        guiFhss =
                false;

        disableRadioFrequencyHopping();
        enableRadioMesh(false);

        guiStatus =
                preset.name()
                        + " preset selected";

        guiInitialized =
                true;

        setChanged();
        return true;
    }

    public boolean tuneBy(
            double deltaHz
    ) {
        double minimum;
        double maximum;

        switch (presetForProfile()) {
            case HF -> {
                minimum =
                        1_800_000.0;

                maximum =
                        30_000_000.0;
            }

            case VHF -> {
                minimum =
                        30_000_000.0;

                maximum =
                        300_000_000.0;
            }

            case UHF -> {
                minimum =
                        300_000_000.0;

                maximum =
                        1_000_000_000.0;
            }

            default -> {
                minimum =
                        1_000_000.0;

                maximum =
                        1_000_000_000.0;
            }
        }

        double next =
                Math.max(
                        minimum,
                        Math.min(
                                maximum,
                                activeFrequencyHz()
                                        + deltaHz
                        )
                );

        boolean configured =
                configureRadioTransceiver(
                        presetForProfile()
                                .channelId(),
                        next,
                        presetForProfile()
                                .bandwidthHz(),
                        guiEmission,
                        guiAccessCode
                );

        if (configured) {
            guiStatus =
                    String.format(
                            Locale.ROOT,
                            "Tuned %.4f MHz",
                            next
                                    / 1_000_000.0
                    );

            reapplyFeatures();
            setChanged();
        }

        return configured;
    }

    public void cycleEmission() {
        guiEmission =
                switch (guiEmission) {
                    case AM ->
                            RadioEmission.FM;

                    case FM ->
                            RadioEmission.DIGITAL;

                    case DIGITAL ->
                            RadioEmission.AM;
                };

        configureRadioTransceiver(
                presetForProfile()
                        .channelId(),
                activeFrequencyHz(),
                presetForProfile()
                        .bandwidthHz(),
                guiEmission,
                guiAccessCode
        );

        reapplyFeatures();

        guiStatus =
                "Emission "
                        + guiEmission.name();

        setChanged();
    }

    public void adjustSquelch(
            double deltaDb
    ) {
        guiSquelchDb =
                Math.max(
                        -10.0,
                        Math.min(
                                30.0,
                                guiSquelchDb
                                        + deltaDb
                        )
                );

        setRadioSquelchSnrThresholdDb(
                guiSquelchDb
        );

        guiStatus =
                String.format(
                        Locale.ROOT,
                        "Squelch %.1f dB",
                        guiSquelchDb
                );

        setChanged();
    }

    public void toggleMesh() {
        guiMesh =
                !guiMesh;

        enableRadioMesh(
                guiMesh
        );

        guiStatus =
                "Mesh "
                        + (
                        guiMesh
                                ? "enabled"
                                : "disabled"
                );

        setChanged();
    }

    public void toggleFhss() {
        guiFhss =
                !guiFhss;

        if (guiFhss) {
            applyFhssPlan();
        } else {
            disableRadioFrequencyHopping();
        }

        guiStatus =
                "FHSS "
                        + (
                        guiFhss
                                ? "enabled"
                                : "disabled"
                );

        setChanged();
    }

    public boolean pttTest() {
        if (!pressRadioPtt()) {
            guiStatus =
                    "PTT unavailable in "
                            + radioMode();

            return false;
        }

        boolean sent =
                sendRadioVoice(
                        "Radio check one two"
                                .getBytes(
                                        StandardCharsets.UTF_8
                                ),
                        true
                );

        releaseRadioPtt();

        guiStatus =
                sent
                        ? "PTT voice test queued"
                        : "PTT voice test failed";

        setChanged();
        return sent;
    }

    public boolean packetTest() {
        CompoundTag payload =
                new CompoundTag();

        payload.putString(
                "text",
                "Radio GUI packet test"
        );

        boolean sent =
                sendRadioPacket(
                        null,
                        payload
                );

        guiStatus =
                sent
                        ? "Broadcast packet queued"
                        : "Packet test failed";

        setChanged();
        return sent;
    }

    public String lastVoiceText() {
        byte[] voice =
                lastReceivedRadioVoice();

        if (voice.length == 0) {
            return "";
        }

        return new String(
                voice,
                StandardCharsets.UTF_8
        );
    }

    public double tuneStepHz() {
        return presetForProfile()
                == RadioBandPreset.HF
                ? 12_500.0
                : 25_000.0;
    }

    private void applyFhssPlan() {
        double center =
                activeFrequencyHz();

        double step =
                Math.max(
                        12_500.0,
                        presetForProfile()
                                .bandwidthHz()
                );

        enableRadioFrequencyHopping(
                new double[]{
                        center
                                - 2.0
                                * step,
                        center
                                - step,
                        center,
                        center
                                + step,
                        center
                                + 2.0
                                * step
                },
                0x5653494152414449L
        );
    }

    private void reapplyFeatures() {
        setRadioSquelchSnrThresholdDb(
                guiSquelchDb
        );

        enableRadioMesh(
                guiMesh
        );

        if (guiFhss) {
            guiFhss = false;
            toggleFhss();
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag
    ) {
        super.saveAdditional(
                tag
        );

        tag.putBoolean(
                "RadioGuiInitialized",
                guiInitialized
        );

        tag.putString(
                "RadioGuiEmission",
                guiEmission.name()
        );

        tag.putString(
                "RadioGuiAccessCode",
                guiAccessCode
        );

        tag.putDouble(
                "RadioGuiSquelchDb",
                guiSquelchDb
        );

        tag.putBoolean(
                "RadioGuiMesh",
                guiMesh
        );

        tag.putBoolean(
                "RadioGuiFhss",
                guiFhss
        );

        tag.putString(
                "RadioGuiStatus",
                guiStatus
        );
    }

    @Override
    public void load(
            CompoundTag tag
    ) {
        super.load(
                tag
        );

        guiInitialized =
                tag.getBoolean(
                        "RadioGuiInitialized"
                );

        try {
            guiEmission =
                    RadioEmission.valueOf(
                            tag.getString(
                                    "RadioGuiEmission"
                            )
                    );
        } catch (Exception ignored) {
            guiEmission =
                    presetForProfile()
                            .emission();
        }

        guiAccessCode =
                tag.contains(
                        "RadioGuiAccessCode"
                )
                        ? tag.getString(
                        "RadioGuiAccessCode"
                )
                        : "100.0";

        guiSquelchDb =
                tag.contains(
                        "RadioGuiSquelchDb"
                )
                        ? tag.getDouble(
                        "RadioGuiSquelchDb"
                )
                        : 3.0;

        guiMesh =
                tag.getBoolean(
                        "RadioGuiMesh"
                );

        guiFhss =
                tag.getBoolean(
                        "RadioGuiFhss"
                );

        guiStatus =
                tag.contains(
                        "RadioGuiStatus"
                )
                        ? tag.getString(
                        "RadioGuiStatus"
                )
                        : "Radio ready";
    }
}
