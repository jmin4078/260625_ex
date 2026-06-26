package oop.search.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record NewsResult(String title, String link, String description, String pubDate) {
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?[0-9a-fA-F]+);");

    public NewsResult {
        title = sanitize(title);
        link = valueOrEmpty(link);
        description = sanitize(description);
        pubDate = valueOrEmpty(pubDate);
    }

    private static String sanitize(String value) {
        String withoutTags = HTML_TAG.matcher(valueOrEmpty(value)).replaceAll("");
        String decoded = decodeNumericEntities(withoutTags)
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&");
        return decoded.strip();
    }

    private static String decodeNumericEntities(String value) {
        Matcher matcher = NUMERIC_ENTITY.matcher(value);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String entityValue = matcher.group(1);
            int radix = entityValue.startsWith("x") || entityValue.startsWith("X") ? 16 : 10;
            String digits = radix == 16 ? entityValue.substring(1) : entityValue;

            try {
                int codePoint = Integer.parseInt(digits, radix);
                matcher.appendReplacement(result,
                        Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
            } catch (IllegalArgumentException exception) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}