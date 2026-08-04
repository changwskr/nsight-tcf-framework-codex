package nhnis.fw.commons.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class StringUtil {

    public static int countSubstring(String s, String sub) {
        return countOccurrencesOf(s, sub);
    }

    public static String toDelimitedString(Object[] arr, String delim) {
        return arrayToDelimitedString(arr, delim);
    }

    public static String toDelimitedString(Collection<?> c, String delim) {
        return collectionToDelimitedString(c, delim);
    }

    public static String[] split(String str) {
        if (str == null) {
            return new String[0];
        }
        return str.trim().split("\\s");
    }

    public static String[] split(String str, String delim) {
        if (str == null) {
            return new String[0];
        }
        if (".".equals(delim)) {
            String[] arr = new String[str.length()];
            if (null != str) {
                int strLength = str.length();
                for (int i = 0; i < strLength; i++) {
                    arr[i] = (new StringBuilder(String.valueOf(str.charAt(i)))).toString();
                }
            }
            return arr;
        }
        return delimitedListToStringArray(str, delim);
    }

    public static boolean isEmpty(String str) {
        return !(str != null && !"".equals(str));
    }

    public static boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        return isNumberOnly(str.trim());
    }

    public static boolean isHangul(char inputChar) {
        String unicodeBlock = Character.UnicodeBlock.of(inputChar).toString();
        return !(!"HANGUL_JAMO".equals(unicodeBlock)
                && !"HANGUL_SYLLABLES".equals(unicodeBlock)
                && !"HANGUL_COMPATIBILITY_JAMO".equals(unicodeBlock));
    }

    public static boolean isHangulOnly(String inputStr) {
        if (inputStr == null || "".equals(inputStr)) {
            return false;
        }
        return isHangul(inputStr, true);
    }

    public static boolean isNumberOnly(String digit) {
        if (digit == null) {
            return false;
        }
        return isDigit(digit);
    }

    public static boolean isAlphabetNumberOnly(String str) {
        if (str == null) {
            return false;
        }
        return str.matches("^[0-9a-zA-Z]+$");
    }

    public static String lpad(String orgStr, char appender, int length) {
        if (orgStr == null) {
            return null;
        }
        int orgLen = orgStr.length();
        if (orgLen >= length) {
            return orgStr;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length - orgLen; i++) {
            sb.append(appender);
        }
        sb.append(orgStr);
        return sb.toString();
    }

    public static String rpad(String str, char appender, int length) {
        if (str == null) {
            return null;
        }
        int orgLen = str.length();
        if (orgLen >= length) {
            return str;
        }
        StringBuffer sb = new StringBuffer();
        sb.append(str);
        for (int i = 0; i < length - orgLen; i++) {
            sb.append(appender);
        }
        return sb.toString();
    }

    public static String ltrim(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceFirst("^\\s+", "");
    }

    public static String rtrim(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceFirst("\\s+$", "");
    }

    public static String left(String str, int len) {
        if (str == null) {
            return null;
        }
        if (str.length() <= len) {
            return str;
        }
        return str.substring(0, len);
    }

    public static String right(String str, int len) {
        if (str == null) {
            return null;
        }
        if (str.length() <= len) {
            return str;
        }
        return str.substring(str.length() - len);
    }

    public static int parseInt(String inputValue) {
        return Integer.parseInt(inputValue.trim());
    }

    public static long parseLong(String inputValue) {
        return Long.parseLong(inputValue.trim());
    }

    public static float parseFloat(String inputValue) {
        return Float.parseFloat(inputValue.trim());
    }

    public static double parseDouble(String inputValue) {
        return Double.parseDouble(inputValue.trim());
    }

    public static byte parseByte(String inputValue) {
        return Byte.parseByte(inputValue.trim());
    }

    public static BigDecimal parseBigDecimal(String inputValue) {
        return new BigDecimal(inputValue.trim());
    }

    public static int[] toIntArray(String strNum) {
        if (strNum == null) {
            return null;
        }
        int strNumLength = strNum.length();
        int[] arr = new int[strNumLength];
        for (int i = 0; i < strNumLength; i++) {
            arr[i] = Character.digit(strNum.charAt(i), 10);
        }
        return arr;
    }

    public static String nullToBlank(String inputValue) {
        if (inputValue == null) {
            return "";
        }
        return inputValue;
    }

    public static String join(String prefix, String suffix, String[] msg, String delim) {
        if (msg == null || msg.length == 0) {
            return null;
        }
        int msgLen = msg.length;
        StringBuffer sb = new StringBuffer(1000);
        for (int i = 0; i < msgLen; i++) {
            if (i > 0) {
                sb.append(delim);
            }
            sb.append(msg[i]);
        }
        if (prefix != null) {
            sb.insert(0, delim);
            sb.insert(0, prefix);
        }
        if (suffix != null) {
            sb.append(delim);
            sb.append(suffix);
        }
        return sb.toString();
    }

    public static String join(String[] msg, String delim) {
        return join((String) null, (String) null, msg, delim);
    }

    public static String checkNull(String param) {
        if (param == null) {
            return "";
        }
        return param;
    }

    public static String convertAtoB(String input, String condition, String output) {
        if (input == null) {
            return (condition == null) ? output : input;
        }
        return input.equals(condition) ? output : input;
    }

    public static Object nvl(Object obj, Object value) {
        return (obj == null) ? value : obj;
    }

    public static String nvl(String str, String value) {
        return (str == null) ? value : str;
    }

    public static String nvl(String str) {
        return (str == null) ? "" : str;
    }

    public static String lpad(String orgStr, int length, char appender) {
        if (orgStr == null) {
            return null;
        }
        int orgLen = orgStr.length();
        if (orgLen >= length) {
            return orgStr;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length - orgLen; i++) {
            sb.append(appender);
        }
        sb.append(orgStr);
        return sb.toString();
    }

    public static final boolean isNull(String str) {
        return !(str != null && !str.trim().equals(""));
    }

    public static String repeat(int repeat, String mask) {
        int j = 0;
        int k = 0;
        if (mask == null) {
            return null;
        }
        if (repeat <= 0) {
            return "";
        }
        int inputLength = mask.length();
        if (repeat == 1 || inputLength == 0) {
            return mask;
        }
        if (inputLength == 1 && repeat <= 8192) {
            char[] arrayOfChar = new char[repeat];
            int arrayOfCharLength = arrayOfChar.length;
            for (int m = 0; m < arrayOfCharLength; m++) {
                arrayOfChar[m] = mask.charAt(0);
            }
            return new String(arrayOfChar);
        }
        int outputLength = inputLength * repeat;
        switch (inputLength) {
            case 1:
                char ch = mask.charAt(0);
                char[] output1 = new char[outputLength];
                for (j = repeat - 1; j >= 0; j--) {
                    output1[j] = ch;
                }
                return new String(output1);
            case 2:
                char ch0 = mask.charAt(0);
                char ch1 = mask.charAt(1);
                char[] output2 = new char[outputLength];
                for (k = repeat * 2 - 2; k >= 0; k -= 2) {
                    output2[k] = ch0;
                    output2[k + 1] = ch1;
                }
                return new String(output2);
            default:
                break;
        }
        StringBuffer buf = new StringBuffer(outputLength);
        for (int i = 0; i < repeat; i++) {
            buf.append(mask);
        }
        return buf.toString();
    }

    public static String repeat(String str, String mask) {
        return repeat(mask, (str != null) ? str.length() : 0);
    }

    public static String repeat(Integer str, String mask) {
        return repeat(mask, (str != null) ? String.valueOf(str).length() : 0);
    }

    public static String repeat(long str, String mask) {
        return repeat(mask, String.valueOf(str).length());
    }

    public static String repeat(Long str, String mask) {
        return repeat(mask, (str != null) ? String.valueOf(str).length() : 0);
    }

    public static String repeat(float str, String mask) {
        return repeat(mask, String.valueOf(str).length());
    }

    public static String repeat(Float str, String mask) {
        return repeat(mask, (str != null) ? String.valueOf(str).length() : 0);
    }

    public static String repeat(double str, String mask) {
        return repeat(mask, String.valueOf(str).length());
    }

    public static String repeat(Double str, String mask) {
        return repeat(mask, (str != null) ? String.valueOf(str).length() : 0);
    }

    public static String repeat(short str, String mask) {
        return repeat(mask, String.valueOf(str).length());
    }

    public static String repeat(Short str, String mask) {
        return repeat(mask, (str != null) ? String.valueOf(str).length() : 0);
    }

    public static String repeat(BigDecimal str, String mask) {
        return repeat(mask, (str != null) ? str.toPlainString().length() : 0);
    }

    public static String repeat(BigInteger str, String mask) {
        return repeat(mask, (str != null) ? str.toString().length() : 0);
    }

    public static String repeat(String mask, int repeat) {
        int j = 0;
        int k = 0;
        if (mask == null) {
            return null;
        }
        if (repeat <= 0) {
            return "";
        }
        int inputLength = mask.length();
        if (repeat == 1 || inputLength == 0) {
            return mask;
        }
        if (inputLength == 1 && repeat <= 8192) {
            return padding(repeat, mask.charAt(0));
        }
        int outputLength = inputLength * repeat;
        switch (inputLength) {
            case 1:
                char ch = mask.charAt(0);
                char output1[] = new char[outputLength];

                for (j = repeat - 1; j >= 0; j--) {
                    output1[j] = ch;
                }
                return new String(output1);
            case 2:
                char ch0 = mask.charAt(0);
                char ch1 = mask.charAt(1);
                char output2[] = new char[outputLength];
                for (k = repeat * 2 - 2; k >= 0; k--) {
                    output2[k] = ch0;
                    output2[k + 1] = ch1;
                    k--;
                }
                return new String(output2);
            default:
                break;
        }
        StringBuffer buf = new StringBuffer(outputLength);
        for (int i = 0; i < repeat; i++) {
            buf.append(mask);
        }
        return buf.toString();
    }

    private static String padding(int repeat, char padChar)
            throws IndexOutOfBoundsException {
        if (repeat < 0)
            throw new IndexOutOfBoundsException(
                    "Cannot pad a negative amount: " + repeat);
        char[] buf = new char[repeat];
        if (null != buf) {
            int bufLength = buf.length;
            for (int i = 0; i < bufLength; i++) {
                buf[i] = padChar;
            }
        }
        return new String(buf);
    }

    public static boolean hasText(String str) {
        int strlen = 0;
        if (str == null || (strlen = str.length()) == 0)
            return false;
        for (int i = 0; i < strlen; i++) {
            if (!Character.isWhitespace(str.charAt(i)))
                return true;
        }
        return false;
    }

    private static int countOccurrencesOf(String s, String sub) {
        if (s == null || sub == null || "".equals(sub))
            return 0;
        int count = 0;
        int pos = 0;
        int idx = 0;
        while ((idx = s.indexOf(sub, pos)) != -1) {
            count++;
            pos = idx + sub.length();
        }
        return count;
    }

    public static String replace(
            String inString, String oldPattern, String newPattern) {
        if (inString == null)
            return null;
        if (oldPattern == null || newPattern == null)
            return inString;
        StringBuffer sbuf = new StringBuffer();
        int pos = 0;
        int index = inString.indexOf(oldPattern);
        int patLen = oldPattern.length();
        while (index >= 0) {
            sbuf.append(inString.substring(pos, index));
            sbuf.append(newPattern);
            pos = index + patLen;
            index = inString.indexOf(oldPattern, pos);
        }
        sbuf.append(inString.substring(pos));
        return sbuf.toString();
    }

    public static String delete(String inString, String pattern) {
        return replace(inString, pattern, "");
    }

    private static String[] delimitedListToStringArray(
            String s, String delim) {
        if (s == null)
            return new String[0];
        if (delim == null)
            return new String[] { s };
        List<String> l = new LinkedList<>();
        int pos = 0;
        int delPos = 0;
        while ((delPos = s.indexOf(delim, pos)) != -1) {
            l.add(s.substring(pos, delPos));
            pos = delPos + delim.length();
        }
        if (pos <= s.length())
            l.add(s.substring(pos));
        return l.<String>toArray(new String[l.size()]);
    }

    private static String arrayToDelimitedString(
            Object[] arr, String delim) {
        if (arr == null)
            return "null";
        StringBuffer sb = new StringBuffer();
        int arrLength = arr.length;
        for (int i = 0; i < arrLength; i++) {
            if (i > 0)
                sb.append(delim);
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private static String collectionToDelimitedString(
            Collection<?> c, String delim) {
        if (c == null)
            return "null";
        StringBuffer sb = new StringBuffer();
        Iterator<?> it = c.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (i++ > 0)
                sb.append(delim);
            sb.append(it.next());
        }
        return sb.toString();
    }

    public static String capitalize(String str) {
        return changeFirstCharacterCase(true, str);
    }

    public static String uncapitalize(String str) {
        return changeFirstCharacterCase(false, str);
    }

    private static String changeFirstCharacterCase(
            boolean capitalize, String str) {
        int strLen = 0;
        if (str == null || (strLen = str.length()) == 0)
            return str;
        StringBuffer buf = new StringBuffer(strLen);
        if (capitalize) {
            buf.append(Character.toUpperCase(str.charAt(0)));
        } else {
            buf.append(Character.toLowerCase(str.charAt(0)));
        }
        buf.append(str.substring(1));
        return buf.toString();
    }

    private static boolean isHangul(String inputStr, boolean full) {
        char[] chars = inputStr.toCharArray();
        int charsLength = chars.length;
        if (!full) {
            for (int j = 0; j < charsLength; j++) {
                if (isHangul(chars[j]))
                    return true;
            }
            return false;
        }
        for (int i = 0; i < charsLength; i++) {
            if (!isHangul(chars[i]))
                return false;
        }
        return true;
    }

    private static boolean isDigit(String digit) {
        if (digit == null)
            return false;
        char[] chars = digit.toCharArray();
        int charsLength = chars.length;
        for (int i = 0; i < charsLength; i++) {
            if (!Character.isDigit(chars[i]))
                return false;
        }
        return true;
    }

    static boolean isFormattedString(String string, String pattern) {
        if (string == null || pattern == null)
            return false;
        return string.matches(pattern);
    }

    static String toZipCodePattern(String string) {
        if (string == null)
            return "";
        if (string.length() != 6 || !isDigit(string))
            return "";
        StringBuffer buffer = new StringBuffer();
        buffer.append(string.substring(0, 3));
        buffer.append('-');
        buffer.append(string.substring(3, 6));
        return buffer.toString();
    }

    static String toErNoPattern(String string) {
        if (string == null)
            return "";
        if (string.length() != 10 || !isDigit(string))
            return "";
        StringBuffer buffer = new StringBuffer();
        buffer.append(string.substring(0, 3));
        buffer.append('-');
        buffer.append(string.substring(3, 5));
        buffer.append('-');
        buffer.append(string.substring(5, 10));
        return buffer.toString();
    }

    static String toSsNoPattern(String string) {
        if (string == null)
            return "";
        if (string.length() != 13 || !isDigit(string))
            return "";
        StringBuffer buffer = new StringBuffer();
        buffer.append(string.substring(0, 6));
        buffer.append('-');
        buffer.append(string.substring(6));
        return buffer.toString();
    }

    public static boolean isBlank(String str) {
        int strLen = 0;
        if (str == null || (strLen = str.length()) == 0)
            return true;
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i)))
                return false;
        }
        return true;
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static String clean(String str) {
        return (str == null) ? "" : str.trim();
    }

    public static String trim(String str) {
        return (str == null) ? null : str.trim();
    }

    public static String trimToNull(String str) {
        String ts = trim(str);
        return isEmpty(ts) ? null : ts;
    }

    public static String trimToEmpty(String str) {
        return (str == null) ? "" : str.trim();
    }

    public static String defaultString(String str) {
        return (str == null) ? "" : str;
    }

    public static String defaultString(String str, String defaultStr) {
        return (str == null) ? defaultStr : str;
    }

    public static String defaultIfBlank(String str, String defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }

    public static String defaultIfEmpty(String str, String defaultStr) {
        return isEmpty(str) ? defaultStr : str;
    }

    public static String join(Object[] array) {
        return join(array, (String) null);
    }

    public static String join(Object[] array, char separator) {
        if (array == null)
            return null;
        return join(array, separator, 0, array.length);
    }

    public static String join(
            Object[] array, char separator, int startIndex, int endIndex) {
        if (array == null)
            return null;
        int bufSize = endIndex - startIndex;
        if (bufSize <= 0)
            return "";
        bufSize *= ((array[startIndex] == null)
                ? 16
                : array[startIndex].toString().length()) + 1;
        StringBuffer buf = new StringBuffer(bufSize);
        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex)
                buf.append(separator);
            if (array[i] != null)
                buf.append(array[i]);
        }
        return buf.toString();
    }

    public static String join(Object[] array, String separator) {
        if (array == null)
            return null;
        return join(array, separator, 0, array.length);
    }

    public static String join(
            Object[] array, String separatorVal,
            int startIndex, int endIndex) {
        if (array == null)
            return null;
        String separator = separatorVal;
        if (separator == null)
            separator = "";
        int bufSize = endIndex - startIndex;
        if (bufSize <= 0)
            return "";
        bufSize *= ((array[startIndex] == null)
                ? 16
                : array[startIndex].toString().length())
                + separator.length();
        StringBuffer buf = new StringBuffer(bufSize);
        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex)
                buf.append(separator);
            if (array[i] != null)
                buf.append(array[i]);
        }
        return buf.toString();
    }

    @SuppressWarnings("rawtypes")
    public static String join(Iterator iterator, char separator) {
        if (iterator == null)
            return null;
        if (!iterator.hasNext())
            return "";
        Object first = iterator.next();
        if (!iterator.hasNext())
            return ObjectUtil.toString(first);
        StringBuffer buf = new StringBuffer(256);
        if (first != null)
            buf.append(first);
        while (iterator.hasNext()) {
            buf.append(separator);
            Object obj = iterator.next();
            if (obj != null)
                buf.append(obj);
        }
        return buf.toString();
    }

    @SuppressWarnings("rawtypes")
    public static String join(Iterator iterator, String separator) {
        if (iterator == null)
            return null;
        if (!iterator.hasNext())
            return "";
        Object first = iterator.next();
        if (!iterator.hasNext())
            return ObjectUtil.toString(first);
        StringBuffer buf = new StringBuffer(256);
        if (first != null)
            buf.append(first);
        while (iterator.hasNext()) {
            if (separator != null)
                buf.append(separator);
            Object obj = iterator.next();
            if (obj != null)
                buf.append(obj);
        }
        return buf.toString();
    }

    @SuppressWarnings("rawtypes")
    public static String join(Collection collection, char separator) {
        if (collection == null)
            return null;
        return join(collection.iterator(), separator);
    }

    @SuppressWarnings("rawtypes")
    public static String join(Collection collection, String separator) {
        if (collection == null)
            return null;
        return join(collection.iterator(), separator);
    }

    public static String cutCarriageReturn(String oriString) {
        int index = oriString.indexOf("\n");
        while (index != -1) {
            String head = oriString.substring(0, index);
            if (index != oriString.length() - 1) {
                String tail = oriString.substring(index);
                oriString = String.valueOf(String.valueOf(head)) + tail;
            } else {
                oriString = head;
            }
            index = oriString.indexOf("\n");
        }
        return oriString;
    }
}
