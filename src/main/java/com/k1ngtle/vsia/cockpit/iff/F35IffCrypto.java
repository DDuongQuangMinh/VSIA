package com.k1ngtle.vsia.cockpit.iff;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Public-crypto analogue only: this is deliberately not an implementation of classified Mode 4/5. */
public final class F35IffCrypto {
    private static final long MAX_SKEW_SECONDS = 10;
    private static final Set<String> USED = ConcurrentHashMap.newKeySet();
    private F35IffCrypto() { }

    public static Challenge challenge(UUID interrogator, long sequence) {
        byte[] nonce = new byte[16]; new SecureRandom().nextBytes(nonce);
        return new Challenge(interrogator, nonce, Instant.now().getEpochSecond(), sequence);
    }
    public static byte[] mode4(F35IffConfig.KeySlot key, Challenge c, UUID aircraft) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(derive(key.secret(), "VSIA-IFF-M4-AUTH"), "HmacSHA256"));
        return mac.doFinal(material(c, aircraft));
    }
    public static byte[] mode5Encrypt(F35IffConfig.KeySlot key, Challenge c, UUID aircraft, byte[] telemetry) throws GeneralSecurityException {
        byte[] iv = Arrays.copyOf(c.nonce(), 12); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(derive(key.secret(), "VSIA-IFF-M5-AEAD"), "AES"), new GCMParameterSpec(128, iv));
        cipher.updateAAD(material(c, aircraft)); return cipher.doFinal(telemetry);
    }
    public static boolean verifyMode4(F35IffConfig.KeySlot key, Challenge c, UUID aircraft, byte[] response) throws GeneralSecurityException {
        return fresh(c) && reserve(c) && MessageDigest.isEqual(mode4(key, c, aircraft), response);
    }
    public static byte[] verifyMode5(F35IffConfig.KeySlot key, Challenge c, UUID aircraft, byte[] encrypted) throws GeneralSecurityException {
        if (!fresh(c) || !reserve(c)) throw new GeneralSecurityException("stale or replayed challenge");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(derive(key.secret(), "VSIA-IFF-M5-AEAD"), "AES"), new GCMParameterSpec(128, Arrays.copyOf(c.nonce(), 12)));
        cipher.updateAAD(material(c, aircraft)); return cipher.doFinal(encrypted);
    }
    private static boolean fresh(Challenge c) { return Math.abs(Instant.now().getEpochSecond() - c.timestamp()) <= MAX_SKEW_SECONDS; }
    private static boolean reserve(Challenge c) { if (USED.size() > 8192) USED.clear(); return USED.add(c.interrogator() + ":" + c.sequence() + ":" + Arrays.hashCode(c.nonce())); }
    private static byte[] material(Challenge c, UUID aircraft) { return ByteBuffer.allocate(64).putLong(c.interrogator().getMostSignificantBits()).putLong(c.interrogator().getLeastSignificantBits()).put(c.nonce()).putLong(c.timestamp()).putLong(c.sequence()).putLong(aircraft.getMostSignificantBits()).putLong(aircraft.getLeastSignificantBits()).array(); }
    private static byte[] derive(byte[] key, String label) throws GeneralSecurityException { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key, "HmacSHA256")); return mac.doFinal(label.getBytes(StandardCharsets.UTF_8)); }
    public record Challenge(UUID interrogator, byte[] nonce, long timestamp, long sequence) { }
}
