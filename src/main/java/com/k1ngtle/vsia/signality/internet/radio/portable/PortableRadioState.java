package com.k1ngtle.vsia.signality.internet.radio.portable;

import com.k1ngtle.vsia.signality.engineering.radio.RadioController;
import com.k1ngtle.vsia.signality.engineering.radio.RadioEmission;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfile;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfileRegistry;
import com.k1ngtle.vsia.signality.internet.radio.RadioBandPreset;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.UUID;

public final class PortableRadioState {
    private static final String ROOT = "VsiaPortableRadio";

    private PortableRadioState() {
    }

    public static CompoundTag root(ItemStack stack) {
        CompoundTag tag =
                stack.getOrCreateTag()
                        .getCompound(ROOT);

        if (!tag.contains("Initialized")) {
            initialize(tag);
            stack.getOrCreateTag().put(ROOT, tag);
        }

        return tag;
    }

    private static void initialize(CompoundTag tag) {
        RadioBandPreset preset = RadioBandPreset.VHF;

        tag.putBoolean("Initialized", true);
        tag.putLong("ConfigRevision", 1L);
        tag.putUUID("RadioId", UUID.randomUUID());
        tag.putString("Band", preset.name());
        tag.putDouble("FrequencyHz", preset.frequencyHz());
        tag.putDouble("BandwidthHz", preset.bandwidthHz());
        tag.putString("Emission", preset.emission().name());
        tag.putString("AccessCode", "100.0");
        tag.putDouble("SquelchDb", 3.0);
        tag.putBoolean("Mesh", false);
        tag.putBoolean("Fhss", false);
        tag.putBoolean("ComsecEnabled", false);
        tag.putInt("ComsecSlot", 1);
        tag.putString("CryptoStatus", "CLEAR");
        tag.putDouble("LastReceivedPowerDbm", Double.NEGATIVE_INFINITY);
        tag.putDouble("LastSnrDb", Double.NEGATIVE_INFINITY);
        tag.putDouble("LastIntelligibility", 0.0);
        tag.putDouble("LastPacketProbability", 0.0);
        tag.putBoolean("LastSquelchOpen", false);
        tag.putString("LastVoice", "");
        tag.putString("Status", "Portable radio ready");
    }

    private static void commit(ItemStack stack, CompoundTag tag) {
        stack.getOrCreateTag().put(ROOT, tag);
    }

    public static UUID id(ItemStack stack) {
        CompoundTag tag = root(stack);

        if (!tag.hasUUID("RadioId")) {
            tag.putUUID("RadioId", UUID.randomUUID());
            commit(stack, tag);
        }

        return tag.getUUID("RadioId");
    }

    public static RadioBandPreset band(ItemStack stack) {
        String name = root(stack).getString("Band");

        try {
            return RadioBandPreset.valueOf(
                    name.toUpperCase(Locale.ROOT)
            );
        } catch (Exception ignored) {
            return RadioBandPreset.VHF;
        }
    }

    public static double frequencyHz(ItemStack stack) {
        return root(stack).getDouble("FrequencyHz");
    }

    public static double bandwidthHz(ItemStack stack) {
        return root(stack).getDouble("BandwidthHz");
    }

    public static RadioEmission emission(ItemStack stack) {
        try {
            return RadioEmission.valueOf(
                    root(stack).getString("Emission")
            );
        } catch (Exception ignored) {
            return band(stack).emission();
        }
    }

    public static String accessCode(ItemStack stack) {
        String value = root(stack).getString("AccessCode");
        return value.isBlank() ? "100.0" : value;
    }

    public static double squelchDb(ItemStack stack) {
        return root(stack).getDouble("SquelchDb");
    }

    public static boolean mesh(ItemStack stack) {
        return root(stack).getBoolean("Mesh");
    }

    public static boolean fhss(ItemStack stack) {
        return root(stack).getBoolean("Fhss");
    }

    public static boolean comsecEnabled(
            ItemStack stack
    ) {
        return root(stack)
                .getBoolean(
                        "ComsecEnabled"
                );
    }

    public static int comsecSlot(
            ItemStack stack
    ) {
        return Math.max(
                1,
                Math.min(
                        8,
                        root(stack)
                                .getInt(
                                        "ComsecSlot"
                                )
                )
        );
    }

    public static String cryptoStatus(
            ItemStack stack
    ) {
        String value =
                root(stack)
                        .getString(
                                "CryptoStatus"
                        );

        return value.isBlank()
                ? "CLEAR"
                : value;
    }

    public static void applyBand(
            ItemStack stack,
            RadioBandPreset preset
    ) {
        if (preset == null) {
            return;
        }

        CompoundTag tag = root(stack);

        tag.putString("Band", preset.name());
        tag.putDouble("FrequencyHz", preset.frequencyHz());
        tag.putDouble("BandwidthHz", preset.bandwidthHz());
        tag.putString(
                "Emission",
                comsecEnabled(stack)
                        ? RadioEmission.DIGITAL.name()
                        : preset.emission().name()
        );

        tag.putString(
                "Status",
                comsecEnabled(stack)
                        ? "Band changed to "
                        + preset.name()
                        + " | COMSEC waveform DIGITAL"
                        : "Band changed to "
                        + preset.name()
        );
        bumpRevision(tag);

        commit(stack, tag);
    }

    public static void tune(
            ItemStack stack,
            double deltaHz
    ) {
        CompoundTag tag = root(stack);

        double minimum;
        double maximum;

        switch (band(stack)) {
            case HF -> {
                minimum = 1_800_000.0;
                maximum = 30_000_000.0;
            }

            case VHF -> {
                minimum = 30_000_000.0;
                maximum = 300_000_000.0;
            }

            case UHF -> {
                minimum = 300_000_000.0;
                maximum = 1_000_000_000.0;
            }

            default -> {
                minimum = 1_000_000.0;
                maximum = 1_000_000_000.0;
            }
        }

        double next =
                Math.max(
                        minimum,
                        Math.min(
                                maximum,
                                frequencyHz(stack) + deltaHz
                        )
                );

        tag.putDouble("FrequencyHz", next);
        tag.putString(
                "Status",
                String.format(
                        Locale.ROOT,
                        "Tuned %.4f MHz",
                        next / 1_000_000.0
                )
        );
        bumpRevision(tag);

        commit(stack, tag);
    }

    public static void cycleEmission(ItemStack stack) {
        CompoundTag tag =
                root(stack);

        if (comsecEnabled(
                stack
        )) {
            tag.putString(
                    "Emission",
                    RadioEmission.DIGITAL.name()
            );

            tag.putString(
                    "Status",
                    "COMSEC secure voice requires DIGITAL waveform"
            );

            bumpRevision(
                    tag
            );

            commit(
                    stack,
                    tag
            );

            return;
        }

        RadioEmission current =
                emission(
                        stack
                );

        RadioEmission next =
                switch (current) {
                    case AM -> RadioEmission.FM;
                    case FM -> RadioEmission.DIGITAL;
                    case DIGITAL -> RadioEmission.AM;
                };

        tag.putString(
                "Emission",
                next.name()
        );

        tag.putString(
                "Status",
                "Emission "
                        + next.name()
        );

        bumpRevision(
                tag
        );

        commit(
                stack,
                tag
        );
    }

    public static void adjustSquelch(
            ItemStack stack,
            double delta
    ) {
        CompoundTag tag = root(stack);

        double next =
                Math.max(
                        -10.0,
                        Math.min(
                                30.0,
                                squelchDb(stack) + delta
                        )
                );

        tag.putDouble("SquelchDb", next);
        tag.putString(
                "Status",
                String.format(
                        Locale.ROOT,
                        "Squelch %.1f dB",
                        next
                )
        );
        bumpRevision(tag);

        commit(stack, tag);
    }

    public static void toggleMesh(ItemStack stack) {
        CompoundTag tag = root(stack);
        boolean next = !tag.getBoolean("Mesh");
        tag.putBoolean("Mesh", next);
        tag.putString("Status", "Mesh " + (next ? "enabled" : "disabled"));
        bumpRevision(tag);
        commit(stack, tag);
    }

    public static void toggleFhss(ItemStack stack) {
        CompoundTag tag = root(stack);
        boolean next = !tag.getBoolean("Fhss");
        tag.putBoolean("Fhss", next);
        tag.putString("Status", "FHSS " + (next ? "enabled" : "disabled"));
        bumpRevision(tag);
        commit(stack, tag);
    }

    public static void toggleComsec(
            ItemStack stack
    ) {
        CompoundTag tag =
                root(stack);

        boolean next =
                !tag.getBoolean(
                        "ComsecEnabled"
                );

        tag.putBoolean(
                "ComsecEnabled",
                next
        );

        if (next) {
            tag.putString(
                    "Emission",
                    RadioEmission.DIGITAL.name()
            );
        }

        tag.putString(
                "CryptoStatus",
                next
                        ? "SECURE TEK-"
                        + String.format(
                                Locale.ROOT,
                                "%02d",
                                comsecSlot(stack)
                        )
                        : "CLEAR"
        );

        tag.putString(
                "Status",
                next
                        ? "COMSEC secure mode enabled"
                        : "COMSEC clear mode enabled"
        );

        bumpRevision(
                tag
        );

        commit(
                stack,
                tag
        );
    }

    public static void nextComsecSlot(
            ItemStack stack
    ) {
        CompoundTag tag =
                root(stack);

        int next =
                comsecSlot(stack)
                        % 8
                        + 1;

        tag.putInt(
                "ComsecSlot",
                next
        );

        tag.putString(
                "CryptoStatus",
                tag.getBoolean(
                        "ComsecEnabled"
                )
                        ? "SECURE TEK-"
                        + String.format(
                                Locale.ROOT,
                                "%02d",
                                next
                        )
                        : "CLEAR / TEK-"
                        + String.format(
                                Locale.ROOT,
                                "%02d",
                                next
                        )
        );

        tag.putString(
                "Status",
                "COMSEC key slot "
                        + next
        );

        bumpRevision(
                tag
        );

        commit(
                stack,
                tag
        );
    }

    public static void cryptoStatus(
            ItemStack stack,
            String status
    ) {
        CompoundTag tag =
                root(stack);

        tag.putString(
                "CryptoStatus",
                status == null
                        ? ""
                        : status
        );

        commit(
                stack,
                tag
        );
    }

    public static long revision(ItemStack stack) {
        return root(stack).getLong("ConfigRevision");
    }

    private static void bumpRevision(
            CompoundTag tag
    ) {
        tag.putLong(
                "ConfigRevision",
                Math.max(
                        1L,
                        tag.getLong("ConfigRevision") + 1L
                )
        );
    }

    public static RadioController controller(ItemStack stack) {
        RadioController controller =
                new RadioController();

        controller.configureTransceiver(
                new com.k1ngtle.vsia.signality.engineering.radio.RadioChannel(
                        band(stack).channelId(),
                        frequencyHz(stack),
                        bandwidthHz(stack),
                        emission(stack),
                        accessCode(stack)
                )
        );

        controller.setSquelchSnrThresholdDb(
                squelchDb(stack)
        );

        controller.enableMesh(
                mesh(stack)
        );

        if (fhss(stack)) {
            double center = frequencyHz(stack);
            double step = Math.max(12_500.0, bandwidthHz(stack));

            controller.enableFrequencyHopping(
                    new double[]{
                            center - 2.0 * step,
                            center - step,
                            center,
                            center + step,
                            center + 2.0 * step
                    },
                    0x5653494152414449L
            );
        }

        return controller;
    }

    public static NetworkProfile profile(ItemStack stack) {
        return NetworkProfileRegistry.getOrDefault(
                band(stack).profileId()
        );
    }

    public static double tuneStepHz(ItemStack stack) {
        return band(stack) == RadioBandPreset.HF
                ? 12_500.0
                : 25_000.0;
    }

    public static void storeLink(
            ItemStack stack,
            double powerDbm,
            double snrDb,
            double intelligibility,
            double packetProbability,
            boolean squelchOpen
    ) {
        CompoundTag tag = root(stack);

        tag.putDouble("LastReceivedPowerDbm", powerDbm);
        tag.putDouble("LastSnrDb", snrDb);
        tag.putDouble("LastIntelligibility", intelligibility);
        tag.putDouble("LastPacketProbability", packetProbability);
        tag.putBoolean("LastSquelchOpen", squelchOpen);

        commit(stack, tag);
    }

    public static void storeVoice(
            ItemStack stack,
            String voice
    ) {
        CompoundTag tag = root(stack);
        tag.putString("LastVoice", voice == null ? "" : voice);
        commit(stack, tag);
    }

    public static String lastVoice(ItemStack stack) {
        return root(stack).getString("LastVoice");
    }

    public static double lastPowerDbm(ItemStack stack) {
        return root(stack).getDouble("LastReceivedPowerDbm");
    }

    public static double lastSnrDb(ItemStack stack) {
        return root(stack).getDouble("LastSnrDb");
    }

    public static double lastIntelligibility(ItemStack stack) {
        return root(stack).getDouble("LastIntelligibility");
    }

    public static double lastPacketProbability(ItemStack stack) {
        return root(stack).getDouble("LastPacketProbability");
    }

    public static boolean lastSquelchOpen(ItemStack stack) {
        return root(stack).getBoolean("LastSquelchOpen");
    }

    public static String status(ItemStack stack) {
        return root(stack).getString("Status");
    }

    public static void status(
            ItemStack stack,
            String status
    ) {
        CompoundTag tag = root(stack);
        tag.putString("Status", status == null ? "" : status);
        commit(stack, tag);
    }
}
