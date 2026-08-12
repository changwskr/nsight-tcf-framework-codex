package nhnis.ontology.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provenance metadata for concepts and relations.
 */
public final class Provenance {

    public enum SourceType {
        SOURCE_CODE,
        MARKDOWN,
        MANUAL,
        SCANNER,
        GENERATED,
        DATABASE,
        OM_CATALOG,
        YAML_MAPPING
    }

    public enum VerificationStatus {
        DISCOVERED,
        VERIFIED,
        APPROVED,
        DEPRECATED
    }

    private final SourceType sourceType;
    private final String sourceSystem;
    private final String sourcePath;
    private final String sourceDocument;
    private final String sourceCommit;
    private final String discoveredBy;
    private final Instant extractedAt;
    private final Instant verifiedAt;
    private final VerificationStatus verificationStatus;

    private Provenance(Builder b) {
        this.sourceType = b.sourceType;
        this.sourceSystem = b.sourceSystem;
        this.sourcePath = b.sourcePath;
        this.sourceDocument = b.sourceDocument;
        this.sourceCommit = b.sourceCommit;
        this.discoveredBy = b.discoveredBy;
        this.extractedAt = b.extractedAt;
        this.verifiedAt = b.verifiedAt;
        this.verificationStatus = b.verificationStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Provenance yamlMapping(String path) {
        return yamlMapping(path, "YamlGraphLoader");
    }

    public static Provenance yamlMapping(String path, String discoveredBy) {
        return builder()
                .sourceType(SourceType.YAML_MAPPING)
                .sourceSystem("pdmg")
                .sourcePath(path)
                .sourceDocument(path)
                .discoveredBy(discoveredBy)
                .extractedAt(Instant.now())
                // YAML discovery only — not source-verified
                .verificationStatus(VerificationStatus.DISCOVERED)
                .build();
    }

    public static Provenance sourceCode(String path) {
        return sourceCode(path, "YamlGraphLoader");
    }

    public static Provenance sourceCode(String path, String discoveredBy) {
        return builder()
                .sourceType(SourceType.SOURCE_CODE)
                .sourceSystem("pdmg-service")
                .sourcePath(path)
                .discoveredBy(discoveredBy)
                .extractedAt(Instant.now())
                // Guessed/derived path from FQCN is DISCOVERED until Scanner verifies
                .verificationStatus(VerificationStatus.DISCOVERED)
                .build();
    }

    /** Scanner가 실제 소스 파일을 확인한 경우. */
    public static Provenance scannerVerified(String path, String discoveredBy) {
        return builder()
                .sourceType(SourceType.SCANNER)
                .sourceSystem("pdmg")
                .sourcePath(path)
                .discoveredBy(discoveredBy)
                .extractedAt(Instant.now())
                .verifiedAt(Instant.now())
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (sourceType != null) {
            m.put("sourceType", sourceType.name());
        }
        put(m, "sourceSystem", sourceSystem);
        put(m, "sourcePath", sourcePath);
        put(m, "sourceDocument", sourceDocument);
        put(m, "sourceCommit", sourceCommit);
        put(m, "discoveredBy", discoveredBy);
        if (extractedAt != null) {
            m.put("extractedAt", extractedAt.toString());
        }
        if (verifiedAt != null) {
            m.put("verifiedAt", verifiedAt.toString());
        }
        if (verificationStatus != null) {
            m.put("verificationStatus", verificationStatus.name());
        }
        return m;
    }

    private static void put(Map<String, Object> m, String k, String v) {
        if (v != null && !v.isBlank()) {
            m.put(k, v);
        }
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public static final class Builder {
        private SourceType sourceType;
        private String sourceSystem;
        private String sourcePath;
        private String sourceDocument;
        private String sourceCommit;
        private String discoveredBy;
        private Instant extractedAt;
        private Instant verifiedAt;
        private VerificationStatus verificationStatus = VerificationStatus.DISCOVERED;

        public Builder sourceType(SourceType sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public Builder sourceSystem(String sourceSystem) {
            this.sourceSystem = sourceSystem;
            return this;
        }

        public Builder sourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder sourceDocument(String sourceDocument) {
            this.sourceDocument = sourceDocument;
            return this;
        }

        public Builder sourceCommit(String sourceCommit) {
            this.sourceCommit = sourceCommit;
            return this;
        }

        public Builder discoveredBy(String discoveredBy) {
            this.discoveredBy = discoveredBy;
            return this;
        }

        public Builder extractedAt(Instant extractedAt) {
            this.extractedAt = extractedAt;
            return this;
        }

        public Builder verifiedAt(Instant verifiedAt) {
            this.verifiedAt = verifiedAt;
            return this;
        }

        public Builder verificationStatus(VerificationStatus verificationStatus) {
            this.verificationStatus = verificationStatus;
            return this;
        }

        public Provenance build() {
            Objects.requireNonNull(sourceType, "sourceType");
            return new Provenance(this);
        }
    }
}
