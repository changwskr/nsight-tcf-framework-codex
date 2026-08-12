package nhnis.ontology.domain.relation;

/**
 * Ontology 1.0 canonical relation predicates.
 * HAS_SERVICE is retained as alias of PROVIDES_SERVICE.
 */
public enum RelationType {
    HAS_BUSINESS,
    HAS_FUNCTION,
    HAS_PROGRAM,
    PROVIDES_SERVICE,
    HAS_SERVICE, // alias → PROVIDES_SERVICE
    BELONGS_TO_PROGRAM,
    HANDLED_BY,
    CALLS,
    USES,
    EXECUTES,
    ACCESSES,
    HAS_COLUMN,
    /** RUNTIME: sequential pipeline step */
    FLOWS_TO,
    /** RUNTIME: dispatcher routes to handler */
    DISPATCHES_TO,
    /** RUNTIME: starts DB UnitOfWork / TransactionTemplate */
    STARTS_TRANSACTION,
    /** RUNTIME: joins existing UnitOfWork */
    PARTICIPATES_IN_TRANSACTION,
    /** RUNTIME: executes on a thread role */
    RUNS_ON_THREAD;

    public RelationType canonical() {
        return this == HAS_SERVICE ? PROVIDES_SERVICE : this;
    }

    public static RelationType parse(String raw) {
        RelationType t = RelationType.valueOf(raw.trim().toUpperCase());
        return t.canonical();
    }
}
