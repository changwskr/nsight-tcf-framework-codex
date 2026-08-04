package nhnis.fw.commons.util;

public class ObjectUtil {

    public static String toString(Object obj) {
        return (obj == null) ? "" : obj.toString();
    }

    public static String toString(Object obj, String nullStr) {
        return (obj == null) ? nullStr : obj.toString();
    }
}
