package com.k1ngtle.vsia.phone.subscriber;

public record PhoneSubscriberSnapshot(
        String eid,
        boolean physicalSimPresent,
        boolean esimPresent,
        String activeType,
        String carrier,
        String plmn,
        String msisdn,
        String iccid,
        String imsi,
        boolean authenticated,
        boolean cellularDataEnabled,
        String serviceStatus
) {
    public PhoneSubscriberSnapshot {
        eid = safe(eid);
        activeType = safe(activeType);
        carrier = safe(carrier);
        plmn = safe(plmn);
        msisdn = safe(msisdn);
        iccid = safe(iccid);
        imsi = safe(imsi);
        serviceStatus = safe(serviceStatus);
    }

    public boolean hasActiveSubscription() {
        return !activeType.isBlank() && authenticated && !msisdn.isBlank();
    }

    public boolean canUseCellularData() {
        return hasActiveSubscription() && cellularDataEnabled;
    }

    public static PhoneSubscriberSnapshot empty() {
        return new PhoneSubscriberSnapshot(
                "",
                false,
                false,
                "",
                "",
                "",
                "",
                "",
                "",
                false,
                true,
                "No SIM"
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
