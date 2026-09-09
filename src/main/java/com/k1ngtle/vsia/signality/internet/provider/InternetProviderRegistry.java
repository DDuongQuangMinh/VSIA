package com.k1ngtle.vsia.signality.internet.provider;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class InternetProviderRegistry {
    public static final String VSIA_PROVIDER_ID = "vsia-net";
    public static final String DEFAULT_WEB_DOMAIN = "www.vsia-net.com";
    public static final String DEFAULT_WEB_IPV4 = "192.168.1.2";

    private static final Map<String, InternetProvider> PROVIDERS = new LinkedHashMap<>();
    private static final Map<String, DomainRegistration> DOMAINS = new LinkedHashMap<>();

    static {
        resetDefaults();
    }

    private InternetProviderRegistry() {
    }

    public static synchronized void resetDefaults() {
        PROVIDERS.clear();
        DOMAINS.clear();
        registerProvider(new InternetProvider(VSIA_PROVIDER_ID, "VSIA Network Provider", "AS64512", "vsia-net.com"));
        registerDomain(new DomainRegistration(DEFAULT_WEB_DOMAIN, "A", DEFAULT_WEB_IPV4, VSIA_PROVIDER_ID, 300L));
        registerDomain(new DomainRegistration("mail.vsia-net.com", "A", DEFAULT_WEB_IPV4, VSIA_PROVIDER_ID, 300L));
    }

    public static synchronized void registerProvider(InternetProvider provider) {
        PROVIDERS.put(provider.id().trim().toLowerCase(Locale.ROOT), provider);
    }

    public static synchronized void registerDomain(DomainRegistration registration) {
        if (!PROVIDERS.containsKey(registration.providerId().trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unknown provider: " + registration.providerId());
        }
        DOMAINS.put(key(registration.domain(), registration.recordType()), registration);
    }

    public static synchronized Optional<InternetProvider> provider(String id) {
        return Optional.ofNullable(PROVIDERS.get(id == null ? "" : id.trim().toLowerCase(Locale.ROOT)));
    }

    public static synchronized Optional<DomainRegistration> resolve(String domain, String recordType) {
        return Optional.ofNullable(DOMAINS.get(key(domain, recordType)));
    }

    public static synchronized Optional<String> resolveA(String domain) {
        return resolve(domain, "A").map(DomainRegistration::value);
    }

    private static String key(String domain, String type) {
        return (domain == null ? "" : domain.trim().toLowerCase(Locale.ROOT))
                + "|"
                + (type == null ? "" : type.trim().toUpperCase(Locale.ROOT));
    }
}
