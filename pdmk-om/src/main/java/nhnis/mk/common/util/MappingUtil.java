package nhnis.mk.common.util;

import java.lang.reflect.Field;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Map 와 DTO 간 자동 매핑 유틸리티
 *
 * @author pdmk Generator
 * @since 2026.07.31
 */
public class MappingUtil {

    private static final Logger log =
            LoggerFactory.getLogger(MappingUtil.class);

    /**
     * Map 를 DTO 로 자동 매핑
     *
     * @param map 소스 Map
     * @param dtoClass 대상 DTO 클래스
     * @param <T> DTO 타입
     * @return 매핑된 DTO 인스턴스
     */
    @SuppressWarnings("unchecked")
    public static <T> T mapToDto(
            Map<String, Object> map,
            Class<T> dtoClass) {
        try {
            T dto = dtoClass.getDeclaredConstructor().newInstance();

            // DTO 의 모든 필드 순회
            Field[] fields = dtoClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);

                String fieldName = field.getName();

                // Map 에서 값 가져오기 (대소문자 무시)
                Object value = null;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    String mapKey = entry.getKey();
                    if (mapKey.equalsIgnoreCase(fieldName)) {
                        value = entry.getValue();
                        break;
                    }
                }

                // 값이 있으면 설정
                if (value != null) {
                    // 타입 변환
                    Object convertedValue =
                            convertType(value, field.getType());
                    field.set(dto, convertedValue);
                }
            }

            return dto;
        } catch (Exception e) {
            log.error(
                    "Map to DTO 매핑 오류: " + e.getMessage(), e);
            throw new RuntimeException(
                    "Map to DTO 매핑 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 타입 변환
     *
     * @param value 변환할 값
     * @param targetType 대상 타입
     * @return 변환된 값
     */
    private static Object convertType(
            Object value,
            Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // 이미 같은 타입이면 직접 반환
        if (targetType.isInstance(value)) {
            return value;
        }

        try {
            // Number 타입 변환
            if (targetType == Integer.class || targetType == int.class) {
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                if (value instanceof String) {
                    return Integer.parseInt(((String) value).trim());
                }
            }

            if (targetType == Long.class || targetType == long.class) {
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                if (value instanceof String) {
                    return Long.parseLong(((String) value).trim());
                }
            }

            if (targetType == Double.class || targetType == double.class) {
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                if (value instanceof String) {
                    return Double.parseDouble(((String) value).trim());
                }
            }

            if (targetType == Float.class || targetType == float.class) {
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                }
                if (value instanceof String) {
                    return Float.parseFloat(((String) value).trim());
                }
            }

            if (targetType == Boolean.class ||
                    targetType == boolean.class) {
                if (value instanceof Boolean) {
                    return value;
                }
                if (value instanceof String) {
                    return Boolean.parseBoolean((String) value);
                }
                if (value instanceof Number) {
                    return ((Number) value).intValue() == 1;
                }
            }

            // String 타입 변환
            if (targetType == String.class) {
                if (value instanceof String) {
                    return value;
                }
                return value.toString();
            }

            // 그 외 타입은 그대로 반환
            return value;

        } catch (Exception e) {
            log.warn(
                    "타입 변환 실패: {} -> {}, 원본값: {}",
                    value.getClass().getName(),
                    targetType.getName(),
                    value);
            return value;
        }
    }
}
