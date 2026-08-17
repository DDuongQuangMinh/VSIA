package com.k1ngtle.vsia.signality.internet;

import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.api.signal.ISignalTransmitter;
import com.k1ngtle.vsia.signality.api.signal.SignalBand;
import com.k1ngtle.vsia.signality.api.signal.SignalPacket;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
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
            NetworkProfileRegistry.defaultProfile().defaultFrequencyHz();

    public NetworkDeviceBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);

        this.macAddress = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (this.level != null && !this.level.isClientSide) {
            normalizeNetworkProfile();
            SignalBus.registerReceiver(this);
            SignalBus.registerTransmitter(this);
        }
    }

    @Override
    public void setRemoved() {
        SignalBus.unregisterReceiver(this.signalId);
        SignalBus.unregisterTransmitter(this.signalId);
        super.setRemoved();
    }

    @Override
    public UUID id() {
        return this.signalId;
    }

    @Override
    public ServerLevel level() {
        return (ServerLevel) this.level;
    }

    @Override
    public Vec3 positionWorld() {
        return Vec3.atCenterOf(this.worldPosition).add(0.0, 0.5, 0.0);
    }

    public ResourceLocation networkProfileId() {
        return this.networkProfileId;
    }

    public NetworkProfile networkProfile() {
        return NetworkProfileRegistry.getOrDefault(this.networkProfileId);
    }

    public double activeFrequencyHz() {
        return this.activeFrequencyHz;
    }

    public boolean setNetworkProfile(ResourceLocation profileId) {
        NetworkProfile profile = NetworkProfileRegistry.get(profileId).orElse(null);

        if (profile == null) {
            return false;
        }

        this.networkProfileId = profile.id();
        this.activeFrequencyHz = profile.defaultFrequencyHz();
        setChanged();
        return true;
    }

    public boolean setActiveFrequencyHz(double frequencyHz) {
        NetworkProfile profile = networkProfile();

        if (!profile.supportsFrequency(frequencyHz)) {
            return false;
        }

        this.activeFrequencyHz = frequencyHz;
        setChanged();
        return true;
    }

    private void normalizeNetworkProfile() {
        NetworkProfile profile = NetworkProfileRegistry.get(this.networkProfileId)
                .orElseGet(NetworkProfileRegistry::defaultProfile);

        this.networkProfileId = profile.id();

        if (!profile.supportsFrequency(this.activeFrequencyHz)) {
            this.activeFrequencyHz = profile.defaultFrequencyHz();
        }
    }

    @Override
    public SignalBand band() {
        return SignalBand.forFrequency(this.activeFrequencyHz);
    }

    @Override
    public double[] tunedFrequenciesHz() {
        return new double[]{this.activeFrequencyHz};
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
        return networkProfile().maximumRangeBlocks();
    }

    @Override
    public void onReceive(SignalPacket signal, double receivedPowerWatts) {
        CompoundTag payloadData = null;

        try {
            ByteArrayInputStream byteInput =
                    new ByteArrayInputStream(signal.payload());

            DataInputStream dataInput =
                    new DataInputStream(byteInput);

            payloadData = NbtIo.read(dataInput);
        } catch (Exception ignored) {
        }

        if (payloadData == null || !payloadData.contains("osi_packet")) {
            return;
        }

        if (payloadData.contains("signality_medium")) {
            String incomingMedium = payloadData.getString("signality_medium");
            String requiredMedium = networkProfile().compatibilityGroup();

            if (!requiredMedium.equals(incomingMedium)) {
                return;
            }
        }

        OSINetworkPacket osiPacket =
                OSINetworkPacket.deserializeNBT(
                        payloadData.getCompound("osi_packet")
                );

        processLayer2(osiPacket);
    }

    protected void processLayer2(OSINetworkPacket packet) {
        boolean addressedToThisDevice =
                packet.targetMac.equals(this.macAddress);

        boolean broadcast =
                packet.targetMac.equals("FF:FF:FF:FF:FF:FF");

        if (addressedToThisDevice || broadcast) {
            processLayer3(packet);
        }
    }

    protected void processLayer3(OSINetworkPacket packet) {
        boolean addressedToThisDevice =
                packet.targetIp.equals(this.ipAddress);

        boolean broadcast =
                packet.targetIp.equals("255.255.255.255");

        if (addressedToThisDevice || broadcast) {
            processLayer4(packet);
        }
    }

    protected void processLayer4(OSINetworkPacket packet) {
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

    protected void handleWebRequest(OSINetworkPacket packet) {
    }

    protected void handleDnsRequest(OSINetworkPacket packet) {
    }

    protected void handleMailRequest(OSINetworkPacket packet) {
    }

    protected void handleDhcpRequest(OSINetworkPacket packet) {
    }

    protected void handleIncomingData(OSINetworkPacket packet) {
    }

    protected void handleDhcpResponse(OSINetworkPacket packet) {
        boolean isDhcp =
                packet.applicationProtocol.equals("DHCP");

        boolean isAcknowledgement =
                packet.payload.getString("type").equals("ACK");

        if (isDhcp && isAcknowledgement) {
            this.ipAddress =
                    packet.payload.getString("assigned_ip");

            this.defaultGatewayMac =
                    packet.sourceMac;

            setChanged();
        }
    }

    public void requestDynamicIp() {
        OSINetworkPacket dhcpRequest =
                new OSINetworkPacket();

        dhcpRequest.sourceMac =
                this.macAddress;

        dhcpRequest.targetMac =
                "FF:FF:FF:FF:FF:FF";

        dhcpRequest.sourceIp =
                "0.0.0.0";

        dhcpRequest.targetIp =
                "255.255.255.255";

        dhcpRequest.sourcePort =
                68;

        dhcpRequest.targetPort =
                67;

        dhcpRequest.applicationProtocol =
                "DHCP";

        dhcpRequest.payload.putString(
                "type",
                "DISCOVER"
        );

        transmitPacket(dhcpRequest);
    }

    protected void transmitPacket(OSINetworkPacket osiPacket) {
        NetworkProfile profile = networkProfile();

        CompoundTag rawPayload =
                new CompoundTag();

        rawPayload.put(
                "osi_packet",
                osiPacket.serializeNBT()
        );

        rawPayload.putString(
                "signality_network_profile",
                profile.id().toString()
        );

        rawPayload.putString(
                "signality_medium",
                profile.compatibilityGroup()
        );

        rawPayload.putString(
                "signality_protocol",
                profile.protocol()
        );

        rawPayload.putString(
                "signality_security",
                profile.security()
        );

        byte[] payloadBytes;

        try {
            ByteArrayOutputStream byteOutput =
                    new ByteArrayOutputStream();

            DataOutputStream dataOutput =
                    new DataOutputStream(byteOutput);

            NbtIo.write(rawPayload, dataOutput);

            payloadBytes =
                    byteOutput.toByteArray();
        } catch (Exception exception) {
            exception.printStackTrace();
            return;
        }

        SignalPacket outgoing =
                new SignalPacket(
                        this.signalId,
                        this.positionWorld(),
                        this.activeFrequencyHz,
                        profile.transmitPowerWatts(),
                        profile.antennaGain(),
                        payloadBytes,
                        System.nanoTime(),
                        64,
                        null
                );

        if (this.level instanceof ServerLevel serverLevel) {
            SignalBus.broadcast(outgoing, serverLevel);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putUUID(
                "SignalId",
                this.signalId
        );

        tag.putString(
                "MacAddress",
                this.macAddress
        );

        tag.putString(
                "IpAddress",
                this.ipAddress
        );

        tag.putString(
                "DefaultGatewayMac",
                this.defaultGatewayMac
        );

        tag.putString(
                "NetworkProfile",
                this.networkProfileId.toString()
        );

        tag.putDouble(
                "ActiveFrequencyHz",
                this.activeFrequencyHz
        );
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.hasUUID("SignalId")) {
            this.signalId =
                    tag.getUUID("SignalId");
        }

        if (tag.contains("MacAddress")) {
            this.macAddress =
                    tag.getString("MacAddress");
        }

        if (tag.contains("IpAddress")) {
            this.ipAddress =
                    tag.getString("IpAddress");
        }

        if (tag.contains("DefaultGatewayMac")) {
            this.defaultGatewayMac =
                    tag.getString("DefaultGatewayMac");
        }

        if (tag.contains("NetworkProfile")) {
            ResourceLocation parsed =
                    ResourceLocation.tryParse(
                            tag.getString("NetworkProfile")
                    );

            if (parsed != null) {
                this.networkProfileId = parsed;
            }
        }

        if (tag.contains("ActiveFrequencyHz")) {
            this.activeFrequencyHz =
                    tag.getDouble("ActiveFrequencyHz");
        }

        normalizeNetworkProfile();
    }
}
