package com.k1ngtle.vsia.phone.messages;

import java.util.List;

public record PhoneMessagesSnapshot(
        String ownNumber,
        boolean serviceAvailable,
        String status,
        List<PhoneSmsMessage> messages
) {
    public PhoneMessagesSnapshot {
        ownNumber = safe(ownNumber);
        status = safe(status);
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static PhoneMessagesSnapshot empty() {
        return new PhoneMessagesSnapshot(
                "",
                false,
                "No SIM",
                List.of()
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
