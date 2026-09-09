package com.k1ngtle.vsia.signality.internet.dns.physical;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

public final class DnssecEngine {
    public static final int DNSKEY_FLAGS_KSK = 257;
    public static final int DNSKEY_PROTOCOL = 3;
    public static final int DNSKEY_ALGORITHM_RSASHA256 = 8;
    public static final int DS_DIGEST_SHA256 = 2;

    private DnssecEngine() {
    }

    public static DnssecZoneKey generate(String zone) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);

            KeyPair pair = generator.generateKeyPair();
            RSAPublicKey rsa = (RSAPublicKey) pair.getPublic();

            byte[] dnskeyPublic = encodeRsaDnskeyPublic(rsa);
            byte[] rdata = dnskeyRdata(
                    DNSKEY_FLAGS_KSK,
                    DNSKEY_PROTOCOL,
                    DNSKEY_ALGORITHM_RSASHA256,
                    dnskeyPublic
            );

            int keyTag = keyTag(rdata);

            return new DnssecZoneKey(
                    normalizeZone(zone),
                    DNSKEY_FLAGS_KSK,
                    DNSKEY_PROTOCOL,
                    DNSKEY_ALGORITHM_RSASHA256,
                    keyTag,
                    Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()),
                    Base64.getEncoder().encodeToString(dnskeyPublic),
                    System.currentTimeMillis()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate DNSSEC RSA/SHA-256 key.", exception);
        }
    }

    public static String ds(String zone, DnssecZoneKey key) {
        return dsFromPresentation(
                zone,
                key.flags(),
                key.protocol(),
                key.algorithm(),
                key.keyTag(),
                key.dnskeyPublicBase64()
        );
    }

    public static String dsFromPresentation(
            String zone,
            int flags,
            int protocol,
            int algorithm,
            int keyTag,
            String dnskeyPublicBase64
    ) {
        try {
            byte[] publicBytes = Base64.getDecoder().decode(dnskeyPublicBase64);
            byte[] rdata = dnskeyRdata(flags, protocol, algorithm, publicBytes);
            int computedKeyTag = keyTag(rdata);

            if (keyTag != computedKeyTag) {
                throw new IllegalArgumentException(
                        "DNSKEY key tag does not match the supplied RDATA."
                );
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(wireName(normalizeZone(zone)));
            digest.update(rdata);

            return computedKeyTag
                    + " "
                    + algorithm
                    + " "
                    + DS_DIGEST_SHA256
                    + " "
                    + HexFormat.of()
                    .withUpperCase()
                    .formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid DNSKEY material.", exception);
        }
    }

    public static String sign(
            String canonicalData,
            DnssecZoneKey key
    ) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey(key.privateKeyPkcs8Base64()));
            signature.update(canonicalData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create DNSSEC signature.", exception);
        }
    }

    public static boolean verify(
            String canonicalData,
            String signatureBase64,
            String publicKeyX509Base64
    ) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey(publicKeyX509Base64));
            signature.update(canonicalData.getBytes(StandardCharsets.UTF_8));

            return signature.verify(
                    Base64.getDecoder().decode(signatureBase64)
            );
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String fingerprint(String publicKeyX509Base64) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of()
                    .withUpperCase()
                    .formatHex(
                            digest.digest(
                                    Base64.getDecoder().decode(publicKeyX509Base64)
                            )
                    );
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid public key.", exception);
        }
    }

    public static String canonicalReferral(
            String signerZone,
            String queryName,
            String delegatedZone,
            String primaryIp,
            String secondaryIp,
            String ds,
            long inception,
            long expiration,
            int keyTag
    ) {
        return "REFERRAL"
                + "|signer=" + normalizeZone(signerZone)
                + "|q=" + queryName.toLowerCase(Locale.ROOT)
                + "|delegated=" + normalizeZone(delegatedZone)
                + "|primary=" + safe(primaryIp)
                + "|secondary=" + safe(secondaryIp)
                + "|ds=" + safe(ds)
                + "|inception=" + inception
                + "|expiration=" + expiration
                + "|keyTag=" + keyTag;
    }

    public static String canonicalDnskey(
            String zone,
            DnssecZoneKey key,
            long inception,
            long expiration
    ) {
        return "DNSKEY"
                + "|zone=" + normalizeZone(zone)
                + "|dnskey=" + key.presentation()
                + "|inception=" + inception
                + "|expiration=" + expiration
                + "|keyTag=" + key.keyTag();
    }

    public static String canonicalAnswer(
            String zone,
            long serial,
            String owner,
            String type,
            int ttl,
            String value,
            long inception,
            long expiration,
            int keyTag
    ) {
        return "ANSWER"
                + "|zone=" + normalizeZone(zone)
                + "|serial=" + Long.toUnsignedString(serial)
                + "|owner=" + owner.toLowerCase(Locale.ROOT)
                + "|type=" + type.toUpperCase(Locale.ROOT)
                + "|ttl=" + ttl
                + "|value=" + value
                + "|inception=" + inception
                + "|expiration=" + expiration
                + "|keyTag=" + keyTag;
    }

    public static boolean signatureTimeValid(
            long inception,
            long expiration,
            long nowEpochSeconds
    ) {
        return inception <= nowEpochSeconds
                && nowEpochSeconds <= expiration;
    }

    private static byte[] encodeRsaDnskeyPublic(RSAPublicKey key) {
        try {
            byte[] exponent = unsigned(key.getPublicExponent());
            byte[] modulus = unsigned(key.getModulus());

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            if (exponent.length < 256) {
                out.write(exponent.length);
            } else {
                out.write(0);
                out.write((exponent.length >>> 8) & 0xFF);
                out.write(exponent.length & 0xFF);
            }

            out.write(exponent);
            out.write(modulus);

            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encode RSA DNSKEY.", exception);
        }
    }

    private static byte[] dnskeyRdata(
            int flags,
            int protocol,
            int algorithm,
            byte[] publicKey
    ) {
        byte[] rdata = new byte[4 + publicKey.length];

        rdata[0] = (byte) ((flags >>> 8) & 0xFF);
        rdata[1] = (byte) (flags & 0xFF);
        rdata[2] = (byte) protocol;
        rdata[3] = (byte) algorithm;

        System.arraycopy(publicKey, 0, rdata, 4, publicKey.length);

        return rdata;
    }

    private static int keyTag(byte[] rdata) {
        long accumulator = 0L;

        for (int index = 0; index < rdata.length; index++) {
            accumulator += (index & 1) == 0
                    ? (long) (rdata[index] & 0xFF) << 8
                    : rdata[index] & 0xFF;
        }

        accumulator += (accumulator >>> 16) & 0xFFFFL;

        return (int) (accumulator & 0xFFFFL);
    }

    private static byte[] wireName(String zone) {
        if (".".equals(zone)) {
            return new byte[]{0};
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (String label : zone.split("\\.")) {
            byte[] bytes = label.toLowerCase(Locale.ROOT)
                    .getBytes(StandardCharsets.US_ASCII);

            out.write(bytes.length);
            out.writeBytes(bytes);
        }

        out.write(0);

        return out.toByteArray();
    }

    private static PublicKey publicKey(String base64) throws Exception {
        byte[] encoded = Base64.getDecoder().decode(base64);

        return KeyFactory.getInstance("RSA")
                .generatePublic(
                        new X509EncodedKeySpec(encoded)
                );
    }

    private static PrivateKey privateKey(String base64) throws Exception {
        byte[] encoded = Base64.getDecoder().decode(base64);

        return KeyFactory.getInstance("RSA")
                .generatePrivate(
                        new PKCS8EncodedKeySpec(encoded)
                );
    }

    private static byte[] unsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }

        return bytes;
    }

    public static String normalizeZone(String value) {
        if (value == null || value.isBlank() || ".".equals(value.trim())) {
            return ".";
        }

        String normalized = value.trim()
                .toLowerCase(Locale.ROOT);

        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
