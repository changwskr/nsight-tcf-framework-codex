package com.nh.nsight.tcf.util.meta;

import com.nh.nsight.tcf.util.DateTimeUtil;
import com.nh.nsight.tcf.util.GuidGenerator;
import com.nh.nsight.tcf.util.MaskingUtils;
import com.nh.nsight.tcf.util.catalog.GatewayBusinessModuleCatalog;
import com.nh.nsight.tcf.util.tpmutil.tpcutil;
import com.nh.nsight.tcf.util.catalog.UjBusinessModuleDefinitions;
import com.nh.nsight.tcf.util.constant.CoreErrorCodeConstants;
import com.nh.nsight.tcf.util.constant.CoreTransactionLogConstants;
import com.nh.nsight.tcf.util.constant.TcfCacheNameConstants;
import com.nh.nsight.tcf.util.crypto.JwtHashUtils;
import com.nh.nsight.tcf.util.http.GatewayCookieParserUtils;
import com.nh.nsight.tcf.util.http.GatewaySessionHeaderRulesUtils;
import com.nh.nsight.tcf.util.http.GatewaySessionIdResolverUtils;
import com.nh.nsight.tcf.util.id.JwtIdUtils;
import com.nh.nsight.tcf.util.json.TpcJsonEscapeUtils;
import com.nh.nsight.tcf.util.logging.CoreTcfConsoleLog;
import com.nh.nsight.tcf.util.logging.GatewayProxyTraceUtils;
import com.nh.nsight.tcf.util.map.JwtMapValueUtils;
import com.nh.nsight.tcf.util.map.OmMapBodyUtils;
import com.nh.nsight.tcf.util.response.OmUpdownloadResponseUtils;
import com.nh.nsight.tcf.util.security.CoreTransactionControlExemptionUtils;
import com.nh.nsight.tcf.util.security.GatewayAuthExemptionUtils;
import com.nh.nsight.tcf.util.string.TcfStringUtils;
import java.util.List;

/**
 * tcf-util에 등록된 유틸리티 클래스 카탈로그.
 */
public final class TcfUtilRegistry {

    public record UtilEntry(
            String utilClass,
            String module,
            String sourceClass,
            UtilCategory category,
            boolean nativeUtility) {
    }

    /** 복사·통합된 유틸리티 목록 (런타임 조회용). */
    public static final List<UtilEntry> ENTRIES = List.of(
            entry(DateTimeUtil.class),
            entry(GuidGenerator.class),
            entry(MaskingUtils.class),
            entry(tpcutil.class),
            entry(TcfStringUtils.class),
            entry(GatewayCookieParserUtils.class),
            entry(GatewaySessionIdResolverUtils.class),
            entry(GatewaySessionHeaderRulesUtils.class),
            entry(OmMapBodyUtils.class),
            entry(JwtMapValueUtils.class),
            entry(JwtIdUtils.class),
            entry(JwtHashUtils.class),
            entry(TpcJsonEscapeUtils.class),
            entry(CoreTcfConsoleLog.class),
            entry(GatewayProxyTraceUtils.class),
            entry(OmUpdownloadResponseUtils.class),
            entry(GatewayAuthExemptionUtils.class),
            entry(CoreTransactionControlExemptionUtils.class),
            entry(GatewayBusinessModuleCatalog.class),
            entry(UjBusinessModuleDefinitions.class),
            entry(TcfCacheNameConstants.class),
            entry(CoreTransactionLogConstants.class),
            entry(CoreErrorCodeConstants.class)
    );

    private TcfUtilRegistry() {
    }

    public static List<UtilEntry> byModule(String module) {
        return ENTRIES.stream().filter(e -> e.module().equals(module)).toList();
    }

    public static List<UtilEntry> byCategory(UtilCategory category) {
        return ENTRIES.stream().filter(e -> e.category() == category).toList();
    }

    private static UtilEntry entry(Class<?> type) {
        CopiedFrom copied = type.getAnnotation(CopiedFrom.class);
        if (copied == null) {
            throw new IllegalStateException("Missing @CopiedFrom: " + type.getName());
        }
        return new UtilEntry(
                type.getSimpleName(),
                copied.module(),
                copied.sourceClass(),
                copied.category(),
                copied.nativeUtility());
    }
}
