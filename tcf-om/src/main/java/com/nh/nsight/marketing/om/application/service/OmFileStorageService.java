package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.marketing.om.config.OmUpdownloadProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Service;

@Service
public class OmFileStorageService {
    private final Path storageRoot;

    public OmFileStorageService(OmUpdownloadProperties properties) {
        this.storageRoot = Paths.get(properties.getStoragePath()).toAbsolutePath().normalize();
    }

    public void save(String fileId, byte[] content) throws IOException {
        ensureRoot();
        Files.write(resolvePath(fileId), content);
    }

    public byte[] load(String fileId) throws IOException {
        return Files.readAllBytes(resolvePath(fileId));
    }

    public void delete(String fileId) throws IOException {
        Files.deleteIfExists(resolvePath(fileId));
    }

    private Path resolvePath(String fileId) {
        return storageRoot.resolve(fileId + ".bin");
    }

    private void ensureRoot() throws IOException {
        Files.createDirectories(storageRoot);
    }
}
