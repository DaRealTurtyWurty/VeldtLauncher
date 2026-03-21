package dev.turtywurty.veldtlauncher.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class JsonUtil {
    private static final Gson GSON = new Gson();

    private JsonUtil() {
    }

    public static boolean contains(JsonObject jsonObject, String fieldName) {
        return jsonObject != null && fieldName != null && jsonObject.has(fieldName);
    }

    public static boolean hasObject(JsonObject jsonObject, String fieldName) {
        JsonElement element = getRawElement(jsonObject, fieldName);
        return element != null && element.isJsonObject();
    }

    public static boolean hasArray(JsonObject jsonObject, String fieldName) {
        JsonElement element = getRawElement(jsonObject, fieldName);
        return element != null && element.isJsonArray();
    }

    public static JsonObject getObject(JsonObject jsonObject, String fieldName) {
        return getObject(jsonObject, fieldName, null);
    }

    public static JsonObject getObject(JsonObject jsonObject, String fieldName, JsonObject fallback) {
        JsonElement element = getRawElement(jsonObject, fieldName);
        if (element == null || !element.isJsonObject())
            return fallback;

        try {
            return element.getAsJsonObject();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static JsonObject getObject(JsonArray jsonArray, int index) {
        return getObject(jsonArray, index, null);
    }

    public static JsonObject getObject(JsonArray jsonArray, int index, JsonObject fallback) {
        JsonElement element = getArrayElement(jsonArray, index);
        if (element == null || !element.isJsonObject())
            return fallback;

        try {
            return element.getAsJsonObject();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static JsonArray getArray(JsonObject jsonObject, String fieldName) {
        return getArray(jsonObject, fieldName, new JsonArray());
    }

    public static JsonArray getArray(JsonObject jsonObject, String fieldName, JsonArray fallback) {
        JsonElement element = getRawElement(jsonObject, fieldName);
        if (element == null || !element.isJsonArray())
            return fallback;

        try {
            return element.getAsJsonArray();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static Stream<JsonElement> streamArray(JsonArray jsonArray) {
        if (jsonArray == null)
            return Stream.empty();

        return StreamSupport.stream(jsonArray.spliterator(), false);
    }

    public static String getString(JsonObject jsonObject, String fieldName) {
        return getString(jsonObject, fieldName, null);
    }

    public static String getString(JsonObject jsonObject, String fieldName, String fallback) {
        JsonElement element = getValueElement(jsonObject, fieldName);
        if (element == null)
            return fallback;

        try {
            return element.getAsString();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static int getInt(JsonObject jsonObject, String fieldName) {
        return getInt(jsonObject, fieldName, 0);
    }

    public static int getInt(JsonObject jsonObject, String fieldName, int fallback) {
        JsonElement element = getValueElement(jsonObject, fieldName);
        if (element == null)
            return fallback;

        try {
            return element.getAsInt();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static Long getLongObject(JsonObject jsonObject, String fieldName) {
        return getLongObject(jsonObject, fieldName, null);
    }

    public static Long getLongObject(JsonObject jsonObject, String fieldName, Long fallback) {
        JsonElement element = getValueElement(jsonObject, fieldName);
        if (element == null)
            return fallback;

        try {
            return element.getAsLong();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static long getLong(JsonObject jsonObject, String fieldName) {
        return getLong(jsonObject, fieldName, 0L);
    }

    public static long getLong(JsonObject jsonObject, String fieldName, long fallback) {
        Long value = getLongObject(jsonObject, fieldName, null);
        return value == null ? fallback : value;
    }

    public static String[] getStringArray(JsonObject jsonObject, String fieldName) {
        return getStringArray(jsonObject, fieldName, new String[0]);
    }

    public static String[] getStringArray(JsonObject jsonObject, String fieldName, String[] fallback) {
        JsonArray array = getArray(jsonObject, fieldName, null);
        if (array == null)
            return fallback;

        try {
            String[] values = GSON.fromJson(array, String[].class);
            return values == null ? fallback : values;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static int[] getIntArray(JsonObject jsonObject, String fieldName) {
        return getIntArray(jsonObject, fieldName, new int[0]);
    }

    public static int[] getIntArray(JsonObject jsonObject, String fieldName, int[] fallback) {
        JsonArray array = getArray(jsonObject, fieldName, null);
        if (array == null)
            return fallback;

        try {
            int[] values = GSON.fromJson(array, int[].class);
            return values == null ? fallback : values;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static JsonElement getRawElement(JsonObject jsonObject, String fieldName) {
        if (!contains(jsonObject, fieldName))
            return null;

        try {
            return jsonObject.get(fieldName);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static JsonElement getValueElement(JsonObject jsonObject, String fieldName) {
        JsonElement element = getRawElement(jsonObject, fieldName);
        if (element == null || element.isJsonNull())
            return null;

        return element;
    }

    private static JsonElement getArrayElement(JsonArray jsonArray, int index) {
        if (jsonArray == null || index < 0 || index >= jsonArray.size())
            return null;

        try {
            JsonElement element = jsonArray.get(index);
            return element == null || element.isJsonNull() ? null : element;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public static Instant getInstant(JsonObject jsonObject, String fieldName) {
        return getInstant(jsonObject, fieldName, null);
    }

    public static Instant getInstant(JsonObject jsonObject, String fieldName, Instant fallback) {
        String value = getString(jsonObject, fieldName, null);
        if (value == null)
            return fallback;

        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
