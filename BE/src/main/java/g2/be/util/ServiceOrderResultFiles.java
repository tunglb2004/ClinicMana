package g2.be.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class ServiceOrderResultFiles {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private ServiceOrderResultFiles() {
    }

    public static List<String> parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }

        String trimmed = rawValue.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> values = OBJECT_MAPPER.readValue(trimmed, STRING_LIST);
                return values.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .toList();
            } catch (Exception ignored) {
            }
        }

        return List.of(trimmed);
    }

    public static String serialize(List<String> urls) {
        List<String> normalized = urls.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(normalized);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize service order result files", ex);
        }
    }

    public static String append(String rawValue, String newUrl) {
        List<String> urls = new ArrayList<>(parse(rawValue));
        if (newUrl != null && !newUrl.isBlank()) {
            urls.add(newUrl.trim());
        }
        return serialize(urls);
    }

    public static String latest(String rawValue) {
        List<String> urls = parse(rawValue);
        if (urls.isEmpty()) {
            return null;
        }
        return urls.get(urls.size() - 1);
    }

    public static boolean hasAny(String rawValue) {
        return !parse(rawValue).isEmpty();
    }
}
