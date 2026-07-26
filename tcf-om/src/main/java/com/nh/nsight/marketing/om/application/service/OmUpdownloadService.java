package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.marketing.om.support.OmFileDownloadAuditListener;
import com.nh.nsight.marketing.om.config.OmUpdownloadProperties;
import com.nh.nsight.tcf.util.DateTimeUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OmUpdownloadService {
    private final JdbcTemplate jdbcTemplate;
    private final OmFileStorageService fileStorageService;
    private final OmUpdownloadProperties properties;
    private final OmFileDownloadAuditListener downloadAuditListener;

    public OmUpdownloadService(JdbcTemplate jdbcTemplate,
                               OmFileStorageService fileStorageService,
                               OmUpdownloadProperties properties,
                               OmFileDownloadAuditListener downloadAuditListener) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileStorageService = fileStorageService;
        this.properties = properties;
        this.downloadAuditListener = downloadAuditListener;
    }

    public Map<String, Object> upload(MultipartFile file, String userId, String description, String businessCode) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("파일 크기가 허용 한도를 초과했습니다.");
        }

        String fileId = UUID.randomUUID().toString();
        String uploadTime = DateTimeUtil.nowKst();
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : fileId;
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String uploadUser = StringUtils.hasText(userId) ? userId : "GUEST";
        String bizCode = StringUtils.hasText(businessCode) ? businessCode : "UD";

        try {
            fileStorageService.save(fileId, file.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장에 실패했습니다.", e);
        }

        jdbcTemplate.update("""
                INSERT INTO UD_FILE_META (FILE_ID, ORIGINAL_NAME, CONTENT_TYPE, FILE_SIZE, DESCRIPTION,
                                          UPLOAD_USER, UPLOAD_TIME, BUSINESS_CODE, USE_YN)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Y')
                """,
                fileId, originalName, contentType, file.getSize(),
                description, uploadUser, uploadTime, bizCode);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("file", toFileMap(selectById(fileId).orElseThrow()));
        return body;
    }

    public Map<String, Object> list(String originalName, String uploadUser, String fromDate, String toDate,
                                    int pageNo, int pageSize, String businessCode) {
        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.min(Math.max(1, pageSize), 100);
        int offset = (safePageNo - 1) * safePageSize;

        StringBuilder where = new StringBuilder(" WHERE USE_YN = 'Y' ");
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(businessCode)) {
            where.append(" AND BUSINESS_CODE = ? ");
            params.add(businessCode);
        }
        if (StringUtils.hasText(originalName)) {
            where.append(" AND ORIGINAL_NAME LIKE ? ");
            params.add("%" + originalName + "%");
        }
        if (StringUtils.hasText(uploadUser)) {
            where.append(" AND UPLOAD_USER = ? ");
            params.add(uploadUser);
        }
        if (StringUtils.hasText(fromDate)) {
            where.append(" AND UPLOAD_TIME >= ? ");
            params.add(fromDate);
        }
        if (StringUtils.hasText(toDate)) {
            where.append(" AND UPLOAD_TIME <= ? ");
            params.add(toDate + "T23:59:59+09:00");
        }

        Integer totalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM UD_FILE_META" + where, Integer.class, params.toArray());
        params.add(offset);
        params.add(safePageSize);
        List<Map<String, Object>> files = jdbcTemplate.queryForList("""
                SELECT FILE_ID, ORIGINAL_NAME, CONTENT_TYPE, FILE_SIZE, DESCRIPTION,
                       UPLOAD_USER, UPLOAD_TIME, BUSINESS_CODE
                  FROM UD_FILE_META
                """ + where + """
                 ORDER BY UPLOAD_TIME DESC
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, params.toArray()).stream().map(this::toFileMap).toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("files", files);
        body.put("totalCount", totalCount == null ? 0 : totalCount);
        body.put("pageNo", safePageNo);
        body.put("pageSize", safePageSize);
        return body;
    }

    public Map<String, Object> detail(String fileId) {
        Map<String, Object> file = selectById(fileId)
                .map(this::toFileMap)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));
        return Map.of("file", file);
    }

    public Map<String, Object> updateDescription(String fileId, String description) {
        int updated = jdbcTemplate.update(
                "UPDATE UD_FILE_META SET DESCRIPTION = ? WHERE FILE_ID = ? AND USE_YN = 'Y'",
                description, fileId);
        if (updated == 0) {
            throw new IllegalArgumentException("파일을 찾을 수 없습니다.");
        }
        Map<String, Object> file = selectById(fileId).map(this::toFileMap).orElseThrow();
        return Map.of("file", file);
    }

    public Map<String, Object> delete(String fileId) {
        Map<String, Object> meta = selectById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));
        jdbcTemplate.update("UPDATE UD_FILE_META SET USE_YN = 'N' WHERE FILE_ID = ?", fileId);
        try {
            fileStorageService.delete(fileId);
        } catch (IOException e) {
            throw new IllegalStateException("파일 삭제에 실패했습니다.", e);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deleted", true);
        body.put("fileId", fileId);
        body.put("originalName", meta.get("ORIGINAL_NAME"));
        return body;
    }

    public ResponseEntity<byte[]> download(String fileId, String userId, String clientIp) {
        Map<String, Object> meta = selectById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));
        String fileName = String.valueOf(meta.get("ORIGINAL_NAME"));
        long fileSize = ((Number) meta.get("FILE_SIZE")).longValue();
        String contentType = meta.get("CONTENT_TYPE") == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : String.valueOf(meta.get("CONTENT_TYPE"));
        String bizCode = meta.get("BUSINESS_CODE") == null ? "UD" : String.valueOf(meta.get("BUSINESS_CODE"));
        String auditUser = StringUtils.hasText(userId) ? userId : "GUEST";

        try {
            byte[] content = fileStorageService.load(fileId);
            downloadAuditListener.recordDownload(auditUser, fileName, fileSize, bizCode, true, clientIp);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
            headers.setContentLength(content.length);
            return ResponseEntity.ok().headers(headers).body(content);
        } catch (IOException e) {
            downloadAuditListener.recordDownload(auditUser, fileName, fileSize, bizCode, false, clientIp);
            throw new IllegalStateException("파일 읽기에 실패했습니다.", e);
        }
    }

    private Optional<Map<String, Object>> selectById(String fileId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT FILE_ID, ORIGINAL_NAME, CONTENT_TYPE, FILE_SIZE, DESCRIPTION,
                       UPLOAD_USER, UPLOAD_TIME, BUSINESS_CODE
                  FROM UD_FILE_META
                 WHERE FILE_ID = ? AND USE_YN = 'Y'
                """, fileId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    private Map<String, Object> toFileMap(Map<String, Object> row) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("fileId", row.get("FILE_ID"));
        file.put("originalName", row.get("ORIGINAL_NAME"));
        file.put("contentType", row.get("CONTENT_TYPE"));
        file.put("fileSize", row.get("FILE_SIZE"));
        file.put("description", row.get("DESCRIPTION"));
        file.put("uploadUser", row.get("UPLOAD_USER"));
        file.put("uploadTime", row.get("UPLOAD_TIME"));
        file.put("businessCode", row.get("BUSINESS_CODE"));
        return file;
    }
}
