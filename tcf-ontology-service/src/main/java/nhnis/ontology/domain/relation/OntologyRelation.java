package nhnis.ontology.domain.relation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import nhnis.ontology.domain.Provenance;

/**
 * First-class ontology relation edge.
 */
public final class OntologyRelation {

    public enum Status {
        ACTIVE,
        DEPRECATED,
        DRAFT
    }

    private final String id;
    private final String fromId;
    private final RelationType predicate;
    private final String toId;
    private final GraphType graphType;
    private final Map<String, Object> attributes;
    private final String version;
    private final Status status;
    private final Provenance provenance;

    private OntologyRelation(Builder b) {
        this.fromId = Objects.requireNonNull(b.fromId, "fromId");
        this.predicate = Objects.requireNonNull(b.predicate, "predicate").canonical();
        this.toId = Objects.requireNonNull(b.toId, "toId");
        this.graphType = b.graphType == null ? GraphType.DESIGN : b.graphType;
        this.attributes = b.attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(b.attributes));
        this.version = b.version == null ? "1.0" : b.version;
        this.status = b.status == null ? Status.ACTIVE : b.status;
        this.provenance = b.provenance;
        this.id = b.id != null ? b.id : edgeKey(fromId, predicate, toId, graphType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static OntologyRelation of(String fromId, RelationType predicate, String toId, GraphType graphType,
            Provenance provenance) {
        return builder()
                .fromId(fromId)
                .predicate(predicate)
                .toId(toId)
                .graphType(graphType)
                .provenance(provenance)
                .build();
    }

    public static String edgeKey(String fromId, RelationType predicate, String toId, GraphType graphType) {
        return fromId + "|" + predicate.canonical().name() + "|" + toId + "|" + graphType.name();
    }

    public String getId() {
        return id;
    }

    public String getFromId() {
        return fromId;
    }

    public RelationType getPredicate() {
        return predicate;
    }

    public String getToId() {
        return toId;
    }

    public GraphType getGraphType() {
        return graphType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
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

    public String edgeKey() {
        return edgeKey(fromId, predicate, toId, graphType);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        out.put("from", fromId);
        out.put("predicate", predicate.name());
        out.put("to", toId);
        out.put("graphType", graphType.name());
        out.put("version", version);
        out.put("status", status.name());
        if (!attributes.isEmpty()) {
            out.put("attributes", attributes);
        }
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
        if (!(o instanceof OntologyRelation that)) {
            return false;
        }
        return edgeKey().equals(that.edgeKey());
    }

    @Override
    public int hashCode() {
        return edgeKey().hashCode();
    }

    public static final class Builder {
        private String id;
        private String fromId;
        private RelationType predicate;
        private String toId;
        private GraphType graphType = GraphType.DESIGN;
        private Map<String, Object> attributes;
        private String version;
        private Status status;
        private Provenance provenance;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder fromId(String fromId) {
            this.fromId = fromId;
            return this;
        }

        public Builder predicate(RelationType predicate) {
            this.predicate = predicate;
            return this;
        }

        public Builder toId(String toId) {
            this.toId = toId;
            return this;
        }

        public Builder graphType(GraphType graphType) {
            this.graphType = graphType;
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

        public OntologyRelation build() {
            return new OntologyRelation(this);
        }
    }
}
