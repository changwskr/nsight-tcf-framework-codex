package nhnis.fw.commons.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * <PRE>
 * 설명
 *
 * </PRE>
 *
 * @author 홍길동
 * @version 1.0, 2026. 6. 30.
 * @logicalName
 */
public class ValidateUtil {

    public static void checkValue(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("");
        }

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            validate(entry.getKey(), entry.getValue());
        }
    }

    private static void validate(String fieldName, Object value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        if (value instanceof String str) {
            if (str.trim().isEmpty()) {
                throw new IllegalArgumentException();
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                throw new IllegalArgumentException();
            }
            for (Object obj : collection) {
                validate(fieldName, obj);
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                throw new IllegalArgumentException();
            }
            for (Map.Entry<?, ?> e : map.entrySet()) {
                validate(String.valueOf(e.getKey()), e.getValue());
            }
            return;
        }
        validateObject(fieldName, value);
    }

    private static void validateObject(String parentName, Object obj) {
        Field[] fields = obj.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);

            try {
                Object value = field.get(obj);
                validate(parentName + "." + field.getName(), value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void isTrue(boolean expression, String message, long value) {
        if (!expression)
            throw new IllegalArgumentException(
                    String.format(
                            message,
                            new Object[] { Long.valueOf(value) }));
    }

    public static void isTrue(boolean expression, String message, double value) {
        if (!expression)
            throw new IllegalArgumentException(
                    String.format(
                            message,
                            new Object[] { Double.valueOf(value) }));
    }

    public static void isTrue(
            boolean expression, String message, Object... values) {
        if (!expression)
            throw new IllegalArgumentException(
                    String.format(message, values));
    }

    public static void isTrue(boolean expression) {
        if (!expression)
            throw new IllegalArgumentException(
                    "The validated expression is false");
    }
}
