package com.k1ngtle.vsia.phone.client;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PhoneLocaleSettings {
    public enum Language {
        ENGLISH_UK("English (UK)"),
        ENGLISH_US("English (US)"),
        VIETNAMESE("Tiếng Việt");

        private final String displayName;

        Language(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public enum Region {
        UNITED_KINGDOM("United Kingdom"),
        UNITED_STATES("United States"),
        VIETNAM("Vietnam");

        private final String displayName;

        Region(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public enum TemperatureUnit {
        CELSIUS("Celsius"),
        FAHRENHEIT("Fahrenheit");

        private final String displayName;

        TemperatureUnit(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public enum MeasurementSystem {
        METRIC("Metric"),
        UK("UK"),
        US("US");

        private final String displayName;

        MeasurementSystem(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public enum FirstDayOfWeek {
        MONDAY("Monday"),
        SUNDAY("Sunday");

        private final String displayName;

        FirstDayOfWeek(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private static Language language = Language.ENGLISH_UK;
    private static Region region = Region.UNITED_KINGDOM;
    private static TemperatureUnit temperatureUnit =
            TemperatureUnit.CELSIUS;
    private static MeasurementSystem measurementSystem =
            MeasurementSystem.UK;
    private static FirstDayOfWeek firstDayOfWeek =
            FirstDayOfWeek.MONDAY;

    private PhoneLocaleSettings() {
    }

    public static Language language() {
        return language;
    }

    public static void cycleLanguage() {
        language = switch (language) {
            case ENGLISH_UK -> Language.ENGLISH_US;
            case ENGLISH_US -> Language.VIETNAMESE;
            case VIETNAMESE -> Language.ENGLISH_UK;
        };
    }

    public static Region region() {
        return region;
    }

    public static void cycleRegion() {
        setRegion(
                switch (region) {
                    case UNITED_KINGDOM -> Region.UNITED_STATES;
                    case UNITED_STATES -> Region.VIETNAM;
                    case VIETNAM -> Region.UNITED_KINGDOM;
                }
        );
    }

    public static void setRegion(Region value) {
        region = value == null
                ? Region.UNITED_KINGDOM
                : value;

        switch (region) {
            case UNITED_KINGDOM -> {
                temperatureUnit = TemperatureUnit.CELSIUS;
                measurementSystem = MeasurementSystem.UK;
                firstDayOfWeek = FirstDayOfWeek.MONDAY;
            }

            case UNITED_STATES -> {
                temperatureUnit = TemperatureUnit.FAHRENHEIT;
                measurementSystem = MeasurementSystem.US;
                firstDayOfWeek = FirstDayOfWeek.SUNDAY;
            }

            case VIETNAM -> {
                temperatureUnit = TemperatureUnit.CELSIUS;
                measurementSystem = MeasurementSystem.METRIC;
                firstDayOfWeek = FirstDayOfWeek.MONDAY;
            }
        }
    }

    public static TemperatureUnit temperatureUnit() {
        return temperatureUnit;
    }

    public static void cycleTemperatureUnit() {
        temperatureUnit =
                temperatureUnit == TemperatureUnit.CELSIUS
                        ? TemperatureUnit.FAHRENHEIT
                        : TemperatureUnit.CELSIUS;
    }

    public static MeasurementSystem measurementSystem() {
        return measurementSystem;
    }

    public static void cycleMeasurementSystem() {
        measurementSystem =
                switch (measurementSystem) {
                    case METRIC -> MeasurementSystem.UK;
                    case UK -> MeasurementSystem.US;
                    case US -> MeasurementSystem.METRIC;
                };
    }

    public static FirstDayOfWeek firstDayOfWeek() {
        return firstDayOfWeek;
    }

    public static void cycleFirstDayOfWeek() {
        firstDayOfWeek =
                firstDayOfWeek == FirstDayOfWeek.MONDAY
                        ? FirstDayOfWeek.SUNDAY
                        : FirstDayOfWeek.MONDAY;
    }

    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }

        return switch (region) {
            case UNITED_STATES ->
                    date.format(
                            DateTimeFormatter.ofPattern(
                                    "MMM d, yyyy",
                                    Locale.US
                            )
                    );

            case VIETNAM ->
                    date.format(
                            DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy",
                                    Locale.ROOT
                            )
                    );

            case UNITED_KINGDOM ->
                    date.format(
                            DateTimeFormatter.ofPattern(
                                    "d MMM yyyy",
                                    Locale.UK
                            )
                    );
        };
    }

    public static String formatTemperature(double celsius) {
        if (temperatureUnit == TemperatureUnit.FAHRENHEIT) {
            long value = Math.round(
                    celsius * 9.0 / 5.0 + 32.0
            );

            return value + "°";
        }

        return Math.round(celsius) + "°";
    }


    public static String partlyCloudyLabel() {
        return language == Language.VIETNAMESE
                ? "Có mây"
                : "Partly Cloudy";
    }

    public static String currentPositionLabel() {
        return language == Language.VIETNAMESE
                ? "Vị trí hiện tại"
                : "Current Position";
    }

    public static void resetToDefaults() {
        language = Language.ENGLISH_UK;
        setRegion(Region.UNITED_KINGDOM);
    }
}
