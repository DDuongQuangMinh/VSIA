package com.k1ngtle.vsia.signality.engineering.wifi.integration.w1222;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiMacController;

import java.util.ArrayList;
import java.util.List;

public final class W1222RoamingUnitTestSuite {
    public record Result(
            String name,
            boolean passed,
            String detail
    ) {
    }

    private W1222RoamingUnitTestSuite() {
    }

    public static List<Result> runAll() {
        List<Result> results =
                new ArrayList<>();

        results.add(
                oldApStationRelease()
        );

        results.add(
                unknownStationReleaseIsSafe()
        );

        return List.copyOf(
                results
        );
    }

    private static Result oldApStationRelease() {
        String station =
                "02:11:22:33:44:55";

        WifiMacController ap =
                new WifiMacController();

        ap.configureAccessPoint(
                "VSIA-ROAM",
                "signality:open",
                ""
        );

        ap.provisionLabAssociatedStation(
                station
        );

        boolean presentBefore =
                ap.associatedStations()
                        .stream()
                        .anyMatch(
                                value ->
                                        value.equalsIgnoreCase(
                                                station
                                        )
                        );

        boolean released =
                ap.disassociateStationForRoam(
                        station
                );

        boolean absentAfter =
                ap.associatedStations()
                        .stream()
                        .noneMatch(
                                value ->
                                        value.equalsIgnoreCase(
                                                station
                                        )
                        );

        boolean passed =
                presentBefore
                        && released
                        && absentAfter;

        return new Result(
                "w1222-old-ap-station-release",
                passed,
                passed
                        ? "Roam cleanup removes the STA from the old AP association set"
                        : "Expected associated STA to be removed from old AP state"
        );
    }

    private static Result unknownStationReleaseIsSafe() {
        WifiMacController ap =
                new WifiMacController();

        ap.configureAccessPoint(
                "VSIA-ROAM",
                "signality:open",
                ""
        );

        boolean released =
                ap.disassociateStationForRoam(
                        "02:AA:BB:CC:DD:EE"
                );

        boolean passed =
                !released
                        && ap.associatedStations()
                        .isEmpty();

        return new Result(
                "w1222-unknown-station-release-safe",
                passed,
                passed
                        ? "Releasing a non-associated STA is a safe no-op"
                        : "Unknown station release unexpectedly mutated AP association state"
        );
    }
}
