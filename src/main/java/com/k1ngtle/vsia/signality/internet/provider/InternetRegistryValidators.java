package com.k1ngtle.vsia.signality.internet.provider;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class InternetRegistryValidators {
    private static final Pattern PROVIDER_ID =
            Pattern.compile("^[a-z0-9][a-z0-9-]{2,31}$");

    private static final Pattern IPV4 =
            Pattern.compile("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$");

    private static final Pattern LABEL =
            Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");

    private static final Pattern DNS_OWNER_LABEL =
            Pattern.compile("^[a-z0-9_](?:[a-z0-9_-]{0,61}[a-z0-9_])?$");

    private static final Set<String> COMMON_TLDS = Set.of(
            "com", "net", "org", "info", "biz", "name",
            "io", "dev", "app", "me", "tv", "ai", "tech",
            "online", "site", "xyz", "games", "cloud", "systems", "network",
            "uk", "us", "ca", "de", "fr", "vn", "jp", "au", "nz", "eu"
    );

    private static final Set<String> MULTI_LABEL_SUFFIXES = Set.of(
            "co.uk", "org.uk", "me.uk", "ac.uk", "gov.uk",
            "com.au", "net.au", "org.au",
            "co.nz", "net.nz", "org.nz",
            "co.jp", "ne.jp"
    );

    private static final Set<String> DNS_TYPES = Set.of(
            "A", "AAAA", "CNAME", "MX", "TXT", "NS", "SRV", "CAA"
    );

    private InternetRegistryValidators() {
    }

    public static String normalizeDomain(String value) {
        if (value == null) {
            return "";
        }

        String result = value.trim().toLowerCase(Locale.ROOT);

        while (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    public static boolean validProviderId(String value) {
        return value != null
                && PROVIDER_ID.matcher(value.trim().toLowerCase(Locale.ROOT)).matches();
    }

    public static boolean validHostname(String value) {
        String normalized = normalizeDomain(value);

        if (normalized.isBlank() || normalized.length() > 253) {
            return false;
        }

        String[] labels = normalized.split("\\.");

        for (String label : labels) {
            if (label.length() > 63 || !LABEL.matcher(label).matches()) {
                return false;
            }
        }

        return true;
    }

    public static boolean validDnsOwnerName(String value) {
        String normalized = normalizeDomain(value);

        if (normalized.isBlank() || normalized.length() > 253) {
            return false;
        }

        String[] labels = normalized.split("\\.");

        for (int index = 0; index < labels.length; index++) {
            String label = labels[index];

            if (index == 0 && "*".equals(label)) {
                continue;
            }

            if (label.length() > 63 || !DNS_OWNER_LABEL.matcher(label).matches()) {
                return false;
            }
        }

        return true;
    }

    public static String publicSuffix(String value) {
        String normalized = normalizeDomain(value);

        if (!validHostname(normalized)) {
            return "";
        }

        for (String suffix : MULTI_LABEL_SUFFIXES) {
            if (normalized.equals(suffix) || normalized.endsWith("." + suffix)) {
                return suffix;
            }
        }

        int dot = normalized.lastIndexOf('.');

        if (dot < 0) {
            return "";
        }

        String tld = normalized.substring(dot + 1);

        return COMMON_TLDS.contains(tld) ? tld : "";
    }

    public static String registrableRoot(String value) {
        String normalized = normalizeDomain(value);
        String suffix = publicSuffix(normalized);

        if (suffix.isBlank()) {
            return "";
        }

        String suffixWithDot = "." + suffix;

        if (!normalized.endsWith(suffixWithDot)) {
            return "";
        }

        String prefix = normalized.substring(0, normalized.length() - suffixWithDot.length());
        int dot = prefix.lastIndexOf('.');
        String rootLabel = dot >= 0 ? prefix.substring(dot + 1) : prefix;

        if (rootLabel.isBlank() || !LABEL.matcher(rootLabel).matches()) {
            return "";
        }

        return rootLabel + "." + suffix;
    }

    public static boolean isRegistrableRoot(String value) {
        String normalized = normalizeDomain(value);
        return normalized.equals(registrableRoot(normalized));
    }

    public static boolean validIpv4(String value) {
        return value != null && IPV4.matcher(value.trim()).matches();
    }

    public static boolean validIpv6(String value) {
        if (value == null || !value.contains(":")) {
            return false;
        }

        try {
            return InetAddress.getByName(value) instanceof Inet6Address;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean supportedDnsType(String value) {
        return value != null && DNS_TYPES.contains(value.trim().toUpperCase(Locale.ROOT));
    }

    public static String fqdn(String zone, String owner) {
        String normalizedZone = normalizeDomain(zone);
        String normalizedOwner = normalizeDomain(owner);

        if (owner == null || owner.isBlank() || "@".equals(owner)) {
            return normalizedZone;
        }

        if (normalizedOwner.equals(normalizedZone)
                || normalizedOwner.endsWith("." + normalizedZone)) {
            return normalizedOwner;
        }

        return normalizedOwner + "." + normalizedZone;
    }

    public static boolean isPrivateAsn(long value) {
        return value >= 64512L && value <= 65534L
                || value >= 4200000000L && value <= 4294967294L;
    }
}
