package com.nh.nsight.harness.util;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SensitiveDataGuard {
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/=-]{10,}");
    private static final Pattern SECRET_OPTION = Pattern.compile(
            "(?i)(--?(?:password|passwd|token|secret|private[-_]?key)(?:=|\\s+))(?!\\$\\{)[^\\s]+"
    );
    private static final Pattern PRIVATE_KEY = Pattern.compile("(?i)-----BEGIN(?: [A-Z]+)? PRIVATE KEY-----");

    private SensitiveDataGuard() {
    }

    public static void assertSafeCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Command must not be blank");
        }
        if (BEARER.matcher(command).find() || SECRET_OPTION.matcher(command).find() || PRIVATE_KEY.matcher(command).find()) {
            throw new IllegalArgumentException(
                    "Command appears to contain a credential literal. Use a secure environment or credential store instead.");
        }
    }

    public static String redact(String text) {
        if (text == null) return "";
        String redacted = BEARER.matcher(text).replaceAll("Bearer ***REDACTED***");
        redacted = SECRET_OPTION.matcher(redacted).replaceAll("$1***REDACTED***");
        return PRIVATE_KEY.matcher(redacted).replaceAll("***PRIVATE_KEY_REDACTED***");
    }

    public static List<String> redact(List<String> command) {
        return command.stream().map(SensitiveDataGuard::redact).toList();
    }

    public static boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.toUpperCase(Locale.ROOT);
        return normalized.contains("PASSWORD") || normalized.contains("PASSWD") || normalized.contains("TOKEN")
                || normalized.contains("SECRET") || normalized.contains("PRIVATE_KEY");
    }
}
