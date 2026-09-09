package com.k1ngtle.vsia.signality.engineering.wifi.integration.w125;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityEngine;
import com.k1ngtle.vsia.signality.engineering.wifi.security.protocol.WifiSecurityProtocol;
import com.k1ngtle.vsia.signality.engineering.wifi.security.protocol.WifiSecurityProtocolRegistry;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class W125SecurityUnitTestSuite {
    public record Result(
            String name,
            boolean passed,
            String detail
    ) {
    }

    private W125SecurityUnitTestSuite() {
    }

    public static List<Result> runAll() {
        return List.of(
                registryAliases(),
                openCapabilities(),
                protectedCapabilities(),
                unsupportedRejected(),
                wpa2CryptoRoundTrip(),
                wpa3CryptoRoundTrip(),
                wrongKeyRejected(),
                protocolExtensibility()
        );
    }

    private static Result registryAliases() {
        boolean passed =
                WifiSecurityProtocolRegistry.canonicalId(
                        "open"
                ).equals(
                        "signality:open"
                )
                        && WifiSecurityProtocolRegistry.canonicalId(
                        "wpa2"
                ).equals(
                        "signality:wpa2"
                )
                        && WifiSecurityProtocolRegistry.canonicalId(
                        "SAE"
                ).equals(
                        "signality:wpa3"
                );

        return result(
                "w125-security-alias-resolution",
                passed,
                "Aliases resolve to canonical protocol IDs"
        );
    }

    private static Result openCapabilities() {
        WifiSecurityProtocol protocol =
                WifiSecurityProtocolRegistry.resolve(
                        "signality:open"
                );

        boolean passed =
                protocol.isOpen()
                        && !protocol.capabilities()
                        .fourWayHandshake()
                        && !protocol.capabilities()
                        .dataProtection();

        return result(
                "w125-open-capabilities",
                passed,
                "OPEN is explicitly modeled as no authentication/handshake/data protection"
        );
    }

    private static Result protectedCapabilities() {
        WifiSecurityProtocol wpa2 =
                WifiSecurityProtocolRegistry.resolve(
                        "wpa2"
                );

        WifiSecurityProtocol wpa3 =
                WifiSecurityProtocolRegistry.resolve(
                        "wpa3"
                );

        boolean passed =
                !wpa2.isOpen()
                        && wpa2.capabilities()
                        .fourWayHandshake()
                        && wpa2.capabilities()
                        .pairwiseKey()
                        && wpa2.capabilities()
                        .dataProtection()
                        && !wpa3.isOpen()
                        && wpa3.capabilities()
                        .fourWayHandshake()
                        && wpa3.capabilities()
                        .dataProtection();

        return result(
                "w125-protected-capabilities",
                passed,
                "Protected suites expose handshake/key/data-protection capabilities through the protocol interface"
        );
    }

    private static Result unsupportedRejected() {
        boolean passed;

        try {
            WifiSecurityProtocolRegistry.resolve(
                    "signality:not-a-real-suite"
            );
            passed =
                    false;
        } catch (IllegalArgumentException expected) {
            passed =
                    true;
        }

        return result(
                "w125-unsupported-security-rejected",
                passed,
                "Unknown security IDs cannot silently become OPEN or generic protected mode"
        );
    }

    private static Result wpa2CryptoRoundTrip() {
        return cryptoRoundTrip(
                "wpa2",
                "w125-wpa2-protocol-roundtrip"
        );
    }

    private static Result wpa3CryptoRoundTrip() {
        return cryptoRoundTrip(
                "wpa3",
                "w125-wpa3-protocol-roundtrip"
        );
    }

    private static Result cryptoRoundTrip(
            String protocolId,
            String testName
    ) {
        WifiSecurityProtocol protocol =
                WifiSecurityProtocolRegistry.resolve(
                        protocolId
                );

        byte[] anonce =
                WifiSecurityEngine.randomNonce();

        byte[] snonce =
                WifiSecurityEngine.randomNonce();

        byte[] pmk =
                protocol.derivePmk(
                        "correct horse battery staple",
                        "VSIA-W125"
                );

        byte[] ptk =
                protocol.derivePtk(
                        pmk,
                        "02:00:00:00:00:01",
                        "02:00:00:00:00:02",
                        anonce,
                        snonce
                );

        byte[] plaintext =
                "W1.25 protocol abstraction"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        byte[] protectedBytes =
                protocol.protect(
                        ptk,
                        plaintext
                );

        byte[] decoded =
                protocol.unprotect(
                        ptk,
                        protectedBytes
                );

        boolean passed =
                Arrays.equals(
                        plaintext,
                        decoded
                );

        return result(
                testName,
                passed,
                protocol.displayName()
                        + " dispatches crypto through the security protocol abstraction"
        );
    }

    private static Result wrongKeyRejected() {
        WifiSecurityProtocol protocol =
                WifiSecurityProtocolRegistry.resolve(
                        "wpa2"
                );

        byte[] anonce =
                WifiSecurityEngine.randomNonce();

        byte[] snonce =
                WifiSecurityEngine.randomNonce();

        byte[] good =
                protocol.derivePtk(
                        protocol.derivePmk(
                                "good-password",
                                "VSIA-W125"
                        ),
                        "02:00:00:00:00:01",
                        "02:00:00:00:00:02",
                        anonce,
                        snonce
                );

        byte[] bad =
                protocol.derivePtk(
                        protocol.derivePmk(
                                "wrong-password",
                                "VSIA-W125"
                        ),
                        "02:00:00:00:00:01",
                        "02:00:00:00:00:02",
                        anonce,
                        snonce
                );

        byte[] protectedBytes =
                protocol.protect(
                        good,
                        new byte[] {
                                1,
                                2,
                                3,
                                4
                        }
                );

        boolean passed;

        try {
            protocol.unprotect(
                    bad,
                    protectedBytes
            );
            passed =
                    false;
        } catch (IllegalArgumentException expected) {
            passed =
                    true;
        }

        return result(
                "w125-wrong-key-rejected",
                passed,
                "Wrong PTK fails authenticated decryption through the protocol interface"
        );
    }

    private static Result protocolExtensibility() {
        String custom =
                "test:w125-custom";

        WifiSecurityProtocol protocol =
                new com.k1ngtle.vsia.signality.engineering.wifi.security.protocol.PskWifiSecurityProtocol(
                        custom,
                        "W1.25 custom test suite"
                );

        WifiSecurityProtocolRegistry.register(
                protocol,
                "w125-custom"
        );

        boolean passed =
                WifiSecurityProtocolRegistry.resolve(
                        "w125-custom"
                ) == protocol
                        && WifiSecurityProtocolRegistry.canonicalId(
                        "w125-custom"
                ).equals(
                        custom
                );

        return result(
                "w125-extensible-registry",
                passed,
                "Additional security protocols can register without changing WifiMacController switch statements"
        );
    }

    private static Result result(
            String name,
            boolean passed,
            String detail
    ) {
        return new Result(
                name,
                passed,
                detail
        );
    }
}
