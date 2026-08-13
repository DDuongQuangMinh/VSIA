package com.k1ngtle.vsia.signality.internet.servers;

import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the Data Center Server implementations for L7 Protocols.
 */
public class InternetServers {

    // ==========================================
    // L7: WEB SERVER (Hosts HTML/CSS/JS)
    // ==========================================
    public static class WebServerBlockEntity extends NetworkDeviceBlockEntity {

        // The raw code hosted on this server
        public String indexHtml = "<html><body><h1>Hello Minecraft Internet!</h1></body></html>";
        public String stylesCss = "body { background-color: #222; color: #00FF00; }";
        public String scriptJs = "console.log('Server JS loaded!');";

        public WebServerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
            this.ipAddress = "192.168.1.80"; // Example Static IP
        }

        @Override
        protected void handleWebRequest(OSINetworkPacket request) {
            if (request.applicationProtocol.equals("HTTP") && !request.isResponse) {
                // Construct HTTP Response
                OSINetworkPacket response = new OSINetworkPacket();
                response.sourceMac = this.macAddress;
                response.targetMac = request.sourceMac;
                response.sourceIp = this.ipAddress;
                response.targetIp = request.sourceIp;
                response.sourcePort = 80;
                response.targetPort = request.sourcePort;
                response.applicationProtocol = "HTTP";
                response.isResponse = true;
                response.sessionId = request.sessionId;

                // Bundle the code
                CompoundTag webData = new CompoundTag();
                webData.putString("html", indexHtml);
                webData.putString("css", stylesCss);
                webData.putString("js", scriptJs);
                response.payload = webData;

                transmitPacket(response);
            }
        }
    }

    // ==========================================
    // L7: DNS SERVER (Domain to IP Resolution)
    // ==========================================
    public static class DnsServerBlockEntity extends NetworkDeviceBlockEntity {

        private final Map<String, String> dnsRecords = new HashMap<>();

        public DnsServerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
            this.ipAddress = "8.8.8.8"; // Standard DNS IP
            dnsRecords.put("www.vsia-net.com", "192.168.1.80");
        }

        @Override
        protected void handleDnsRequest(OSINetworkPacket request) {
            String domainRequested = request.payload.getString("domain");
            String resolvedIp = dnsRecords.getOrDefault(domainRequested, "404.0.0.0");

            OSINetworkPacket response = new OSINetworkPacket();
            response.sourceMac = this.macAddress;
            response.targetMac = request.sourceMac;
            response.sourceIp = this.ipAddress;
            response.targetIp = request.sourceIp;
            response.sourcePort = 53;
            response.targetPort = request.sourcePort;
            response.applicationProtocol = "DNS";
            response.isResponse = true;
            response.sessionId = request.sessionId;

            response.payload.putString("resolved_ip", resolvedIp);
            transmitPacket(response);
        }
    }

    // ==========================================
    // L7: DHCP SERVER (Dynamic IP Assignment)
    // ==========================================
    public static class DhcpServerBlockEntity extends NetworkDeviceBlockEntity {

        // Maps a MAC Address to a Leased IP
        private final Map<String, String> leasedIps = new HashMap<>();
        private int nextIpSuffix = 100; // IP pool starts at 192.168.1.100

        public DhcpServerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
            this.ipAddress = "192.168.1.1"; // The DHCP Server / Router Gateway IP
        }

        @Override
        protected void handleDhcpRequest(OSINetworkPacket request) {
            if (request.applicationProtocol.equals("DHCP") && request.payload.getString("type").equals("DISCOVER")) {

                String assignedIp;
                // Check if device already has a lease, otherwise generate a new IP from the pool
                if (leasedIps.containsKey(request.sourceMac)) {
                    assignedIp = leasedIps.get(request.sourceMac);
                } else {
                    assignedIp = "192.168.1." + nextIpSuffix++;
                    leasedIps.put(request.sourceMac, assignedIp);
                }

                // Construct DHCP ACK (Acknowledge/Offer) Response
                OSINetworkPacket response = new OSINetworkPacket();
                response.sourceMac = this.macAddress;
                response.targetMac = request.sourceMac;
                response.sourceIp = this.ipAddress;
                response.targetIp = assignedIp;
                response.sourcePort = 67;
                response.targetPort = 68; // Send back to DHCP Client Port
                response.applicationProtocol = "DHCP";
                response.isResponse = true;

                // Attach the assigned network configurations
                response.payload.putString("type", "ACK");
                response.payload.putString("assigned_ip", assignedIp);
                response.payload.putString("subnet_mask", "255.255.255.0");
                response.payload.putString("router_ip", this.ipAddress);

                transmitPacket(response);
            }
        }
    }
}