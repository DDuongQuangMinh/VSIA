package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularCqiModel;
import com.k1ngtle.vsia.signality.engineering.cellular.nas.FiveGAkaEngine;
import com.k1ngtle.vsia.signality.engineering.cellular.pdcp.PdcpEntity;
import com.k1ngtle.vsia.signality.engineering.cellular.pdcp.PdcpPdu;
import com.k1ngtle.vsia.signality.engineering.cellular.pdcp.PdcpSecurityContext;
import com.k1ngtle.vsia.signality.engineering.cellular.rlc.RlcAmEntity;
import com.k1ngtle.vsia.signality.engineering.cellular.rlc.RlcPdu;
import com.k1ngtle.vsia.signality.engineering.math.RfMath;
import com.k1ngtle.vsia.signality.engineering.phy.PhyProfile;
import com.k1ngtle.vsia.signality.engineering.radio.FrequencyHopPlan;
import com.k1ngtle.vsia.signality.engineering.radio.PacketRadioFrame;
import com.k1ngtle.vsia.signality.engineering.radio.RadioSecurityEngine;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolInstruction;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolOpcode;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolProgram;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolProgramRegistry;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVirtualMachine;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmEnvironment;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmHost;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmLimits;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmRunResult;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMacController;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcs;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcsTable;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMode;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfile;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfileRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SignalityTestCommand {

    private SignalityTestCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("signalitytest")
                        .requires(
                                source ->
                                        source.hasPermission(2)
                        )
                        .executes(
                                context ->
                                        showHelp(
                                                context.getSource()
                                        )
                        )
                        .then(
                                Commands.literal("help")
                                        .executes(
                                                context ->
                                                        showHelp(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("all")
                                        .executes(
                                                context ->
                                                        runAll(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("phase")
                                        .then(
                                                Commands.argument(
                                                                "number",
                                                                IntegerArgumentType.integer(
                                                                        1,
                                                                        9
                                                                )
                                                        )
                                                        .executes(
                                                                context ->
                                                                        runPhase(
                                                                                context.getSource(),
                                                                                IntegerArgumentType.getInteger(
                                                                                        context,
                                                                                        "number"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int showHelp(
            CommandSourceStack source
    ) {
        source.sendSuccess(
                () -> Component.literal(
                                "Signality regression test commands"
                        )
                        .withStyle(
                                ChatFormatting.AQUA
                        ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "/signalitytest all"
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "/signalitytest phase <1-9>"
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Tests are self-contained and do not require placed AP/UE/radio blocks."
                ).withStyle(
                        ChatFormatting.GRAY
                ),
                false
        );

        return 1;
    }

    private static int runAll(
            CommandSourceStack source
    ) {
        TestReport total =
                new TestReport(
                        "Signality Phase 1-9"
                );

        source.sendSuccess(
                () -> Component.literal(
                        "Running Signality Phase 1-9 regression suite..."
                ).withStyle(
                        ChatFormatting.GOLD
                ),
                false
        );

        for (int phase = 1;
             phase <= 9;
             phase++) {

            TestReport report =
                    executePhase(
                            phase
                    );

            total.merge(
                    report
            );

            printReport(
                    source,
                    report
            );
        }

        source.sendSuccess(
                () -> Component.literal(
                        "--------------------------------"
                ).withStyle(
                        ChatFormatting.DARK_GRAY
                ),
                false
        );

        printSummary(
                source,
                total
        );

        return total.failed == 0
                ? 1
                : 0;
    }

    private static int runPhase(
            CommandSourceStack source,
            int phase
    ) {
        TestReport report =
                executePhase(
                        phase
                );

        printReport(
                source,
                report
        );

        printSummary(
                source,
                report
        );

        return report.failed == 0
                ? 1
                : 0;
    }

    private static TestReport executePhase(
            int phase
    ) {
        return switch (phase) {
            case 1 -> testPhase1();
            case 2 -> testPhase2();
            case 3 -> testPhase3();
            case 4 -> testPhase4();
            case 5 -> testPhase5();
            case 6 -> testPhase6();
            case 7 -> testPhase7();
            case 8 -> testPhase8();
            case 9 -> testPhase9();
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported phase "
                                    + phase
                    );
        };
    }

    private static TestReport testPhase1() {
        TestReport report =
                new TestReport(
                        "Phase 1 - Network Profiles"
                );

        report.run(
                "Default profile exists",
                () -> {
                    NetworkProfile profile =
                            NetworkProfileRegistry.defaultProfile();

                    require(
                            profile != null,
                            "defaultProfile() returned null"
                    );
                }
        );

        report.run(
                "Default profile ID resolves",
                () -> {
                    NetworkProfile profile =
                            NetworkProfileRegistry
                                    .get(
                                            NetworkProfileRegistry.DEFAULT_PROFILE_ID
                                    )
                                    .orElseThrow(
                                            () ->
                                                    new IllegalStateException(
                                                            "DEFAULT_PROFILE_ID is not registered"
                                                    )
                                    );

                    require(
                            profile.id().equals(
                                    NetworkProfileRegistry.DEFAULT_PROFILE_ID
                            ),
                            "resolved profile ID mismatch"
                    );
                }
        );

        report.run(
                "Default RF parameters are valid",
                () -> {
                    NetworkProfile profile =
                            NetworkProfileRegistry.defaultProfile();

                    require(
                            profile.defaultFrequencyHz()
                                    > 0.0,
                            "frequency must be > 0"
                    );

                    require(
                            profile.bandwidthHz()
                                    > 0.0,
                            "bandwidth must be > 0"
                    );

                    require(
                            profile.transmitPowerWatts()
                                    > 0.0,
                            "TX power must be > 0"
                    );

                    require(
                            profile.maximumRangeBlocks()
                                    > 0.0,
                            "range must be > 0"
                    );
                }
        );

        return report;
    }

    private static TestReport testPhase2() {
        TestReport report =
                new TestReport(
                        "Phase 2 - RF / PHY Math"
                );

        report.run(
                "1 watt equals 30 dBm",
                () ->
                        requireNear(
                                RfMath.wattsToDbm(
                                        1.0
                                ),
                                30.0,
                                1.0E-9,
                                "1 W -> dBm"
                        )
        );

        report.run(
                "30 dBm equals 1 watt",
                () ->
                        requireNear(
                                RfMath.dbmToWatts(
                                        30.0
                                ),
                                1.0,
                                1.0E-9,
                                "30 dBm -> W"
                        )
        );

        report.run(
                "FSPL increases with distance",
                () -> {
                    double near =
                            RfMath.freeSpacePathLossDb(
                                    10.0,
                                    2.4E9
                            );

                    double far =
                            RfMath.freeSpacePathLossDb(
                                    100.0,
                                    2.4E9
                            );

                    require(
                            far > near,
                            "far FSPL must exceed near FSPL"
                    );
                }
        );

        report.run(
                "Noise floor increases with bandwidth",
                () -> {
                    double narrow =
                            RfMath.noiseFloorDbm(
                                    25_000.0,
                                    RfMath.STANDARD_TEMPERATURE_K,
                                    5.0
                            );

                    double wide =
                            RfMath.noiseFloorDbm(
                                    20_000_000.0,
                                    RfMath.STANDARD_TEMPERATURE_K,
                                    5.0
                            );

                    require(
                            wide > narrow,
                            "20 MHz noise floor must exceed 25 kHz noise floor"
                    );
                }
        );

        report.run(
                "Shannon: 20 MHz at 0 dB is about 20 Mbps",
                () ->
                        requireNear(
                                RfMath.shannonCapacityBps(
                                        20_000_000.0,
                                        0.0
                                ),
                                20_000_000.0,
                                1.0,
                                "Shannon capacity"
                        )
        );

        return report;
    }

    private static TestReport testPhase3() {
        TestReport report =
                new TestReport(
                        "Phase 3 - Data-driven PHY"
                );

        report.run(
                "Default profile contains PHY definition",
                () -> {
                    NetworkProfile profile =
                            NetworkProfileRegistry.defaultProfile();

                    require(
                            profile.phy() != null,
                            "profile PHY is null"
                    );
                }
        );

        report.run(
                "PHY definition creates runtime profile",
                () -> {
                    NetworkProfile profile =
                            NetworkProfileRegistry.defaultProfile();

                    PhyProfile phy =
                            profile.phy()
                                    .toRuntimeProfile(
                                            profile.defaultFrequencyHz(),
                                            profile.bandwidthHz(),
                                            profile.transmitPowerWatts(),
                                            profile.antennaGain()
                                    );

                    require(
                            phy != null,
                            "runtime PHY is null"
                    );

                    require(
                            phy.centerFrequencyHz()
                                    > 0.0,
                            "runtime PHY frequency invalid"
                    );

                    require(
                            phy.bandwidthHz()
                                    > 0.0,
                            "runtime PHY bandwidth invalid"
                    );

                    require(
                            phy.modulation() != null,
                            "runtime PHY modulation missing"
                    );
                }
        );

        return report;
    }

    private static TestReport testPhase4() {
        TestReport report =
                new TestReport(
                        "Phase 4 - Wi-Fi MAC Foundation"
                );

        report.run(
                "Wi-Fi controller starts in legacy mode",
                () -> {
                    WifiMacController controller =
                            new WifiMacController();

                    require(
                            controller.mode()
                                    == WifiMode.LEGACY_DIRECT,
                            "unexpected initial Wi-Fi mode: "
                                    + controller.mode()
                    );
                }
        );

        report.run(
                "Wi-Fi station configuration changes mode",
                () -> {
                    WifiMacController controller =
                            new WifiMacController();

                    controller.configureStation(
                            ""
                    );

                    require(
                            controller.mode()
                                    == WifiMode.STATION,
                            "controller did not enter STATION mode"
                    );

                    require(
                            controller.stationState()
                                    != null,
                            "station state is null"
                    );
                }
        );

        report.run(
                "Wi-Fi AP configuration changes mode",
                () -> {
                    WifiMacController controller =
                            new WifiMacController();

                    controller.configureAccessPoint(
                            "VSIA-DEBUG",
                            "OPEN",
                            ""
                    );

                    require(
                            controller.mode()
                                    == WifiMode.ACCESS_POINT,
                            "controller did not enter ACCESS_POINT mode"
                    );
                }
        );

        return report;
    }

    private static TestReport testPhase5() {
        TestReport report =
                new TestReport(
                        "Phase 5 - Wi-Fi MCS / Adaptation"
                );

        report.run(
                "802.11be high SNR selects higher MCS than weak SNR",
                () -> {
                    WifiMcs weak =
                            WifiMcsTable.select(
                                    "80211be",
                                    0.0
                            );

                    WifiMcs strong =
                            WifiMcsTable.select(
                                    "80211be",
                                    40.0
                            );

                    require(
                            strong.index()
                                    > weak.index(),
                            "MCS did not increase with SNR"
                    );
                }
        );

        report.run(
                "MCS lookup by index is stable",
                () -> {
                    WifiMcs selected =
                            WifiMcsTable.select(
                                    "80211be",
                                    25.0
                            );

                    WifiMcs lookup =
                            WifiMcsTable.byIndex(
                                    selected.index()
                            );

                    require(
                            lookup.index()
                                    == selected.index(),
                            "MCS lookup index mismatch"
                    );

                    require(
                            lookup.modulation()
                                    == selected.modulation(),
                            "MCS modulation mismatch"
                    );
                }
        );

        report.run(
                "Very poor SNR falls back to robust MCS",
                () -> {
                    WifiMcs selected =
                            WifiMcsTable.select(
                                    "80211be",
                                    -100.0
                            );

                    require(
                            selected.index()
                                    == 0,
                            "expected MCS 0, got "
                                    + selected.index()
                    );
                }
        );

        return report;
    }

    private static TestReport testPhase6() {
        TestReport report =
                new TestReport(
                        "Phase 6 - Cellular RAN Math"
                );

        report.run(
                "CQI stays in 1-15 range",
                () -> {
                    for (double snr = -50.0;
                         snr <= 60.0;
                         snr += 1.0) {

                        int cqi =
                                CellularCqiModel.fromSnrDb(
                                        snr
                                );

                        require(
                                cqi >= 1
                                        && cqi <= 15,
                                "CQI out of range at SNR "
                                        + snr
                                        + ": "
                                        + cqi
                        );
                    }
                }
        );

        report.run(
                "CQI increases with SNR",
                () -> {
                    int weak =
                            CellularCqiModel.fromSnrDb(
                                    -5.0
                            );

                    int strong =
                            CellularCqiModel.fromSnrDb(
                                    25.0
                            );

                    require(
                            strong > weak,
                            "CQI did not increase"
                    );
                }
        );

        report.run(
                "CQI saturates at 15",
                () ->
                        require(
                                CellularCqiModel.fromSnrDb(
                                        1000.0
                                ) == 15,
                                "CQI did not saturate at 15"
                        )
        );

        return report;
    }

    private static TestReport testPhase7() {
        TestReport report =
                new TestReport(
                        "Phase 7 - Cellular Stack / Security"
                );

        report.run(
                "5G-AKA simulation is deterministic for same inputs",
                () -> {
                    byte[] key =
                            fixedKey();

                    byte[] challenge =
                            "0123456789ABCDEF"
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    );

                    byte[] first =
                            FiveGAkaEngine.calculateResponse(
                                    key,
                                    "imsi-001010000000001",
                                    challenge
                            );

                    byte[] second =
                            FiveGAkaEngine.calculateResponse(
                                    key,
                                    "imsi-001010000000001",
                                    challenge
                            );

                    require(
                            Arrays.equals(
                                    first,
                                    second
                            ),
                            "AKA response changed for identical inputs"
                    );
                }
        );

        report.run(
                "Wrong subscriber key changes AKA response",
                () -> {
                    byte[] challenge =
                            "0123456789ABCDEF"
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    );

                    byte[] correct =
                            FiveGAkaEngine.calculateResponse(
                                    fixedKey(),
                                    "imsi-001010000000001",
                                    challenge
                            );

                    byte[] wrongKey =
                            fixedKey();

                    wrongKey[0] ^= 0x55;

                    byte[] wrong =
                            FiveGAkaEngine.calculateResponse(
                                    wrongKey,
                                    "imsi-001010000000001",
                                    challenge
                            );

                    require(
                            !Arrays.equals(
                                    correct,
                                    wrong
                            ),
                            "wrong key produced same AKA response"
                    );
                }
        );

        report.run(
                "RLC segments and reassembles a large SDU",
                () -> {
                    byte[] payload =
                            new byte[3000];

                    for (int i = 0;
                         i < payload.length;
                         i++) {
                        payload[i] =
                                (byte) (
                                        i & 0xFF
                                );
                    }

                    RlcAmEntity sender =
                            new RlcAmEntity();

                    RlcAmEntity receiver =
                            new RlcAmEntity();

                    List<RlcPdu> segments =
                            sender.segment(
                                    payload,
                                    768
                            );

                    require(
                            segments.size() > 1,
                            "payload was not segmented"
                    );

                    byte[] rebuilt =
                            null;

                    for (RlcPdu segment : segments) {
                        byte[] candidate =
                                receiver.receive(
                                        segment
                                );

                        if (candidate != null) {
                            rebuilt =
                                    candidate;
                        }
                    }

                    require(
                            Arrays.equals(
                                    payload,
                                    rebuilt
                            ),
                            "RLC reassembled payload mismatch"
                    );
                }
        );

        report.run(
                "PDCP protect/unprotect round trip",
                () -> {
                    byte[] cipherKey =
                            fixedKey();

                    byte[] integrityKey =
                            Arrays.copyOf(
                                    fixedKey(),
                                    32
                            );

                    PdcpSecurityContext context =
                            new PdcpSecurityContext(
                                    cipherKey,
                                    integrityKey
                            );

                    PdcpEntity sender =
                            new PdcpEntity();

                    PdcpEntity receiver =
                            new PdcpEntity();

                    byte[] clear =
                            "VSIA-PDCP-TEST"
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    );

                    PdcpPdu protectedPdu =
                            sender.protect(
                                    clear,
                                    context
                            );

                    byte[] decoded =
                            receiver.unprotect(
                                    protectedPdu,
                                    context
                            );

                    require(
                            Arrays.equals(
                                    clear,
                                    decoded
                            ),
                            "PDCP round trip mismatch"
                    );
                }
        );

        return report;
    }

    private static TestReport testPhase8() {
        TestReport report =
                new TestReport(
                        "Phase 8 - Radio Network Stack"
                );

        report.run(
                "Packet-radio encode/decode + CRC",
                () -> {
                    UUID source =
                            UUID.fromString(
                                    "00000000-0000-0000-0000-000000000001"
                            );

                    UUID destination =
                            UUID.fromString(
                                    "00000000-0000-0000-0000-000000000002"
                            );

                    byte[] payload =
                            "VSIA-RADIO"
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    );

                    PacketRadioFrame original =
                            new PacketRadioFrame(
                                    source,
                                    destination,
                                    42,
                                    16,
                                    payload
                            );

                    PacketRadioFrame decoded =
                            PacketRadioFrame.decode(
                                    original.encode()
                            );

                    require(
                            decoded.sourceId()
                                    .equals(
                                            source
                                    ),
                            "source mismatch"
                    );

                    require(
                            decoded.destinationId()
                                    .equals(
                                            destination
                                    ),
                            "destination mismatch"
                    );

                    require(
                            decoded.sequenceNumber()
                                    == 42,
                            "sequence mismatch"
                    );

                    require(
                            Arrays.equals(
                                    payload,
                                    decoded.payload()
                            ),
                            "payload mismatch"
                    );
                }
        );

        report.run(
                "Packet-radio detects corrupted CRC",
                () -> {
                    PacketRadioFrame frame =
                            new PacketRadioFrame(
                                    UUID.randomUUID(),
                                    UUID.randomUUID(),
                                    1,
                                    4,
                                    new byte[]{
                                            1,
                                            2,
                                            3,
                                            4
                                    }
                            );

                    byte[] encoded =
                            frame.encode();

                    encoded[
                            encoded.length / 2
                            ] ^= 0x01;

                    boolean rejected =
                            false;

                    try {
                        PacketRadioFrame.decode(
                                encoded
                        );
                    } catch (IllegalArgumentException expected) {
                        rejected =
                                true;
                    }

                    require(
                            rejected,
                            "corrupted packet was accepted"
                    );
                }
        );

        report.run(
                "Radio AES-GCM protection round trip",
                () -> {
                    byte[] key =
                            fixedKey();

                    byte[] clear =
                            "VSIA-SECURE-RADIO"
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    );

                    byte[] protectedBytes =
                            RadioSecurityEngine.protect(
                                    key,
                                    clear
                            );

                    require(
                            !Arrays.equals(
                                    clear,
                                    protectedBytes
                            ),
                            "protected bytes equal plaintext"
                    );

                    byte[] decoded =
                            RadioSecurityEngine.unprotect(
                                    key,
                                    protectedBytes
                            );

                    require(
                            Arrays.equals(
                                    clear,
                                    decoded
                            ),
                            "radio security round trip mismatch"
                    );
                }
        );

        report.run(
                "FHSS same seed produces same hop sequence",
                () -> {
                    double[] frequencies =
                            new double[]{
                                    149_000_000.0,
                                    149_025_000.0,
                                    149_050_000.0,
                                    149_075_000.0
                            };

                    FrequencyHopPlan a =
                            new FrequencyHopPlan(
                                    frequencies,
                                    123456789L
                            );

                    FrequencyHopPlan b =
                            new FrequencyHopPlan(
                                    frequencies,
                                    123456789L
                            );

                    for (int i = 0;
                         i < 20;
                         i++) {
                        requireNear(
                                a.currentFrequencyHz(),
                                b.currentFrequencyHz(),
                                0.0,
                                "FHSS mismatch at hop "
                                        + i
                        );

                        a.advance();
                        b.advance();
                    }
                }
        );

        return report;
    }

    private static TestReport testPhase9() {
        TestReport report =
                new TestReport(
                        "Phase 9 - Protocol VM"
                );

        report.run(
                "Protocol registry is accessible",
                () -> {
                    require(
                            ProtocolProgramRegistry.values()
                                    != null,
                            "program registry returned null"
                    );
                }
        );

        report.run(
                "VM can deliver host input",
                () -> {
                    byte[] expected =
                            "VSIA-VM-TEST"
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    );

                    CapturingVmHost host =
                            new CapturingVmHost();

                    ProtocolProgram program =
                            new ProtocolProgram(
                                    new ResourceLocation(
                                            Signality.MODID,
                                            "debug_vm_delivery"
                                    ),
                                    1,
                                    new ProtocolVmLimits(
                                            64,
                                            4096,
                                            4096,
                                            4,
                                            100
                                    ),
                                    Map.of(
                                            "on_tx",
                                            0
                                    ),
                                    List.of(
                                            new ProtocolInstruction(
                                                    ProtocolOpcode.BUFFER_COPY_INPUT,
                                                    0,
                                                    0,
                                                    0,
                                                    0.0,
                                                    "",
                                                    -1
                                            ),
                                            new ProtocolInstruction(
                                                    ProtocolOpcode.DELIVER_HOST,
                                                    0,
                                                    0,
                                                    0,
                                                    0.0,
                                                    "",
                                                    -1
                                            ),
                                            new ProtocolInstruction(
                                                    ProtocolOpcode.HALT,
                                                    0,
                                                    0,
                                                    0,
                                                    0.0,
                                                    "",
                                                    -1
                                            )
                                    )
                            );

                    ProtocolVirtualMachine vm =
                            new ProtocolVirtualMachine(
                                    program,
                                    host
                            );

                    ProtocolVmRunResult result =
                            vm.run(
                                    "on_tx",
                                    expected,
                                    ProtocolVmEnvironment.empty()
                            );

                    require(
                            result.success(),
                            "VM failed: "
                                    + result.error()
                    );

                    require(
                            Arrays.equals(
                                    expected,
                                    host.delivered
                            ),
                            "VM host-delivery payload mismatch"
                    );
                }
        );

        report.run(
                "VM instruction budget stops infinite loop",
                () -> {
                    CapturingVmHost host =
                            new CapturingVmHost();

                    ProtocolProgram program =
                            new ProtocolProgram(
                                    new ResourceLocation(
                                            Signality.MODID,
                                            "debug_vm_budget"
                                    ),
                                    1,
                                    new ProtocolVmLimits(
                                            32,
                                            1024,
                                            1024,
                                            0,
                                            20
                                    ),
                                    Map.of(
                                            "loop",
                                            0
                                    ),
                                    List.of(
                                            new ProtocolInstruction(
                                                    ProtocolOpcode.JUMP,
                                                    0,
                                                    0,
                                                    0,
                                                    0.0,
                                                    "",
                                                    0
                                            )
                                    )
                            );

                    ProtocolVirtualMachine vm =
                            new ProtocolVirtualMachine(
                                    program,
                                    host
                            );

                    ProtocolVmRunResult result =
                            vm.run(
                                    "loop",
                                    new byte[0],
                                    ProtocolVmEnvironment.empty()
                            );

                    require(
                            !result.success(),
                            "infinite loop unexpectedly succeeded"
                    );

                    require(
                            result.error()
                                    .contains(
                                            "Instruction budget"
                                    ),
                            "unexpected VM error: "
                                    + result.error()
                    );
                }
        );

        report.run(
                "VM RF primitive calls real RfMath",
                () -> {
                    CapturingVmHost host =
                            new CapturingVmHost();

                    ProtocolProgram program =
                            new ProtocolProgram(
                                    new ResourceLocation(
                                            Signality.MODID,
                                            "debug_vm_rf"
                                    ),
                                    1,
                                    new ProtocolVmLimits(
                                            64,
                                            1024,
                                            1024,
                                            0,
                                            20
                                    ),
                                    Map.of(
                                            "on_tx",
                                            0
                                    ),
                                    List.of(
                                            new ProtocolInstruction(
                                                    ProtocolOpcode.SET_NUM,
                                                    1,
                                                    0,
                                                    0,
                                                    1.0,
                                                    "",
                                                    -1
                                            ),
                                            new ProtocolInstruction(
                                                    ProtocolOpcode.PRIMITIVE,
                                                    0,
                                                    1,
                                                    0,
                                                    0.0,
                                                    "rf.watts_to_dbm",
                                                    -1
                                            ),
                                            new ProtocolInstruction(
                                                    ProtocolOpcode.HALT,
                                                    0,
                                                    0,
                                                    0,
                                                    0.0,
                                                    "",
                                                    -1
                                            )
                                    )
                            );

                    ProtocolVirtualMachine vm =
                            new ProtocolVirtualMachine(
                                    program,
                                    host
                            );

                    ProtocolVmRunResult result =
                            vm.run(
                                    "on_tx",
                                    new byte[0],
                                    ProtocolVmEnvironment.empty()
                            );

                    require(
                            result.success(),
                            "VM RF program failed: "
                                    + result.error()
                    );

                    requireNear(
                            vm.number(
                                    0
                            ),
                            30.0,
                            1.0E-9,
                            "VM watts_to_dbm"
                    );
                }
        );

        return report;
    }

    private static byte[] fixedKey() {
        return new byte[]{
                0x10,
                0x11,
                0x12,
                0x13,
                0x14,
                0x15,
                0x16,
                0x17,
                0x18,
                0x19,
                0x1A,
                0x1B,
                0x1C,
                0x1D,
                0x1E,
                0x1F
        };
    }

    private static void require(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new IllegalStateException(
                    message
            );
        }
    }

    private static void requireNear(
            double actual,
            double expected,
            double tolerance,
            String label
    ) {
        if (!Double.isFinite(
                actual
        )) {
            throw new IllegalStateException(
                    label
                            + " produced non-finite value "
                            + actual
            );
        }

        if (Math.abs(
                actual - expected
        ) > tolerance) {
            throw new IllegalStateException(
                    label
                            + " expected "
                            + expected
                            + " but got "
                            + actual
            );
        }
    }

    private static void printReport(
            CommandSourceStack source,
            TestReport report
    ) {
        source.sendSuccess(
                () -> Component.literal(
                        "=== "
                                + report.name
                                + " ==="
                ).withStyle(
                        ChatFormatting.AQUA
                ),
                false
        );

        for (TestResult result : report.results) {
            if (result.passed) {
                source.sendSuccess(
                        () -> Component.literal(
                                "[PASS] "
                                        + result.name
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                        false
                );
            } else {
                source.sendFailure(
                        Component.literal(
                                "[FAIL] "
                                        + result.name
                                        + " - "
                                        + result.message
                        ).withStyle(
                                ChatFormatting.RED
                        )
                );
            }
        }
    }

    private static void printSummary(
            CommandSourceStack source,
            TestReport report
    ) {
        ChatFormatting color =
                report.failed == 0
                        ? ChatFormatting.GREEN
                        : ChatFormatting.RED;

        source.sendSuccess(
                () -> Component.literal(
                        "Result: "
                                + report.passed
                                + " passed, "
                                + report.failed
                                + " failed"
                ).withStyle(
                        color
                ),
                false
        );
    }

    private static final class TestReport {
        private final String name;
        private final List<TestResult> results =
                new ArrayList<>();

        private int passed;
        private int failed;

        private TestReport(
                String name
        ) {
            this.name =
                    name;
        }

        private void run(
                String name,
                CheckedRunnable test
        ) {
            try {
                test.run();

                results.add(
                        new TestResult(
                                name,
                                true,
                                ""
                        )
                );

                passed++;
            } catch (Throwable throwable) {
                String message =
                        throwable.getMessage();

                if (message == null
                        || message.isBlank()) {
                    message =
                            throwable
                                    .getClass()
                                    .getSimpleName();
                }

                results.add(
                        new TestResult(
                                name,
                                false,
                                message
                        )
                );

                failed++;

                Signality.LOGGER.warn(
                        "Signality debug test failed: {} / {}",
                        this.name,
                        name,
                        throwable
                );
            }
        }

        private void merge(
                TestReport other
        ) {
            passed +=
                    other.passed;

            failed +=
                    other.failed;

            results.addAll(
                    other.results
            );
        }
    }

    private record TestResult(
            String name,
            boolean passed,
            String message
    ) {
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    private static final class CapturingVmHost
            implements ProtocolVmHost {

        private byte[] sent =
                new byte[0];

        private byte[] delivered =
                new byte[0];

        private long tick;

        @Override
        public void sendFrame(
                byte[] frame
        ) {
            sent =
                    frame == null
                            ? new byte[0]
                            : frame.clone();
        }

        @Override
        public void deliverToHost(
                byte[] payload
        ) {
            delivered =
                    payload == null
                            ? new byte[0]
                            : payload.clone();
        }

        @Override
        public long currentTick() {
            return tick;
        }
    }
}
