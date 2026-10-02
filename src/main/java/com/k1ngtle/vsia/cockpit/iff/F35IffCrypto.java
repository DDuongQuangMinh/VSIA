package com.k1ngtle.vsia.cockpit.iff;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Public-crypto game protocol, NOT classified Mode 4/5 or real RF interoperability. */
public final class F35IffCrypto {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, Challenge> PENDING = new HashMap<>();
    private static final int MAX_PENDING = 8192;
    private F35IffCrypto() { }
    public static synchronized Challenge challenge(UUID interrogator, UUID aircraft, String keyId, int mode, long sequence) throws GeneralSecurityException {
        long now = Instant.now().getEpochSecond();
        PENDING.values().removeIf(c -> now - c.timestamp() > 10);
        if (PENDING.size() >= MAX_PENDING || sequence <= 0 || (mode != 4 && mode != 5))
            throw new GeneralSecurityException("IFF challenge capacity/sequence/mode");
        byte[] nonce = new byte[16]; do{RANDOM.nextBytes(nonce);}while(PENDING.containsKey(Base64.getEncoder().encodeToString(nonce)));
        Challenge c = new Challenge(interrogator, aircraft, keyId, mode, nonce, now, sequence);
        PENDING.put(token(c), c); return c;
    }
    public static byte[] mode4(F35IffConfig.KeySlot key, Challenge c, UUID aircraft) throws GeneralSecurityException {
        requireBinding(key, c, aircraft, 4); return hmac(sessionKey(key, c, "M4-AUTH"), material(c));
    }
    public static byte[] mode5Encrypt(F35IffConfig.KeySlot key, Challenge c, UUID aircraft, byte[] telemetry) throws GeneralSecurityException {
        requireBinding(key, c, aircraft, 5); return cipher(key, c, Cipher.ENCRYPT_MODE).doFinal(telemetry);
    }
    public static boolean verifyMode4(F35IffConfig.KeySlot key, Challenge c, UUID aircraft, byte[] response) throws GeneralSecurityException {
        if (!consume(c)) return false; return MessageDigest.isEqual(mode4(key, c, aircraft), response);
    }
    public static byte[] verifyMode5(F35IffConfig.KeySlot key, Challenge c, UUID aircraft, byte[] encrypted) throws GeneralSecurityException {
        if (!consume(c)) throw new GeneralSecurityException("Unissued, stale, altered or replayed challenge");
        requireBinding(key, c, aircraft, 5); return cipher(key, c, Cipher.DECRYPT_MODE).doFinal(encrypted);
    }
    private static Cipher cipher(F35IffConfig.KeySlot key, Challenge c, int operation) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(operation, new SecretKeySpec(sessionKey(key, c, "M5-AEAD"), "AES"),
                new GCMParameterSpec(128, Arrays.copyOf(c.nonce(), 12)));
        cipher.updateAAD(material(c)); return cipher;
    }
    private static void requireBinding(F35IffConfig.KeySlot key, Challenge c, UUID aircraft, int mode) throws GeneralSecurityException {
        if (!key.valid(Instant.now().getEpochSecond()) || !key.id().equals(c.keyId()) || !aircraft.equals(c.aircraft()) || c.mode() != mode)
            throw new GeneralSecurityException("Key expired or identity/mode mismatch");
    }
    private static synchronized boolean consume(Challenge c) {
        Challenge issued = PENDING.get(token(c)); long now = Instant.now().getEpochSecond();
        if (issued == null || now < c.timestamp() || now - c.timestamp() > 10 ||
                !MessageDigest.isEqual(material(issued), material(c))) return false;
        PENDING.remove(token(c)); return true;
    }
    private static String token(Challenge c) { return Base64.getEncoder().encodeToString(c.nonce()); }
    private static byte[] material(Challenge c) {
        byte[] id = c.keyId().getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(72 + id.length)
                .putLong(c.interrogator().getMostSignificantBits()).putLong(c.interrogator().getLeastSignificantBits())
                .putLong(c.aircraft().getMostSignificantBits()).putLong(c.aircraft().getLeastSignificantBits())
                .put(c.nonce()).putLong(c.timestamp()).putLong(c.sequence()).putInt(c.mode()).putInt(id.length).put(id).array();
    }
    private static byte[] sessionKey(F35IffConfig.KeySlot key, Challenge c, String label) throws GeneralSecurityException {
        byte[] secret = key.secret();
        try { return hkdf(secret, material(c), ("VSIA-IFF-v1/" + label).getBytes(StandardCharsets.UTF_8), 32); }
        finally { Arrays.fill(secret, (byte)0); }
    }
    /** RFC 5869 extract + expand, SHA-256. */
    public static byte[] hkdf(byte[] ikm, byte[] salt, byte[] info, int length) throws GeneralSecurityException {
        if (length < 1 || length > 255 * 32) throw new GeneralSecurityException("HKDF length");
        byte[] prk = hmac(salt.length == 0 ? new byte[32] : salt, ikm), previous = new byte[0], out = new byte[length];
        try {
            for (int offset = 0, counter = 1; offset < length; counter++) {
                byte[] input = ByteBuffer.allocate(previous.length + info.length + 1).put(previous).put(info).put((byte)counter).array();
                previous = hmac(prk, input); int count = Math.min(32, length - offset);
                System.arraycopy(previous, 0, out, offset, count); offset += count;
            } return out;
        } finally { Arrays.fill(prk, (byte)0); Arrays.fill(previous, (byte)0); }
    }
    private static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key, "HmacSHA256")); return mac.doFinal(data);
    }
    public static synchronized void clear() { PENDING.clear(); }
    public record Challenge(UUID interrogator, UUID aircraft, String keyId, int mode, byte[] nonce, long timestamp, long sequence) {
        public Challenge { if(nonce.length != 16)throw new IllegalArgumentException("Nonce length"); nonce = nonce.clone(); }
        @Override public byte[] nonce() { return nonce.clone(); }
    }
}
