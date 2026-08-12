package nhnis.ontology.domain.concept;

/**
 * Ontology 1.0 concept types.
 */
public enum ConceptType {
    SYSTEM,
    BUSINESS,
    FUNCTION,
    PROGRAM,
    SERVICE_ID,
    COMPONENT,
    MAPPER,
    SQL_ID,
    TABLE,
    COLUMN,
    /** Runtime pipeline step / policy node (TX chain). */
    RUNTIME_COMPONENT
}
