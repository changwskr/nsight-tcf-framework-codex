package com.nh.nsight.harness.git;

import java.text.Normalizer;
import java.util.Locale;

public final class BranchNameFactory {
    private BranchNameFactory() {
    }

    public static String create(String workItemId, String title) {
        String normalized = Normalizer.normalize(title == null ? "work" : title, Normalizer.Form.NFKD)
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            normalized = "work";
        }
        if (normalized.length() > 40) {
            normalized = normalized.substring(0, 40).replaceAll("-+$", "");
        }
        return "harness/" + workItemId + "-" + normalized;
    }
}
