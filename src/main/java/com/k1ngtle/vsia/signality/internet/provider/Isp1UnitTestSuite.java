package com.k1ngtle.vsia.signality.internet.provider;

import java.util.List;
import java.util.UUID;

public final class Isp1UnitTestSuite {
    public record Result(String name, boolean passed, String detail) {
    }

    private Isp1UnitTestSuite() {
    }

    public static List<Result> runAll() {
        UUID owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID attacker = UUID.fromString("22222222-2222-2222-2222-222222222222");
        long now = System.currentTimeMillis();

        InternetRegistrySavedData data = new InternetRegistrySavedData();

        InternetRegistryResult provider = data.registerProvider(
                owner,
                "OwnerA",
                "owner-net",
                "Owner Networks",
                now
        );

        InternetRegistryResult domain = data.registerDomain(
                owner,
                "OwnerA",
                "owner-example.net",
                "owner-net",
                now
        );

        InternetRegistryResult record = data.addDnsRecord(
                owner,
                "owner-example.net",
                "www",
                "A",
                "192.0.2.10",
                300
        );

        InternetRegistryResult unauthorized = data.addDnsRecord(
                attacker,
                "owner-example.net",
                "bad",
                "A",
                "192.0.2.11",
                300
        );

        InternetRegistryResult cname = data.addDnsRecord(
                owner,
                "owner-example.net",
                "alias",
                "CNAME",
                "www.owner-example.net",
                300
        );

        InternetRegistryResult cnameConflict = data.addDnsRecord(
                owner,
                "owner-example.net",
                "alias",
                "A",
                "192.0.2.12",
                300
        );

        return List.of(
                result(
                        "isp1-domain-normalization",
                        "example.com".equals(
                                InternetRegistryValidators.normalizeDomain("Example.COM.")
                        ),
                        "Example.COM. -> example.com"
                ),
                result(
                        "isp1-public-suffix-co-uk",
                        "example.co.uk".equals(
                                InternetRegistryValidators.registrableRoot("www.example.co.uk")
                        ),
                        "www.example.co.uk -> example.co.uk"
                ),
                result(
                        "isp1-root-registration-boundary",
                        !InternetRegistryValidators.isRegistrableRoot("www.example.com")
                                && InternetRegistryValidators.isRegistrableRoot("example.com"),
                        "Subdomains cannot be separately claimed as root registrations"
                ),
                result(
                        "isp1-private-asn-ranges",
                        InternetRegistryValidators.isPrivateAsn(64512L)
                                && InternetRegistryValidators.isPrivateAsn(4200000000L)
                                && !InternetRegistryValidators.isPrivateAsn(64496L),
                        "16-bit and 32-bit private-use ASN ranges recognized"
                ),
                result(
                        "isp1-provider-registration",
                        provider.success(),
                        provider.message()
                ),
                result(
                        "isp1-domain-registration",
                        domain.success(),
                        domain.message()
                ),
                result(
                        "isp1-owner-dns-write",
                        record.success(),
                        record.message()
                ),
                result(
                        "isp1-unauthorized-dns-rejected",
                        !unauthorized.success(),
                        unauthorized.message()
                ),
                result(
                        "isp1-global-a-resolution",
                        "192.0.2.10".equals(
                                data.resolveFirst(
                                        "www.owner-example.net",
                                        "A",
                                        now
                                ).map(InternetDnsAnswer::value).orElse("")
                        ),
                        data.delegationTrace("www.owner-example.net", "A", now)
                ),
                result(
                        "isp1-cname-exclusivity",
                        cname.success() && !cnameConflict.success(),
                        "CNAME cannot coexist with an A RRset at the same owner"
                )
        );
    }

    private static Result result(String name, boolean passed, String detail) {
        return new Result(name, passed, detail);
    }
}
