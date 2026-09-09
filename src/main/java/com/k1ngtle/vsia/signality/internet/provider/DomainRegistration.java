package com.k1ngtle.vsia.signality.internet.provider;

public record DomainRegistration(
        String domain,
        String recordType,
        String value,
        String providerId,
        long ttlSeconds
) {
    public DomainRegistration {
        domain = domain == null ? "" : domain.trim().toLowerCase(java.util.Locale.ROOT);
        recordType = recordType == null ? "" : recordType.trim().toUpperCase(java.util.Locale.ROOT);
        value = value == null ? "" : value.trim();
        providerId = providerId == null ? "" : providerId.trim();
        if (domain.isBlank() || recordType.isBlank() || value.isBlank() || providerId.isBlank() || ttlSeconds <= 0L) {
            throw new IllegalArgumentException("Invalid domain registration");
        }
    }
}
