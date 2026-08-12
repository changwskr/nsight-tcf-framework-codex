package nhnis.ontology.support;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * PDMG ProgramId: 9 chars — major(2) + business(2) + function(1) + identifier(4).
 * Example: {@code mgcoa8888}
 */
public final class ProgramIdParser {

    private static final Pattern PATTERN = Pattern.compile(
            "^([a-z]{2})([a-z]{2})([a-z])([0-9]{4})$",
            Pattern.CASE_INSENSITIVE);

    private ProgramIdParser() {
    }

    public static boolean isValid(String programId) {
        if (programId == null || programId.isBlank()) {
            return false;
        }
        String raw = programId.trim();
        if (raw.length() != 9) {
            return false;
        }
        return PATTERN.matcher(raw.toLowerCase(Locale.ROOT)).matches();
    }

    public static String canonical(String programId) {
        if (!isValid(programId)) {
            throw new IllegalArgumentException("Invalid ProgramId (expect 9 chars like mgcoa8888): " + programId);
        }
        return programId.trim().toLowerCase(Locale.ROOT);
    }
}
