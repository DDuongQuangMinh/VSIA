package com.k1ngtle.vsia.signality.internet.satellite;

public enum SatelliteBandPreset {
    L(
            1_626_500_000.0,
            1_525_000_000.0,
            1_000_000.0,
            20.0,
            40.0,
            3.0,
            16.0,
            3.0,
            3.0,
            0.20
    ),

    KU(
            14_250_000_000.0,
            12_000_000_000.0,
            36_000_000.0,
            10.0,
            80.0,
            35.0,
            30.0,
            2.5,
            2.5,
            0.80
    ),

    KA(
            29_500_000_000.0,
            19_700_000_000.0,
            100_000_000.0,
            5.0,
            120.0,
            42.0,
            38.0,
            2.0,
            2.0,
            1.50
    );

    private final double uplinkHz;
    private final double downlinkHz;
    private final double bandwidthHz;
    private final double terminalPowerWatts;
    private final double satellitePowerWatts;
    private final double terminalGainDbi;
    private final double satelliteGainDbi;
    private final double terminalNoiseFigureDb;
    private final double satelliteNoiseFigureDb;
    private final double zenithAtmosphericLossDb;

    SatelliteBandPreset(
            double uplinkHz,
            double downlinkHz,
            double bandwidthHz,
            double terminalPowerWatts,
            double satellitePowerWatts,
            double terminalGainDbi,
            double satelliteGainDbi,
            double terminalNoiseFigureDb,
            double satelliteNoiseFigureDb,
            double zenithAtmosphericLossDb
    ) {
        this.uplinkHz = uplinkHz;
        this.downlinkHz = downlinkHz;
        this.bandwidthHz = bandwidthHz;
        this.terminalPowerWatts = terminalPowerWatts;
        this.satellitePowerWatts = satellitePowerWatts;
        this.terminalGainDbi = terminalGainDbi;
        this.satelliteGainDbi = satelliteGainDbi;
        this.terminalNoiseFigureDb = terminalNoiseFigureDb;
        this.satelliteNoiseFigureDb = satelliteNoiseFigureDb;
        this.zenithAtmosphericLossDb = zenithAtmosphericLossDb;
    }

    public double uplinkHz() {
        return uplinkHz;
    }

    public double downlinkHz() {
        return downlinkHz;
    }

    public double bandwidthHz() {
        return bandwidthHz;
    }

    public double terminalPowerWatts() {
        return terminalPowerWatts;
    }

    public double satellitePowerWatts() {
        return satellitePowerWatts;
    }

    public double terminalGainDbi() {
        return terminalGainDbi;
    }

    public double satelliteGainDbi() {
        return satelliteGainDbi;
    }

    public double terminalNoiseFigureDb() {
        return terminalNoiseFigureDb;
    }

    public double satelliteNoiseFigureDb() {
        return satelliteNoiseFigureDb;
    }

    public double zenithAtmosphericLossDb() {
        return zenithAtmosphericLossDb;
    }

    public SatelliteBandPreset next() {
        SatelliteBandPreset[] values =
                values();

        return values[
                (ordinal() + 1)
                        % values.length
                ];
    }
}
