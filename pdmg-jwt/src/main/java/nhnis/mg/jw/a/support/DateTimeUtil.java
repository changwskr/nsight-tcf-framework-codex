package nhnis.mg.jw.a.support;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KST_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private DateTimeUtil() {}

    public static String nowKst() {
        return ZonedDateTime.now(KST).format(KST_FMT);
    }
}
