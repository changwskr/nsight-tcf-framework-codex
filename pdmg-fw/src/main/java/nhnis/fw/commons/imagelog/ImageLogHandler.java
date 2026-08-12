/****************************************************************************
 * Copyright 2026 by Nonghyup. All rights reserved. Nonghyup 의 사전 승인 없이
 * 본 내용의 전부 또는 일부에 대한 복사, 배포, 사용을 금합니다. Nonghyup의 사전 승인
 * 없이 소스코드를 변경하여 사용하는 경우 소스코드에 대한 품질과 성능을 보장하지 않습니다.
 *
 * If you modify this source without Nonghyup’s approval. Nonghyup does
 * not guarantee the quality and performance of source.
 ****************************************************************************/
package nhnis.fw.commons.imagelog;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.dto.header.sys_comm;
import nhnis.fw.commons.dto.imagelog.ImageLogDTO;
import nhnis.fw.commons.exception.NhBaseException;
import nhnis.fw.commons.util.DateUtil;
import nhnis.fw.exception.BizException;
import nhnis.fw.tcf.timeout.OnlineOverloadException;
import nhnis.fw.tcf.timeout.OnlineTimeoutException;

/**
 * 이미지로그(시스템 전문 헤더) 핸들러.
 *
 * <p>시스템 선처리에서 INSERT(요청전문 포함), 후처리에서 응답시각·응답전문 UPDATE,
 * 예외/오류 응답({@code result.stdErrCode})도 동일 테이블에 EXCEPTION_* 와 함께 남긴다.
 * DB 실패는 업무 거래에 영향을 주지 않도록 삼킨다.
 */
@Component
public class ImageLogHandler {

    private static final Logger log = LoggerFactory.getLogger(ImageLogHandler.class);

    private static final int MAX_EXCEPTION_MSG = 1000;
    private static final int MAX_WIRE_MSG = 20000;

    private static final String INSERT_SQL = """
            INSERT INTO TB_FW_IMAGE_LOG (
                GUID, SERVICE_ID, SCREEN_ID, OPTR_ENO, CLIENT_IP,
                REQUEST_TIME, RESPONSE_TIME, EXCEPTION_TYPE, EXCEPTION_CODE, EXCEPTION_MSG,
                REQUEST_MSG, RESPONSE_MSG
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_RESPONSE_SQL = """
            UPDATE TB_FW_IMAGE_LOG
               SET RESPONSE_TIME = ?,
                   RESPONSE_MSG = ?,
                   REQUEST_MSG = COALESCE(REQUEST_MSG, ?)
             WHERE GUID = ?
            """;

    /** 동일 GUID 재요청(샘플 재전송 등) 시 선처리 행을 덮어쓴다. */
    private static final String UPDATE_PRE_SQL = """
            UPDATE TB_FW_IMAGE_LOG
               SET SERVICE_ID = ?,
                   SCREEN_ID = ?,
                   OPTR_ENO = ?,
                   CLIENT_IP = ?,
                   REQUEST_TIME = ?,
                   REQUEST_MSG = ?,
                   RESPONSE_TIME = NULL,
                   RESPONSE_MSG = NULL,
                   EXCEPTION_TYPE = NULL,
                   EXCEPTION_CODE = NULL,
                   EXCEPTION_MSG = NULL
             WHERE GUID = ?
            """;

    private static final String UPDATE_EXCEPTION_SQL = """
            UPDATE TB_FW_IMAGE_LOG
               SET RESPONSE_TIME = ?,
                   EXCEPTION_TYPE = ?,
                   EXCEPTION_CODE = ?,
                   EXCEPTION_MSG = ?,
                   RESPONSE_MSG = ?,
                   REQUEST_MSG = COALESCE(REQUEST_MSG, ?)
             WHERE GUID = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ImageLogHandler(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 기존 테이블에 전문 컬럼이 없을 때만 추가한다.
     * (이미 있으면 ALTER 를 생략해 Duplicate column ERROR 로그를 내지 않는다)
     */
    @PostConstruct
    public void ensureWireMsgColumns() {
        ensureColumn("REQUEST_MSG", "CLOB");
        ensureColumn("RESPONSE_MSG", "CLOB");
    }

    private void ensureColumn(String columnName, String type) {
        if (columnExists(columnName)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE TB_FW_IMAGE_LOG ADD " + columnName + " " + type);
            log.info("[ImageLog] added column TB_FW_IMAGE_LOG.{}", columnName);
        } catch (Exception e) {
            log.debug("[ImageLog] ensure column {} skipped: {}", columnName, e.getMessage());
        }
    }

    private boolean columnExists(String columnName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                            SELECT COUNT(*)
                              FROM INFORMATION_SCHEMA.COLUMNS
                             WHERE UPPER(TABLE_NAME) = 'TB_FW_IMAGE_LOG'
                               AND UPPER(COLUMN_NAME) = ?
                            """,
                    Integer.class,
                    columnName.toUpperCase());
            return count != null && count > 0;
        } catch (Exception ignored) {
            // H2/일반 카탈로그 실패 시 Oracle USER_TAB_COLUMNS 로 재시도
        }
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                            SELECT COUNT(*)
                              FROM USER_TAB_COLUMNS
                             WHERE TABLE_NAME = 'TB_FW_IMAGE_LOG'
                               AND COLUMN_NAME = ?
                            """,
                    Integer.class,
                    columnName.toUpperCase());
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 시스템 선처리: 전문 헤더·요청 전문 기준 이미지로그 INSERT. */
    public void preImagelog(hdr_nhnis header, Object inputDto) {
        ImageLogDTO dto = toDto(header);
        if (dto.getGuid() == null || dto.getGuid().isBlank()) {
            log.warn("[ImageLog] skip preImagelog: guid is empty");
            return;
        }
        // 이미지로그 요청시각 = 서버 수신 시각 (헤더 tr_dtm 은 업무일시이므로 사용하지 않음)
        dto.setRequestTime(DateUtil.getCurrentTime());
        dto.setRequestMsg(resolveWireMsg(inputDto, true));
        if (dto.getRequestMsg() == null || dto.getRequestMsg().isBlank()) {
            log.warn("[ImageLog] requestMsg empty guid={}", dto.getGuid());
        }
        persist(dto, null);
    }

    /**
     * 시스템 후처리: 응답시각·응답 전문 UPDATE.
     * 응답에 {@code result.stdErrCode} 가 있으면 동일 테이블에 예외 컬럼도 함께 남긴다.
     */
    public void postImagelog(hdr_nhnis header, Object inputDto) {
        ImageLogDTO dto = toDto(header);
        if (dto.getGuid() == null || dto.getGuid().isBlank()) {
            return;
        }
        dto.setResponseTime(DateUtil.getCurrentTime());
        dto.setResponseMsg(resolveWireMsg(inputDto, false));
        dto.setRequestMsg(currentRequestMsg());
        if (dto.getResponseMsg() == null || dto.getResponseMsg().isBlank()) {
            log.warn("[ImageLog] responseMsg empty guid={}", dto.getGuid());
        }
        if (fillExceptionFromResult(dto, dto.getResponseMsg())) {
            persistException(dto);
            return;
        }
        try {
            int updated = jdbcTemplate.update(
                    UPDATE_RESPONSE_SQL,
                    dto.getResponseTime(),
                    dto.getResponseMsg(),
                    dto.getRequestMsg(),
                    dto.getGuid());
            if (updated == 0) {
                log.warn("[ImageLog] postImagelog: no row for guid={}", dto.getGuid());
            } else if (log.isDebugEnabled()) {
                log.debug("[ImageLog] postImagelog guid={}", dto.getGuid());
            }
        } catch (Exception e) {
            log.error("[ImageLog] postImagelog failed guid={}", dto.getGuid(), e);
        }
    }

    /** 예외 후처리: 예외 정보 UPDATE(없으면 INSERT). 응답 전문이 있으면 함께 저장. */
    public void exceptionImagelog(hdr_nhnis header, Object inputDto, Throwable t) {
        ImageLogDTO dto = toDto(header);
        if (dto.getGuid() == null || dto.getGuid().isBlank()) {
            return;
        }
        dto.setResponseTime(DateUtil.getCurrentTime());
        dto.setResponseMsg(resolveWireMsg(inputDto, false));
        dto.setRequestMsg(currentRequestMsg());
        fillException(dto, t);
        // Advice 가 이미 result 전문을 만든 경우 코드/메시지를 보강
        fillExceptionFromResult(dto, dto.getResponseMsg());
        persistException(dto);
    }

    private void persistException(ImageLogDTO dto) {
        try {
            int updated = jdbcTemplate.update(
                    UPDATE_EXCEPTION_SQL,
                    dto.getResponseTime(),
                    dto.getExceptionType(),
                    dto.getExceptionCode(),
                    dto.getExceptionMsg(),
                    dto.getResponseMsg(),
                    dto.getRequestMsg(),
                    dto.getGuid());
            if (updated == 0) {
                jdbcTemplate.update(
                        INSERT_SQL,
                        dto.getGuid(),
                        dto.getServiceId(),
                        dto.getScreenId(),
                        dto.getOptrEno(),
                        dto.getClientIp(),
                        dto.getRequestTime() != null ? dto.getRequestTime() : DateUtil.getCurrentTime(),
                        dto.getResponseTime(),
                        dto.getExceptionType(),
                        dto.getExceptionCode(),
                        dto.getExceptionMsg(),
                        dto.getRequestMsg(),
                        dto.getResponseMsg());
            } else if (log.isDebugEnabled()) {
                log.debug("[ImageLog] exceptionImagelog guid={} code={}",
                        dto.getGuid(), dto.getExceptionCode());
            }
        } catch (Exception e) {
            log.error("[ImageLog] exceptionImagelog failed guid={}", dto.getGuid(), e);
        }
    }

    private void persist(ImageLogDTO dto, Throwable t) {
        try {
            if (t != null) {
                persistException(dto);
                return;
            }

            // UPDATE 우선 → 0건이면 INSERT (동일 GUID 재전송 시 PK 위반 ERROR 로그를 피한다)
            int updated = jdbcTemplate.update(
                    UPDATE_PRE_SQL,
                    dto.getServiceId(),
                    dto.getScreenId(),
                    dto.getOptrEno(),
                    dto.getClientIp(),
                    dto.getRequestTime(),
                    dto.getRequestMsg(),
                    dto.getGuid());
            if (updated > 0) {
                log.info("[ImageLog] guid already exists, refreshed pre fields guid={}", dto.getGuid());
                return;
            }
            try {
                jdbcTemplate.update(
                        INSERT_SQL,
                        dto.getGuid(),
                        dto.getServiceId(),
                        dto.getScreenId(),
                        dto.getOptrEno(),
                        dto.getClientIp(),
                        dto.getRequestTime(),
                        dto.getResponseTime(),
                        dto.getExceptionType(),
                        dto.getExceptionCode(),
                        dto.getExceptionMsg(),
                        dto.getRequestMsg(),
                        dto.getResponseMsg());
                if (log.isDebugEnabled()) {
                    log.debug("[ImageLog] inserted guid={} serviceId={}", dto.getGuid(), dto.getServiceId());
                }
            } catch (DuplicateKeyException dup) {
                // 동시성으로 INSERT 경합 시 한 번 더 갱신 (log4jdbc ERROR는 남을 수 있음)
                jdbcTemplate.update(
                        UPDATE_PRE_SQL,
                        dto.getServiceId(),
                        dto.getScreenId(),
                        dto.getOptrEno(),
                        dto.getClientIp(),
                        dto.getRequestTime(),
                        dto.getRequestMsg(),
                        dto.getGuid());
                log.info("[ImageLog] guid already exists, refreshed pre fields guid={}", dto.getGuid());
            }
        } catch (Exception e) {
            log.error("[ImageLog] persist failed guid={}", dto.getGuid(), e);
        }
    }

    private static ImageLogDTO toDto(hdr_nhnis header) {
        ImageLogDTO dto = new ImageLogDTO();
        if (header == null || header.getSys_comm() == null) {
            return dto;
        }
        sys_comm sys = header.getSys_comm();
        dto.setGuid(sys.getStd_gbl_id());
        dto.setServiceId(sys.getRms_svc_c());
        dto.setScreenId(sys.getScid());
        dto.setOptrEno(sys.getOptr_eno());
        dto.setClientIp(sys.getTr_trm_ipadr());
        // REQUEST_TIME 은 preImagelog 에서 서버 시각으로 설정한다 (tr_dtm ≠ 수신시각)
        return dto;
    }

    private static void fillException(ImageLogDTO dto, Throwable t) {
        if (t == null) {
            return;
        }
        dto.setExceptionType(t.getClass().getName());
        if (t instanceof NhBaseException nbe) {
            dto.setExceptionCode(nbe.getStdErrCode());
            String msg = nbe.getStdErrMsgContents();
            if (msg == null || msg.isBlank()) {
                msg = nbe.getMessage();
            }
            dto.setExceptionMsg(truncate(msg, MAX_EXCEPTION_MSG));
        } else if (t instanceof BizException be) {
            dto.setExceptionCode(be.getCode());
            dto.setExceptionMsg(truncate(be.getMessage(), MAX_EXCEPTION_MSG));
        } else if (t instanceof OnlineTimeoutException ote) {
            dto.setExceptionCode("FW_TIMEOUT");
            dto.setExceptionMsg(truncate(ote.getMessage(), MAX_EXCEPTION_MSG));
        } else if (t instanceof OnlineOverloadException ooe) {
            dto.setExceptionCode("FW_OVERLOADED");
            dto.setExceptionMsg(truncate(ooe.getMessage(), MAX_EXCEPTION_MSG));
        } else {
            dto.setExceptionCode(null);
            dto.setExceptionMsg(truncate(t.getMessage(), MAX_EXCEPTION_MSG));
        }
    }

    /**
     * 오류 응답 전문({@code result.stdErrCode})에서 예외 컬럼을 채운다.
     * @return 오류 result 가 있으면 true
     */
    private boolean fillExceptionFromResult(ImageLogDTO dto, String responseMsg) {
        if (responseMsg == null || responseMsg.isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(responseMsg);
            JsonNode result = root.get("result");
            if (result == null || !result.isObject()) {
                return false;
            }
            JsonNode codeNode = result.get("stdErrCode");
            if (codeNode == null || codeNode.isNull() || codeNode.asText().isBlank()) {
                return false;
            }
            dto.setExceptionCode(codeNode.asText().trim());
            JsonNode msgNode = result.get("stdErrMsgCntn");
            if (msgNode != null && !msgNode.isNull() && !msgNode.asText().isBlank()) {
                dto.setExceptionMsg(truncate(msgNode.asText(), MAX_EXCEPTION_MSG));
            }
            JsonNode typeNode = result.get("errType");
            if (typeNode != null && !typeNode.isNull() && !typeNode.asText().isBlank()) {
                dto.setExceptionType(typeNode.asText().trim());
            } else if (dto.getExceptionType() == null || dto.getExceptionType().isBlank()) {
                dto.setExceptionType("RESULT_ERROR");
            }
            return true;
        } catch (Exception e) {
            log.debug("[ImageLog] parse result error skipped: {}", e.getMessage());
            return false;
        }
    }

    private static String resolveWireMsg(Object inputDto, boolean request) {
        if (inputDto instanceof String s && !s.isBlank()) {
            return truncate(s, MAX_WIRE_MSG);
        }
        return request ? currentRequestMsg() : currentResponseMsg();
    }

    private static String currentRequestMsg() {
        ServiceContext ctx = ServiceContextHolder.getInstance();
        if (ctx == null) {
            return null;
        }
        String body = ctx.getRequestBody();
        if (body != null && !body.isBlank()) {
            return truncate(body, MAX_WIRE_MSG);
        }
        return null;
    }

    private static String currentResponseMsg() {
        ServiceContext ctx = ServiceContextHolder.getInstance();
        return ctx == null ? null : truncate(ctx.getResponseBody(), MAX_WIRE_MSG);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
