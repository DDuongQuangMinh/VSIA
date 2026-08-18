package com.k1ngtle.vsia.signality.internet;

import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.api.signal.ISignalTransmitter;
import com.k1ngtle.vsia.signality.api.signal.SignalBand;
import com.k1ngtle.vsia.signality.api.signal.SignalPacket;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.engineering.EngineeringPhyEngine;
import com.k1ngtle.vsia.signality.engineering.cellular.CellRecord;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularMode;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularRanController;
import com.k1ngtle.vsia.signality.engineering.cellular.ResourceBlockAllocation;
import com.k1ngtle.vsia.signality.engineering.cellular.UeRanState;
import com.k1ngtle.vsia.signality.engineering.phy.PhyProfile;
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

    private final WifiMacController wifiMac =
            new WifiMacController();

    private final CellularRanController cellularRan =
            new CellularRanController();

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
        }
    }

    @Override
    public void setRemoved() {
        SignalBus.unregisterReceiver(signalId);
        SignalBus.unregisterTransmitter(signalId);
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

    @Override
    public Vec3 positionWorld() {
        return Vec3.atCenterOf(worldPosition)
                .add(0.0, 0.5, 0.0);
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

    public PhyProfile currentPhyProfile() {
        NetworkProfile profile = networkProfile();

        return profile.phy().toRuntimeProfile(
                activeFrequencyHz,
                profile.bandwidthHz(),
                profile.transmitPowerWatts(),
                profile.antennaGain()
        );
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

    public Collection<WifiNetworkRecord> discoveredWifiNetworks() {
        return wifiMac.discoveredNetworks();
    }

    public CellularMode cellularMode() {
        return cellularRan.mode();
    }

    public UeRanState cellularUeState() {
        return cellularRan.ueState();
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
                    this::transmitWifiFrame
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
                        this::transmitWifiFrame
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
                this::transmitWifiFrame
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
        setChanged();
        return true;
    }

    private boolean isWifiProfile() {
        return networkProfile().kind()
                == NetworkKind.WIFI;
    }

    private boolean isCellularProfile() {
        return networkProfile().kind()
                == NetworkKind.CELLULAR;
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
        return SignalBand.forFrequency(
                activeFrequencyHz
        );
    }

    @Override
    public double[] tunedFrequenciesHz() {
        return new double[]{
                activeFrequencyHz
        };
    }

    @Override
    public double tuningBandwidthHz() {
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

        lastPhyResult =
                EngineeringPhyEngine
                        .evaluateReceivedFrame(
                                receiveProfile,
                                receivedPowerWatts,
                                frameBits
                        );

        if (!EngineeringPhyEngine
                .shouldDeliverFrame(lastPhyResult)) {
            return;
        }

        if (isWifiProfile()) {
            wifiMac.observeSnr(
                    networkProfile().protocol(),
                    lastPhyResult.snrDb()
            );
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

        CompoundTag data =
                wifiMac.receive(
                        macAddress,
                        frame,
                        networkProfile().id().toString(),
                        activeFrequencyHz,
                        this::transmitWifiFrame
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
                    this::transmitWifiFrame
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

    private void transmitWifiFrame(
            WifiMacFrame frame
    ) {
        CompoundTag payload =
                baseEnvelope();

        WifiMcs mcs =
                WifiMcsTable.select(
                        networkProfile().protocol(),
                        wifiMac.lastObservedSnrDb()
                );

        payload.putInt(
                "wifi_mcs_index",
                mcs.index()
        );

        payload.putByteArray(
                "wifi_mac_frame",
                frame.encode()
        );

        broadcastPayload(payload);
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
                        activeFrequencyHz,
                        profile.transmitPowerWatts(),
                        profile.antennaGain(),
                        payloadBytes,
                        System.nanoTime(),
                        64,
                        null
                );

        if (level
                instanceof ServerLevel serverLevel) {
            SignalBus.broadcast(
                    outgoing,
                    serverLevel
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

        tag.put(
                "WifiMac",
                wifiMac.save()
        );

        tag.put(
                "CellularRan",
                cellularRan.save()
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

        normalizeNetworkProfile();
        lastPhyResult = null;
    }
}