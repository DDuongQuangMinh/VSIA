package com.k1ngtle.vsia.phone.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PhoneAutoTranslationService {
    private static final String ENDPOINT =
            "https://translate.googleapis.com/translate_a/single"
                    + "?client=gtx"
                    + "&sl=en"
                    + "&dt=t"
                    + "&tl=%s"
                    + "&q=%s";

    private static final HttpClient HTTP =
            HttpClient.newBuilder()
                    .connectTimeout(
                            Duration.ofSeconds(8L)
                    )
                    .build();

    private static final ExecutorService DEMAND_WORKER =
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread =
                                new Thread(
                                        runnable,
                                        "VSIA-Phone-Translation-Demand"
                                );

                        thread.setDaemon(true);
                        return thread;
                    }
            );

    private static final ExecutorService PREFETCH_WORKER =
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread =
                                new Thread(
                                        runnable,
                                        "VSIA-Phone-Translation-Prefetch"
                                );

                        thread.setDaemon(true);
                        return thread;
                    }
            );

    private static final Map<String, Map<String, String>>
            CACHE =
            new ConcurrentHashMap<>();

    private static final Set<String>
            LOADED =
            ConcurrentHashMap.newKeySet();

    private static final Set<String>
            PENDING_DEMAND =
            ConcurrentHashMap.newKeySet();

    private static final Set<String>
            PENDING_PREFETCH =
            ConcurrentHashMap.newKeySet();

    private PhoneAutoTranslationService() {
    }

    public static String translateOrQueue(
            String languageTag,
            String sourceText
    ) {
        if (sourceText == null
                || sourceText.isEmpty()) {
            return sourceText;
        }

        String tag =
                normalizeTag(
                        languageTag
                );

        if (isEnglish(tag)) {
            return sourceText;
        }

        ensureLoaded(
                tag
        );

        String cached =
                CACHE.getOrDefault(
                        tag,
                        Map.of()
                )
                .get(
                        sourceText
                );

        if (cached != null
                && !cached.isBlank()) {
            return cached;
        }

        queueDemand(
                tag,
                sourceText
        );

        return null;
    }

    public static void prefetch(
            String languageTag,
            Collection<String> sourceStrings
    ) {
        String tag =
                normalizeTag(
                        languageTag
                );

        if (isEnglish(tag)
                || sourceStrings == null
                || sourceStrings.isEmpty()) {
            return;
        }

        ensureLoaded(
                tag
        );

        for (String sourceText :
                sourceStrings) {
            if (sourceText == null
                    || sourceText.isBlank()) {
                continue;
            }

            if (CACHE.getOrDefault(
                    tag,
                    Map.of()
            ).containsKey(sourceText)) {
                continue;
            }

            queuePrefetch(
                    tag,
                    sourceText
            );
        }
    }

    public static int cachedCount(
            String languageTag
    ) {
        String tag =
                normalizeTag(
                        languageTag
                );

        ensureLoaded(
                tag
        );

        return CACHE.getOrDefault(
                tag,
                Map.of()
        ).size();
    }

    public static int pendingCount(
            String languageTag
    ) {
        String prefix =
                normalizeTag(
                        languageTag
                )
                        + "\u0000";

        int count = 0;

        for (String key : PENDING_DEMAND) {
            if (key.startsWith(prefix)) {
                count++;
            }
        }

        for (String key : PENDING_PREFETCH) {
            if (key.startsWith(prefix)) {
                count++;
            }
        }

        return count;
    }

    public static void clearMemoryCache() {
        CACHE.clear();
        LOADED.clear();
        PENDING_DEMAND.clear();
        PENDING_PREFETCH.clear();
    }

    private static void queueDemand(
            String languageTag,
            String sourceText
    ) {
        queueOn(
                DEMAND_WORKER,
                PENDING_DEMAND,
                languageTag,
                sourceText,
                25L
        );
    }

    private static void queuePrefetch(
            String languageTag,
            String sourceText
    ) {
        String key =
                languageTag
                        + "\u0000"
                        + sourceText;

        if (PENDING_DEMAND.contains(key)) {
            return;
        }

        queueOn(
                PREFETCH_WORKER,
                PENDING_PREFETCH,
                languageTag,
                sourceText,
                120L
        );
    }

    private static void queueOn(
            ExecutorService executor,
            Set<String> pendingSet,
            String languageTag,
            String sourceText,
            long delayMillis
    ) {
        String key =
                languageTag
                        + "\u0000"
                        + sourceText;

        if (!pendingSet.add(key)) {
            return;
        }

        executor.execute(
                () -> {
                    try {
                        String already =
                                CACHE.getOrDefault(
                                        languageTag,
                                        Map.of()
                                )
                                .get(
                                        sourceText
                                );

                        if (already != null
                                && !already.isBlank()) {
                            return;
                        }

                        String translated =
                                requestTranslation(
                                        languageTag,
                                        sourceText
                                );

                        if (translated != null
                                && !translated.isBlank()) {
                            CACHE.computeIfAbsent(
                                    languageTag,
                                    ignored ->
                                            new ConcurrentHashMap<>()
                            )
                            .put(
                                    sourceText,
                                    translated
                            );

                            save(
                                    languageTag
                            );
                        }
                    } finally {
                        pendingSet.remove(
                                key
                        );

                        try {
                            Thread.sleep(
                                    delayMillis
                            );
                        } catch (InterruptedException ignored) {
                            Thread.currentThread()
                                    .interrupt();
                        }
                    }
                }
        );
    }

    private static String requestTranslation(
            String languageTag,
            String sourceText
    ) {
        try {
            String url =
                    ENDPOINT.formatted(
                            URLEncoder.encode(
                                    googleLanguageTag(
                                            languageTag
                                    ),
                                    StandardCharsets.UTF_8
                            ),
                            URLEncoder.encode(
                                    sourceText,
                                    StandardCharsets.UTF_8
                            )
                    );

            HttpRequest request =
                    HttpRequest.newBuilder(
                            URI.create(
                                    url
                            )
                    )
                    .timeout(
                            Duration.ofSeconds(12L)
                    )
                    .header(
                            "User-Agent",
                            "VSIA-Phone/1.0"
                    )
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HTTP.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300
                    || response.body() == null
                    || response.body().isBlank()) {
                return null;
            }

            JsonElement root =
                    JsonParser.parseString(
                            response.body()
                    );

            if (!root.isJsonArray()) {
                return null;
            }

            JsonArray rootArray =
                    root.getAsJsonArray();

            if (rootArray.size() == 0
                    || rootArray.get(0).isJsonNull()
                    || !rootArray.get(0).isJsonArray()) {
                return null;
            }

            JsonArray segments =
                    rootArray.get(0)
                            .getAsJsonArray();

            StringBuilder translated =
                    new StringBuilder();

            for (JsonElement segmentElement :
                    segments) {
                if (segmentElement == null
                        || !segmentElement.isJsonArray()) {
                    continue;
                }

                JsonArray segment =
                        segmentElement.getAsJsonArray();

                if (segment.size() == 0
                        || segment.get(0).isJsonNull()) {
                    continue;
                }

                translated.append(
                        segment.get(0)
                                .getAsString()
                );
            }

            String value =
                    translated
                            .toString()
                            .trim();

            return value.isBlank()
                    ? null
                    : value;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void ensureLoaded(
            String languageTag
    ) {
        if (!LOADED.add(
                languageTag
        )) {
            return;
        }

        Map<String, String> values =
                new ConcurrentHashMap<>();

        Path path =
                cachePath(
                        languageTag
                );

        if (Files.isRegularFile(path)) {
            Properties properties =
                    new Properties();

            try (Reader reader =
                         Files.newBufferedReader(
                                 path,
                                 StandardCharsets.UTF_8
                         )) {
                properties.load(
                        reader
                );

                for (String key :
                        properties.stringPropertyNames()) {
                    String value =
                            properties.getProperty(
                                    key
                            );

                    if (value != null
                            && !value.isBlank()) {
                        values.put(
                                key,
                                value
                        );
                    }
                }
            } catch (IOException ignored) {
            }
        }

        CACHE.put(
                languageTag,
                values
        );
    }

    private static void save(
            String languageTag
    ) {
        Path path =
                cachePath(
                        languageTag
                );

        try {
            Files.createDirectories(
                    path.getParent()
            );

            Properties properties =
                    new Properties();

            properties.putAll(
                    CACHE.getOrDefault(
                            languageTag,
                            Map.of()
                    )
            );

            try (Writer writer =
                         Files.newBufferedWriter(
                                 path,
                                 StandardCharsets.UTF_8
                         )) {
                properties.store(
                        writer,
                        "VSIA phone translation cache"
                );
            }
        } catch (IOException ignored) {
        }
    }

    private static Path cachePath(
            String languageTag
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        return minecraft.gameDirectory
                .toPath()
                .resolve("config")
                .resolve("vsia")
                .resolve("phone_lang_cache")
                .resolve(
                        normalizeTag(
                                languageTag
                        )
                                + ".properties"
                );
    }

    private static boolean isEnglish(
            String languageTag
    ) {
        return "en".equals(
                languageTag
                        .split("-")[0]
        );
    }

    private static String normalizeTag(
            String languageTag
    ) {
        if (languageTag == null
                || languageTag.isBlank()) {
            return "en";
        }

        return languageTag
                .trim()
                .replace(
                        '_',
                        '-'
                )
                .toLowerCase();
    }

    private static String googleLanguageTag(
            String languageTag
    ) {
        return switch (languageTag) {
            case "zh-tw" ->
                    "zh-TW";

            case "fr-ca" ->
                    "fr-CA";

            case "pt-pt" ->
                    "pt-PT";

            case "fa-af" ->
                    "fa-AF";

            case "crh-latn" ->
                    "crh-Latn";

            case "iu-latn" ->
                    "iu-Latn";

            case "ms-arab" ->
                    "ms-Arab";

            case "mni-mtei" ->
                    "mni-Mtei";

            case "bm-nkoo" ->
                    "bm-Nkoo";

            case "ndc-zw" ->
                    "ndc-ZW";

            case "pa-arab" ->
                    "pa-Arab";

            case "sat-latn" ->
                    "sat-Latn";

            case "ber-latn" ->
                    "ber-Latn";

            default ->
                    languageTag;
        };
    }
}
