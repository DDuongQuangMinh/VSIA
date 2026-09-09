package com.k1ngtle.vsia.signality.internet.dns.physical;

import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistryValidators;
import com.k1ngtle.vsia.signality.internet.provider.RegisteredDomain;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PhysicalDnsBootstrap {
    private PhysicalDnsBootstrap() {
    }

    public static Result configure(
            ServerLevel level,
            ServerRackBlockEntity resolver,
            ServerRackBlockEntity root,
            ServerRackBlockEntity tld,
            ServerRackBlockEntity primary,
            ServerRackBlockEntity secondary,
            String requestedZone
    ) {
        String zone =
                InternetRegistryValidators.normalizeDomain(
                        requestedZone
                );

        RegisteredDomain registration =
                InternetRegistrySavedData.get(
                        level
                ).domain(
                        zone
                ).orElse(
                        null
                );

        if (registration == null) {
            return Result.fail(
                    "Registered ISP1 domain not found: "
                            + zone
            );
        }

        List<ServerRackBlockEntity> racks =
                List.of(
                        resolver,
                        root,
                        tld,
                        primary,
                        secondary
                );

        Set<String> ips =
                new HashSet<>();

        for (ServerRackBlockEntity rack :
                racks) {
            if (!rack.dnsEnabled()) {
                return Result.fail(
                        rack.displayName()
                                + " must have DNS enabled."
                );
            }

            String ip =
                    rack.ipAddress();

            if (ip == null
                    || ip.isBlank()
                    || "0.0.0.0".equals(
                    ip
            )) {
                return Result.fail(
                        rack.displayName()
                                + " needs a configured IPv4 address."
                );
            }

            if (!ips.add(
                    ip
            )) {
                return Result.fail(
                        "All five DNS ServerRacks require unique IPv4 addresses. Duplicate: "
                                + ip
                );
            }
        }

        String tldZone =
                PhysicalDnsStateSavedData.tldOf(
                        zone
                );

        if (tldZone.isBlank()) {
            return Result.fail(
                    "Cannot derive TLD from "
                            + zone
            );
        }

        PhysicalDnsStateSavedData state =
                PhysicalDnsStateSavedData.get(
                        level
                );

        DnssecZoneKey rootKey =
                state.ensureKey(
                        "."
                );

        state.ensureKey(
                tldZone
        );

        state.ensureKey(
                zone
        );

        String rootTrustAnchor =
                DnssecEngine.ds(
                        ".",
                        rootKey
                );

        List<String> nameServers =
                registration.nameServers();

        String primaryNameServer =
                nameServers.isEmpty()
                        ? "ns1."
                        + zone
                        : nameServers.get(
                        0
                );

        String secondaryNameServer =
                nameServers.size() >= 2
                        ? nameServers.get(
                        1
                )
                        : "ns2."
                        + zone;

        state.setTldServer(
                tldZone,
                tld.ipAddress()
        );

        state.setDelegation(
                new DnsAuthorityDelegation(
                        zone,
                        tldZone,
                        primary.ipAddress(),
                        secondary.ipAddress(),
                        primaryNameServer,
                        secondaryNameServer
                )
        );

        PhysicalDnsRackConfig.write(
                resolver,
                PhysicalDnsRole.RECURSIVE,
                "",
                root.ipAddress(),
                "",
                true,
                rootTrustAnchor
        );

        PhysicalDnsRackConfig.write(
                root,
                PhysicalDnsRole.ROOT,
                ".",
                "",
                "",
                false,
                ""
        );

        PhysicalDnsRackConfig.write(
                tld,
                PhysicalDnsRole.TLD,
                tldZone,
                "",
                "",
                false,
                ""
        );

        PhysicalDnsRackConfig.write(
                primary,
                PhysicalDnsRole.AUTHORITATIVE_PRIMARY,
                zone,
                "",
                "",
                false,
                ""
        );

        PhysicalDnsRackConfig.write(
                secondary,
                PhysicalDnsRole.AUTHORITATIVE_SECONDARY,
                zone,
                "",
                primary.ipAddress(),
                false,
                ""
        );

        DnsZoneSnapshot primarySnapshot =
                state.observePrimary(
                        level,
                        zone
                );

        return Result.ok(
                "Configured physical DNS hierarchy"
                        + " | resolver="
                        + resolver.ipAddress()
                        + " | root="
                        + root.ipAddress()
                        + " | ."
                        + tldZone
                        + "="
                        + tld.ipAddress()
                        + " | primary="
                        + primary.ipAddress()
                        + " | secondary="
                        + secondary.ipAddress()
                        + " | zone="
                        + zone
                        + " | SOA-serial="
                        + Long.toUnsignedString(
                        primarySnapshot.serial()
                )
                        + " | root-DS="
                        + rootTrustAnchor
        );
    }

    public record Result(
            boolean success,
            String detail
    ) {
        public static Result ok(String detail) {
            return new Result(
                    true,
                    detail
            );
        }

        public static Result fail(String detail) {
            return new Result(
                    false,
                    detail
            );
        }
    }
}
