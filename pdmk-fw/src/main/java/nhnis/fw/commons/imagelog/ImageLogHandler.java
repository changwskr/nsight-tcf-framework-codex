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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.dto.header.sys_comm;
import nhnis.fw.commons.dto.imagelog.ImageLogDTO;
import nhnis.fw.commons.exception.NhBaseException;
import nhnis.fw.commons.util.DateUtil;

/**
 * 이미지로그(시스템 전문 헤더) 핸들러.
 *
 * <p>시스템 선처리에서 INSERT, 후처리에서 응답시각 UPDATE, 예외 시 예외정보 UPDATE.
 * DB 실패는 업무 거래에 영향을 주지 않도록 삼킨다.
 */
@Component
public class ImageLogHandler {

    private static final Logger log = LoggerFactory.getLogger(ImageLogHandler.class);

    private static final int MAX_EXCEPTION_MSG = 1000;

    private static final String INSERT_SQL = """
            INSERT INTO TB_FW_IMAGE_LOG (
                GUID, SERVICE_ID, SCREEN_ID, OPTR_ENO, CLIENT_IP,
                REQUEST_TIME, RESPONSE_TIME, EXCEPTION_TYPE, EXCEPTION_CODE, EXCEPTION_MSG
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_RESPONSE_SQL = """
            UPDATE TB_FW_IMAGE_LOG
               SET RESPONSE_TIME = ?
             WHERE GUID = ?
            """;

    private static final String UPDATE_EXCEPTION_SQL = """
            UPDATE TB_FW_IMAGE_LOG
               SET RESPONSE_TIME = ?,
                   EXCEPTION_TYPE = ?,
                   EXCEPTION_CODE = ?,
                   EXCEPTION_MSG = ?
             WHERE GUID = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ImageLogHandler(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /** 시스템 선처리: 전문 헤더 기준 이미지로그 INSERT. */
    public void preImagelog(hdr_nhnis header, Object inputDto) {
        ImageLogDTO dto = toDto(header);
        if (dto.getGuid() == null || dto.getGuid().isBlank()) {
            log.warn("[ImageLog] skip preImagelog: guid is empty");
            return;
        }
        if (dto.getRequestTime() == null || dto.getRequestTime().isBlank()) {
            dto.setRequestTime(DateUtil.getCurrentTime());
        }
        persist(dto, null);
    }

    /** 시스템 후처리: 응답시각 UPDATE. */
    public void postImagelog(hdr_nhnis header, Object inputDto) {
        ImageLogDTO dto = toDto(header);
        if (dto.getGuid() == null || dto.getGuid().isBlank()) {
            return;
        }
        dto.setResponseTime(DateUtil.getCurrentTime());
        try {
            int updated = jdbcTemplate.update(UPDATE_RESPONSE_SQL, dto.getResponseTime(), dto.getGuid());
            if (updated == 0) {
                log.warn("[ImageLog] postImagelog: no row for guid={}", dto.getGuid());
            } else if (log.isDebugEnabled()) {
                log.debug("[ImageLog] postImagelog guid={}", dto.getGuid());
            }
        } catch (Exception e) {
            log.error("[ImageLog] postImagelog failed guid={}", dto.getGuid(), e);
        }
    }

    /** 예외 후처리: 예외 정보 UPDATE(없으면 INSERT). */
    public void exceptionImagelog(hdr_nhnis header, Object inputDto, Throwable t) {
        ImageLogDTO dto = toDto(header);
        if (dto.getGuid() == null || dto.getGuid().isBlank()) {
            return;
        }
        dto.setResponseTime(DateUtil.getCurrentTime());
        fillException(dto, t);
        persist(dto, t);
    }

    private void persist(ImageLogDTO dto, Throwable t) {
        try {
            if (t != null) {
                int updated = jdbcTemplate.update(
                        UPDATE_EXCEPTION_SQL,
                        dto.getResponseTime(),
                        dto.getExceptionType(),
                        dto.getExceptionCode(),
                        dto.getExceptionMsg(),
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
                            dto.getExceptionMsg());
                }
                return;
            }

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
                    dto.getExceptionMsg());
            if (log.isDebugEnabled()) {
                log.debug("[ImageLog] inserted guid={} serviceId={}", dto.getGuid(), dto.getServiceId());
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
        if (sys.getTr_dtm() != null && !sys.getTr_dtm().isBlank()) {
            dto.setRequestTime(sys.getTr_dtm());
        }
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
            dto.setExceptionMsg(truncate(msg));
        } else {
            dto.setExceptionCode(null);
            dto.setExceptionMsg(truncate(t.getMessage()));
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_EXCEPTION_MSG ? value : value.substring(0, MAX_EXCEPTION_MSG);
    }
}
