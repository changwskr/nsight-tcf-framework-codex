package nhnis.fw.commons.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 일반적으로 사용되는 Format 관련 함수들의 모아놓은 클래스이다.
 * <ul>
 * <li>주민 번호, 사업자 번호 format
 * <li>금액의 천단위 구분자 format, 금액에서 소수점 구분자 format
 * <li>날짜 format, 시간 format
 * </ul>
 */
public class CommonUtil {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BigDecimalUtil.class);

    private static final String threadContextDefaultValue = "-";
    private static final List<String> threadContextkeys = new ArrayList<String>(
            Arrays.asList("btIoKind", "mciServiceId", "Std_gbl_id", "Orgtr_gbl_id",
                    "Trz_gbl_id", "guid", "userId", "ip", "ptServiceId", "btServiceId", "btUuid"));

    /**
     * StackTrace에서 String을 추출 하는 Method
     * @param throwable exception절에서 catch한 Throwable e 또는 Exception e
     * @return String StackTrace String
     */
    public static String makeStackTraceString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\\n");
        }

        return sb != null ? sb.toString() : "";
    }

    /**
     * 원하는 형식에 맞춰 문자열 포맷팅 변환.
     * <p>
     * <pre>
     * "123-45-67890" = formatStringDelimiter("1234567890", "-", new int[]{3,2,5})
     * "123-45-67890102" = formatStringDelimiter("1234567890102", "-", new int[]{3,2,1,1})
     * // 나머지 문자열은 마지막 구간에 자동 포함
     * </pre>
     *
     * @param str 문자열
     * @param delimiter 삽입될 문자열
     * @param num 삽입될 문자열 길이 (마지막 요소는 나머지 전체를 포함)
     * @return String
     */
    public static String formatStringDelimiter(String str, String delimiter, int[] num) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        if (null == num || num.length == 1) {
            return str;
        }
        int size = num.length;
        StringBuilder stringBuilder = new StringBuilder(str.length() + size);
        int index = 0;
        int strLength = str.length();

        for (int i = 0; i < size; i++) {
            int chunkSize = num[i];

            // 마지막 요소인 경우, 남은 문자열 전체를 포함
            if (i == size - 1) {
                chunkSize = strLength - index;
            }

            // 범위를 초과하지 않도록 제한
            if (index + chunkSize > strLength) {
                chunkSize = strLength - index;
            }

            if (chunkSize > 0) {
                stringBuilder.append(str.subSequence(index, index + chunkSize));
            }

            if (i != size - 1) {
                stringBuilder.append(delimiter);
            }
            index += chunkSize;
        }
        return stringBuilder.toString();
    }

    /**
     * 사업자번호, 주민번호 포맷팅.
     * <p>
     * <pre>
     * "123456-1234567" = formatNO("1234561234567", false);
     * "123456-*******" = formatNO("1234561234567", true);
     *
     * "123-45-6789" = formatNO("1234567890", false);
     * "123-45-*****" = formatNO("1234567890", true);
     * </pre>
     *
     * @param no 주민번호 또는 사업자 번호
     * @return java.lang.String
     */
    public static String formatNO(String no) {
        return formatNO(no, true);
    }

    /**
     * 사업자번호, 주민번호 포맷팅.
     * <p>
     * <pre>
     * "123456-1234567" = formatNO("1234561234567", false);
     * "123456-*******" = formatNO("1234561234567", true);
     *
     * "123-45-6789" = formatNO("1234567890", false);
     * "123-45-*****" = formatNO("1234567890", true);
     * </pre>
     *
     * @param no 주민번호 또는 사업자 번호
     * @param mask 주민번호 뒷자리 마스킹 여부
     * @return String
     */
    public static String formatNO(String no, boolean mask) {

        if (StringUtils.isBlank(no)) {
            return "";
        }
        // 입력값에 포함된 하이픈 제거
        String noClean = no.trim().replace("-", "");
        StringBuffer sb = new StringBuffer(noClean);
        if (sb.length() == 10) { //10 자리 포맷 (사업자번호)
            sb.insert(3, "-").insert(6, "-");
            if (mask) {
                sb.replace(7, 12, "*****"); // 뒷 5 자리 마스킹 (67890 -> *****)
            }
        } else if (sb.length() == 13) {
            if (sb.charAt(0) == '0' && sb.charAt(6) == '0' && sb.charAt(7) == '0') { //13자리 포맷
                sb.deleteCharAt(0);
                sb.deleteCharAt(5);
                sb.deleteCharAt(5);
                sb.insert(3, "-").insert(6, "-");
            } else { //개인사업자 주민번호
                sb.insert(6, "-");
                if (mask) {
                    sb.replace(7, 14, "*******");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 금액 문자열을 comma 표시 여부와 소수점 표시 여부를 확인하여 format된 문자열로 반환한다.
     * <p>
     * <ul>
     * <li>금액 문자열이 음수(-)이면 반환된 값에도 음수(-)가 붙는다.</li>
     * <li>금액 문자열에 소수점이 있고 point 값이 0이라면 둘 중 큰 숫자만큼 소수점 자리가 나온다.</li>
     * <li>금액 문자열에 소수점이 있고 point 값이 0이라면 금액 문자열의 소수점이하가 0이면 절삭처리한다. 하지만 값이 있다면 절삭하지 않는다.</li>
     * </ul>
     * <pre>
     * "121,564,899.231" = CommonUtil.formatMoney("0121564899.231", true, 0)
     * "121,564,899.231" = CommonUtil.formatMoney("0121564899.231", true, 2)
     * "121564899.231" = CommonUtil.formatMoney("0121564899.231", false, 2)
     * "121,564,899.2310" = CommonUtil.formatMoney("0121564899.231", true, 4)
     * "121564899.2310" = CommonUtil.formatMoney("0121564899.231", false, 4)
     * "-121,564,899.2310" = CommonUtil.formatMoney("-0121564899.231", true, 4)
     * "-121564899.2310" = CommonUtil.formatMoney("-0121564899.231", false, 4)
     * </pre>
     * @param money 금액
     * @param comma 금액 표시시 천단위 구분자(,) 삽입여부
     * @param point 소수점의 자릿수
     * @return String
     */
    public static String formatMoney(String money, boolean comma, int pointVal) {
        int point = pointVal;
        StringBuilder pattern = new StringBuilder();
        if (comma) {
            pattern.append("#,##0");
        } else {
            pattern.append("###0");
        }
        int index = money.lastIndexOf('.');
        if (point < 1) {
            pattern.append(".");
            index = money.length() - (index + 1);
            pattern.append(StringUtils.repeat('#', Math.max(index, 1)));
        } else {
            pattern.append(".");
            if (index < 0) {
                pattern.append(StringUtils.repeat('0', point));
            } else {
                index = money.length() - (index + 1);
                point = NumberUtils.max(index, point, 1);
                pattern.append(StringUtils.repeat('0', point));
            }
        }
        DecimalFormat decimalFormat = new DecimalFormat(pattern.toString());
        double lAmt = Double.parseDouble(money.trim());
        return decimalFormat.format(lAmt);
    }

    public static final String DEFAULT_DELIMITER_DATE = ".";

    /**
     * 날짜 8자리를 구분자(.)를 넣어서 반환.
     * <p>
     * <pre>
     * "2013.01.31" = formatDate("20130131", DEFAULT_DELIMITER_DATE);
     * "2013/01/31" = formatDate("20130131", "/");
     * </pre>
     * @param str (yyyymmdd)
     * @return String
     */
    public static String formatDate(String str, String delimiter) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        if (str.length() != 8) {
            return str;
        }
        if (("00000000".equals(str)) || ("        ".equals(str))) {
            return "";
        }
        return str.substring(0, 4) + delimiter + str.substring(4, 6) + delimiter + str.substring(6, 8);
    }

    public static final String DEFAULT_DELIMITER_TIME = ":";

    /**
     * 시분초 6자리를 구분자(:)를 넣어서 반환.
     * <p>
     * <pre>
     * "16:45:55" = formatTime("164555", DEFAULT_DELIMITER_TIME);
     * "16-45-55" = formatTime("164555", "-");
     * </pre>
     * @param str (HHMMSS)
     * @return String (HH:MM:SS)
     */
    public static String formatTime(String str, String delimiter) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        if (str.length() != 6) {
            return str;
        }
        return str.substring(0, 2) + delimiter + str.substring(2, 4) + delimiter + str.substring(4, 6);
    }

    /**
     * 특수 문자 포함여부 체크.
     * <p>
     * <pre>
     * 지정된 Key가 없으면 Character제공 특수문자 체크하고 Key가 있으면 해당 키를 체크
     * </pre>
     * @param str 문자 조합
     * @return boolean
     */
    public static boolean isSpecialKey(String str) {
        return isSpecialKey(str, null);
    }

    /**
     * 특수 문자열
     */
    private static final String DEFAULT_SPECIAL_KEY = "!,*&^%$#@~;|>+";

    /**
     * 특수 문자 포함여부 체크
     * <p>

     * 지정된 Key가 없으면 Character제공 특수문자 체크하고 Key가 있으면 해당 키를 체크
     * </pre>
     * @param str 문자 조합
     * @param keys 문자 키 (default key = "!,*&^%$#@~;|>+")
     * @return boolean
     */
    public static boolean isSpecialKey(String strVal, String keysVal) {
        String str = strVal;
        String keys = keysVal;
        boolean bReturn = false;

        if (keys == null) {
            char cCharAt = '\0';
            if (null != str) {
                int strLength = str.length();
                for (int i = 0; i < strLength; i++) {
                    cCharAt = str.charAt(i);
                    if (!Character.isJavaIdentifierPart(cCharAt)) {
                        bReturn = true;
                        break;
                    }
                }
            }
        } else {
            keys = StringUtils.defaultIfBlank(keys, DEFAULT_SPECIAL_KEY);
            str = StringUtils.defaultIfBlank(str, "");

            if (null != keys) {
                int keysLength = keys.length();
                for (int i = 0; i < keysLength; i++) {
                    if (str.indexOf(keys.charAt(i)) > -1) {
                        bReturn = true;
                        break;
                    }
                }
            }
        }
        return bReturn;
    }

    /**
     * 카드번호 중간8자리 *표처리.
     * <p>
     * <pre>
     * "1234-****-****-1234" = formatCardNoWithAsterisk("1234567856781234");
     * "1234-****-****-1234" = formatCardNoWithAsterisk("1234-5678-5678-1234");
     * </pre>
     * @param cardNo 카드번호(16자리)
     * @return String 카드번호 (1111-****-****-1111)
     */
    public static String formatCardNoWithAsterisk(String cardNoVal) {
        String cardNo = cardNoVal;

        cardNo = StringUtils.defaultIfBlank(cardNo, "");
        cardNo = cardNo.replaceAll("[-]", "");

        if (cardNo.length() == 16) {
            cardNo = cardNo.substring(0, 4) + "-****-****-" + cardNo.substring(12, 16);
        }
        return cardNo;
    }

    /**
     * 유효기간 *표처리.
     * <p>
     * <pre>
     * "20**-**" = formatCardValidDateWithAsterisk("201302");
     * "20**-**" = formatCardValidDateWithAsterisk("2013-02");
     * </pre>
     * @param validDate 유효기간(6자리)
     * @return String 유효기간 (11**-1*)
     */
    public static String formatCardValidDateWithAsterisk(String validDateVal) {
        String validDate = validDateVal;
        validDate = StringUtils.defaultIfBlank(validDate, "");
        validDate = validDate.replaceAll("[-]", "");

        if (validDate.length() == 6) {
            validDate = validDate.substring(0, 2) + "***-**"; // 앞 2 자리 (년) 노출, 뒷 4 자리 (월) 마스킹 -> 20**-**
        }
        return validDate;
    }

    /**
     * 전화번호의 마지막 번호를 마스킹.
     * <p>
     * 뒷 4자리 마스킹 처리<br>
     * 입력값이 형식에 안 맞으면 "" 값을 리턴한다.
     * <pre>
     * ex) 010-1111-2222 -> 010-1111-****
     * </pre>
     * @param phoneNumber
     * @return String
     */
    public static String formatPhoneNumberWithAsterisk(String phoneNumber) {
        if (StringUtils.isBlank(phoneNumber) || phoneNumber.length() <= 4) {
            return "";
        }
        int index = phoneNumber.length();
        return phoneNumber.substring(0, index - 4) + "****";
    }

    /**
     * email 주소를 마스킹
     * <p>
     * 앞 4자리 마스킹 처리<br>
     * 입력값이 형식에 안 맞으면 "" 값을 리턴한다.
     * <pre>
     * ex) aaabbcc@nonghyup.com -> ****bcc@nonghyup.com
     * </pre>
     * @param email
     * @return String
     */
    public static String formatEMailWithAsterisk(String email) {
        if (StringUtils.isBlank(email)) {
            return "";
        }
        int index = email.indexOf('@');
        if (index < 1) {
            return "";
        }
        if (index > 4) {
            return StringUtils.repeat("*", 4) + email.substring(4);
        } else {
            return StringUtils.repeat("*", index) + email.substring(index);
        }
    }

    /**
     * 인터넷 주소 항목을 마스킹.
     * <p>
     * ipv4 : 17~24 비트 영역<br>
     * ipv6 : 113~128 비트 영역<br>
     * 입력값이 형식에 안 맞으면 "" 값을 리턴한다.
     * <pre>
     * ex) 123.123.123.123 -> 123.123.***.123
     *     21DA:00D3:0000:2F3B:02AA:00FF:FE28:9C5A ->
     *     21DA:00D3:0000:2F3B:02AA:00FF:FE28:****
     * </pre>
     * @param ip
     * @return String
     */
    public static String formatIPWithAsterisk(String ipVal) {
        String ip = ipVal;
        if (StringUtils.isBlank(ip)) {
            return "";
        }
        if (ip.indexOf('.') > -1) { // ipv4
            String temp[] = ip.split("[.]");
            if (temp.length == 4) {
                temp[2] = "***";
                return StringUtils.join(temp, ".");
            } else {
                return "";
            }
        } else if (ip.indexOf(':') > -1) { // ipv6
            String temp[] = ip.split("[:]");
            if (temp.length == 8) {
                temp[7] = "****";
                return StringUtils.join(temp, ":");
            } else {
                return "";
            }
        }
        return "";
    }

    /**
     * UTF-8 인코딩으로 byte 수에 맞춰 문자열 잘라내는 메소드
     * 한글이 바이트 수에 걸쳐서 깨지는 경우 해당 한글 문자를 생략한다.
     */
    public static String cutWordUtf8(String wordVal, int byteLength) {
        String word = wordVal;
        try {
            double max_utf8_char_length = 4.0;
            if (word.length() > byteLength) {
                word = word.substring(0, byteLength);
            }
            if (word.length() > byteLength / max_utf8_char_length) {
                int residual = word.getBytes("UTF-8").length - byteLength;
                if (residual > 0) {
                    int tempResidual = residual, start, end = word.length();
                    while (tempResidual > 0) {
                        start = end - ((int) Math.ceil((double) tempResidual / max_utf8_char_length));
                        tempResidual = tempResidual - word.substring(start, end).getBytes("UTF-8").length;
                        end = start;
                    }
                    word = word.substring(0, end);
                }
            }
            return word;
        } catch (UnsupportedEncodingException e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(e.getMessage());
            }
        }
        return word;
    }

    /**
     * @description 현재 구동중인 서버가 Paas 환경인지 반환
     * @method isPaas
     * @author CS527980
     * @return
     * @date   2022. 7. 27.
     */
    public static boolean isPaas() {
        boolean paas = false;
        if (System.getenv().containsKey("INSTANCE_GUID")) {
            paas = true;
        } else {
            paas = false;
        }
        return paas;
    }

    public static void getInform(HttpServletRequest request) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("====================getInform(HttpServletRequest request)====================");
            LOGGER.debug("--------------------------------header--------------------------------");
            Enumeration<String> headers = request.getHeaderNames();
            if (headers != null) {
                while (headers.hasMoreElements()) {
                    String header = headers.nextElement();
                    LOGGER.debug("header {} : {}", header, request.getHeader(header));
                }
            }
            LOGGER.debug("--------------------------------attribute--------------------------------");
            Enumeration<String> attributes = request.getAttributeNames();
            if (attributes != null) {
                while (attributes.hasMoreElements()) {
                    String attribute = attributes.nextElement();
                    LOGGER.debug("attribute {} : {}", attribute, request.getAttribute(attribute));
                }
            }

            LOGGER.debug("--------------------------------parameter--------------------------------");
            Enumeration<String> parameters = request.getParameterNames();
            if (parameters != null) {
                while (parameters.hasMoreElements()) {
                    String parameter = parameters.nextElement();
                    LOGGER.debug("parameter {} : {}", parameter, request.getParameter(parameter));
                }
            }
            LOGGER.debug("--------------------------------body--------------------------------");
            BufferedReader bufferedReader = request.getReader();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                LOGGER.debug(line);
            }
        }
    }

    /**
     * @description IP주소 반환
     * @method getRemoteIp
     * @author CS530226
     * @param request
     * @return sourceIp
     * @date   2022. 7. 27.
     */
    public static String getRemoteIp(HttpServletRequest request) {
        String sourceIp = request.getHeader("X-Forwarded-For");

        //if (logger.isDebugEnabled()) logger.debug("(getRemoteIp) :: X-Forwarded-For :: {} ", sourceIp);

        if (sourceIp == null || sourceIp.length() == 0 || "unknown".equalsIgnoreCase(sourceIp)) {
            sourceIp = request.getHeader("Proxy-Client-IP");
            //if (logger.isDebugEnabled()) logger.debug("(getRemoteIp) :: Proxy-Client-IP :: {} ", sourceIp);
        }
        if (sourceIp == null || sourceIp.length() == 0 || "unknown".equalsIgnoreCase(sourceIp)) {
            sourceIp = request.getHeader("WL-Proxy-Client-IP");
            //if (logger.isDebugEnabled()) logger.debug("(getRemoteIp) :: WL-Proxy-Client-IP :: {} ", sourceIp);
        }
        if (sourceIp == null || sourceIp.length() == 0 || "unknown".equalsIgnoreCase(sourceIp)) {
            sourceIp = request.getHeader("HTTP_CLIENT_IP");
            //if (logger.isDebugEnabled()) logger.debug("(getRemoteIp) :: HTTP_CLIENT_IP :: {} ", sourceIp);
        }
        if (sourceIp == null || sourceIp.length() == 0 || "unknown".equalsIgnoreCase(sourceIp)) {
            sourceIp = request.getHeader("HTTP_X_FORWARDED_FOR");
            //if (logger.isDebugEnabled()) logger.debug("(getRemoteIp) :: HTTP_X_FORWARDED_FOR :: {} ", sourceIp);
        }
        //위에 언급한 헤더에 모두 없는 경우 getRemoteAddr() 를 통해 ip 추출
        if (sourceIp == null || sourceIp.length() == 0 || "unknown".equalsIgnoreCase(sourceIp)) {
            sourceIp = request.getRemoteAddr();
            //if (logger.isDebugEnabled()) logger.debug("(getRemoteIp) :: HttpServletRequest.getRemoteAddr() :: {} ", sourceIp);
        }

        if (sourceIp.indexOf(",") > -1) {
            sourceIp = sourceIp.split(",")[0];
        }

        //if (logger.isDebugEnabled()) logger.debug("(getRemoteIp) :: return IP  :: {} ", sourceIp);
        return sourceIp;
    }

    public static void clearThreadContext() {
        for (String s : threadContextkeys) {
            ThreadContext.remove(s);
        }
    }

    public static void initThreadContext() {
        for (String s : threadContextkeys) {
            ThreadContext.put(s, threadContextDefaultValue);
        }
    }

    public static Cookie[] getCookieArray(HttpServletRequest request) {
        return request.getCookies();
    }

    public static String getCookieValue(Cookie cookie) {
        return cookie.getValue();
    }

    /**
     * 전화번호의 중간 번호를 마스킹.
     * <p>
     * 가운데 번호 마스킹 처리<br>
     * 입력값이 형식에 안 맞으면 "" 값을 리턴한다.
     * <pre>
     * ex) 010-1111-2222 -> 010-****-2222
     * </pre>
     * @param phoneNumber
     * @return String
     */
    public static String elkFormatPhoneNumberWithAsterisk(String phoneNumber) {
        String maskingPhoneNumber = "";

        if (StringUtils.isBlank(phoneNumber) || phoneNumber.length() < 11) {
            return "";
        }

        try {
            // 핸드폰 번호 형식에 "-" 문자가 포함 된 경우
            if (phoneNumber.indexOf("-") > -1) {
                int firstDashIndex = phoneNumber.indexOf("-") + 1;
                int lastDashIndex = phoneNumber.lastIndexOf("-");
                int midNoLength = phoneNumber.substring(firstDashIndex, lastDashIndex).length();

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < midNoLength; i++) {
                    sb.append("*");
                }
                maskingPhoneNumber = phoneNumber.substring(0, phoneNumber.indexOf("-") + 1)
                        + sb.toString() + phoneNumber.substring(phoneNumber.lastIndexOf("-"));
            } else {
                maskingPhoneNumber = phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
            }
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("[elkFormatPhoneNumberWithAsterisk ERROR : [{}]", phoneNumber);
            }
        }

        return maskingPhoneNumber;
    }

    /**
     * 사업자번호, 주민번호 포멧팅.
     * <p>
     * <pre>
     * "123456-1234567" = formatNO("1234561234567", false);
     * "123456-*******" = formatNO("1234561234567", true);
     *
     * "123-45-6789" = formatNO("1234567890", false);
     * "123-45-*****" = formatNO("1234567890", true);
     * </pre>
     *
     * @param no 주민번호 또는 사업자 번호
     * @return java.lang.String
     */
    public static String elkFormatR1NoWithAsterisk(String r1No) {
        if (StringUtils.isEmpty(r1No)) {
            return "";
        }

        String ssn = r1No;
        if (ssn.indexOf("-") > -1) {
            ssn.replace("-", "");
        }

        return formatNO(ssn, true);
    }

    /**
     * 사용자명 마스킹.
     * <p>
     * <pre>
     * ex) 심청 -> *청
     * ex) 홍길동 -> 홍*동
     * ex) 김수한무 -> 김**무
     * </pre>
     *
     * @param 사용자명
     * @return java.lang.String
     */
    public static String elkFormatUserNameWithAsterisk(String userName) {
        String maskingValue = "";
        if (StringUtils.isEmpty(userName)) {
            return "";
        }

        try {

            if (userName.length() == 1) {
                maskingValue = "*";
            } else if (userName.length() == 2) {
                maskingValue = "*" + userName.substring(1);
            } else if (userName.length() == 3) {
                maskingValue = userName.substring(0, 1) + "*" + userName.substring(2);
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < userName.length() - 2; i++) {
                    sb.append("*");
                }
                maskingValue = userName.substring(0, 1) + sb.toString()
                        + userName.substring(userName.length() - 1);
            }
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("elkFormatUserNameWithAsterisk ERROR : [{}]", userName);
            }
        }
        return maskingValue;
    }

    /**
     * Exception 정보 간소화 조회
     * @param e
     * @return
     */
    public static String getSimpleExceptionInfo(Throwable e) {
        String exceptionInfo = "";
        try {
            if (ObjectUtils.isNotEmpty(e)) {
                StackTraceElement[] se = e.getStackTrace();
                String throwableExceptionClass = e.getClass().toString();
                String exceptionClassName = se[0].getClassName();
                String exceptionMethodName = se[0].getMethodName();
                int exceptionThrowableLine = se[0].getLineNumber();

                exceptionInfo = throwableExceptionClass + ": " + exceptionClassName + "."
                        + exceptionMethodName + ":" + exceptionThrowableLine;
            }
        } catch (Exception ex) {
            LOGGER.error("CommonUtil > getExceptionInfo exception.");
            LOGGER.error("{}", ex.getMessage());
            exceptionInfo = e.getMessage();
        }

        return exceptionInfo;
    }
}
