package com.k1ngtle.vsia.signality.internet.dns.physical;

import java.util.List;

public final class PhysicalDnsUnitTestSuite {
    public record Result(
            String name,
            boolean passed,
            String detail
    ) {
    }

    private PhysicalDnsUnitTestSuite() {
    }

    public static List<Result> runAll() {
        DnssecZoneKey root =
                DnssecEngine.generate(
                        "."
                );

        DnssecZoneKey com =
                DnssecEngine.generate(
                        "com"
                );

        long now =
                System.currentTimeMillis()
                        / 1000L;

        long inception =
                now - 60L;

        long expiration =
                now + 3600L;

        String ds1 =
                DnssecEngine.ds(
                        "com",
                        com
                );

        String ds2 =
                DnssecEngine.ds(
                        "com",
                        com
                );

        String referral =
                DnssecEngine.canonicalReferral(
                        ".",
                        "www.example.com",
                        "com",
                        "192.0.2.53",
                        "",
                        ds1,
                        inception,
                        expiration,
                        root.keyTag()
                );

        String referralSignature =
                DnssecEngine.sign(
                        referral,
                        root
                );

        String answer =
                DnssecEngine.canonicalAnswer(
                        "example.com",
                        2026090900L,
                        "www.example.com",
                        "A",
                        300,
                        "192.0.2.10",
                        inception,
                        expiration,
                        com.keyTag()
                );

        String answerSignature =
                DnssecEngine.sign(
                        answer,
                        com
                );

        return List.of(
                result(
                        "isp1dns-rsasha256-algorithm",
                        root.algorithm()
                                == 8,
                        "DNSKEY algorithm 8 = RSASHA256"
                ),
                result(
                        "isp1dns-key-tag",
                        root.keyTag() >= 0
                                && root.keyTag() <= 65535,
                        "RFC-style 16-bit DNSKEY key tag="
                                + root.keyTag()
                ),
                result(
                        "isp1dns-ds-sha256",
                        ds1.equals(
                                ds2
                        )
                                && ds1.contains(
                                " 8 2 "
                        ),
                        ds1
                ),
                result(
                        "isp1dns-signed-root-referral",
                        DnssecEngine.verify(
                                referral,
                                referralSignature,
                                root.publicKeyX509Base64()
                        ),
                        "Root referral validates with SHA256withRSA"
                ),
                result(
                        "isp1dns-referral-tamper-rejected",
                        !DnssecEngine.verify(
                                referral.replace(
                                        "192.0.2.53",
                                        "203.0.113.53"
                                ),
                                referralSignature,
                                root.publicKeyX509Base64()
                        ),
                        "Modified glue/referral cannot reuse the original signature"
                ),
                result(
                        "isp1dns-signed-rrset",
                        DnssecEngine.verify(
                                answer,
                                answerSignature,
                                com.publicKeyX509Base64()
                        ),
                        "Authoritative RRset signature validates"
                ),
                result(
                        "isp1dns-answer-tamper-rejected",
                        !DnssecEngine.verify(
                                answer.replace(
                                        "192.0.2.10",
                                        "192.0.2.99"
                                ),
                                answerSignature,
                                com.publicKeyX509Base64()
                        ),
                        "Modified A record fails RRSIG verification"
                ),
                result(
                        "isp1dns-signature-time-window",
                        DnssecEngine.signatureTimeValid(
                                inception,
                                expiration,
                                now
                        )
                                && !DnssecEngine.signatureTimeValid(
                                inception,
                                expiration,
                                expiration + 1L
                        ),
                        "RRSIG-style inception/expiration window enforced"
                )
        );
    }

    private static Result result(
            String name,
            boolean passed,
            String detail
    ) {
        return new Result(
                name,
                passed,
                detail
        );
    }
}
