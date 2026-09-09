package com.k1ngtle.vsia.signality.engineering.wifi.security.protocol;

public interface WifiSecurityProtocol {
    String id();

    String displayName();

    WifiSecurityCapabilities capabilities();

    default boolean isOpen() {
        return !capabilities()
                .authenticationRequired();
    }

    byte[] derivePmk(
            String passphrase,
            String ssid
    );

    byte[] derivePtk(
            byte[] pmk,
            String apMac,
            String stationMac,
            byte[] anonce,
            byte[] snonce
    );

    byte[] mic(
            byte[] ptk,
            byte[] message
    );

    boolean verifyMic(
            byte[] ptk,
            byte[] message,
            byte[] expectedMic
    );

    byte[] protect(
            byte[] ptk,
            byte[] plaintext
    );

    byte[] unprotect(
            byte[] ptk,
            byte[] protectedBytes
    );
}
