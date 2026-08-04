package com.nh.nsight.harness.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Arguments {
    private final List<String> positionals;
    private final Map<String, String> options;

    private Arguments(List<String> positionals, Map<String, String> options) {
        this.positionals = positionals;
        this.options = options;
    }

    public static Arguments parse(String[] args) {
        List<String> positionals = new ArrayList<>();
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (!token.startsWith("--")) {
                positionals.add(token);
                continue;
            }
            String key = token.substring(2);
            String value = "true";
            int equals = key.indexOf('=');
            if (equals >= 0) {
                value = key.substring(equals + 1);
                key = key.substring(0, equals);
            } else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                value = args[++i];
            }
            options.put(key, value);
        }
        return new Arguments(List.copyOf(positionals), Map.copyOf(options));
    }

    public String positional(int index, String defaultValue) {
        return index < positionals.size() ? positionals.get(index) : defaultValue;
    }

    public String require(String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option --" + key);
        }
        return value;
    }

    public String option(String key, String defaultValue) {
        return options.getOrDefault(key, defaultValue);
    }

    public boolean flag(String key) {
        return Boolean.parseBoolean(options.getOrDefault(key, "false"));
    }
}
