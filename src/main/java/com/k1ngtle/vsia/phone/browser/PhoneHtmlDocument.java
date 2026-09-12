package com.k1ngtle.vsia.phone.browser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PhoneHtmlDocument {
    private static final Pattern TITLE =
            Pattern.compile("(?is)<title[^>]*>(.*?)</title>");

    private static final Pattern STYLE =
            Pattern.compile("(?is)<style[^>]*>(.*?)</style>");

    private static final Pattern BODY =
            Pattern.compile("(?is)<body([^>]*)>(.*)</body>");

    private static final Pattern TOKEN =
            Pattern.compile("(?is)<[^>]+>|[^<]+");

    private static final Pattern ATTRIBUTE =
            Pattern.compile(
                    "([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))"
            );

    private final String title;
    private final int backgroundColor;
    private final int defaultTextColor;
    private final List<Block> blocks;

    private PhoneHtmlDocument(
            String title,
            int backgroundColor,
            int defaultTextColor,
            List<Block> blocks
    ) {
        this.title = title == null ? "" : title;
        this.backgroundColor = backgroundColor;
        this.defaultTextColor = defaultTextColor;
        this.blocks = List.copyOf(blocks);
    }

    public static PhoneHtmlDocument from(BrowserResponse response) {
        String contentType = response.contentType().toLowerCase(Locale.ROOT);

        if (!contentType.contains("html")) {
            return plain(response.reason(), response.body());
        }

        return parseHtml(response.body(), response.styleSheet());
    }

    public static PhoneHtmlDocument plain(String title, String text) {
        List<Block> blocks = new ArrayList<>();
        String value = text == null ? "" : text;

        if (!value.isBlank()) {
            blocks.add(
                    new Block(
                            Kind.TEXT,
                            "pre",
                            value,
                            "",
                            0xFF1C1C1E,
                            0,
                            1.0F,
                            Align.LEFT
                    )
            );
        }

        return new PhoneHtmlDocument(
                title,
                0xFFFFFFFF,
                0xFF1C1C1E,
                blocks
        );
    }

    public static PhoneHtmlDocument parseHtml(String html, String externalCss) {
        String source = html == null ? "" : html;
        String title = extractTitle(source);

        StringBuilder css = new StringBuilder();

        if (externalCss != null && !externalCss.isBlank()) {
            css.append(externalCss).append('\n');
        }

        Matcher styleMatcher = STYLE.matcher(source);

        while (styleMatcher.find()) {
            css.append(styleMatcher.group(1)).append('\n');
        }

        CssRules rules = CssRules.parse(css.toString());

        String bodyAttributes = "";
        String bodyHtml = source;

        Matcher bodyMatcher = BODY.matcher(source);

        if (bodyMatcher.find()) {
            bodyAttributes = bodyMatcher.group(1);
            bodyHtml = bodyMatcher.group(2);
        }

        StyleState bodyStyle = defaultStyleFor("body", 0xFF1C1C1E);
        rules.apply(bodyStyle, "body", "", "");
        applyInlineStyle(bodyStyle, attribute(bodyAttributes, "style"));

        bodyHtml = bodyHtml.replaceAll("(?is)<!--.*?-->", "");
        bodyHtml = bodyHtml.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        bodyHtml = bodyHtml.replaceAll("(?is)<style[^>]*>.*?</style>", "");

        List<Block> blocks = new ArrayList<>();
        Deque<Context> stack = new ArrayDeque<>();

        Matcher tokenMatcher = TOKEN.matcher(bodyHtml);

        while (tokenMatcher.find()) {
            String token = tokenMatcher.group();

            if (token.startsWith("<")) {
                processTag(token, stack, blocks, rules, bodyStyle);
            } else {
                processText(token, stack, blocks, rules, bodyStyle);
            }
        }

        while (!stack.isEmpty()) {
            flushContext(stack.pop(), blocks, rules, bodyStyle);
        }

        return new PhoneHtmlDocument(
                title,
                bodyStyle.backgroundColor == 0
                        ? 0xFFFFFFFF
                        : bodyStyle.backgroundColor,
                bodyStyle.textColor,
                blocks
        );
    }

    private static void processTag(
            String token,
            Deque<Context> stack,
            List<Block> blocks,
            CssRules rules,
            StyleState bodyStyle
    ) {
        String trimmed = token.substring(1, token.length() - 1).trim();

        if (trimmed.isBlank()
                || trimmed.startsWith("!")
                || trimmed.startsWith("?")) {
            return;
        }

        boolean closing = trimmed.startsWith("/");

        if (closing) {
            String tag = firstWord(trimmed.substring(1));

            if (!stack.isEmpty() && stack.peek().tag.equals(tag)) {
                Context context = stack.pop();
                flushContext(context, blocks, rules, bodyStyle);
            }

            return;
        }

        boolean selfClosing = trimmed.endsWith("/");

        if (selfClosing) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }

        String tag = firstWord(trimmed);
        String attributes = trimmed.length() > tag.length()
                ? trimmed.substring(tag.length())
                : "";

        if ("br".equals(tag)) {
            if (!stack.isEmpty()) {
                stack.peek().text.append('\n');
            }
            return;
        }

        if ("hr".equals(tag)) {
            flushTopText(stack, blocks, rules, bodyStyle);

            blocks.add(
                    new Block(
                            Kind.HR,
                            "hr",
                            "",
                            "",
                            0xFF8E8E93,
                            0,
                            1.0F,
                            Align.LEFT
                    )
            );
            return;
        }

        if ("img".equals(tag)) {
            flushTopText(stack, blocks, rules, bodyStyle);

            String alt = attribute(attributes, "alt");

            if (alt.isBlank()) {
                alt = "Image";
            }

            StyleState style = styleFor(tag, attributes, rules, bodyStyle);

            blocks.add(
                    new Block(
                            Kind.IMAGE,
                            tag,
                            alt,
                            "",
                            style.textColor,
                            style.backgroundColor,
                            1.0F,
                            style.align
                    )
            );
            return;
        }

        if (!blockTag(tag)) {
            return;
        }

        flushTopText(stack, blocks, rules, bodyStyle);

        Context context = new Context(tag, attributes);
        stack.push(context);

        if (selfClosing) {
            stack.pop();
            flushContext(context, blocks, rules, bodyStyle);
        }
    }

    private static void processText(
            String token,
            Deque<Context> stack,
            List<Block> blocks,
            CssRules rules,
            StyleState bodyStyle
    ) {
        String decoded = decodeEntities(token);

        if (stack.isEmpty()) {
            String text = normalizeText(decoded, false);

            if (!text.isBlank()) {
                Context context = new Context("p", "");
                context.text.append(text);
                flushContext(context, blocks, rules, bodyStyle);
            }

            return;
        }

        Context context = stack.peek();
        boolean pre = "pre".equals(context.tag);
        String value = normalizeText(decoded, pre);

        if (!value.isBlank() || pre) {
            context.text.append(value);
        }
    }

    private static void flushTopText(
            Deque<Context> stack,
            List<Block> blocks,
            CssRules rules,
            StyleState bodyStyle
    ) {
        if (stack.isEmpty()) {
            return;
        }

        Context context = stack.peek();

        if (context.text.toString().trim().isEmpty()) {
            return;
        }

        Context segment = new Context(context.tag, context.attributes);
        segment.text.append(context.text);
        context.text.setLength(0);

        flushContext(segment, blocks, rules, bodyStyle);
    }

    private static void flushContext(
            Context context,
            List<Block> blocks,
            CssRules rules,
            StyleState bodyStyle
    ) {
        String text = normalizeText(
                context.text.toString(),
                "pre".equals(context.tag)
        );

        if (text.isBlank()) {
            return;
        }

        StyleState style = styleFor(
                context.tag,
                context.attributes,
                rules,
                bodyStyle
        );

        Kind kind = switch (context.tag) {
            case "a" -> Kind.LINK;
            case "button" -> Kind.BUTTON;
            default -> Kind.TEXT;
        };

        String href = kind == Kind.LINK || kind == Kind.BUTTON
                ? attribute(context.attributes, "href")
                : "";

        if (href.isBlank() && kind == Kind.BUTTON) {
            href = attribute(context.attributes, "data-href");
        }

        if ("li".equals(context.tag)) {
            text = "• " + text;
        }

        blocks.add(
                new Block(
                        kind,
                        context.tag,
                        text,
                        href,
                        style.textColor,
                        style.backgroundColor,
                        style.scale,
                        style.align
                )
        );
    }

    private static StyleState styleFor(
            String tag,
            String attributes,
            CssRules rules,
            StyleState bodyStyle
    ) {
        StyleState style = defaultStyleFor(tag, bodyStyle.textColor);

        String className = attribute(attributes, "class");
        String id = attribute(attributes, "id");

        rules.apply(style, tag, className, id);
        applyInlineStyle(style, attribute(attributes, "style"));

        return style;
    }

    private static StyleState defaultStyleFor(String tag, int inheritedTextColor) {
        StyleState style = new StyleState();

        style.textColor = inheritedTextColor;
        style.backgroundColor = 0;
        style.scale = 1.0F;
        style.align = Align.LEFT;

        switch (tag) {
            case "h1" -> style.scale = 1.55F;
            case "h2" -> style.scale = 1.35F;
            case "h3" -> style.scale = 1.18F;
            case "a" -> style.textColor = 0xFF007AFF;

            case "button" -> {
                style.textColor = 0xFFFFFFFF;
                style.backgroundColor = 0xFF007AFF;
            }

            case "body" -> {
                style.textColor = 0xFF1C1C1E;
                style.backgroundColor = 0xFFFFFFFF;
            }

            default -> {
            }
        }

        return style;
    }

    private static void applyInlineStyle(StyleState style, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        applyDeclarations(style, declarations(raw));
    }

    private static void applyDeclarations(
            StyleState style,
            Map<String, String> declarations
    ) {
        String color = declarations.get("color");

        if (color != null) {
            style.textColor = parseColor(color, style.textColor);
        }

        String background = declarations.get("background-color");

        if (background == null) {
            background = declarations.get("background");
        }

        if (background != null) {
            style.backgroundColor = parseColor(
                    background,
                    style.backgroundColor
            );
        }

        String fontSize = declarations.get("font-size");

        if (fontSize != null) {
            style.scale = parseScale(fontSize, style.scale);
        }

        String textAlign = declarations.get("text-align");

        if (textAlign != null) {
            style.align = switch (textAlign.trim().toLowerCase(Locale.ROOT)) {
                case "center" -> Align.CENTER;
                case "right" -> Align.RIGHT;
                default -> Align.LEFT;
            };
        }
    }

    private static float parseScale(String value, float fallback) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);

        try {
            if (trimmed.endsWith("px")) {
                float px = Float.parseFloat(
                        trimmed.substring(0, trimmed.length() - 2).trim()
                );
                return clamp(px / 16.0F, 0.7F, 2.0F);
            }

            if (trimmed.endsWith("rem")) {
                float rem = Float.parseFloat(
                        trimmed.substring(0, trimmed.length() - 3).trim()
                );
                return clamp(rem, 0.7F, 2.0F);
            }
        } catch (NumberFormatException ignored) {
        }

        return fallback;
    }

    private static int parseColor(String raw, int fallback) {
        String value = raw.trim().toLowerCase(Locale.ROOT);

        int space = value.indexOf(' ');

        if (space > 0 && !value.startsWith("rgb")) {
            value = value.substring(0, space);
        }

        if ("transparent".equals(value)) {
            return 0;
        }

        if (value.startsWith("#")) {
            String hex = value.substring(1);

            try {
                if (hex.length() == 3) {
                    int r = Integer.parseInt(
                            hex.substring(0, 1) + hex.substring(0, 1),
                            16
                    );

                    int g = Integer.parseInt(
                            hex.substring(1, 2) + hex.substring(1, 2),
                            16
                    );

                    int b = Integer.parseInt(
                            hex.substring(2, 3) + hex.substring(2, 3),
                            16
                    );

                    return 0xFF000000 | r << 16 | g << 8 | b;
                }

                if (hex.length() == 6) {
                    return 0xFF000000 | Integer.parseInt(hex, 16);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        Matcher rgb = Pattern.compile(
                "rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)"
        ).matcher(value);

        if (rgb.find()) {
            int r = clamp255(Integer.parseInt(rgb.group(1)));
            int g = clamp255(Integer.parseInt(rgb.group(2)));
            int b = clamp255(Integer.parseInt(rgb.group(3)));

            return 0xFF000000 | r << 16 | g << 8 | b;
        }

        return switch (value) {
            case "black" -> 0xFF000000;
            case "white" -> 0xFFFFFFFF;
            case "red" -> 0xFFFF3B30;
            case "green" -> 0xFF34C759;
            case "blue" -> 0xFF007AFF;
            case "gray", "grey" -> 0xFF8E8E93;
            case "orange" -> 0xFFFF9500;
            case "yellow" -> 0xFFFFCC00;
            case "purple" -> 0xFFAF52DE;
            case "pink" -> 0xFFFF2D55;
            default -> fallback;
        };
    }

    private static Map<String, String> declarations(String body) {
        Map<String, String> result = new LinkedHashMap<>();

        for (String declaration : body.split(";")) {
            int colon = declaration.indexOf(':');

            if (colon <= 0) {
                continue;
            }

            String name = declaration.substring(0, colon)
                    .trim()
                    .toLowerCase(Locale.ROOT);

            String value = declaration.substring(colon + 1).trim();

            if (!name.isBlank() && !value.isBlank()) {
                result.put(name, value);
            }
        }

        return result;
    }

    private static String attribute(String attributes, String name) {
        if (attributes == null || attributes.isBlank()) {
            return "";
        }

        Matcher matcher = ATTRIBUTE.matcher(attributes);

        while (matcher.find()) {
            if (!matcher.group(1).equalsIgnoreCase(name)) {
                continue;
            }

            if (matcher.group(2) != null) return matcher.group(2);
            if (matcher.group(3) != null) return matcher.group(3);
            if (matcher.group(4) != null) return matcher.group(4);
            return "";
        }

        return "";
    }

    private static String extractTitle(String html) {
        Matcher matcher = TITLE.matcher(html);

        if (!matcher.find()) {
            return "";
        }

        return decodeEntities(
                matcher.group(1)
                        .replaceAll("<[^>]+>", "")
                        .trim()
        );
    }

    private static String firstWord(String value) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        int space = trimmed.indexOf(' ');
        return space >= 0 ? trimmed.substring(0, space) : trimmed;
    }

    private static boolean blockTag(String tag) {
        return switch (tag) {
            case "h1",
                 "h2",
                 "h3",
                 "p",
                 "a",
                 "button",
                 "li",
                 "pre",
                 "div",
                 "section",
                 "article",
                 "header",
                 "footer",
                 "main" -> true;
            default -> false;
        };
    }

    private static String normalizeText(String value, boolean pre) {
        if (pre) {
            return value
                    .replace("\r\n", "\n")
                    .replace('\r', '\n');
        }

        return value
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String decodeEntities(String value) {
        return value
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public String title() {
        return title;
    }

    public int backgroundColor() {
        return backgroundColor;
    }

    public int defaultTextColor() {
        return defaultTextColor;
    }

    public List<Block> blocks() {
        return blocks;
    }

    public enum Kind {
        TEXT,
        LINK,
        BUTTON,
        HR,
        IMAGE
    }

    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

    public record Block(
            Kind kind,
            String tag,
            String text,
            String href,
            int textColor,
            int backgroundColor,
            float scale,
            Align align
    ) {
    }

    private static final class Context {
        private final String tag;
        private final String attributes;
        private final StringBuilder text = new StringBuilder();

        private Context(String tag, String attributes) {
            this.tag = tag;
            this.attributes = attributes == null ? "" : attributes;
        }
    }

    private static final class StyleState {
        private int textColor;
        private int backgroundColor;
        private float scale;
        private Align align;
    }

    private static final class CssRules {
        private final Map<String, Map<String, String>> rules = new HashMap<>();

        private static CssRules parse(String css) {
            CssRules result = new CssRules();

            String withoutComments = css == null
                    ? ""
                    : css.replaceAll("(?s)/\\*.*?\\*/", "");

            Matcher matcher = Pattern.compile(
                    "(?s)([^{}]+)\\{([^{}]*)}"
            ).matcher(withoutComments);

            while (matcher.find()) {
                Map<String, String> declarations = declarations(matcher.group(2));

                for (String rawSelector : matcher.group(1).split(",")) {
                    String selector = rawSelector.trim().toLowerCase(Locale.ROOT);

                    if (selector.isBlank()) {
                        continue;
                    }

                    result.rules
                            .computeIfAbsent(
                                    selector,
                                    ignored -> new LinkedHashMap<>()
                            )
                            .putAll(declarations);
                }
            }

            return result;
        }

        private void apply(
                StyleState style,
                String tag,
                String className,
                String id
        ) {
            applySelector(style, tag);

            if (className != null && !className.isBlank()) {
                for (String cssClass : className.trim().split("\\s+")) {
                    String normalized = cssClass.toLowerCase(Locale.ROOT);
                    applySelector(style, "." + normalized);
                    applySelector(style, tag + "." + normalized);
                }
            }

            if (id != null && !id.isBlank()) {
                applySelector(
                        style,
                        "#" + id.toLowerCase(Locale.ROOT)
                );
            }
        }

        private void applySelector(StyleState style, String selector) {
            Map<String, String> declarations = rules.get(selector);

            if (declarations != null) {
                applyDeclarations(style, declarations);
            }
        }
    }
}
