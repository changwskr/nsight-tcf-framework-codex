package nhnis.ontology.support;

import java.util.Locale;
import java.util.regex.Pattern;

import nhnis.ontology.domain.concept.ServiceIdParts;

/**
 * Parses PDMG 11-char ServiceId into structured parts.
 * Convention: lowercase body (mgcoa8888) + uppercase op/seq (S0).
 */
public final class ServiceIdParser {

    private static final Pattern PATTERN = Pattern.compile(
            "^([a-z]{2})([a-z]{2})([a-z])([0-9]{4})([scudar])([0-9a-z])$",
            Pattern.CASE_INSENSITIVE);

    private ServiceIdParser() {
    }

    public static boolean isValid(String serviceId) {
        try {
            parse(serviceId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static ServiceIdParts parse(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId is blank");
        }
        String raw = serviceId.trim();
        if (raw.length() != 11) {
            throw new IllegalArgumentException(
                    "Invalid ServiceId (expect 11 chars like mgcoa8888S0): " + serviceId);
        }
        String prefix = raw.substring(0, 9).toLowerCase(Locale.ROOT);
        String op = raw.substring(9, 10).toUpperCase(Locale.ROOT);
        String seq = raw.substring(10, 11).toUpperCase(Locale.ROOT);
        String candidate = prefix + op.toLowerCase(Locale.ROOT) + seq.toLowerCase(Locale.ROOT);
        var matcher = PATTERN.matcher(candidate);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid ServiceId (expect 11 chars like mgcoa8888S0): " + serviceId);
        }
        String full = matcher.group(1) + matcher.group(2) + matcher.group(3) + matcher.group(4) + op + seq;
        return new ServiceIdParts(
                matcher.group(1),
                matcher.group(2),
                matcher.group(3),
                matcher.group(4),
                op,
                seq,
                full);
    }

    /**
     * Keep lowercase body + uppercase op/seq as PDMG convention: mgcoa8888S0
     */
    public static String canonical(String serviceId) {
        return parse(serviceId).getFullServiceId();
    }
}
