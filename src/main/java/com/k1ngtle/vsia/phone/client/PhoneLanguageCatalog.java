package com.k1ngtle.vsia.phone.client;

import java.util.List;
import java.util.Locale;

public final class PhoneLanguageCatalog {
    public record LanguageProfile(
            String languageTag,
            String englishName,
            String nativeName,
            String defaultRegionCode
    ) {
        public String displayName() {
            String nativeDisplay =
                    Locale.forLanguageTag(
                            languageTag
                    )
                    .getDisplayLanguage(
                            Locale.forLanguageTag(
                                    languageTag
                            )
                    );

            if (nativeDisplay == null
                    || nativeDisplay.isBlank()
                    || nativeDisplay.equalsIgnoreCase(languageTag)) {
                nativeDisplay = nativeName;
            }

            if (nativeDisplay == null
                    || nativeDisplay.isBlank()
                    || nativeDisplay.equalsIgnoreCase(englishName)) {
                return englishName;
            }

            return nativeDisplay
                    + " — "
                    + englishName;
        }
    }

    private static final List<LanguageProfile> LANGUAGES =
            List.of(
            new LanguageProfile("ab", "Abkhaz", "Abkhaz", "GE"),
            new LanguageProfile("ace", "Acehnese", "Acehnese", "GB"),
            new LanguageProfile("ach", "Acholi", "Acholi", "GB"),
            new LanguageProfile("aa", "Afar", "Afar", "DJ"),
            new LanguageProfile("af", "Afrikaans", "Afrikaans", "ZA"),
            new LanguageProfile("sq", "Albanian", "Albanian", "AL"),
            new LanguageProfile("alz", "Alur", "Alur", "GB"),
            new LanguageProfile("am", "Amharic", "Amharic", "ET"),
            new LanguageProfile("ar", "Arabic", "Arabic", "EH"),
            new LanguageProfile("hy", "Armenian", "Armenian", "AM"),
            new LanguageProfile("as", "Assamese", "Assamese", "IN"),
            new LanguageProfile("av", "Avar", "Avar", "RU"),
            new LanguageProfile("awa", "Awadhi", "Awadhi", "GB"),
            new LanguageProfile("ay", "Aymara", "Aymara", "BO"),
            new LanguageProfile("az", "Azerbaijani", "Azerbaijani", "AZ"),
            new LanguageProfile("ban", "Balinese", "Balinese", "GB"),
            new LanguageProfile("bal", "Baluchi", "Baluchi", "OM"),
            new LanguageProfile("bm", "Bambara", "Bambara", "GB"),
            new LanguageProfile("bci", "Baoulé", "Baoulé", "GB"),
            new LanguageProfile("ba", "Bashkir", "Bashkir", "RU"),
            new LanguageProfile("eu", "Basque", "Basque", "ES"),
            new LanguageProfile("btx", "Batak Karo", "Batak Karo", "GB"),
            new LanguageProfile("bts", "Batak Simalungun", "Batak Simalungun", "GB"),
            new LanguageProfile("bbc", "Batak Toba", "Batak Toba", "GB"),
            new LanguageProfile("be", "Belarusian", "Belarusian", "BY"),
            new LanguageProfile("bem", "Bemba", "Bemba", "GB"),
            new LanguageProfile("bn", "Bengali", "Bengali", "BD"),
            new LanguageProfile("bew", "Betawi", "Betawi", "GB"),
            new LanguageProfile("bho", "Bhojpuri", "Bhojpuri", "MU"),
            new LanguageProfile("bik", "Bikol", "Bikol", "GB"),
            new LanguageProfile("bs", "Bosnian", "Bosnian", "BA"),
            new LanguageProfile("br", "Breton", "Breton", "GB"),
            new LanguageProfile("bg", "Bulgarian", "Bulgarian", "BG"),
            new LanguageProfile("bua", "Buryat", "Buryat", "GB"),
            new LanguageProfile("yue", "Cantonese", "Cantonese", "HK"),
            new LanguageProfile("ca", "Catalan", "Catalan", "AD"),
            new LanguageProfile("ceb", "Cebuano", "Cebuano", "PH"),
            new LanguageProfile("ch", "Chamorro", "Chamorro", "GU"),
            new LanguageProfile("ce", "Chechen", "Chechen", "RU"),
            new LanguageProfile("ny", "Chichewa", "Chichewa", "MW"),
            new LanguageProfile("zh", "Chinese (Simplified)", "Chinese (Simplified)", "CN"),
            new LanguageProfile("zh-TW", "Chinese (Traditional)", "Chinese (Traditional)", "GB"),
            new LanguageProfile("chk", "Chuukese", "Chuukese", "GB"),
            new LanguageProfile("cv", "Chuvash", "Chuvash", "GB"),
            new LanguageProfile("co", "Corsican", "Corsican", "GB"),
            new LanguageProfile("crh", "Crimean Tatar (Cyrillic)", "Crimean Tatar (Cyrillic)", "GB"),
            new LanguageProfile("crh-Latn", "Crimean Tatar (Latin)", "Crimean Tatar (Latin)", "GB"),
            new LanguageProfile("hr", "Croatian", "Croatian", "HR"),
            new LanguageProfile("cs", "Czech", "Czech", "CZ"),
            new LanguageProfile("da", "Danish", "Danish", "DK"),
            new LanguageProfile("fa-AF", "Dari", "Dari", "GB"),
            new LanguageProfile("dv", "Dhivehi", "Dhivehi", "MV"),
            new LanguageProfile("din", "Dinka", "Dinka", "GB"),
            new LanguageProfile("doi", "Dogri", "Dogri", "GB"),
            new LanguageProfile("dov", "Dombe", "Dombe", "GB"),
            new LanguageProfile("nl", "Dutch", "Dutch", "NL"),
            new LanguageProfile("dyu", "Dyula", "Dyula", "GB"),
            new LanguageProfile("dz", "Dzongkha", "Dzongkha", "BT"),
            new LanguageProfile("en", "English", "English", "BB"),
            new LanguageProfile("eo", "Esperanto", "Esperanto", "GB"),
            new LanguageProfile("et", "Estonian", "Estonian", "EE"),
            new LanguageProfile("ee", "Ewe", "Ewe", "GH"),
            new LanguageProfile("fo", "Faroese", "Faroese", "FO"),
            new LanguageProfile("fj", "Fijian", "Fijian", "FJ"),
            new LanguageProfile("tl", "Filipino", "Filipino", "GB"),
            new LanguageProfile("fi", "Finnish", "Finnish", "FI"),
            new LanguageProfile("fon", "Fon", "Fon", "GB"),
            new LanguageProfile("fr", "French", "French", "PM"),
            new LanguageProfile("fr-CA", "French (Canada)", "French (Canada)", "GB"),
            new LanguageProfile("fy", "Frisian", "Frisian", "NL"),
            new LanguageProfile("fur", "Friulian", "Friulian", "GB"),
            new LanguageProfile("ff", "Fulani", "Fulani", "SN"),
            new LanguageProfile("gaa", "Ga", "Ga", "GH"),
            new LanguageProfile("gl", "Galician", "Galician", "ES"),
            new LanguageProfile("ka", "Georgian", "Georgian", "GE"),
            new LanguageProfile("de", "German", "German", "LI"),
            new LanguageProfile("el", "Greek", "Greek", "GR"),
            new LanguageProfile("gn", "Guarani", "Guarani", "PY"),
            new LanguageProfile("gu", "Gujarati", "Gujarati", "IN"),
            new LanguageProfile("ht", "Haitian Creole", "Haitian Creole", "HT"),
            new LanguageProfile("cnh", "Hakha Chin", "Hakha Chin", "GB"),
            new LanguageProfile("ha", "Hausa", "Hausa", "NE"),
            new LanguageProfile("haw", "Hawaiian", "Hawaiian", "US"),
            new LanguageProfile("he", "Hebrew", "Hebrew", "IL"),
            new LanguageProfile("hil", "Hiligaynon", "Hiligaynon", "PH"),
            new LanguageProfile("hi", "Hindi", "Hindi", "IN"),
            new LanguageProfile("hmn", "Hmong", "Hmong", "GB"),
            new LanguageProfile("hu", "Hungarian", "Hungarian", "HU"),
            new LanguageProfile("hrx", "Hunsrik", "Hunsrik", "GB"),
            new LanguageProfile("iba", "Iban", "Iban", "GB"),
            new LanguageProfile("is", "Icelandic", "Icelandic", "IS"),
            new LanguageProfile("ig", "Igbo", "Igbo", "GB"),
            new LanguageProfile("ilo", "Ilocano", "Ilocano", "PH"),
            new LanguageProfile("id", "Indonesian", "Indonesian", "ID"),
            new LanguageProfile("iu-Latn", "Inuktut (Latin)", "Inuktut (Latin)", "CA"),
            new LanguageProfile("iu", "Inuktut (Syllabics)", "Inuktut (Syllabics)", "CA"),
            new LanguageProfile("ga", "Irish", "Irish", "IE"),
            new LanguageProfile("it", "Italian", "Italian", "IT"),
            new LanguageProfile("jam", "Jamaican Patois", "Jamaican Patois", "GB"),
            new LanguageProfile("ja", "Japanese", "Japanese", "JP"),
            new LanguageProfile("jw", "Javanese", "Javanese", "GB"),
            new LanguageProfile("kac", "Jingpo", "Jingpo", "GB"),
            new LanguageProfile("kl", "Kalaallisut", "Kalaallisut", "GL"),
            new LanguageProfile("kn", "Kannada", "Kannada", "IN"),
            new LanguageProfile("kr", "Kanuri", "Kanuri", "GB"),
            new LanguageProfile("pam", "Kapampangan", "Kapampangan", "GB"),
            new LanguageProfile("kk", "Kazakh", "Kazakh", "KZ"),
            new LanguageProfile("kha", "Khasi", "Khasi", "IN"),
            new LanguageProfile("km", "Khmer", "Khmer", "KH"),
            new LanguageProfile("cgg", "Kiga", "Kiga", "GB"),
            new LanguageProfile("kg", "Kikongo", "Kikongo", "CD"),
            new LanguageProfile("rw", "Kinyarwanda", "Kinyarwanda", "RW"),
            new LanguageProfile("ktu", "Kituba", "Kituba", "GB"),
            new LanguageProfile("trp", "Kokborok", "Kokborok", "GB"),
            new LanguageProfile("kv", "Komi", "Komi", "RU"),
            new LanguageProfile("gom", "Konkani", "Konkani", "GB"),
            new LanguageProfile("ko", "Korean", "Korean", "KR"),
            new LanguageProfile("kri", "Krio", "Krio", "GB"),
            new LanguageProfile("ku", "Kurdish (Kurmanji)", "Kurdish (Kurmanji)", "SY"),
            new LanguageProfile("ckb", "Kurdish (Sorani)", "Kurdish (Sorani)", "IQ"),
            new LanguageProfile("ky", "Kyrgyz", "Kyrgyz", "KG"),
            new LanguageProfile("lo", "Lao", "Lao", "LA"),
            new LanguageProfile("ltg", "Latgalian", "Latgalian", "GB"),
            new LanguageProfile("la", "Latin", "Latin", "GB"),
            new LanguageProfile("lv", "Latvian", "Latvian", "LV"),
            new LanguageProfile("lij", "Ligurian", "Ligurian", "GB"),
            new LanguageProfile("li", "Limburgish", "Limburgish", "GB"),
            new LanguageProfile("ln", "Lingala", "Lingala", "CD"),
            new LanguageProfile("lt", "Lithuanian", "Lithuanian", "LT"),
            new LanguageProfile("lmo", "Lombard", "Lombard", "GB"),
            new LanguageProfile("lg", "Luganda", "Luganda", "GB"),
            new LanguageProfile("luo", "Luo", "Luo", "GB"),
            new LanguageProfile("lb", "Luxembourgish", "Luxembourgish", "LU"),
            new LanguageProfile("mk", "Macedonian", "Macedonian", "MK"),
            new LanguageProfile("mad", "Madurese", "Madurese", "GB"),
            new LanguageProfile("mai", "Maithili", "Maithili", "IN"),
            new LanguageProfile("mak", "Makassar", "Makassar", "GB"),
            new LanguageProfile("mg", "Malagasy", "Malagasy", "MG"),
            new LanguageProfile("ms", "Malay", "Malay", "BN"),
            new LanguageProfile("ms-Arab", "Malay (Jawi)", "Malay (Jawi)", "BN"),
            new LanguageProfile("ml", "Malayalam", "Malayalam", "IN"),
            new LanguageProfile("mt", "Maltese", "Maltese", "MT"),
            new LanguageProfile("mam", "Mam", "Mam", "GB"),
            new LanguageProfile("gv", "Manx", "Manx", "IM"),
            new LanguageProfile("mi", "Maori", "Maori", "NZ"),
            new LanguageProfile("mr", "Marathi", "Marathi", "IN"),
            new LanguageProfile("mh", "Marshallese", "Marshallese", "MH"),
            new LanguageProfile("mwr", "Marwadi", "Marwadi", "GB"),
            new LanguageProfile("mfe", "Mauritian Creole", "Mauritian Creole", "GB"),
            new LanguageProfile("chm", "Meadow Mari", "Meadow Mari", "GB"),
            new LanguageProfile("mni-Mtei", "Meiteilon (Manipuri)", "Meiteilon (Manipuri)", "GB"),
            new LanguageProfile("min", "Minang", "Minang", "GB"),
            new LanguageProfile("lus", "Mizo", "Mizo", "GB"),
            new LanguageProfile("mn", "Mongolian", "Mongolian", "MN"),
            new LanguageProfile("my", "Myanmar (Burmese)", "Myanmar (Burmese)", "MM"),
            new LanguageProfile("bm-Nkoo", "NKo", "NKo", "GB"),
            new LanguageProfile("nhe", "Nahuatl (Eastern Huasteca)", "Nahuatl (Eastern Huasteca)", "GB"),
            new LanguageProfile("ndc-ZW", "Ndau", "Ndau", "GB"),
            new LanguageProfile("nr", "Ndebele (South)", "Ndebele (South)", "ZA"),
            new LanguageProfile("new", "Nepalbhasa (Newari)", "Nepalbhasa (Newari)", "GB"),
            new LanguageProfile("ne", "Nepali", "Nepali", "NP"),
            new LanguageProfile("no", "Norwegian", "Norwegian", "NO"),
            new LanguageProfile("nus", "Nuer", "Nuer", "GB"),
            new LanguageProfile("oc", "Occitan", "Occitan", "ES"),
            new LanguageProfile("or", "Odia (Oriya)", "Odia (Oriya)", "IN"),
            new LanguageProfile("om", "Oromo", "Oromo", "ET"),
            new LanguageProfile("os", "Ossetian", "Ossetian", "GE"),
            new LanguageProfile("pag", "Pangasinan", "Pangasinan", "PH"),
            new LanguageProfile("pap", "Papiamento", "Papiamento", "AW"),
            new LanguageProfile("ps", "Pashto", "Pashto", "AF"),
            new LanguageProfile("fa", "Persian", "Persian", "IR"),
            new LanguageProfile("pl", "Polish", "Polish", "PL"),
            new LanguageProfile("pt", "Portuguese (Brazil)", "Portuguese (Brazil)", "GW"),
            new LanguageProfile("pt-PT", "Portuguese (Portugal)", "Portuguese (Portugal)", "GB"),
            new LanguageProfile("pa", "Punjabi (Gurmukhi)", "Punjabi (Gurmukhi)", "IN"),
            new LanguageProfile("pa-Arab", "Punjabi (Shahmukhi)", "Punjabi (Shahmukhi)", "GB"),
            new LanguageProfile("qu", "Quechua", "Quechua", "BO"),
            new LanguageProfile("kek", "Qʼeqchiʼ", "Qʼeqchiʼ", "GB"),
            new LanguageProfile("rom", "Romani", "Romani", "GB"),
            new LanguageProfile("ro", "Romanian", "Romanian", "RO"),
            new LanguageProfile("rn", "Rundi", "Rundi", "BI"),
            new LanguageProfile("ru", "Russian", "Russian", "RU"),
            new LanguageProfile("se", "Sami (North)", "Sami (North)", "NO"),
            new LanguageProfile("sm", "Samoan", "Samoan", "WS"),
            new LanguageProfile("sg", "Sango", "Sango", "CF"),
            new LanguageProfile("sa", "Sanskrit", "Sanskrit", "IN"),
            new LanguageProfile("sat-Latn", "Santali (Latin)", "Santali (Latin)", "GB"),
            new LanguageProfile("sat", "Santali (Ol Chiki)", "Santali (Ol Chiki)", "IN"),
            new LanguageProfile("gd", "Scots Gaelic", "Scots Gaelic", "GB"),
            new LanguageProfile("nso", "Sepedi", "Sepedi", "ZA"),
            new LanguageProfile("sr", "Serbian", "Serbian", "RS"),
            new LanguageProfile("st", "Sesotho", "Sesotho", "LS"),
            new LanguageProfile("crs", "Seychellois Creole", "Seychellois Creole", "GB"),
            new LanguageProfile("shn", "Shan", "Shan", "GB"),
            new LanguageProfile("sn", "Shona", "Shona", "ZW"),
            new LanguageProfile("scn", "Sicilian", "Sicilian", "GB"),
            new LanguageProfile("szl", "Silesian", "Silesian", "GB"),
            new LanguageProfile("sd", "Sindhi", "Sindhi", "IN"),
            new LanguageProfile("si", "Sinhala", "Sinhala", "LK"),
            new LanguageProfile("sk", "Slovak", "Slovak", "SK"),
            new LanguageProfile("sl", "Slovenian", "Slovenian", "SI"),
            new LanguageProfile("so", "Somali", "Somali", "SO"),
            new LanguageProfile("es", "Spanish", "Spanish", "AR"),
            new LanguageProfile("su", "Sundanese", "Sundanese", "GB"),
            new LanguageProfile("sus", "Susu", "Susu", "GB"),
            new LanguageProfile("sw", "Swahili", "Swahili", "TZ"),
            new LanguageProfile("ss", "Swati", "Swati", "SZ"),
            new LanguageProfile("sv", "Swedish", "Swedish", "AX"),
            new LanguageProfile("ty", "Tahitian", "Tahitian", "PF"),
            new LanguageProfile("tg", "Tajik", "Tajik", "TJ"),
            new LanguageProfile("ber-Latn", "Tamazight", "Tamazight", "GB"),
            new LanguageProfile("ber", "Tamazight (Tifinagh)", "Tamazight (Tifinagh)", "GB"),
            new LanguageProfile("ta", "Tamil", "Tamil", "LK"),
            new LanguageProfile("tt", "Tatar", "Tatar", "RU"),
            new LanguageProfile("te", "Telugu", "Telugu", "IN"),
            new LanguageProfile("tet", "Tetum", "Tetum", "TL"),
            new LanguageProfile("th", "Thai", "Thai", "TH"),
            new LanguageProfile("bo", "Tibetan", "Tibetan", "CN"),
            new LanguageProfile("ti", "Tigrinya", "Tigrinya", "ER"),
            new LanguageProfile("tiv", "Tiv", "Tiv", "GB"),
            new LanguageProfile("tpi", "Tok Pisin", "Tok Pisin", "PG"),
            new LanguageProfile("to", "Tongan", "Tongan", "TO"),
            new LanguageProfile("lua", "Tshiluba", "Tshiluba", "CD"),
            new LanguageProfile("ts", "Tsonga", "Tsonga", "ZA"),
            new LanguageProfile("tn", "Tswana", "Tswana", "BW"),
            new LanguageProfile("tcy", "Tulu", "Tulu", "GB"),
            new LanguageProfile("tum", "Tumbuka", "Tumbuka", "MW"),
            new LanguageProfile("tr", "Turkish", "Turkish", "TR"),
            new LanguageProfile("tk", "Turkmen", "Turkmen", "TM"),
            new LanguageProfile("tyv", "Tuvan", "Tuvan", "RU"),
            new LanguageProfile("ak", "Twi", "Twi", "GH"),
            new LanguageProfile("udm", "Udmurt", "Udmurt", "RU"),
            new LanguageProfile("uk", "Ukrainian", "Ukrainian", "UA"),
            new LanguageProfile("ur", "Urdu", "Urdu", "PK"),
            new LanguageProfile("ug", "Uyghur", "Uyghur", "CN"),
            new LanguageProfile("uz", "Uzbek", "Uzbek", "UZ"),
            new LanguageProfile("ve", "Venda", "Venda", "ZA"),
            new LanguageProfile("vec", "Venetian", "Venetian", "SI"),
            new LanguageProfile("vi", "Vietnamese", "Vietnamese", "VN"),
            new LanguageProfile("war", "Waray", "Waray", "PH"),
            new LanguageProfile("cy", "Welsh", "Welsh", "GB"),
            new LanguageProfile("wo", "Wolof", "Wolof", "SN"),
            new LanguageProfile("xh", "Xhosa", "Xhosa", "ZA"),
            new LanguageProfile("sah", "Yakut", "Yakut", "RU"),
            new LanguageProfile("yi", "Yiddish", "Yiddish", "IL"),
            new LanguageProfile("yo", "Yoruba", "Yoruba", "NG"),
            new LanguageProfile("yua", "Yucatec Maya", "Yucatec Maya", "GB"),
            new LanguageProfile("zap", "Zapotec", "Zapotec", "GB"),
            new LanguageProfile("zu", "Zulu", "Zulu", "ZA")
            );

    private PhoneLanguageCatalog() {
    }

    public static List<LanguageProfile> all() {
        return LANGUAGES;
    }

    public static LanguageProfile byTag(String tag) {
        if (tag != null) {
            for (LanguageProfile profile : LANGUAGES) {
                if (profile.languageTag()
                        .equalsIgnoreCase(tag)) {
                    return profile;
                }
            }
        }

        return englishUk();
    }

    public static LanguageProfile englishUk() {
        for (LanguageProfile profile : LANGUAGES) {
            if (profile.languageTag()
                    .equalsIgnoreCase("en")) {
                return profile;
            }
        }

        return LANGUAGES.get(0);
    }

    public static int size() {
        return LANGUAGES.size();
    }
}
