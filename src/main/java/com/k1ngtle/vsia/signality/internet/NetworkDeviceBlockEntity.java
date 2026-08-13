package com.k1ngtle.vsia.signality.internet;

import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.api.signal.ISignalTransmitter;
import com.k1ngtle.vsia.signality.api.signal.SignalBand;
import com.k1ngtle.vsia.signality.api.signal.SignalPacket;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
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

/**
 * Base class for all internet-connected devices
 * (Computers, Routers, and Data Centers).
 *
 * Hooks into Signality Layer 1 (radio waves/Wi-Fi 7)
 * to process OSI packets.
 */
public abstract class NetworkDeviceBlockEntity
        extends BlockEntity
        implements ISignalReceiver, ISignalTransmitter {

    private UUID signalId = UUID.randomUUID();

    protected String macAddress;
    protected String ipAddress = "0.0.0.0";
    protected String defaultGatewayMac = "";

    public NetworkDeviceBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);

        // Generate a unique MAC address for new devices.
        this.macAddress = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);
    }

    // -------------------------------------------------------------------------
    // SIGNAL BUS REGISTRATION
    // -------------------------------------------------------------------------

    @Override
    public void onLoad() {
        super.onLoad();

        if (this.level != null && !this.level.isClientSide) {
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

    // -------------------------------------------------------------------------
    // ISignalReceiver / ISignalTransmitter
    // -------------------------------------------------------------------------

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

    @Override
    public SignalBand band() {
        /*
         * Wi-Fi 7 uses 6 GHz.
         *
         * SignalBand.SHF covers frequencies from 3 GHz through 30 GHz,
         * so SHF is the correct existing SignalBand value.
         */
        return SignalBand.SHF;
    }

    @Override
    public double[] tunedFrequenciesHz() {
        // 6 GHz Wi-Fi 7 frequency.
        return new double[]{6_000_000_000.0};
    }

    @Override
    public double tuningBandwidthHz() {
        // 320 MHz Wi-Fi 7 channel bandwidth.
        return 320_000_000.0;
    }

    @Override
    public double sensitivityWatts() {
        // Approximate Wi-Fi receiver sensitivity.
        return 1e-12;
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
            // The payload probably was not an OSI packet or valid NBT.
        }

        if (payloadData != null && payloadData.contains("osi_packet")) {
            OSINetworkPacket osiPacket =
                    OSINetworkPacket.deserializeNBT(
                            payloadData.getCompound("osi_packet")
                    );

            processLayer2(osiPacket);
        }
    }

    // -------------------------------------------------------------------------
    // LAYER 2: DATA LINK
    // -------------------------------------------------------------------------

    protected void processLayer2(OSINetworkPacket packet) {
        boolean addressedToThisDevice =
                packet.targetMac.equals(this.macAddress);

        boolean broadcast =
                packet.targetMac.equals("FF:FF:FF:FF:FF:FF");

        if (addressedToThisDevice || broadcast) {
            processLayer3(packet);
        }
    }

    // -------------------------------------------------------------------------
    // LAYER 3: NETWORK
    // -------------------------------------------------------------------------

    protected void processLayer3(OSINetworkPacket packet) {
        boolean addressedToThisDevice =
                packet.targetIp.equals(this.ipAddress);

        boolean broadcast =
                packet.targetIp.equals("255.255.255.255");

        if (addressedToThisDevice || broadcast) {
            processLayer4(packet);
        }
    }

    // -------------------------------------------------------------------------
    // LAYER 4: TRANSPORT
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // LAYER 7: APPLICATION
    // Child server classes can override these methods.
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // DHCP CLIENT
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // TRANSMISSION
    // -------------------------------------------------------------------------

    protected void transmitPacket(OSINetworkPacket osiPacket) {
        CompoundTag rawPayload =
                new CompoundTag();

        rawPayload.put(
                "osi_packet",
                osiPacket.serializeNBT()
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

        /*
         * Construct the physical-layer signal packet.
         *
         * 6 GHz is inside SignalBand.SHF.
         */
        SignalPacket outgoing =
                new SignalPacket(
                        this.signalId,
                        this.positionWorld(),
                        6_000_000_000.0,
                        1.0,
                        1.0,
                        payloadBytes,
                        System.nanoTime(),
                        64,
                        null
                );

        if (this.level instanceof ServerLevel serverLevel) {
            SignalBus.broadcast(outgoing, serverLevel);
        }
    }

    // -------------------------------------------------------------------------
    // SAVING AND LOADING
    // -------------------------------------------------------------------------

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
    }
}