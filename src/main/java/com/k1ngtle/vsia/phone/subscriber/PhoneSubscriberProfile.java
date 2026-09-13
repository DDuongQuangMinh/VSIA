package com.k1ngtle.vsia.phone.subscriber;

import net.minecraft.nbt.CompoundTag;

public record PhoneSubscriberProfile(
        String iccid,
        String imsi,
        String msisdn,
        String carrier,
        String plmn,
        String profileType,
        String eidBinding,
        String authSecret,
        boolean enabled
) {
    public PhoneSubscriberProfile {
        iccid = safe(iccid);
        imsi = safe(imsi);
        msisdn = safe(msisdn);
        carrier = safe(carrier);
        plmn = safe(plmn);
        profileType = safe(profileType);
        eidBinding = safe(eidBinding);
        authSecret = safe(authSecret);
    }

    public boolean isEsim() {
        return "ESIM".equalsIgnoreCase(profileType);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Iccid", iccid);
        tag.putString("Imsi", imsi);
        tag.putString("Msisdn", msisdn);
        tag.putString("Carrier", carrier);
        tag.putString("Plmn", plmn);
        tag.putString("ProfileType", profileType);
        tag.putString("EidBinding", eidBinding);
        tag.putString("AuthSecret", authSecret);
        tag.putBoolean("Enabled", enabled);
        return tag;
    }

    public static PhoneSubscriberProfile load(CompoundTag tag) {
        return new PhoneSubscriberProfile(
                tag.getString("Iccid"),
                tag.getString("Imsi"),
                tag.getString("Msisdn"),
                tag.getString("Carrier"),
                tag.getString("Plmn"),
                tag.getString("ProfileType"),
                tag.getString("EidBinding"),
                tag.getString("AuthSecret"),
                tag.getBoolean("Enabled")
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
