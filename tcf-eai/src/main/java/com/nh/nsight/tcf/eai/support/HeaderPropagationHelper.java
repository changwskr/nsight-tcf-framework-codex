package com.nh.nsight.tcf.eai.support;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.util.GuidGenerator;

/**
 * 호출 측 {@link TransactionContext} 로부터 연동 전문 header 에 전파할 상관관계 정보를 추출한다.
 *
 * <p>GUID/TraceId 는 원 거래의 값을 유지하여 서비스 간 추적성을 보장하고,
 * caller(business/service) 정보를 함께 전달한다.
 */
public final class HeaderPropagationHelper {

    private HeaderPropagationHelper() {
    }

    /** caller 컨텍스트에서 전파 정보를 추출한다. context 가 null 이면 신규 GUID 로 생성한다. */
    public static Propagation from(TransactionContext callerContext) {
        Propagation p = new Propagation();
        if (callerContext != null && callerContext.getHeader() != null) {
            StandardHeader h = callerContext.getHeader();
            p.guid = hasText(h.getGuid()) ? h.getGuid() : GuidGenerator.newGuid();
            p.traceId = hasText(h.getTraceId()) ? h.getTraceId() : p.guid;
            p.userId = h.getUserId();
            p.branchId = h.getBranchId();
            p.channelId = h.getChannelId();
            p.callerBusinessCode = h.getBusinessCode();
            p.callerServiceId = h.getServiceId();
        } else {
            p.guid = GuidGenerator.newGuid();
            p.traceId = p.guid;
        }
        return p;
    }

    private static boolean hasText(String v) {
        return v != null && !v.isBlank();
    }

    /** 전파 대상 상관관계/호출자 정보. */
    public static class Propagation {
        private String guid;
        private String traceId;
        private String userId;
        private String branchId;
        private String channelId;
        private String callerBusinessCode;
        private String callerServiceId;

        public String getGuid() {
            return guid;
        }

        public String getTraceId() {
            return traceId;
        }

        public String getUserId() {
            return userId;
        }

        public String getBranchId() {
            return branchId;
        }

        public String getChannelId() {
            return channelId;
        }

        public String getCallerBusinessCode() {
            return callerBusinessCode;
        }

        public String getCallerServiceId() {
            return callerServiceId;
        }
    }
}
