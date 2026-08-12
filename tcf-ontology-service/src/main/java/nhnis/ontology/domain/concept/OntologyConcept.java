package nhnis.ontology.domain.concept;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import nhnis.ontology.domain.Provenance;

/**
 * First-class ontology concept node.
 */
public final class OntologyConcept {

    public enum Status {
        ACTIVE,
        DEPRECATED,
        DRAFT
    }

    private final String id;
    private final ConceptType type;
    private final String name;
    private final String description;
    private final Map<String, Object> attributes;
    private final String version;
    private final Status status;
    private final Provenance provenance;

    private OntologyConcept(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.type = Objects.requireNonNull(b.type, "type");
        this.name = b.name == null ? b.id : b.name;
        this.description = b.description == null ? "" : b.description;
        this.attributes = b.attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(b.attributes));
        this.version = b.version == null ? "1.0" : b.version;
        this.status = b.status == null ? Status.ACTIVE : b.status;
        this.provenance = b.provenance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static OntologyConcept of(String id, ConceptType type, String name, Provenance provenance) {
        return builder().id(id).type(type).name(name).provenance(provenance).build();
    }

    public String getId() {
        return id;
    }

    public ConceptType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object attr(String key) {
        return attributes.get(key);
    }

    public String getVersion() {
        return version;
    }

    public Status getStatus() {
        return status;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        out.put("type", type.name());
        out.put("name", name);
        out.put("description", description);
        out.put("attributes", attributes);
        out.put("version", version);
        out.put("status", status.name());
        if (provenance != null) {
            out.put("provenance", provenance.toMap());
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OntologyConcept that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static final class Builder {
        private String id;
        private ConceptType type;
        private String name;
        private String description;
        private Map<String, Object> attributes;
        private String version;
        private Status status;
        private Provenance provenance;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(ConceptType type) {
            this.type = type;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder provenance(Provenance provenance) {
            this.provenance = provenance;
            return this;
        }

        public OntologyConcept build() {
            return new OntologyConcept(this);
        }
    }
}
