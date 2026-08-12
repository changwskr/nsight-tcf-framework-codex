package nhnis.fw.commons.util;

import java.math.BigDecimal;
import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BigDecimalUtil {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BigDecimalUtil.class);

    /**
     * 설명 : 설명 : Bigdecimal Object 를 반환한다.
     * null 또는 "" 값인 경우 0 값을 반환한다. (scale 설정 없음)
     * ex)
     * newBigDecimal("1701677800.82"); => 1701677800.82
     * newBigDecimal("1.7016778E9"); => 1701677800
     * newBigDecimal(null); => 0
     * newBigDecimal(""); => 0
     * @param value
     * @return Bigdecimal
     */
    public static BigDecimal newBigDecimal(Object value) {
        return new BigDecimal(unExponential(defaultString(value, "0")));
    }

    /**
     * 설명 : BigDecimal Object 를 반환한다.
     * null 또는 "" 값인 경우 0 값을 반환한다.
     * ex)
     * newBigDecimal("1701677800.82", 0, BigDecimal.ROUND_DOWN); => 1701677800
     * newBigDecimal("1.7016778E9", 0, BigDecimal.ROUND_DOWN); => 1701677800
     * newBigDecimal("1701677800.82", 1, BigDecimal.ROUND_DOWN); => 1701677800.8
     * newBigDecimal(null, 1, BigDecimal.ROUND_DOWN); => 0
     * newBigDecimal("", 1, BigDecimal.ROUND_DOWN); => 0
     * @param value
     * @param scale
     * @param round
     * @return Bigdecimal
     */
    public static BigDecimal newBigDecimal(Object value, int scale, int round) {
        return new BigDecimal(unExponential(defaultString(value, "0"))).setScale(scale, round);
    }

    /**
     * 설명 : 지수 표시된 수를 정수/실수 형태로 변경
     * null 또는 "" 값인 경우 NumberFormatException 이 발생한다.
     * ex)
     * unExponential("1.7016778E9"); => "1701677800"
     * unExponential(null); => NumberFormatException
     * unExponential(""); => NumberFormatException
     * @param value
     * @return String
     */
    public static String unExponential(Object value) {
        String number = String.valueOf(value);

        NumberFormat formatter = NumberFormat.getInstance();
        formatter.setGroupingUsed(false);

        number = formatter.format(new BigDecimal(number).doubleValue());

        return number;
    }

    /**
     * 설명 : 지수 표시된 수를 정수/실수 형태로 변경
     * null 또는 "" 값인 경우 defaultValue 값을 반환한다.
     * ex)
     * unExponential("1.7016778E9", "0"); => "1701677800"
     * unExponential(null, "0"); => "0"
     * unExponential("", "0"); => "0"
     * @param value
     * @param defaultValue
     * @return String
     */
    public static String unExponential(Object value, String defaultValue) {
        return unExponential(defaultString(value, defaultValue));
    }

    /**
     * 설명 : arg 값이 null 또는 "" 문자열인 경우 defaultValue 반환
     * @param arg
     * @param defaultValue
     * @return String
     */
    public static String defaultString(Object arg, String defaultValue) {
        if (arg == null) {
            return defaultValue;
        }

        String str = String.valueOf(arg);

        if ("".equals(str)) {
            return defaultValue;
        }

        return str;
    }

    /**
     * 설명 : arg 값이 null 또는 trim 후 "" 문자열인 경우 defaultValue 반환
     * @param arg
     * @param defaultValue
     * @return String
     */
    public static String defaultStringTrim(Object arg, String defaultValue) {
        if (arg == null) {
            return defaultValue;
        }

        return defaultString(String.valueOf(arg).trim(), defaultValue);
    }
}
