package com.nh.nsight.tcf.util;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@CopiedFrom(module = "tcf-util", sourceClass = "DateTimeUtil", category = UtilCategory.DATETIME, nativeUtility = true)
public final class DateTimeUtil implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-util";
    public static final String COPIED_FROM_CLASS = "DateTimeUtil";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private DateTimeUtil() {}

    public static String nowKst() {
        return OffsetDateTime.now(KST).toString();
    }

    public static String todayKst() {
        return LocalDate.now(KST).format(BASIC_DATE);
    }
}
