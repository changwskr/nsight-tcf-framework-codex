package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.application.dto.env.ConfigImportResult;
import com.nh.nsight.marketing.oc.application.dto.env.ParsedConfigEntry;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConfigImportService {

    private static final long MAX_FILE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_FILES = 20;

    private final ConfigParserService parserService;
    private final AssessmentRunStore runStore;

    public ConfigImportService(ConfigParserService parserService, AssessmentRunStore runStore) {
        this.parserService = parserService;
        this.runStore = runStore;
    }

    public ConfigImportResult importFiles(MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("업로드할 설정 파일을 선택하세요.");
        }
        if (files.length > MAX_FILES) {
            throw new IllegalArgumentException("한 번에 최대 " + MAX_FILES + "개 파일까지 업로드할 수 있습니다.");
        }

        List<ParsedConfigEntry> all = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_FILE_BYTES) {
                throw new IllegalArgumentException("파일 크기 초과 (2MB): " + file.getOriginalFilename());
            }
            String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
            names.add(name);
            all.addAll(parserService.parse(name, file.getBytes()));
        }

        ConfigImportResult result = new ConfigImportResult(
                "IMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                names.size(),
                all.size(),
                names,
                all
        );
        runStore.saveImport(result);
        return result;
    }
}
