package nhnis.ontology.model;

/**
 * Standard ontology relation predicates (see ontology/core/relations.yml).
 */
public final class RelationPredicates {

    public static final String HAS_BUSINESS = "HAS_BUSINESS";
    public static final String HAS_FUNCTION = "HAS_FUNCTION";
    public static final String HAS_PROGRAM = "HAS_PROGRAM";
    public static final String PROVIDES_SERVICE = "PROVIDES_SERVICE";
    /** Alias of PROVIDES_SERVICE (kept for compatibility). */
    public static final String HAS_SERVICE = "HAS_SERVICE";
    public static final String BELONGS_TO_PROGRAM = "BELONGS_TO_PROGRAM";
    public static final String HANDLED_BY = "HANDLED_BY";
    public static final String CALLS = "CALLS";
    public static final String USES = "USES";
    public static final String EXECUTES = "EXECUTES";
    public static final String ACCESSES = "ACCESSES";
    public static final String HAS_COLUMN = "HAS_COLUMN";
    public static final String FLOWS_TO = "FLOWS_TO";
    public static final String DISPATCHES_TO = "DISPATCHES_TO";
    public static final String STARTS_TRANSACTION = "STARTS_TRANSACTION";
    public static final String PARTICIPATES_IN_TRANSACTION = "PARTICIPATES_IN_TRANSACTION";
    public static final String RUNS_ON_THREAD = "RUNS_ON_THREAD";
    public static final String ORCHESTRATED_BY = "ORCHESTRATED_BY";
    public static final String IMPLEMENTED_BY = "IMPLEMENTED_BY";
    public static final String USES_DTO = "USES_DTO";
    public static final String MAPPED_BY = "MAPPED_BY";
    public static final String PERSISTS_TO = "PERSISTS_TO";
    public static final String EXPOSED_AT = "EXPOSED_AT";
    public static final String USES_EXCEPTION = "USES_EXCEPTION";
    public static final String CONFIGURED_BY = "CONFIGURED_BY";

    private RelationPredicates() {
    }

    public static String canonical(String predicate) {
        if (HAS_SERVICE.equals(predicate)) {
            return PROVIDES_SERVICE;
        }
        return predicate;
    }
}
