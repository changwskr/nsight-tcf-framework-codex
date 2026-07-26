package com.nh.nsight.marketing.oc.application.dto.env;

import java.util.List;

public record ConfigImportResult(
        String importId,
        int fileCount,
        int entryCount,
        List<String> fileNames,
        List<ParsedConfigEntry> entries
) {
}
