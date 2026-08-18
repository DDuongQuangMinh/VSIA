package com.k1ngtle.vsia.signality.internet;

import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.api.signal.ISignalTransmitter;
import com.k1ngtle.vsia.signality.api.signal.SignalBand;
import com.k1ngtle.vsia.signality.api.signal.SignalPacket;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.engineering.EngineeringPhyEngine;
import com.k1ngtle.vsia.signality.engineering.channel.ActiveRfTransmission;
import com.k1ngtle.vsia.signality.engineering.channel.RfChannelAssessment;
import com.k1ngtle.vsia.signality.engineering.channel.RfChannelSettings;
import com.k1ngtle.vsia.signality.engineering.channel.RfChannelEnvironment;
import com.k1ngtle.vsia.signality.engineering.channel.RfDiscreteEventScheduler;
import com.k1ngtle.vsia.signality.engineering.channel.RfMediumState;
import com.k1ngtle.vsia.signality.engineering.channel.RfAntennaPattern;
import com.k1ngtle.vsia.signality.engineering.channel.RfAntennaState;
import com.k1ngtle.vsia.signality.engineering.channel.RfAntennaTransform;
import com.k1ngtle.vsia.signality.engineering.channel.VsWorldPoseResolver;
import com.k1ngtle.vsia.signality.engineering.channel.RfKinematicTracker;
import com.k1ngtle.vsia.signality.engineering.channel.RfPolarization;
import com.k1ngtle.vsia.signality.engineering.channel.ScheduledRfTransmission;
import com.k1ngtle.vsia.signality.engineering.cellular.CellRecord;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularMode;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularRanController;
import com.k1ngtle.vsia.signality.engineering.cellular.ResourceBlockAllocation;
import com.k1ngtle.vsia.signality.engineering.cellular.UeRanState;
import com.k1ngtle.vsia.signality.engineering.cellular.core.PduSession;
import com.k1ngtle.vsia.signality.engineering.cellular.nas.NasState;
import com.k1ngtle.vsia.signality.engineering.phy.PhyProfile;
import com.k1ngtle.vsia.signality.engineering.radio.RadioChannel;
import com.k1ngtle.vsia.signality.engineering.radio.RadioController;
import com.k1ngtle.vsia.signality.engineering.radio.RadioEmission;
import com.k1ngtle.vsia.signality.engineering.radio.RadioLinkQuality;
import com.k1ngtle.vsia.signality.engineering.radio.RadioMode;
import com.k1ngtle.vsia.signality.engineering.radio.RepeaterConfig;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmController;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmEnvironment;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmHost;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmRunResult;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmScheduler;
import com.k1ngtle.vsia.signality.engineering.phy.PhyResult;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiAccessCategory;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMacController;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMacFrame;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcs;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcsTable;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMode;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityState;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiChannelWidth;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiGuardInterval;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyConfiguration;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyController;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyGeneration;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyLinkAssessment;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyLinkModel;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyAirtimeModel;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPpduEstimate;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPuncturingPattern;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyDecision;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyEngine;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyMode;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketDirection;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketOutcome;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketTraceBuffer;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketTraceEvent;
import com.k1ngtle.vsia.signality.engineering.reality.GeneralRfAirtimeModel;
import com.k1ngtle.vsia.signality.engineering.reality.NetworkRealityAssessment;
import com.k1ngtle.vsia.signality.engineering.reality.NetworkRealityEngine;
import com.k1ngtle.vsia.signality.engineering.reality.NetworkTimebase;
import com.k1ngtle.vsia.signality.engineering.reality.RfMicroTiming;
import com.k1ngtle.vsia.signality.engineering.reality.RfMicroTimingRegistry;
import com.k1ngtle.vsia.signality.engineering.channel.RfTransmissionRegistry;
import com.k1ngtle.vsia.signality.internet.network.NetworkKind;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfile;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfileRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public abstract class NetworkDeviceBlockEntity
        extends BlockEntity
        implements ISignalReceiver, ISignalTransmitter {

    private UUID signalId = UUID.randomUUID();

    protected String macAddress;
    protected String ipAddress = "0.0.0.0";
    protected String defaultGatewayMac = "";

    private ResourceLocation networkProfileId =
            NetworkProfileRegistry.DEFAULT_PROFILE_ID;

    private double activeFrequencyHz =
            NetworkProfileRegistry
                    .defaultProfile()
                    .defaultFrequencyHz();

    private PhyResult lastPhyResult;

    private RfChannelAssessment lastRfChannelAssessment;

    private NetworkRealityAssessment lastNetworkRealityAssessment;

    private RfAntennaState rfAntennaState =
            RfAntennaState.isotropic();

    private final WifiMacController wifiMac =
            new WifiMacController();

    private final WifiPhyController wifiPhy =
            new WifiPhyController();

    private WifiLivePhyMode wifiLivePhyMode =
            WifiLivePhyMode.ANALYTICAL;

    private WifiLivePhyDecision lastWifiLivePhyDecision =
            WifiLivePhyDecision.bypass(
                    WifiLivePhyMode.ANALYTICAL,
                    Double.NaN,
                    "Not evaluated"
            );

    private long activeWifiResponseReferenceMicros =
            -1L;

    private int wifiEngineeringTestSequence;

    private final WifiPacketTraceBuffer wifiPacketTrace =
            new WifiPacketTraceBuffer();

    private final CellularRanController cellularRan =
            new CellularRanController();

    private final RadioController radio =
            new RadioController();

    private final ProtocolVmController protocolVm =
            new ProtocolVmController(
                    new ProtocolVmHost() {
                        @Override
                        public void sendFrame(
                                byte[] frame
                        ) {
                            transmitProtocolVmFrame(
                                    frame
                            );
                        }

                        @Override
                        public void deliverToHost(
                                byte[] payload
                        ) {
                            handleProtocolVmHostPayload(
                                    payload
                            );
                        }

                        @Override
                        public long currentTick() {
                            return level == null
                                    ? 0L
                                    : level.getGameTime();
                        }
                    }
            );

    public NetworkDeviceBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);

        this.macAddress =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level != null && !level.isClientSide) {
            normalizeNetworkProfile();

            SignalBus.registerReceiver(this);
            SignalBus.registerTransmitter(this);

            ProtocolVmScheduler.register(
                    signalId,
                    protocolVm
            );
        }
    }

    @Override
    public void setRemoved() {
        ProtocolVmScheduler.unregister(
                signalId
        );

        SignalBus.unregisterReceiver(signalId);
        SignalBus.unregisterTransmitter(signalId);

        RfKinematicTracker.remove(
                signalId
        );

        super.setRemoved();
    }

    @Override
    public UUID id() {
        return signalId;
    }

    @Override
    public ServerLevel level() {
        return (ServerLevel) level;
    }

    private Vec3 localRfPosition() {
        return Vec3.atCenterOf(
                worldPosition
        ).add(
                0.0,
                0.5,
                0.0
        );
    }

    @Override
    public Vec3 positionWorld() {
        Vec3 local =
                localRfPosition();

        if (level == null) {
            return local;
        }

        return VsWorldPoseResolver.toWorld(
                level,
                local
        );
    }

    public RfAntennaState worldRfAntennaState() {
        return RfAntennaTransform.toWorld(
                level,
                localRfPosition(),
                rfAntennaState
        );
    }

    public NetworkProfile networkProfile() {
        return NetworkProfileRegistry
                .getOrDefault(networkProfileId);
    }

    public ResourceLocation networkProfileId() {
        return networkProfileId;
    }

    public double activeFrequencyHz() {
        return activeFrequencyHz;
    }

    public PhyResult lastPhyResult() {
        return lastPhyResult;
    }

    public RfChannelAssessment lastRfChannelAssessment() {
        return lastRfChannelAssessment;
    }

    public NetworkRealityAssessment lastNetworkRealityAssessment() {
        return lastNetworkRealityAssessment;
    }

    public RfAntennaState rfAntennaState() {
        return rfAntennaState;
    }

    public void configureRfAntenna(
            RfAntennaPattern pattern,
            RfPolarization polarization,
            Vec3 boresight,
            double peakGainDbi,
            double horizontalBeamwidthDeg,
            double verticalBeamwidthDeg,
            double frontToBackRatioDb
    ) {
        rfAntennaState =
                new RfAntennaState(
                        pattern,
                        polarization,
                        boresight,
                        peakGainDbi,
                        horizontalBeamwidthDeg,
                        verticalBeamwidthDeg,
                        frontToBackRatioDb
                );

        setChanged();
    }

    public void resetRfAntenna() {
        rfAntennaState =
                RfAntennaState.isotropic();

        setChanged();
    }

    public Vec3 rfVelocityMetersPerSecond() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Vec3.ZERO;
        }

        return RfKinematicTracker
                .updateAndGetVelocityMetersPerSecond(
                        signalId,
                        positionWorld(),
                        serverLevel.getGameTime()
                );
    }

    public RfMediumState senseRfMedium(
            double busyThresholdDbm
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return new RfMediumState(
                    0.0,
                    Double.NEGATIVE_INFINITY,
                    0,
                    false
            );
        }

        return RfDiscreteEventScheduler.sense(
                serverLevel,
                positionWorld(),
                activeFrequencyHz,
                tuningBandwidthHz(),
                busyThresholdDbm,
                worldRfAntennaState()
        );
    }

    public boolean isRfMediumBusy(
            double busyThresholdDbm
    ) {
        return senseRfMedium(
                busyThresholdDbm
        ).busy();
    }

    public PhyProfile currentPhyProfile() {
        PhyProfile base =
                currentBasePhyProfile();

        if (isRadioProfile()
                && radio.mode()
                != RadioMode.LEGACY_DIRECT
                && radio.receiveBandwidthHz() > 0.0) {
            return new PhyProfile(
                    base.centerFrequencyHz(),
                    radio.receiveBandwidthHz(),
                    base.txPowerDbm(),
                    base.txGainDbi(),
                    base.rxGainDbi(),
                    base.receiverNoiseFigureDb(),
                    base.modulation(),
                    base.coding(),
                    base.spatialStreams(),
                    base.guardEfficiency(),
                    base.macEfficiency()
            );
        }

        return base;
    }

    public WifiMode wifiMode() {
        return wifiMac.mode();
    }

    public WifiStationState wifiStationState() {
        return wifiMac.stationState();
    }

    public WifiSecurityState wifiSecurityState() {
        return wifiMac.securityState();
    }

    public int wifiMcsIndex() {
        return wifiMac.currentMcsIndex();
    }

    public WifiPhyConfiguration wifiPhyConfiguration() {
        ensureWifiPhyConfigured();

        return wifiPhy.configuration();
    }

    public WifiPhyLinkAssessment lastWifiPhyAssessment() {
        return wifiPhy.lastAssessment();
    }

    public double wifiEstimatedPhyRateBps() {
        WifiPhyLinkAssessment assessment =
                wifiPhy.lastAssessment();

        return assessment == null
                ? 0.0
                : assessment.rate()
                .effectivePhyRateBps();
    }

    public double wifiDopplerIciFraction() {
        WifiPhyLinkAssessment assessment =
                wifiPhy.lastAssessment();

        return assessment == null
                ? 0.0
                : assessment.ofdmIciPowerFraction();
    }

    public WifiLivePhyMode wifiLivePhyMode() {
        return wifiLivePhyMode;
    }

    public WifiLivePhyDecision lastWifiLivePhyDecision() {
        return lastWifiLivePhyDecision;
    }

    public void setWifiLivePhyMode(
            WifiLivePhyMode mode
    ) {
        wifiLivePhyMode =
                mode == null
                        ? WifiLivePhyMode.ANALYTICAL
                        : mode;

        lastWifiLivePhyDecision =
                WifiLivePhyDecision.bypass(
                        wifiLivePhyMode,
                        Double.NaN,
                        "Mode changed"
                );

        setChanged();
    }

    public boolean sendWifiEngineeringTestFrame(
            int requestedFrameBytes
    ) {
        if (!isWifiProfile()
                || !(level instanceof ServerLevel)) {
            return false;
        }

        int frameBytes =
                Math.max(
                        64,
                        Math.min(
                                4096,
                                requestedFrameBytes
                        )
                );

        byte[] body =
                new byte[
                        frameBytes
                        ];

        byte[] signature =
                "VSIA-W1.6.4-LINK-TEST"
                        .getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        );

        System.arraycopy(
                signature,
                0,
                body,
                0,
                Math.min(
                        signature.length,
                        body.length
                )
        );

        for (int i = signature.length;
             i < body.length;
             i++) {
            body[i] =
                    (byte) (
                            i * 31
                                    + wifiEngineeringTestSequence
                    );
        }

        byte[] broadcast =
                new byte[] {
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF
                };

        WifiMacFrame frame =
                new WifiMacFrame(
                        WifiMacController.FC_DATA,
                        0,
                        broadcast,
                        engineeringMacBytes(
                                macAddress
                        ),
                        broadcast,
                        (
                                wifiEngineeringTestSequence++
                                        & 0x0FFF
                        )
                                << 4,
                        body
                );

        transmitWifiFrame(
                frame
        );

        return true;
    }

    private byte[] engineeringMacBytes(
            String value
    ) {
        String normalized =
                value == null
                        ? ""
                        : value.replace(
                                ":",
                                ""
                        )
                        .replace(
                                "-",
                                ""
                        );

        byte[] out =
                new byte[
                        6
                        ];

        if (normalized.length() != 12) {
            return out;
        }

        try {
            for (int i = 0;
                 i < out.length;
                 i++) {
                out[i] =
                        (byte) Integer.parseInt(
                                normalized.substring(
                                        i * 2,
                                        i * 2 + 2
                                ),
                                16
                        );
            }
        } catch (Exception ignored) {
            return new byte[
                    6
                    ];
        }

        return out;
    }

    public java.util.List<WifiPacketTraceEvent> wifiPacketTraceSnapshot() {
        return wifiPacketTrace.snapshot();
    }

    public void clearWifiPacketTrace() {
        wifiPacketTrace.clear();
    }

    public WifiPpduEstimate estimateWifiPpdu(
            int frameBytes
    ) {
        if (!isWifiProfile()) {
            return null;
        }

        ensureWifiPhyConfigured();

        WifiMcs mcs =
                WifiMcsTable.byIndex(
                        wifiMac.currentMcsIndex()
                );

        double snrDb =
                wifiMac.lastObservedSnrDb();

        if (!Double.isFinite(
                snrDb
        )) {
            snrDb =
                    0.0;
        }

        return WifiPhyAirtimeModel.estimate(
                wifiPhy.configuration(),
                mcs,
                Math.max(
                        1L,
                        (long) frameBytes
                                * 8L
                ),
                wifiPhy.puncturing(),
                snrDb
        );
    }

    public void configureWifiPhy(
            WifiChannelWidth channelWidth,
            WifiGuardInterval guardInterval,
            int spatialStreams,
            int transmitAntennas,
            int receiveAntennas,
            double spatialCorrelation
    ) {
        if (!isWifiProfile()) {
            return;
        }

        WifiPhyGeneration generation =
                WifiPhyGeneration.fromProtocol(
                        networkProfile().protocol()
                );

        wifiPhy.configure(
                new WifiPhyConfiguration(
                        generation,
                        channelWidth,
                        guardInterval,
                        spatialStreams,
                        transmitAntennas,
                        receiveAntennas,
                        spatialCorrelation,
                        1.0
                )
        );

        setChanged();
    }

    public void setWifiPuncturingMask(
            long inactiveTwentyMhzMask
    ) {
        if (!isWifiProfile()) {
            return;
        }

        ensureWifiPhyConfigured();

        WifiChannelWidth width =
                wifiPhy.configuration()
                        .channelWidth();

        wifiPhy.setPuncturing(
                new WifiPuncturingPattern(
                        Math.max(
                                1,
                                width.mhz() / 20
                        ),
                        inactiveTwentyMhzMask
                )
        );

        setChanged();
    }

    public Collection<WifiNetworkRecord> discoveredWifiNetworks() {
        return wifiMac.discoveredNetworks();
    }
    public String wifiMacAddress() {
        return macAddress;
    }

    public java.util.Set<String> wifiAssociatedStations() {
        return wifiMac.associatedStations();
    }

    public int wifiPendingDataTransmissions() {
        return wifiMac.pendingDataTransmissions();
    }

    public boolean sendWifiEngineeringAssociatedData(
            int requestedBytes
    ) {
        if (!isWifiProfile()
                || wifiMac.mode()
                == WifiMode.LEGACY_DIRECT) {
            return false;
        }

        int dataBytes =
                Math.max(
                        64,
                        Math.min(
                                4096,
                                requestedBytes
                        )
                );

        String targetMac;

        if (wifiMac.mode()
                == WifiMode.STATION) {
            if (!wifiMac.isAssociated()) {
                return false;
            }

            targetMac =
                    WifiMacController.BROADCAST;
        } else {
            targetMac =
                    wifiMac.associatedStations()
                            .stream()
                            .findFirst()
                            .orElse(
                                    ""
                            );

            if (targetMac.isBlank()) {
                return false;
            }
        }

        CompoundTag body =
                new CompoundTag();

        body.putString(
                "wifi_engineering_control",
                "W1.8_ASSOCIATED_DATA"
        );

        body.putInt(
                "wifi_engineering_length",
                dataBytes
        );

        byte[] payload =
                new byte[
                        dataBytes
                        ];

        byte[] signature =
                "VSIA-W1.8-ASSOCIATED-DATA"
                        .getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        );

        System.arraycopy(
                signature,
                0,
                payload,
                0,
                Math.min(
                        signature.length,
                        payload.length
                )
        );

        for (int i = signature.length;
             i < payload.length;
             i++) {
            payload[i] =
                    (byte) (
                            i * 17
                                    + wifiEngineeringTestSequence
                    );
        }

        body.putByteArray(
                "wifi_engineering_payload",
                payload
        );

        return wifiMac.sendData(
                macAddress,
                targetMac,
                body,
                WifiAccessCategory.BEST_EFFORT,
                wifiSender()
        );
    }


    public CellularMode cellularMode() {
        return cellularRan.mode();
    }

    public UeRanState cellularUeState() {
        return cellularRan.ueState();
    }

    public NasState cellularNasState() {
        return cellularRan.nasState();
    }

    public PduSession cellularPduSession() {
        return cellularRan.pduSession();
    }

    public RadioMode radioMode() {
        return radio.mode();
    }

    public RadioLinkQuality lastRadioLinkQuality() {
        return radio.lastLinkQuality();
    }

    public byte[] lastReceivedRadioVoice() {
        return radio.lastReceivedVoice();
    }

    public boolean bindProtocolProgram(
            ResourceLocation programId
    ) {
        boolean bound =
                protocolVm.bind(
                        programId
                );

        if (bound) {
            setChanged();
        }

        return bound;
    }

    public void unbindProtocolProgram() {
        protocolVm.unbind();
        setChanged();
    }

    public ResourceLocation boundProtocolProgramId() {
        return protocolVm.programId();
    }

    public ProtocolVmRunResult lastProtocolVmResult() {
        return protocolVm.lastResult();
    }

    public UUID servingCellId() {
        return cellularRan.servingCellId();
    }

    public Collection<CellRecord> discoveredCells() {
        return cellularRan.discoveredCells();
    }

    public Collection<ResourceBlockAllocation> scheduleCellularResourceBlocks(
            int totalResourceBlocks
    ) {
        return cellularRan.schedule(totalResourceBlocks);
    }

    public boolean configureWifiStation() {
        return configureWifiStation("");
    }

    public boolean configureWifiStation(String passphrase) {
        if (!isWifiProfile()) {
            return false;
        }

        wifiMac.configureStation(passphrase);
        setChanged();
        return true;
    }

    public boolean configureWifiAccessPoint(String ssid) {
        return configureWifiAccessPoint(ssid, "");
    }

    public boolean configureWifiAccessPoint(
            String ssid,
            String passphrase
    ) {
        if (!isWifiProfile()) {
            return false;
        }

        wifiMac.configureAccessPoint(
                ssid,
                networkProfile().security(),
                passphrase
        );

        setChanged();
        return true;
    }

    public void useLegacyWifiDirectMode() {
        wifiMac.useLegacyDirectMode();
        setChanged();
    }

    public boolean scanWifi() {
        if (!isWifiProfile()
                || wifiMac.mode() != WifiMode.STATION) {
            return false;
        }

        double originalFrequency = activeFrequencyHz;

        for (double frequency
                : networkProfile().frequenciesHz()) {
            activeFrequencyHz = frequency;

            wifiMac.startScan(
                    macAddress,
                    wifiSender()
            );
        }

        activeFrequencyHz = originalFrequency;
        setChanged();
        return true;
    }

    public boolean connectWifi(String ssid) {
        if (!isWifiProfile()
                || wifiMac.mode() != WifiMode.STATION) {
            return false;
        }

        WifiNetworkRecord selected =
                discoveredWifiNetworks()
                        .stream()
                        .filter(network ->
                                network.ssid().equals(ssid))
                        .findFirst()
                        .orElse(null);

        if (selected == null) {
            return false;
        }

        if (networkProfile().supportsFrequency(
                selected.frequencyHz()
        )) {
            activeFrequencyHz =
                    selected.frequencyHz();
        }

        boolean result =
                wifiMac.connect(
                        macAddress,
                        ssid,
                        wifiSender()
                );

        setChanged();
        return result;
    }

    public boolean sendWifiBeacon() {
        if (!isWifiProfile()
                || wifiMac.mode() != WifiMode.ACCESS_POINT) {
            return false;
        }

        wifiMac.sendBeacon(
                macAddress,
                networkProfile().id().toString(),
                activeFrequencyHz,
                wifiSender()
        );

        return true;
    }

    public boolean configureCellularUe() {
        if (!isCellularProfile()) {
            return false;
        }

        cellularRan.configureUe();
        setChanged();
        return true;
    }

    public boolean configureCellularBaseStation(
            int physicalCellId,
            long cellIdentity,
            String plmn
    ) {
        if (!isCellularProfile()) {
            return false;
        }

        cellularRan.configureBaseStation(
                physicalCellId,
                cellIdentity,
                plmn
        );

        setChanged();
        return true;
    }

    public boolean provisionCellularSubscriber(
            String supi,
            byte[] subscriberKey
    ) {
        if (!isCellularProfile()
                || cellularRan.mode()
                != CellularMode.BASE_STATION) {
            return false;
        }

        cellularRan.provisionSubscriber(
                supi,
                subscriberKey
        );

        return true;
    }

    public boolean requestCellularPduSession(
            String dnn,
            int fiveQi
    ) {
        if (!isCellularProfile()
                || cellularRan.mode()
                != CellularMode.UE) {
            return false;
        }

        boolean requested =
                cellularRan.requestPduSession(
                        signalId,
                        dnn,
                        fiveQi,
                        this::transmitCellularControl
                );

        PduSession session =
                cellularRan.pduSession();

        if (requested
                && session != null
                && session.active()) {
            ipAddress =
                    session.ipAddress();

            setChanged();
        }

        return requested;
    }

    public boolean startCellSearch() {
        if (!isCellularProfile()
                || cellularRan.mode() != CellularMode.UE) {
            return false;
        }

        cellularRan.startCellSearch();
        setChanged();
        return true;
    }

    public boolean selectAndAttachStrongestCell() {
        if (!isCellularProfile()
                || cellularRan.mode() != CellularMode.UE) {
            return false;
        }

        CellRecord strongest =
                cellularRan.discoveredCells()
                        .stream()
                        .max((a, b) ->
                                Double.compare(
                                        a.rsrpDbm(),
                                        b.rsrpDbm()
                                ))
                        .orElse(null);

        if (strongest == null) {
            return false;
        }

        if (networkProfile().supportsFrequency(
                strongest.frequencyHz()
        )) {
            activeFrequencyHz =
                    strongest.frequencyHz();
        }

        boolean result =
                cellularRan.selectStrongestCell(
                        signalId,
                        this::transmitCellularControl
                );

        setChanged();
        return result;
    }

    public boolean sendCellBroadcast() {
        if (!isCellularProfile()
                || cellularRan.mode()
                != CellularMode.BASE_STATION) {
            return false;
        }

        cellularRan.sendSystemInformation(
                signalId,
                activeFrequencyHz,
                networkProfile().id().toString(),
                this::transmitCellularControl
        );

        return true;
    }

    public boolean sendCellularMeasurementReport() {
        if (!isCellularProfile()
                || cellularRan.mode() != CellularMode.UE) {
            return false;
        }

        cellularRan.sendMeasurementReport(
                signalId,
                this::transmitCellularControl
        );

        return true;
    }

    public boolean configureRadioTransceiver(
            String channelId,
            double frequencyHz,
            double bandwidthHz,
            RadioEmission emission,
            String accessCode
    ) {
        if (!isRadioProfile()) {
            return false;
        }

        radio.configureTransceiver(
                new RadioChannel(
                        channelId,
                        frequencyHz,
                        bandwidthHz,
                        emission,
                        accessCode
                )
        );

        activeFrequencyHz =
                frequencyHz;

        setChanged();
        return true;
    }

    public boolean configureRadioRepeater(
            String channelId,
            double inputFrequencyHz,
            double outputFrequencyHz,
            double bandwidthHz,
            RadioEmission emission,
            String accessCode
    ) {
        if (!isRadioProfile()) {
            return false;
        }

        radio.configureRepeater(
                new RadioChannel(
                        channelId,
                        inputFrequencyHz,
                        bandwidthHz,
                        emission,
                        accessCode
                ),
                new RepeaterConfig(
                        inputFrequencyHz,
                        outputFrequencyHz,
                        accessCode
                )
        );

        activeFrequencyHz =
                inputFrequencyHz;

        setChanged();
        return true;
    }

    public void setRadioSquelchSnrThresholdDb(
            double thresholdDb
    ) {
        radio.setSquelchSnrThresholdDb(
                thresholdDb
        );

        setChanged();
    }

    public void setRadioSecurityKey(
            byte[] key
    ) {
        radio.setSecurityKey(
                key
        );
    }

    public void enableRadioMesh(
            boolean enabled
    ) {
        radio.enableMesh(
                enabled
        );

        setChanged();
    }

    public void enableRadioFrequencyHopping(
            double[] frequenciesHz,
            long seed
    ) {
        radio.enableFrequencyHopping(
                frequenciesHz,
                seed
        );

        setChanged();
    }

    public void disableRadioFrequencyHopping() {
        radio.disableFrequencyHopping();
        setChanged();
    }

    public boolean pressRadioPtt() {
        return radio.pressPtt();
    }

    public void releaseRadioPtt() {
        radio.releasePtt();
    }

    public boolean sendRadioVoice(
            byte[] encodedAudio,
            boolean endOfTransmission
    ) {
        if (!isRadioProfile()) {
            return false;
        }

        return radio.sendVoice(
                signalId,
                encodedAudio,
                endOfTransmission,
                this::transmitRadioMessage
        );
    }

    public boolean sendRadioPacket(
            UUID destinationId,
            CompoundTag payload
    ) {
        if (!isRadioProfile()) {
            return false;
        }

        return radio.sendPacket(
                signalId,
                destinationId,
                payload,
                this::transmitRadioMessage
        );
    }

    public void discoverRadioRoute(
            UUID destinationId
    ) {
        if (!isRadioProfile()) {
            return;
        }

        radio.discoverRoute(
                signalId,
                destinationId,
                this::transmitRadioMessage
        );
    }

    public boolean setNetworkProfile(
            ResourceLocation profileId
    ) {
        NetworkProfile profile =
                NetworkProfileRegistry.get(profileId)
                        .orElse(null);

        if (profile == null) {
            return false;
        }

        networkProfileId = profile.id();
        activeFrequencyHz =
                profile.defaultFrequencyHz();

        lastPhyResult = null;
        lastRfChannelAssessment = null;
        lastNetworkRealityAssessment = null;
        lastWifiLivePhyDecision =
                WifiLivePhyDecision.bypass(
                        wifiLivePhyMode,
                        Double.NaN,
                        "Network profile changed"
                );

        if (profile.kind() != NetworkKind.WIFI) {
            wifiMac.useLegacyDirectMode();
        }

        if (profile.kind() != NetworkKind.CELLULAR) {
            cellularRan.useLegacyDirectMode();
        }

        setChanged();
        return true;
    }

    public boolean setActiveFrequencyHz(
            double frequencyHz
    ) {
        if (!networkProfile().supportsFrequency(
                frequencyHz
        )) {
            return false;
        }

        activeFrequencyHz = frequencyHz;
        lastPhyResult = null;
        lastRfChannelAssessment = null;
        lastNetworkRealityAssessment = null;
        setChanged();
        return true;
    }

    private void ensureWifiPhyConfigured() {
        if (!isWifiProfile()) {
            return;
        }

        PhyProfile base =
                currentBasePhyProfile();

        WifiPhyGeneration generation =
                WifiPhyGeneration.fromProtocol(
                        networkProfile().protocol()
                );

        WifiChannelWidth width =
                WifiChannelWidth.nearest(
                        base.bandwidthHz()
                );

        WifiPhyConfiguration current =
                wifiPhy.configuration();

        if (current == null
                || current.generation() != generation
                || current.channelWidth() != width) {
            wifiPhy.configure(
                    WifiPhyConfiguration.from(
                            networkProfile().protocol(),
                            base.bandwidthHz(),
                            base.spatialStreams()
                    )
            );
        }
    }

    private PhyProfile currentBasePhyProfile() {
        NetworkProfile profile =
                networkProfile();

        return profile.phy()
                .toRuntimeProfile(
                        activeFrequencyHz,
                        profile.bandwidthHz(),
                        profile.transmitPowerWatts(),
                        profile.antennaGain()
                );
    }

    private boolean isWifiProfile() {
        return networkProfile().kind()
                == NetworkKind.WIFI;
    }

    private boolean isCellularProfile() {
        return networkProfile().kind()
                == NetworkKind.CELLULAR;
    }

    private boolean isRadioProfile() {
        return networkProfile().kind()
                == NetworkKind.RADIO;
    }

    private void normalizeNetworkProfile() {
        NetworkProfile profile =
                NetworkProfileRegistry.get(
                                networkProfileId
                        )
                        .orElseGet(
                                NetworkProfileRegistry::defaultProfile
                        );

        networkProfileId = profile.id();

        if (!profile.supportsFrequency(
                activeFrequencyHz
        )) {
            activeFrequencyHz =
                    profile.defaultFrequencyHz();
        }
    }

    @Override
    public SignalBand band() {
        if (isRadioProfile()
                && radio.mode()
                != RadioMode.LEGACY_DIRECT) {
            double[] frequencies =
                    radio.receiveFrequenciesHz();

            if (frequencies.length > 0) {
                return SignalBand.forFrequency(
                        frequencies[0]
                );
            }
        }

        return SignalBand.forFrequency(
                activeFrequencyHz
        );
    }

    @Override
    public double[] tunedFrequenciesHz() {
        if (isRadioProfile()
                && radio.mode()
                != RadioMode.LEGACY_DIRECT) {
            double[] frequencies =
                    radio.receiveFrequenciesHz();

            if (frequencies.length > 0) {
                return frequencies;
            }
        }

        return new double[]{
                activeFrequencyHz
        };
    }

    @Override
    public double tuningBandwidthHz() {
        if (isRadioProfile()
                && radio.mode()
                != RadioMode.LEGACY_DIRECT
                && radio.receiveBandwidthHz() > 0.0) {
            return radio.receiveBandwidthHz();
        }

        return networkProfile().bandwidthHz();
    }

    @Override
    public double antennaGain() {
        return networkProfile().antennaGain();
    }

    @Override
    public double sensitivityWatts() {
        return networkProfile().sensitivityWatts();
    }

    @Override
    public double maximumReceptionRangeBlocks() {
        return networkProfile()
                .maximumRangeBlocks();
    }

    @Override
    public void onReceive(
            SignalPacket signal,
            double receivedPowerWatts
    ) {
        CompoundTag envelope =
                readSignalPayload(signal);

        if (envelope == null
                || !isCompatibleMedium(envelope)) {
            return;
        }

        PhyProfile receiveProfile =
                receivePhyProfile(envelope);

        long frameBits =
                Math.max(
                        1L,
                        (long) signal.payload().length
                                * 8L
                );

        double phyEvaluationPowerWatts =
                receivedPowerWatts;

        lastRfChannelAssessment =
                null;

        lastNetworkRealityAssessment =
                null;

        if (envelope.hasUUID(
                "rf_tx_id"
        )
                && level
                instanceof ServerLevel serverLevel) {

            lastRfChannelAssessment =
                    RfChannelEnvironment.assess(
                            serverLevel,
                            signalId,
                            envelope.getUUID(
                                    "rf_tx_id"
                            ),
                            positionWorld(),
                            receivedPowerWatts,
                            signal.frequencyHz(),
                            receiveProfile.bandwidthHz(),
                            receiveProfile.receiverNoiseFigureDb(),
                            worldRfAntennaState(),
                            rfVelocityMetersPerSecond()
                    );

            UUID desiredRfId =
                    envelope.getUUID(
                            "rf_tx_id"
                    );

            RfMicroTiming desiredTiming =
                    RfMicroTimingRegistry.get(
                            desiredRfId
                    );

            if (desiredTiming != null) {
                ActiveRfTransmission desiredMetadata =
                        RfTransmissionRegistry.get(
                                desiredRfId,
                                serverLevel.getGameTime()
                        );

                double propagationDistanceMeters =
                        desiredMetadata == null
                                ? 0.0
                                : desiredMetadata
                                .transmitterPosition()
                                .distanceTo(
                                        positionWorld()
                                );

                NetworkRealityEngine.Result realityResult =
                        NetworkRealityEngine.apply(
                                lastRfChannelAssessment,
                                desiredTiming,
                                RfMicroTimingRegistry.inDimension(
                                        serverLevel.dimension()
                                                .location()
                                                .toString(),
                                        desiredTiming.startMicros()
                                ),
                                propagationDistanceMeters
                        );

                lastRfChannelAssessment =
                        realityResult.channel();

                lastNetworkRealityAssessment =
                        realityResult.reality();

                if (!lastNetworkRealityAssessment
                        .receiverCapturedDesiredFrame()) {
                    traceWifiReceive(
                            envelope,
                            receivedPowerWatts,
                            WifiPacketOutcome.CAPTURE_DROP,
                            "Receiver capture model rejected desired frame"
                    );
                    return;
                }
            }

            phyEvaluationPowerWatts =
                    RfChannelEnvironment
                            .equivalentSignalPowerForSinr(
                                    lastRfChannelAssessment
                            );

            if (isWifiProfile()
                    && envelope.contains(
                    "wifi_mcs_index"
            )) {
                ensureWifiPhyConfigured();

                WifiMcs wifiMcs =
                        WifiMcsTable.byIndex(
                                envelope.getInt(
                                        "wifi_mcs_index"
                                )
                        );

                WifiPhyLinkAssessment wifiAssessment =
                        wifiPhy.assess(
                                wifiMcs,
                                lastRfChannelAssessment
                        );

                phyEvaluationPowerWatts =
                        WifiPhyLinkModel
                                .equivalentSignalPowerForNoiseFloor(
                                        wifiAssessment,
                                        lastRfChannelAssessment
                                                .noisePowerWatts()
                                );
            }
        }

        lastPhyResult =
                EngineeringPhyEngine
                        .evaluateReceivedFrame(
                                receiveProfile,
                                phyEvaluationPowerWatts,
                                frameBits
                        );

        if (!EngineeringPhyEngine
                .shouldDeliverFrame(lastPhyResult)) {
            if (envelope.contains(
                    "wifi_mac_frame"
            )) {
                traceWifiReceive(
                        envelope,
                        receivedPowerWatts,
                        WifiPacketOutcome.ANALYTICAL_PHY_DROP,
                        "EngineeringPhyEngine rejected frame"
                );
            }
            return;
        }

        if (isWifiProfile()
                && envelope.contains(
                "wifi_mac_frame"
        )) {
            WifiLivePhyMode incomingMode =
                    parseWifiLivePhyMode(
                            envelope
                    );

            if (wifiLivePhyMode
                    == WifiLivePhyMode.BIT_LEVEL_AUTO
                    && incomingMode
                    == WifiLivePhyMode.BIT_LEVEL_AUTO
                    && envelope.contains(
                    "wifi_mcs_index"
            )) {
                WifiMcs liveMcs =
                        WifiMcsTable.byIndex(
                                envelope.getInt(
                                        "wifi_mcs_index"
                                )
                        );

                WifiPhyGeneration liveGeneration =
                        WifiPhyGeneration.fromProtocol(
                                envelope.getString(
                                        "signality_protocol"
                                )
                        );

                lastWifiLivePhyDecision =
                        WifiLivePhyEngine.evaluate(
                                envelope.getByteArray(
                                        "wifi_mac_frame"
                                ),
                                liveGeneration,
                                liveMcs,
                                lastPhyResult.snrDb(),
                                WifiLivePhyMode.BIT_LEVEL_AUTO
                        );

                if (lastWifiLivePhyDecision.evaluated()
                        && !lastWifiLivePhyDecision.delivered()) {
                    traceWifiReceive(
                            envelope,
                            receivedPowerWatts,
                            WifiPacketOutcome.DETAILED_PHY_DROP,
                            lastWifiLivePhyDecision.detail()
                    );
                    return;
                }
            } else {
                lastWifiLivePhyDecision =
                        WifiLivePhyDecision.bypass(
                                wifiLivePhyMode,
                                lastPhyResult.snrDb(),
                                "Detailed live PHY not enabled at both endpoints"
                        );
            }
        }

        if (envelope.contains(
                "wifi_mac_frame"
        )) {
            traceWifiReceive(
                    envelope,
                    receivedPowerWatts,
                    WifiPacketOutcome.DELIVERED,
                    lastWifiLivePhyDecision == null
                            ? "Delivered by analytical PHY"
                            : lastWifiLivePhyDecision.detail()
            );
        }

        if (isWifiProfile()) {
            wifiMac.observeSnr(
                    networkProfile().protocol(),
                    lastPhyResult.snrDb()
            );
        }

        if (envelope.contains(
                "protocol_vm_frame"
        )) {
            processProtocolVmEnvelope(
                    envelope,
                    signal.frequencyHz(),
                    lastPhyResult
            );

            return;
        }

        if (envelope.contains(
                "wifi_mac_frame"
        )) {
            processWifiMacEnvelope(envelope);
            return;
        }

        if (envelope.contains(
                "cellular_control"
        )) {
            processCellularEnvelope(
                    envelope,
                    lastPhyResult
            );
            return;
        }

        if (envelope.contains(
                "radio_message"
        )) {
            processRadioEnvelope(
                    envelope,
                    signal.frequencyHz(),
                    lastPhyResult
            );
            return;
        }

        if (!envelope.contains(
                "osi_packet"
        )) {
            return;
        }

        processLayer2(
                OSINetworkPacket.deserializeNBT(
                        envelope.getCompound(
                                "osi_packet"
                        )
                )
        );
    }

    private PhyProfile receivePhyProfile(
            CompoundTag envelope
    ) {
        PhyProfile base =
                currentPhyProfile();

        if (!isWifiProfile()
                || !envelope.contains(
                "wifi_mcs_index"
        )) {
            return base;
        }

        WifiMcs mcs =
                WifiMcsTable.byIndex(
                        envelope.getInt(
                                "wifi_mcs_index"
                        )
                );

        return mcs.applyTo(base);
    }

    private void processProtocolVmEnvelope(
            CompoundTag envelope,
            double actualFrequencyHz,
            PhyResult phyResult
    ) {
        if (!protocolVm.bound()) {
            return;
        }

        ResourceLocation incomingProgram =
                ResourceLocation.tryParse(
                        envelope.getString(
                                "protocol_vm_program"
                        )
                );

        if (incomingProgram == null
                || !incomingProgram.equals(
                protocolVm.programId()
        )) {
            return;
        }

        protocolVm.receiveFrame(
                envelope.getByteArray(
                        "protocol_vm_frame"
                ),
                new ProtocolVmEnvironment(
                        Map.of(
                                "received_power_dbm",
                                phyResult.receivedPowerDbm(),
                                "snr_db",
                                phyResult.snrDb(),
                                "frequency_hz",
                                actualFrequencyHz,
                                "bandwidth_hz",
                                tuningBandwidthHz()
                        ),
                        Map.of(
                                "network_profile",
                                networkProfile()
                                        .id()
                                        .toString(),
                                "protocol_program",
                                incomingProgram.toString()
                        )
                )
        );

        setChanged();
    }

    private WifiLivePhyMode parseWifiLivePhyMode(
            CompoundTag envelope
    ) {
        if (!envelope.contains(
                "wifi_live_phy_mode"
        )) {
            return WifiLivePhyMode.ANALYTICAL;
        }

        try {
            return WifiLivePhyMode.valueOf(
                    envelope.getString(
                            "wifi_live_phy_mode"
                    )
            );
        } catch (Exception ignored) {
            return WifiLivePhyMode.ANALYTICAL;
        }
    }

    private void processWifiMacEnvelope(
            CompoundTag envelope
    ) {
        if (!isWifiProfile()
                || wifiMac.mode()
                == WifiMode.LEGACY_DIRECT) {
            return;
        }

        WifiMacFrame frame;

        try {
            frame = WifiMacFrame.decode(
                    envelope.getByteArray(
                            "wifi_mac_frame"
                    )
            );
        } catch (Exception ignored) {
            return;
        }

        RfMicroTiming incomingTiming =
                envelope.hasUUID(
                        "rf_tx_id"
                )
                        ? RfMicroTimingRegistry.get(
                        envelope.getUUID(
                                "rf_tx_id"
                        )
                )
                        : null;

        activeWifiResponseReferenceMicros =
                incomingTiming == null
                        ? -1L
                        : incomingTiming.endMicros();

        CompoundTag data;

        try {
            data =
                    wifiMac.receive(
                            macAddress,
                            frame,
                            networkProfile().id().toString(),
                            activeFrequencyHz,
                            wifiSender()
                    );
        } finally {
            activeWifiResponseReferenceMicros =
                    -1L;
        }

        if (data != null
                && data.contains(
                "osi_packet"
        )) {
            processLayer2(
                    OSINetworkPacket.deserializeNBT(
                            data.getCompound(
                                    "osi_packet"
                            )
                    )
            );
        }

        PduSession session =
                cellularRan.pduSession();

        if (session != null
                && session.active()
                && !session.ipAddress()
                .isBlank()) {
            ipAddress =
                    session.ipAddress();
        }

        setChanged();
    }

    private void processCellularEnvelope(
            CompoundTag envelope,
            PhyResult phyResult
    ) {
        if (!isCellularProfile()
                || cellularRan.mode()
                == CellularMode.LEGACY_DIRECT) {
            return;
        }

        CompoundTag data =
                cellularRan.receive(
                        signalId,
                        envelope.getCompound(
                                "cellular_control"
                        ),
                        phyResult.receivedPowerDbm(),
                        phyResult.snrDb(),
                        this::transmitCellularControl
                );

        if (data != null
                && data.contains(
                "osi_packet"
        )) {
            processLayer2(
                    OSINetworkPacket.deserializeNBT(
                            data.getCompound(
                                    "osi_packet"
                            )
                    )
            );
        }

        setChanged();
    }

    private void processRadioEnvelope(
            CompoundTag envelope,
            double actualFrequencyHz,
            PhyResult phyResult
    ) {
        if (!isRadioProfile()
                || radio.mode()
                == RadioMode.LEGACY_DIRECT) {
            return;
        }

        CompoundTag payload =
                radio.receive(
                        signalId,
                        envelope.getCompound(
                                "radio_message"
                        ),
                        actualFrequencyHz,
                        phyResult.receivedPowerDbm(),
                        phyResult.snrDb(),
                        this::transmitRadioMessage
                );

        if (payload != null
                && payload.contains(
                "osi_packet"
        )) {
            processLayer2(
                    OSINetworkPacket.deserializeNBT(
                            payload.getCompound(
                                    "osi_packet"
                            )
                    )
            );
        }

        setChanged();
    }

    private CompoundTag readSignalPayload(
            SignalPacket signal
    ) {
        try {
            return NbtIo.read(
                    new DataInputStream(
                            new ByteArrayInputStream(
                                    signal.payload()
                            )
                    )
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isCompatibleMedium(
            CompoundTag payload
    ) {
        if (!payload.contains(
                "signality_medium"
        )) {
            return true;
        }

        return networkProfile()
                .compatibilityGroup()
                .equals(
                        payload.getString(
                                "signality_medium"
                        )
                );
    }

    protected void processLayer2(
            OSINetworkPacket packet
    ) {
        boolean addressed =
                packet.targetMac.equals(
                        macAddress
                );

        boolean broadcast =
                packet.targetMac.equals(
                        "FF:FF:FF:FF:FF:FF"
                );

        if (addressed || broadcast) {
            processLayer3(packet);
        }
    }

    protected void processLayer3(
            OSINetworkPacket packet
    ) {
        boolean addressed =
                packet.targetIp.equals(
                        ipAddress
                );

        boolean broadcast =
                packet.targetIp.equals(
                        "255.255.255.255"
                );

        if (addressed || broadcast) {
            processLayer4(packet);
        }
    }

    protected void processLayer4(
            OSINetworkPacket packet
    ) {
        if (packet.targetPort == 80) {
            handleWebRequest(packet);
        } else if (packet.targetPort == 53) {
            handleDnsRequest(packet);
        } else if (packet.targetPort == 25) {
            handleMailRequest(packet);
        } else if (packet.targetPort == 68) {
            handleDhcpResponse(packet);
        } else if (packet.targetPort == 67) {
            handleDhcpRequest(packet);
        } else {
            handleIncomingData(packet);
        }
    }

    protected void handleWebRequest(
            OSINetworkPacket packet
    ) {
    }

    protected void handleDnsRequest(
            OSINetworkPacket packet
    ) {
    }

    protected void handleMailRequest(
            OSINetworkPacket packet
    ) {
    }

    protected void handleDhcpRequest(
            OSINetworkPacket packet
    ) {
    }

    protected void handleIncomingData(
            OSINetworkPacket packet
    ) {
    }

    protected void handleDhcpResponse(
            OSINetworkPacket packet
    ) {
        if (packet.applicationProtocol
                .equals("DHCP")
                && packet.payload
                .getString("type")
                .equals("ACK")) {

            ipAddress =
                    packet.payload
                            .getString(
                                    "assigned_ip"
                            );

            defaultGatewayMac =
                    packet.sourceMac;

            setChanged();
        }
    }

    public void requestDynamicIp() {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac = macAddress;
        packet.targetMac =
                "FF:FF:FF:FF:FF:FF";

        packet.sourceIp = "0.0.0.0";
        packet.targetIp =
                "255.255.255.255";

        packet.sourcePort = 68;
        packet.targetPort = 67;

        packet.applicationProtocol =
                "DHCP";

        packet.payload.putString(
                "type",
                "DISCOVER"
        );

        transmitPacket(packet);
    }

    protected void transmitPacket(
            OSINetworkPacket packet
    ) {
        if (protocolVm.bound()) {
            CompoundTag hostPayload =
                    new CompoundTag();

            hostPayload.put(
                    "osi_packet",
                    packet.serializeNBT()
            );

            protocolVm.transmitHostPayload(
                    serializeCompoundTag(
                            hostPayload
                    ),
                    new ProtocolVmEnvironment(
                            Map.of(
                                    "frequency_hz",
                                    activeFrequencyHz,
                                    "bandwidth_hz",
                                    tuningBandwidthHz(),
                                    "last_snr_db",
                                    lastPhyResult == null
                                            ? 0.0
                                            : lastPhyResult.snrDb()
                            ),
                            Map.of(
                                    "network_profile",
                                    networkProfile()
                                            .id()
                                            .toString(),
                                    "protocol_program",
                                    protocolVm.programId()
                                            .toString()
                            )
                    )
            );

            return;
        }

        if (isWifiProfile()
                && wifiMac.mode()
                != WifiMode.LEGACY_DIRECT) {

            CompoundTag body =
                    new CompoundTag();

            body.put(
                    "osi_packet",
                    packet.serializeNBT()
            );

            wifiMac.sendData(
                    macAddress,
                    packet.targetMac,
                    body,
                    classifyAccessCategory(packet),
                    wifiSender()
            );

            return;
        }

        if (isCellularProfile()
                && cellularRan.mode()
                == CellularMode.UE) {

            CompoundTag body =
                    new CompoundTag();

            body.put(
                    "osi_packet",
                    packet.serializeNBT()
            );

            cellularRan.sendDataFromUe(
                    signalId,
                    body,
                    estimatePacketBits(packet),
                    this::transmitCellularControl
            );

            return;
        }

        if (isRadioProfile()
                && radio.mode()
                == RadioMode.TRANSCEIVER) {
            CompoundTag body =
                    new CompoundTag();

            body.put(
                    "osi_packet",
                    packet.serializeNBT()
            );

            radio.sendPacket(
                    signalId,
                    null,
                    body,
                    this::transmitRadioMessage
            );

            return;
        }

        CompoundTag payload =
                baseEnvelope();

        payload.put(
                "osi_packet",
                packet.serializeNBT()
        );

        broadcastPayload(payload);
    }

    private long estimatePacketBits(
            OSINetworkPacket packet
    ) {
        try {
            ByteArrayOutputStream bytes =
                    new ByteArrayOutputStream();

            NbtIo.write(
                    packet.serializeNBT(),
                    new DataOutputStream(bytes)
            );

            return Math.max(
                    1L,
                    (long) bytes.size() * 8L
            );
        } catch (Exception ignored) {
            return 1L;
        }
    }

    private WifiAccessCategory classifyAccessCategory(
            OSINetworkPacket packet
    ) {
        int port =
                packet.targetPort;

        if (port == 5060
                || port == 5061
                || (port >= 16384
                && port <= 32767)) {
            return WifiAccessCategory.VOICE;
        }

        if (port == 554
                || port == 1935) {
            return WifiAccessCategory.VIDEO;
        }

        if (port == 20
                || port == 21
                || port == 25) {
            return WifiAccessCategory.BACKGROUND;
        }

        return WifiAccessCategory.BEST_EFFORT;
    }

    private void transmitProtocolVmFrame(
            byte[] frame
    ) {
        if (!protocolVm.bound()) {
            return;
        }

        CompoundTag payload =
                baseEnvelope();

        payload.putString(
                "protocol_vm_program",
                protocolVm.programId()
                        .toString()
        );

        payload.putByteArray(
                "protocol_vm_frame",
                frame
        );

        broadcastPayload(
                payload
        );
    }

    private void handleProtocolVmHostPayload(
            byte[] payload
    ) {
        CompoundTag hostPayload =
                deserializeCompoundTag(
                        payload
                );

        if (hostPayload == null
                || !hostPayload.contains(
                "osi_packet"
        )) {
            return;
        }

        processLayer2(
                OSINetworkPacket.deserializeNBT(
                        hostPayload.getCompound(
                                "osi_packet"
                        )
                )
        );
    }

    private byte[] serializeCompoundTag(
            CompoundTag tag
    ) {
        try {
            ByteArrayOutputStream bytes =
                    new ByteArrayOutputStream();

            NbtIo.write(
                    tag,
                    new DataOutputStream(
                            bytes
                    )
            );

            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to serialize protocol VM host payload",
                    exception
            );
        }
    }

    private CompoundTag deserializeCompoundTag(
            byte[] bytes
    ) {
        try {
            return NbtIo.read(
                    new DataInputStream(
                            new ByteArrayInputStream(
                                    bytes
                            )
                    )
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private WifiMacController.Sender wifiSender() {
        return new WifiMacController.Sender() {
            @Override
            public void send(
                    WifiMacFrame frame
            ) {
                transmitWifiFrame(
                        frame
                );
            }

            @Override
            public boolean mediumBusy() {
                return isRfMediumBusy(
                        RfChannelSettings
                                .WIFI_ENERGY_DETECT_THRESHOLD_DBM
                );
            }
        };
    }

    private void transmitWifiFrame(
            WifiMacFrame frame
    ) {
        CompoundTag payload =
                baseEnvelope();

        WifiMcs mcs =
                selectWifiMcsForFrame(
                        frame
                );

        payload.putInt(
                "wifi_mcs_index",
                mcs.index()
        );

        payload.putString(
                "wifi_live_phy_mode",
                wifiLivePhyMode.name()
        );

        ensureWifiPhyConfigured();

        WifiPhyConfiguration phyConfiguration =
                wifiPhy.configuration();

        if (phyConfiguration != null) {
            payload.putString(
                    "wifi_phy_generation",
                    phyConfiguration.generation()
                            .name()
            );

            payload.putInt(
                    "wifi_channel_width_mhz",
                    phyConfiguration.channelWidth()
                            .mhz()
            );

            payload.putDouble(
                    "wifi_guard_interval_us",
                    phyConfiguration.guardInterval()
                            .microseconds()
            );

            payload.putInt(
                    "wifi_spatial_streams",
                    phyConfiguration.spatialStreams()
            );
        }

        byte[] wifiWire =
                frame.encode();

        payload.putByteArray(
                "wifi_mac_frame",
                wifiWire
        );

        double snrForAirtime =
                wifiMac.lastObservedSnrDb();

        if (!Double.isFinite(
                snrForAirtime
        )) {
            snrForAirtime =
                    0.0;
        }

        WifiPpduEstimate ppdu =
                WifiPhyAirtimeModel.estimate(
                        wifiPhy.configuration(),
                        mcs,
                        Math.max(
                                1L,
                                (long) wifiWire.length
                                        * 8L
                        ),
                        wifiPhy.puncturing(),
                        snrForAirtime
                );

        payload.putLong(
                "rf_airtime_us",
                Math.max(
                        1L,
                        (long) Math.ceil(
                                ppdu.totalTimeUs()
                        )
                )
        );

        if (activeWifiResponseReferenceMicros >= 0L
                && (
                frame.isAck()
                        || frame.isCts()
                        || frame.type()
                        == com.k1ngtle.vsia.signality.engineering.wifi.WifiFrameType.DATA
        )) {
            payload.putLong(
                    "rf_absolute_start_us",
                    activeWifiResponseReferenceMicros
                            + Math.max(
                            0,
                            wifiMac.timingProfile()
                                    .sifsUs()
                    )
            );
        } else if (frame.isAck()
                || frame.isCts()) {
            payload.putLong(
                    "rf_start_delay_us",
                    Math.max(
                            0,
                            wifiMac.timingProfile()
                                    .sifsUs()
                    )
            );
        }

        traceWifiTransmit(
                frame,
                payload,
                mcs
        );

        broadcastPayload(payload);
    }

    private WifiMcs selectWifiMcsForFrame(
            WifiMacFrame frame
    ) {
        if (requiresRobustBasicRate(
                frame
        )) {
            return WifiMcsTable.byIndex(
                    0
            );
        }

        return WifiMcsTable.select(
                networkProfile().protocol(),
                wifiMac.lastObservedSnrDb()
        );
    }

    private boolean requiresRobustBasicRate(
            WifiMacFrame frame
    ) {
        if (frame == null) {
            return true;
        }

        if (frame.type()
                == com.k1ngtle.vsia.signality.engineering.wifi.WifiFrameType.MANAGEMENT
                || frame.type()
                == com.k1ngtle.vsia.signality.engineering.wifi.WifiFrameType.CONTROL) {
            return true;
        }

        if (frame.type()
                != com.k1ngtle.vsia.signality.engineering.wifi.WifiFrameType.DATA) {
            return true;
        }

        CompoundTag body =
                deserializeCompoundTag(
                        frame.payload()
                );

        if (body == null) {
            return false;
        }

        return "EAPOL_KEY".equals(
                body.getString(
                        "wifi_control"
                )
        );
    }

    private void traceWifiTransmit(
            WifiMacFrame frame,
            CompoundTag envelope,
            WifiMcs mcs
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long timestampMicros =
                envelope.contains(
                        "rf_absolute_start_us"
                )
                        ? envelope.getLong(
                        "rf_absolute_start_us"
                )
                        : NetworkTimebase.nowMicros(
                        serverLevel
                )
                        + (
                        envelope.contains(
                                "rf_start_delay_us"
                        )
                                ? envelope.getLong(
                                "rf_start_delay_us"
                        )
                                : 0L
                );

        WifiPhyConfiguration configuration =
                wifiPhy.configuration();

        wifiPacketTrace.append(
                timestampMicros,
                WifiPacketDirection.TX,
                frame.type().name(),
                frame.subtype(),
                formatEngineeringMac(
                        frame.address2()
                ),
                formatEngineeringMac(
                        frame.address1()
                ),
                mcs.index(),
                configuration == null
                        ? ""
                        : configuration.generation()
                        .name(),
                frame.encode().length,
                envelope.contains(
                        "rf_airtime_us"
                )
                        ? envelope.getLong(
                        "rf_airtime_us"
                )
                        : 0L,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                frame.retry(),
                wifiLivePhyMode.name(),
                WifiPacketOutcome.QUEUED,
                "Queued into RF scheduler"
        );
    }

    private void traceWifiReceive(
            CompoundTag envelope,
            double receivedPowerWatts,
            WifiPacketOutcome outcome,
            String detail
    ) {
        if (!(level instanceof ServerLevel serverLevel)
                || !envelope.contains(
                "wifi_mac_frame"
        )) {
            return;
        }

        WifiMacFrame frame;

        try {
            frame =
                    WifiMacFrame.decode(
                            envelope.getByteArray(
                                    "wifi_mac_frame"
                            )
                    );
        } catch (Exception exception) {
            wifiPacketTrace.append(
                    NetworkTimebase.nowMicros(
                            serverLevel
                    ),
                    WifiPacketDirection.RX,
                    "UNKNOWN",
                    -1,
                    "",
                    "",
                    envelope.contains(
                            "wifi_mcs_index"
                    )
                            ? envelope.getInt(
                            "wifi_mcs_index"
                    )
                            : -1,
                    envelope.getString(
                            "wifi_phy_generation"
                    ),
                    envelope.getByteArray(
                            "wifi_mac_frame"
                    ).length,
                    envelope.contains(
                            "rf_airtime_us"
                    )
                            ? envelope.getLong(
                            "rf_airtime_us"
                    )
                            : 0L,
                    wattsToDbm(
                            receivedPowerWatts
                    ),
                    lastPhyResult == null
                            ? Double.NaN
                            : lastPhyResult.snrDb(),
                    lastNetworkRealityAssessment == null
                            ? Double.NaN
                            : lastNetworkRealityAssessment
                            .correctedSinrDb(),
                    false,
                    liveTracePath(),
                    WifiPacketOutcome.DECODE_DROP,
                    "802.11 decode failed: "
                            + exception.getClass()
                            .getSimpleName()
            );
            return;
        }

        long timestampMicros =
                NetworkTimebase.nowMicros(
                        serverLevel
                );

        if (envelope.hasUUID(
                "rf_tx_id"
        )) {
            RfMicroTiming timing =
                    RfMicroTimingRegistry.get(
                            envelope.getUUID(
                                    "rf_tx_id"
                            )
                    );

            if (timing != null) {
                timestampMicros =
                        timing.startMicros();
            }
        }

        wifiPacketTrace.append(
                timestampMicros,
                WifiPacketDirection.RX,
                frame.type().name(),
                frame.subtype(),
                formatEngineeringMac(
                        frame.address2()
                ),
                formatEngineeringMac(
                        frame.address1()
                ),
                envelope.contains(
                        "wifi_mcs_index"
                )
                        ? envelope.getInt(
                        "wifi_mcs_index"
                )
                        : -1,
                envelope.getString(
                        "wifi_phy_generation"
                ),
                envelope.getByteArray(
                        "wifi_mac_frame"
                ).length,
                envelope.contains(
                        "rf_airtime_us"
                )
                        ? envelope.getLong(
                        "rf_airtime_us"
                )
                        : 0L,
                outcome == WifiPacketOutcome.CAPTURE_DROP
                        || lastPhyResult == null
                        ? wattsToDbm(
                        receivedPowerWatts
                )
                        : lastPhyResult
                        .receivedPowerDbm(),
                outcome == WifiPacketOutcome.CAPTURE_DROP
                        || lastPhyResult == null
                        ? Double.NaN
                        : lastPhyResult.snrDb(),
                lastNetworkRealityAssessment == null
                        ? (
                        lastPhyResult == null
                                ? Double.NaN
                                : lastPhyResult.snrDb()
                )
                        : lastNetworkRealityAssessment
                        .correctedSinrDb(),
                frame.retry(),
                liveTracePath(),
                outcome,
                detail
        );
    }

    private String liveTracePath() {
        return lastWifiLivePhyDecision == null
                ? wifiLivePhyMode.name()
                : lastWifiLivePhyDecision.path()
                .name();
    }

    private double wattsToDbm(
            double watts
    ) {
        if (!Double.isFinite(
                watts
        )
                || watts <= 0.0) {
            return Double.NaN;
        }

        return 10.0
                * Math.log10(
                watts * 1000.0
        );
    }

    private String formatEngineeringMac(
            byte[] value
    ) {
        if (value == null
                || value.length != 6) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0; i < value.length; i++) {
            if (i > 0) {
                builder.append(':');
            }

            builder.append(
                    String.format(
                            java.util.Locale.ROOT,
                            "%02X",
                            value[i] & 0xFF
                    )
            );
        }

        return builder.toString();
    }

    private void transmitCellularControl(
            CompoundTag cellularMessage
    ) {
        CompoundTag payload =
                baseEnvelope();

        payload.put(
                "cellular_control",
                cellularMessage
        );

        broadcastPayload(payload);
    }

    private void transmitRadioMessage(
            CompoundTag radioMessage,
            double frequencyHz
    ) {
        CompoundTag payload =
                baseEnvelope();

        payload.put(
                "radio_message",
                radioMessage
        );

        broadcastPayload(
                payload,
                frequencyHz
        );
    }

    private CompoundTag baseEnvelope() {
        NetworkProfile profile =
                networkProfile();

        CompoundTag payload =
                new CompoundTag();

        payload.putString(
                "signality_network_profile",
                profile.id().toString()
        );

        payload.putString(
                "signality_medium",
                profile.compatibilityGroup()
        );

        payload.putString(
                "signality_protocol",
                profile.protocol()
        );

        payload.putString(
                "signality_security",
                profile.security()
        );

        return payload;
    }

    private void broadcastPayload(
            CompoundTag rawPayload
    ) {
        broadcastPayload(
                rawPayload,
                activeFrequencyHz
        );
    }

    private void broadcastPayload(
            CompoundTag rawPayload,
            double transmitFrequencyHz
    ) {
        UUID rfTransmissionId =
                UUID.randomUUID();

        rawPayload.putUUID(
                "rf_tx_id",
                rfTransmissionId
        );

        byte[] payloadBytes;

        try {
            ByteArrayOutputStream bytes =
                    new ByteArrayOutputStream();

            NbtIo.write(
                    rawPayload,
                    new DataOutputStream(bytes)
            );

            payloadBytes =
                    bytes.toByteArray();
        } catch (Exception exception) {
            exception.printStackTrace();
            return;
        }

        NetworkProfile profile =
                networkProfile();

        SignalPacket outgoing =
                new SignalPacket(
                        signalId,
                        positionWorld(),
                        transmitFrequencyHz,
                        profile.transmitPowerWatts(),
                        profile.antennaGain(),
                        payloadBytes,
                        System.nanoTime(),
                        64,
                        null
                );

        if (level
                instanceof ServerLevel serverLevel) {

            long currentTick =
                    serverLevel.getGameTime();

            long payloadBits =
                    Math.max(
                            1L,
                            (long) payloadBytes.length
                                    * 8L
                    );

            long airtimeMicros =
                    rawPayload.contains(
                            "rf_airtime_us"
                    )
                            ? Math.max(
                            1L,
                            rawPayload.getLong(
                                    "rf_airtime_us"
                            )
                    )
                            : GeneralRfAirtimeModel
                            .estimateMicros(
                                    payloadBits,
                                    profile.bandwidthHz()
                            );

            long startDelayMicros =
                    rawPayload.contains(
                            "rf_start_delay_us"
                    )
                            ? Math.max(
                            0L,
                            rawPayload.getLong(
                                    "rf_start_delay_us"
                            )
                    )
                            : 0L;

            long microStart =
                    rawPayload.contains(
                            "rf_absolute_start_us"
                    )
                            ? Math.max(
                            0L,
                            rawPayload.getLong(
                                    "rf_absolute_start_us"
                            )
                    )
                            : NetworkTimebase.nowMicros(
                            serverLevel
                    )
                            + startDelayMicros;

            long microEnd =
                    microStart
                            + airtimeMicros
                            - 1L;

            RfMicroTimingRegistry.register(
                    new RfMicroTiming(
                            rfTransmissionId,
                            serverLevel.dimension()
                                    .location()
                                    .toString(),
                            transmitFrequencyHz,
                            profile.bandwidthHz(),
                            microStart,
                            microEnd
                    )
            );

            long airtimeTicks =
                    Math.max(
                            1L,
                            (long) Math.ceil(
                                    airtimeMicros
                                            / (double) NetworkTimebase
                                            .MICROS_PER_SERVER_TICK
                            )
                    );

            long startTick =
                    currentTick
                            + Math.max(
                            1L,
                            RfChannelSettings
                                    .MIN_EVENT_LATENCY_TICKS
                    );

            ActiveRfTransmission metadata =
                    new ActiveRfTransmission(
                            rfTransmissionId,
                            signalId,
                            serverLevel.dimension()
                                    .location()
                                    .toString(),
                            positionWorld(),
                            transmitFrequencyHz,
                            profile.bandwidthHz(),
                            profile.transmitPowerWatts(),
                            profile.antennaGain(),
                            worldRfAntennaState(),
                            RfKinematicTracker
                                    .updateAndGetVelocityMetersPerSecond(
                                            signalId,
                                            positionWorld(),
                                            currentTick
                                    ),
                            startTick,
                            startTick
                                    + airtimeTicks
                                    - 1L,
                            payloadBits
                    );

            RfDiscreteEventScheduler.schedule(
                    new ScheduledRfTransmission(
                            metadata,
                            outgoing,
                            serverLevel
                    )
            );
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag
    ) {
        super.saveAdditional(tag);

        tag.putUUID(
                "SignalId",
                signalId
        );

        tag.putString(
                "MacAddress",
                macAddress
        );

        tag.putString(
                "IpAddress",
                ipAddress
        );

        tag.putString(
                "DefaultGatewayMac",
                defaultGatewayMac
        );

        tag.putString(
                "NetworkProfile",
                networkProfileId.toString()
        );

        tag.putDouble(
                "ActiveFrequencyHz",
                activeFrequencyHz
        );

        CompoundTag antennaTag =
                new CompoundTag();

        antennaTag.putString(
                "Pattern",
                rfAntennaState.pattern().name()
        );

        antennaTag.putString(
                "Polarization",
                rfAntennaState.polarization().name()
        );

        antennaTag.putDouble(
                "BoresightX",
                rfAntennaState.boresight().x
        );

        antennaTag.putDouble(
                "BoresightY",
                rfAntennaState.boresight().y
        );

        antennaTag.putDouble(
                "BoresightZ",
                rfAntennaState.boresight().z
        );

        antennaTag.putDouble(
                "PeakGainDbi",
                rfAntennaState.peakGainDbi()
        );

        antennaTag.putDouble(
                "HorizontalBeamwidthDeg",
                rfAntennaState.horizontalBeamwidthDeg()
        );

        antennaTag.putDouble(
                "VerticalBeamwidthDeg",
                rfAntennaState.verticalBeamwidthDeg()
        );

        antennaTag.putDouble(
                "FrontToBackRatioDb",
                rfAntennaState.frontToBackRatioDb()
        );

        tag.put(
                "RfAntenna",
                antennaTag
        );

        tag.putString(
                "WifiLivePhyMode",
                wifiLivePhyMode.name()
        );

        tag.put(
                "WifiMac",
                wifiMac.save()
        );

        tag.put(
                "CellularRan",
                cellularRan.save()
        );

        tag.put(
                "Radio",
                radio.save()
        );

        tag.put(
                "ProtocolVm",
                protocolVm.save()
        );
    }

    @Override
    public void load(
            CompoundTag tag
    ) {
        super.load(tag);

        if (tag.hasUUID("SignalId")) {
            signalId =
                    tag.getUUID("SignalId");
        }

        if (tag.contains("MacAddress")) {
            macAddress =
                    tag.getString("MacAddress");
        }

        if (tag.contains("IpAddress")) {
            ipAddress =
                    tag.getString("IpAddress");
        }

        if (tag.contains("DefaultGatewayMac")) {
            defaultGatewayMac =
                    tag.getString(
                            "DefaultGatewayMac"
                    );
        }

        if (tag.contains("NetworkProfile")) {
            ResourceLocation parsed =
                    ResourceLocation.tryParse(
                            tag.getString(
                                    "NetworkProfile"
                            )
                    );

            if (parsed != null) {
                networkProfileId =
                        parsed;
            }
        }

        if (tag.contains(
                "ActiveFrequencyHz"
        )) {
            activeFrequencyHz =
                    tag.getDouble(
                            "ActiveFrequencyHz"
                    );
        }

        if (tag.contains("RfAntenna")) {
            CompoundTag antennaTag =
                    tag.getCompound(
                            "RfAntenna"
                    );

            try {
                rfAntennaState =
                        new RfAntennaState(
                                RfAntennaPattern.valueOf(
                                        antennaTag.getString(
                                                "Pattern"
                                        )
                                ),
                                RfPolarization.valueOf(
                                        antennaTag.getString(
                                                "Polarization"
                                        )
                                ),
                                new Vec3(
                                        antennaTag.getDouble(
                                                "BoresightX"
                                        ),
                                        antennaTag.getDouble(
                                                "BoresightY"
                                        ),
                                        antennaTag.getDouble(
                                                "BoresightZ"
                                        )
                                ),
                                antennaTag.getDouble(
                                        "PeakGainDbi"
                                ),
                                antennaTag.getDouble(
                                        "HorizontalBeamwidthDeg"
                                ),
                                antennaTag.getDouble(
                                        "VerticalBeamwidthDeg"
                                ),
                                antennaTag.getDouble(
                                        "FrontToBackRatioDb"
                                )
                        );
            } catch (Exception ignored) {
                rfAntennaState =
                        RfAntennaState.isotropic();
            }
        }

        if (tag.contains(
                "WifiLivePhyMode"
        )) {
            try {
                wifiLivePhyMode =
                        WifiLivePhyMode.valueOf(
                                tag.getString(
                                        "WifiLivePhyMode"
                                )
                        );
            } catch (Exception ignored) {
                wifiLivePhyMode =
                        WifiLivePhyMode.ANALYTICAL;
            }
        }

        if (tag.contains("WifiMac")) {
            wifiMac.load(
                    tag.getCompound(
                            "WifiMac"
                    )
            );
        }

        if (tag.contains("CellularRan")) {
            cellularRan.load(
                    tag.getCompound(
                            "CellularRan"
                    )
            );
        }

        if (tag.contains("Radio")) {
            radio.load(
                    tag.getCompound(
                            "Radio"
                    )
            );
        }

        if (tag.contains("ProtocolVm")) {
            protocolVm.load(
                    tag.getCompound(
                            "ProtocolVm"
                    )
            );
        }

        normalizeNetworkProfile();
        lastPhyResult = null;
        lastRfChannelAssessment = null;
        lastNetworkRealityAssessment = null;
        lastWifiLivePhyDecision =
                WifiLivePhyDecision.bypass(
                        wifiLivePhyMode,
                        Double.NaN,
                        "Loaded"
                );
    }
}
