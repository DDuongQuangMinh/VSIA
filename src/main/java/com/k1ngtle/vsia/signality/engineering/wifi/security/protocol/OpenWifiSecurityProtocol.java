package com.k1ngtle.vsia.signality.engineering.wifi.security.protocol;

import java.util.Arrays;

public final class OpenWifiSecurityProtocol
        implements WifiSecurityProtocol {
    private static final WifiSecurityCapabilities CAPABILITIES =
            new WifiSecurityCapabilities(
                    false,
                    false,
                    false,
                    false,
                    false
            );

    @Override
    public String id() {
        return "signality:open";
    }

    @Override
    public String displayName() {
        return "Open";
    }

    @Override
    public WifiSecurityCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public byte[] derivePmk(
            String passphrase,
            String ssid
    ) {
        return new byte[0];
    }

    @Override
    public byte[] derivePtk(
            byte[] pmk,
            String apMac,
            String stationMac,
            byte[] anonce,
            byte[] snonce
    ) {
        return new byte[0];
    }

    @Override
    public byte[] mic(
            byte[] ptk,
            byte[] message
    ) {
        return new byte[0];
    }

    @Override
    public boolean verifyMic(
            byte[] ptk,
            byte[] message,
            byte[] expectedMic
    ) {
        return expectedMic == null
                || expectedMic.length == 0;
    }

    @Override
    public byte[] protect(
            byte[] ptk,
            byte[] plaintext
    ) {
        return plaintext == null
                ? new byte[0]
                : Arrays.copyOf(
                plaintext,
                plaintext.length
        );
    }

    @Override
    public byte[] unprotect(
            byte[] ptk,
            byte[] protectedBytes
    ) {
        return protectedBytes == null
                ? new byte[0]
                : Arrays.copyOf(
                protectedBytes,
                protectedBytes.length
        );
    }
}
