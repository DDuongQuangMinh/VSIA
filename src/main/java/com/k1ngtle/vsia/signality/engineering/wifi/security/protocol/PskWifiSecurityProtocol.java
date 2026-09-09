package com.k1ngtle.vsia.signality.engineering.wifi.security.protocol;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityEngine;

public final class PskWifiSecurityProtocol
        implements WifiSecurityProtocol {
    private static final WifiSecurityCapabilities CAPABILITIES =
            new WifiSecurityCapabilities(
                    true,
                    true,
                    true,
                    true,
                    true
            );

    private final String id;
    private final String displayName;

    public PskWifiSecurityProtocol(
            String id,
            String displayName
    ) {
        if (id == null
                || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Security protocol id is required"
            );
        }

        this.id =
                id;

        this.displayName =
                displayName == null
                        || displayName.isBlank()
                        ? id
                        : displayName;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
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
        return WifiSecurityEngine.derivePmk(
                passphrase,
                ssid
        );
    }

    @Override
    public byte[] derivePtk(
            byte[] pmk,
            String apMac,
            String stationMac,
            byte[] anonce,
            byte[] snonce
    ) {
        return WifiSecurityEngine.derivePtk(
                pmk,
                apMac,
                stationMac,
                anonce,
                snonce
        );
    }

    @Override
    public byte[] mic(
            byte[] ptk,
            byte[] message
    ) {
        return WifiSecurityEngine.mic(
                ptk,
                message
        );
    }

    @Override
    public boolean verifyMic(
            byte[] ptk,
            byte[] message,
            byte[] expectedMic
    ) {
        return WifiSecurityEngine.verifyMic(
                ptk,
                message,
                expectedMic
        );
    }

    @Override
    public byte[] protect(
            byte[] ptk,
            byte[] plaintext
    ) {
        return WifiSecurityEngine.protect(
                ptk,
                plaintext
        );
    }

    @Override
    public byte[] unprotect(
            byte[] ptk,
            byte[] protectedBytes
    ) {
        return WifiSecurityEngine.unprotect(
                ptk,
                protectedBytes
        );
    }
}
