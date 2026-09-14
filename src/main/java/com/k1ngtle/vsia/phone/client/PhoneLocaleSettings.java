package com.k1ngtle.vsia.phone.client;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PhoneLocaleSettings {
    public enum Language {
        ENGLISH_UK("en", "GB", "English (UK)"),
        ENGLISH_US("en", "US", "English (US)"),
        VIETNAMESE("vi", "VN", "Tiếng Việt");

        private final String languageCode;
        private final String defaultCountryCode;
        private final String displayName;

        Language(
                String languageCode,
                String defaultCountryCode,
                String displayName
        ) {
            this.languageCode = languageCode;
            this.defaultCountryCode = defaultCountryCode;
            this.displayName = displayName;
        }

        public String languageCode() {
            return languageCode;
        }

        public String defaultCountryCode() {
            return defaultCountryCode;
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
            return PhoneI18n.translate(displayName);
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
            return PhoneI18n.translate(displayName);
        }
    }

    public enum FirstDayOfWeek {
        MONDAY("Monday"),
        TUESDAY("Tuesday"),
        WEDNESDAY("Wednesday"),
        THURSDAY("Thursday"),
        FRIDAY("Friday"),
        SATURDAY("Saturday"),
        SUNDAY("Sunday");

        private final String displayName;

        FirstDayOfWeek(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return PhoneI18n.translate(displayName);
        }
    }

    public record RegionOption(
            String code,
            String displayName,
            String timeZoneId
    ) {
    }

    private static final Map<String, String> REGION_TIME_ZONES =
            Map.ofEntries(
            Map.entry("AD", "Europe/Andorra"),
            Map.entry("AE", "Asia/Dubai"),
            Map.entry("AF", "Asia/Kabul"),
            Map.entry("AG", "America/Antigua"),
            Map.entry("AI", "America/Anguilla"),
            Map.entry("AL", "Europe/Tirane"),
            Map.entry("AM", "Asia/Yerevan"),
            Map.entry("AO", "Africa/Luanda"),
            Map.entry("AQ", "Antarctica/McMurdo"),
            Map.entry("AR", "America/Argentina/Buenos_Aires"),
            Map.entry("AS", "Pacific/Pago_Pago"),
            Map.entry("AT", "Europe/Vienna"),
            Map.entry("AU", "Australia/Sydney"),
            Map.entry("AW", "America/Aruba"),
            Map.entry("AX", "Europe/Mariehamn"),
            Map.entry("AZ", "Asia/Baku"),
            Map.entry("BA", "Europe/Sarajevo"),
            Map.entry("BB", "America/Barbados"),
            Map.entry("BD", "Asia/Dhaka"),
            Map.entry("BE", "Europe/Brussels"),
            Map.entry("BF", "Africa/Ouagadougou"),
            Map.entry("BG", "Europe/Sofia"),
            Map.entry("BH", "Asia/Bahrain"),
            Map.entry("BI", "Africa/Bujumbura"),
            Map.entry("BJ", "Africa/Porto-Novo"),
            Map.entry("BL", "America/St_Barthelemy"),
            Map.entry("BM", "Atlantic/Bermuda"),
            Map.entry("BN", "Asia/Brunei"),
            Map.entry("BO", "America/La_Paz"),
            Map.entry("BQ", "America/Kralendijk"),
            Map.entry("BR", "America/Sao_Paulo"),
            Map.entry("BS", "America/Nassau"),
            Map.entry("BT", "Asia/Thimphu"),
            Map.entry("BV", "Etc/UTC"),
            Map.entry("BW", "Africa/Gaborone"),
            Map.entry("BY", "Europe/Minsk"),
            Map.entry("BZ", "America/Belize"),
            Map.entry("CA", "America/Toronto"),
            Map.entry("CC", "Indian/Cocos"),
            Map.entry("CD", "Africa/Kinshasa"),
            Map.entry("CF", "Africa/Bangui"),
            Map.entry("CG", "Africa/Brazzaville"),
            Map.entry("CH", "Europe/Zurich"),
            Map.entry("CI", "Africa/Abidjan"),
            Map.entry("CK", "Pacific/Rarotonga"),
            Map.entry("CL", "America/Santiago"),
            Map.entry("CM", "Africa/Douala"),
            Map.entry("CN", "Asia/Shanghai"),
            Map.entry("CO", "America/Bogota"),
            Map.entry("CR", "America/Costa_Rica"),
            Map.entry("CU", "America/Havana"),
            Map.entry("CV", "Atlantic/Cape_Verde"),
            Map.entry("CW", "America/Curacao"),
            Map.entry("CX", "Indian/Christmas"),
            Map.entry("CY", "Asia/Nicosia"),
            Map.entry("CZ", "Europe/Prague"),
            Map.entry("DE", "Europe/Berlin"),
            Map.entry("DJ", "Africa/Djibouti"),
            Map.entry("DK", "Europe/Copenhagen"),
            Map.entry("DM", "America/Dominica"),
            Map.entry("DO", "America/Santo_Domingo"),
            Map.entry("DZ", "Africa/Algiers"),
            Map.entry("EC", "America/Guayaquil"),
            Map.entry("EE", "Europe/Tallinn"),
            Map.entry("EG", "Africa/Cairo"),
            Map.entry("EH", "Africa/El_Aaiun"),
            Map.entry("ER", "Africa/Asmara"),
            Map.entry("ES", "Europe/Madrid"),
            Map.entry("ET", "Africa/Addis_Ababa"),
            Map.entry("FI", "Europe/Helsinki"),
            Map.entry("FJ", "Pacific/Fiji"),
            Map.entry("FK", "Atlantic/Stanley"),
            Map.entry("FM", "Pacific/Chuuk"),
            Map.entry("FO", "Atlantic/Faroe"),
            Map.entry("FR", "Europe/Paris"),
            Map.entry("GA", "Africa/Libreville"),
            Map.entry("GB", "Europe/London"),
            Map.entry("GD", "America/Grenada"),
            Map.entry("GE", "Asia/Tbilisi"),
            Map.entry("GF", "America/Cayenne"),
            Map.entry("GG", "Europe/Guernsey"),
            Map.entry("GH", "Africa/Accra"),
            Map.entry("GI", "Europe/Gibraltar"),
            Map.entry("GL", "America/Nuuk"),
            Map.entry("GM", "Africa/Banjul"),
            Map.entry("GN", "Africa/Conakry"),
            Map.entry("GP", "America/Guadeloupe"),
            Map.entry("GQ", "Africa/Malabo"),
            Map.entry("GR", "Europe/Athens"),
            Map.entry("GS", "Atlantic/South_Georgia"),
            Map.entry("GT", "America/Guatemala"),
            Map.entry("GU", "Pacific/Guam"),
            Map.entry("GW", "Africa/Bissau"),
            Map.entry("GY", "America/Guyana"),
            Map.entry("HK", "Asia/Hong_Kong"),
            Map.entry("HM", "Indian/Kerguelen"),
            Map.entry("HN", "America/Tegucigalpa"),
            Map.entry("HR", "Europe/Zagreb"),
            Map.entry("HT", "America/Port-au-Prince"),
            Map.entry("HU", "Europe/Budapest"),
            Map.entry("ID", "Asia/Jakarta"),
            Map.entry("IE", "Europe/Dublin"),
            Map.entry("IL", "Asia/Jerusalem"),
            Map.entry("IM", "Europe/Isle_of_Man"),
            Map.entry("IN", "Asia/Kolkata"),
            Map.entry("IO", "Indian/Chagos"),
            Map.entry("IQ", "Asia/Baghdad"),
            Map.entry("IR", "Asia/Tehran"),
            Map.entry("IS", "Atlantic/Reykjavik"),
            Map.entry("IT", "Europe/Rome"),
            Map.entry("JE", "Europe/Jersey"),
            Map.entry("JM", "America/Jamaica"),
            Map.entry("JO", "Asia/Amman"),
            Map.entry("JP", "Asia/Tokyo"),
            Map.entry("KE", "Africa/Nairobi"),
            Map.entry("KG", "Asia/Bishkek"),
            Map.entry("KH", "Asia/Phnom_Penh"),
            Map.entry("KI", "Pacific/Tarawa"),
            Map.entry("KM", "Indian/Comoro"),
            Map.entry("KN", "America/St_Kitts"),
            Map.entry("KP", "Asia/Pyongyang"),
            Map.entry("KR", "Asia/Seoul"),
            Map.entry("KW", "Asia/Kuwait"),
            Map.entry("KY", "America/Cayman"),
            Map.entry("KZ", "Asia/Almaty"),
            Map.entry("LA", "Asia/Vientiane"),
            Map.entry("LB", "Asia/Beirut"),
            Map.entry("LC", "America/St_Lucia"),
            Map.entry("LI", "Europe/Vaduz"),
            Map.entry("LK", "Asia/Colombo"),
            Map.entry("LR", "Africa/Monrovia"),
            Map.entry("LS", "Africa/Maseru"),
            Map.entry("LT", "Europe/Vilnius"),
            Map.entry("LU", "Europe/Luxembourg"),
            Map.entry("LV", "Europe/Riga"),
            Map.entry("LY", "Africa/Tripoli"),
            Map.entry("MA", "Africa/Casablanca"),
            Map.entry("MC", "Europe/Monaco"),
            Map.entry("MD", "Europe/Chisinau"),
            Map.entry("ME", "Europe/Podgorica"),
            Map.entry("MF", "America/Marigot"),
            Map.entry("MG", "Indian/Antananarivo"),
            Map.entry("MH", "Pacific/Majuro"),
            Map.entry("MK", "Europe/Skopje"),
            Map.entry("ML", "Africa/Bamako"),
            Map.entry("MM", "Asia/Yangon"),
            Map.entry("MN", "Asia/Ulaanbaatar"),
            Map.entry("MO", "Asia/Macau"),
            Map.entry("MP", "Pacific/Saipan"),
            Map.entry("MQ", "America/Martinique"),
            Map.entry("MR", "Africa/Nouakchott"),
            Map.entry("MS", "America/Montserrat"),
            Map.entry("MT", "Europe/Malta"),
            Map.entry("MU", "Indian/Mauritius"),
            Map.entry("MV", "Indian/Maldives"),
            Map.entry("MW", "Africa/Blantyre"),
            Map.entry("MX", "America/Mexico_City"),
            Map.entry("MY", "Asia/Kuala_Lumpur"),
            Map.entry("MZ", "Africa/Maputo"),
            Map.entry("NA", "Africa/Windhoek"),
            Map.entry("NC", "Pacific/Noumea"),
            Map.entry("NE", "Africa/Niamey"),
            Map.entry("NF", "Pacific/Norfolk"),
            Map.entry("NG", "Africa/Lagos"),
            Map.entry("NI", "America/Managua"),
            Map.entry("NL", "Europe/Amsterdam"),
            Map.entry("NO", "Europe/Oslo"),
            Map.entry("NP", "Asia/Kathmandu"),
            Map.entry("NR", "Pacific/Nauru"),
            Map.entry("NU", "Pacific/Niue"),
            Map.entry("NZ", "Pacific/Auckland"),
            Map.entry("OM", "Asia/Muscat"),
            Map.entry("PA", "America/Panama"),
            Map.entry("PE", "America/Lima"),
            Map.entry("PF", "Pacific/Tahiti"),
            Map.entry("PG", "Pacific/Port_Moresby"),
            Map.entry("PH", "Asia/Manila"),
            Map.entry("PK", "Asia/Karachi"),
            Map.entry("PL", "Europe/Warsaw"),
            Map.entry("PM", "America/Miquelon"),
            Map.entry("PN", "Pacific/Pitcairn"),
            Map.entry("PR", "America/Puerto_Rico"),
            Map.entry("PS", "Asia/Gaza"),
            Map.entry("PT", "Europe/Lisbon"),
            Map.entry("PW", "Pacific/Palau"),
            Map.entry("PY", "America/Asuncion"),
            Map.entry("QA", "Asia/Qatar"),
            Map.entry("RE", "Indian/Reunion"),
            Map.entry("RO", "Europe/Bucharest"),
            Map.entry("RS", "Europe/Belgrade"),
            Map.entry("RU", "Europe/Moscow"),
            Map.entry("RW", "Africa/Kigali"),
            Map.entry("SA", "Asia/Riyadh"),
            Map.entry("SB", "Pacific/Guadalcanal"),
            Map.entry("SC", "Indian/Mahe"),
            Map.entry("SD", "Africa/Khartoum"),
            Map.entry("SE", "Europe/Stockholm"),
            Map.entry("SG", "Asia/Singapore"),
            Map.entry("SH", "Atlantic/St_Helena"),
            Map.entry("SI", "Europe/Ljubljana"),
            Map.entry("SJ", "Arctic/Longyearbyen"),
            Map.entry("SK", "Europe/Bratislava"),
            Map.entry("SL", "Africa/Freetown"),
            Map.entry("SM", "Europe/San_Marino"),
            Map.entry("SN", "Africa/Dakar"),
            Map.entry("SO", "Africa/Mogadishu"),
            Map.entry("SR", "America/Paramaribo"),
            Map.entry("SS", "Africa/Juba"),
            Map.entry("ST", "Africa/Sao_Tome"),
            Map.entry("SV", "America/El_Salvador"),
            Map.entry("SX", "America/Lower_Princes"),
            Map.entry("SY", "Asia/Damascus"),
            Map.entry("SZ", "Africa/Mbabane"),
            Map.entry("TC", "America/Grand_Turk"),
            Map.entry("TD", "Africa/Ndjamena"),
            Map.entry("TF", "Indian/Kerguelen"),
            Map.entry("TG", "Africa/Lome"),
            Map.entry("TH", "Asia/Bangkok"),
            Map.entry("TJ", "Asia/Dushanbe"),
            Map.entry("TK", "Pacific/Fakaofo"),
            Map.entry("TL", "Asia/Dili"),
            Map.entry("TM", "Asia/Ashgabat"),
            Map.entry("TN", "Africa/Tunis"),
            Map.entry("TO", "Pacific/Tongatapu"),
            Map.entry("TR", "Europe/Istanbul"),
            Map.entry("TT", "America/Port_of_Spain"),
            Map.entry("TV", "Pacific/Funafuti"),
            Map.entry("TW", "Asia/Taipei"),
            Map.entry("TZ", "Africa/Dar_es_Salaam"),
            Map.entry("UA", "Europe/Kyiv"),
            Map.entry("UG", "Africa/Kampala"),
            Map.entry("UM", "Pacific/Midway"),
            Map.entry("US", "America/New_York"),
            Map.entry("UY", "America/Montevideo"),
            Map.entry("UZ", "Asia/Samarkand"),
            Map.entry("VA", "Europe/Vatican"),
            Map.entry("VC", "America/St_Vincent"),
            Map.entry("VE", "America/Caracas"),
            Map.entry("VG", "America/Tortola"),
            Map.entry("VI", "America/St_Thomas"),
            Map.entry("VN", "Asia/Ho_Chi_Minh"),
            Map.entry("VU", "Pacific/Efate"),
            Map.entry("WF", "Pacific/Wallis"),
            Map.entry("WS", "Pacific/Apia"),
            Map.entry("YE", "Asia/Aden"),
            Map.entry("YT", "Indian/Mayotte"),
            Map.entry("ZA", "Africa/Johannesburg"),
            Map.entry("ZM", "Africa/Lusaka"),
            Map.entry("ZW", "Africa/Harare")
            );

    private static Language language =
            Language.ENGLISH_UK;

    private static String regionCode = "GB";

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

    public static void setLanguage(Language value) {
        language = value == null
                ? Language.ENGLISH_UK
                : value;
    }

    public static List<Language> availableLanguages() {
        return List.of(Language.values());
    }

    public static String regionCode() {
        return regionCode;
    }

    public static String regionDisplayName() {
        return displayCountry(regionCode);
    }

    public static void setRegionCode(String code) {
        String normalized =
                code == null
                        ? "GB"
                        : code.trim()
                        .toUpperCase(Locale.ROOT);

        if (!REGION_TIME_ZONES.containsKey(normalized)) {
            normalized = "GB";
        }

        regionCode = normalized;
        applyRegionalDefaults();

        PhoneSystemSettings.setUse24HourTime(
                regionUses24HourTime()
        );
    }

    public static List<RegionOption> availableRegions() {
        Locale displayLocale = uiLocale();
        List<RegionOption> result =
                new ArrayList<>();

        for (String code : Locale.getISOCountries()) {
            String name =
                    new Locale("", code)
                            .getDisplayCountry(displayLocale);

            if (name == null || name.isBlank()) {
                name = code;
            }

            result.add(
                    new RegionOption(
                            code,
                            name,
                            timeZoneIdFor(code)
                    )
            );
        }

        result.sort(
                Comparator.comparing(
                        RegionOption::displayName,
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        return List.copyOf(result);
    }

    public static String displayCountry(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }

        String value =
                new Locale("", code)
                        .getDisplayCountry(uiLocale());

        return value == null || value.isBlank()
                ? code
                : value;
    }

    public static Locale uiLocale() {
        return new Locale(
                language.languageCode(),
                regionCode
        );
    }

    public static Locale regionLocale() {
        return new Locale(
                language.languageCode(),
                regionCode
        );
    }

    public static String timeZoneId() {
        return timeZoneIdFor(regionCode);
    }

    public static ZoneId regionZoneId() {
        try {
            return ZoneId.of(
                    timeZoneId()
            );
        } catch (Exception ignored) {
            return ZoneId.of("UTC");
        }
    }

    public static ZonedDateTime currentDateTime() {
        return ZonedDateTime.now(
                regionZoneId()
        );
    }

    public static LocalDate currentDate() {
        return currentDateTime().toLocalDate();
    }

    public static LocalTime currentTime() {
        return currentDateTime().toLocalTime();
    }

    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }

        return date.format(
                DateTimeFormatter
                        .ofLocalizedDate(
                                FormatStyle.MEDIUM
                        )
                        .withLocale(
                                regionLocale()
                        )
        );
    }

    public static String formatTime(
            LocalTime time,
            boolean use24Hour
    ) {
        if (time == null) {
            return "";
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        use24Hour
                                ? "HH:mm"
                                : "h:mm a",
                        regionLocale()
                );

        return time.format(formatter);
    }

    public static boolean regionUses24HourTime() {
        try {
            String pattern =
                    DateTimeFormatterBuilder
                            .getLocalizedDateTimePattern(
                                    null,
                                    FormatStyle.SHORT,
                                    IsoChronology.INSTANCE,
                                    regionLocale()
                            );

            return !pattern.contains("a");
        } catch (Exception ignored) {
            return true;
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
        FirstDayOfWeek[] values =
                FirstDayOfWeek.values();

        int next =
                (firstDayOfWeek.ordinal() + 1)
                        % values.length;

        firstDayOfWeek = values[next];
    }

    public static String calendarWeekdayShort() {
        return currentDate()
                .getDayOfWeek()
                .getDisplayName(
                        TextStyle.SHORT,
                        regionLocale()
                );
    }

    public static String calendarDayOfMonth() {
        return Integer.toString(
                currentDate().getDayOfMonth()
        );
    }

    public static String formatTemperature(double celsius) {
        if (temperatureUnit == TemperatureUnit.FAHRENHEIT) {
            long value =
                    Math.round(
                            celsius * 9.0 / 5.0 + 32.0
                    );

            return value + "°";
        }

        return Math.round(celsius) + "°";
    }

    public static String partlyCloudyLabel() {
        return PhoneI18n.translate("Partly Cloudy");
    }

    public static String currentPositionLabel() {
        return PhoneI18n.translate("Current Position");
    }

    public static void resetToDefaults() {
        language = Language.ENGLISH_UK;
        setRegionCode("GB");
    }

    private static String timeZoneIdFor(String code) {
        return REGION_TIME_ZONES
                .getOrDefault(
                        code,
                        "UTC"
                );
    }

    private static void applyRegionalDefaults() {
        temperatureUnit =
                usesFahrenheitByDefault(regionCode)
                        ? TemperatureUnit.FAHRENHEIT
                        : TemperatureUnit.CELSIUS;

        measurementSystem =
                switch (regionCode) {
                    case "US", "LR", "MM" ->
                            MeasurementSystem.US;

                    case "GB" ->
                            MeasurementSystem.UK;

                    default ->
                            MeasurementSystem.METRIC;
                };

        DayOfWeek first =
                WeekFields
                        .of(
                                new Locale(
                                        "",
                                        regionCode
                                )
                        )
                        .getFirstDayOfWeek();

        firstDayOfWeek =
                switch (first) {
                    case MONDAY ->
                            FirstDayOfWeek.MONDAY;
                    case TUESDAY ->
                            FirstDayOfWeek.TUESDAY;
                    case WEDNESDAY ->
                            FirstDayOfWeek.WEDNESDAY;
                    case THURSDAY ->
                            FirstDayOfWeek.THURSDAY;
                    case FRIDAY ->
                            FirstDayOfWeek.FRIDAY;
                    case SATURDAY ->
                            FirstDayOfWeek.SATURDAY;
                    case SUNDAY ->
                            FirstDayOfWeek.SUNDAY;
                };
    }

    private static boolean usesFahrenheitByDefault(
            String code
    ) {
        return switch (code) {
            case "US", "BS", "BZ", "KY",
                 "PW", "FM", "MH", "LR" ->
                    true;

            default ->
                    false;
        };
    }
}
