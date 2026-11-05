/*
 * This file is part of Aliucord, an Android Discord client mod.
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.coreplugins.slashcommandsfix;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets the client show localized command names while sending the default ones.
 * <p>
 * The command index returns the localized name in {@code name} and the default one in
 * {@code name_default} (for commands and options). This Discord version predates
 * localization: whatever is in {@code name} is both displayed and sent back when the
 * command runs, but Discord only accepts default names in interactions. So we record
 * localized-to-default mappings per command and rewrite the outgoing payload.
 * <p>
 * JSON is read with org.json and Discord's classes through field names, because
 * Discord's bundled Gson and getters are obfuscated.
 */
final class DefaultNames {
    /** Default name of a command or option, plus localized-to-default mappings of its child options. */
    private static final class Node {
        final String defaultName;
        final Map<String, Node> options;

        Node(String defaultName, Map<String, Node> options) {
            this.defaultName = defaultName;
            this.options = options;
        }
    }

    /** Keyed by command id (as sent in the interaction). */
    private final Map<String, Node> commands = new ConcurrentHashMap<>();

    /** Records the default names of every command (and its options) in an index response. */
    void update(String json) throws Exception {
        var commands = new JSONObject(json).optJSONArray("application_commands");
        if (commands == null) return;
        for (int i = 0; i < commands.length(); i++) {
            var command = commands.optJSONObject(i);
            if (command == null) continue;
            this.commands.put(command.optString("id"), new Node(
                defaultName(command),
                options(command.optJSONArray("options"))
            ));
        }
    }

    private static Map<String, Node> options(JSONArray options) {
        if (options == null) return Collections.emptyMap();
        var result = new HashMap<String, Node>();
        for (int i = 0; i < options.length(); i++) {
            var option = options.optJSONObject(i);
            if (option == null) continue;
            result.put(option.optString("name"), new Node(
                defaultName(option),
                options(option.optJSONArray("options"))
            ));
        }
        return result;
    }

    /** {@code name_default} if present, else {@code name} (not localized). */
    static String defaultName(JSONObject object) {
        if (object.has("name_default") && !object.isNull("name_default")) {
            var name = object.optString("name_default");
            if (!name.isEmpty()) return name;
        }
        return object.optString("name");
    }

    /**
     * Returns {@code json} with every command and option {@code name} replaced by its default,
     * for when the send-time rewrite isn't available (then default names must be displayed).
     */
    static String withDefaultNames(String json) throws Exception {
        var root = new JSONObject(json);
        var commands = root.optJSONArray("application_commands");
        if (commands == null) return json;
        for (int i = 0; i < commands.length(); i++) {
            useDefaultNames(commands.optJSONObject(i));
        }
        return root.toString();
    }

    private static void useDefaultNames(JSONObject object) throws Exception {
        if (object == null) return;
        object.put("name", defaultName(object));
        var options = object.optJSONArray("options");
        if (options == null) return;
        for (int i = 0; i < options.length(); i++) {
            useDefaultNames(options.optJSONObject(i));
        }
    }

    // Fields of com.discord.api.commands.ApplicationCommandData / ApplicationCommandValue
    private final Field dataIdField;
    private final Field dataNameField;
    private final Field dataOptionsField;
    private final Field valueNameField;
    private final Field valueOptionsField;

    DefaultNames(Class<?> dataClass, Class<?> valueClass) throws NoSuchFieldException {
        dataIdField = accessible(dataClass, "id");
        dataNameField = accessible(dataClass, "name");
        dataOptionsField = accessible(dataClass, "options");
        valueNameField = accessible(valueClass, "name");
        valueOptionsField = accessible(valueClass, "options");
    }

    private static Field accessible(Class<?> cls, String name) throws NoSuchFieldException {
        var field = cls.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    /**
     * Rewrites a freshly built interaction {@code data} object in place so its command and
     * option names are the defaults. Leaves it untouched for unknown commands.
     */
    void applyTo(Object data) throws IllegalAccessException {
        if (data == null) return;
        var command = commands.get(String.valueOf(dataIdField.get(data)));
        if (command == null) return;
        if (!command.defaultName.isEmpty()) {
            dataNameField.set(data, command.defaultName);
        }
        applyToValues(dataOptionsField.get(data), command.options);
    }

    private void applyToValues(Object values, Map<String, Node> options) throws IllegalAccessException {
        if (!(values instanceof List) || options.isEmpty()) return;
        for (var value : (List<?>) values) {
            if (value == null) continue;
            var option = options.get(String.valueOf(valueNameField.get(value)));
            if (option == null) continue;
            if (!option.defaultName.isEmpty()) {
                valueNameField.set(value, option.defaultName);
            }
            applyToValues(valueOptionsField.get(value), option.options);
        }
    }
}
