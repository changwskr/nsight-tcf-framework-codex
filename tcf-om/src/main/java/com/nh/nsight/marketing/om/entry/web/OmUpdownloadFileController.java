package com.nh.nsight.marketing.om.entry.web;

import com.nh.nsight.marketing.om.application.service.OmUpdownloadService;
import com.nh.nsight.marketing.om.support.OmUpdownloadResponseSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ud/files")
public class OmUpdownloadFileController {
    private final OmUpdownloadService service;

    public OmUpdownloadFileController(OmUpdownloadService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false, defaultValue = "GUEST") String userId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "businessCode", required = false, defaultValue = "UD") String businessCode) {
        try {
            return OmUpdownloadResponseSupport.success(service.upload(file, userId, description, businessCode));
        } catch (IllegalArgumentException e) {
            return OmUpdownloadResponseSupport.fail("E-UD-VAL-0001", e.getMessage());
        } catch (Exception e) {
            return OmUpdownloadResponseSupport.fail("E-UD-SYS-0001", "업로드 처리 중 오류가 발생했습니다.");
        }
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(value = "originalName", required = false) String originalName,
            @RequestParam(value = "uploadUser", required = false) String uploadUser,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "businessCode", required = false) String businessCode) {
        return OmUpdownloadResponseSupport.success(
                service.list(originalName, uploadUser, fromDate, toDate, pageNo, pageSize, businessCode));
    }

    @GetMapping("/{fileId}")
    public Map<String, Object> detail(@PathVariable("fileId") String fileId) {
        try {
            return OmUpdownloadResponseSupport.success(service.detail(fileId));
        } catch (IllegalArgumentException e) {
            return OmUpdownloadResponseSupport.fail("E-UD-BIZ-0001", e.getMessage());
        }
    }

    @PutMapping("/{fileId}")
    public Map<String, Object> update(@PathVariable("fileId") String fileId,
                                      @RequestBody(required = false) Map<String, String> body,
                                      @RequestParam(value = "description", required = false) String descriptionParam) {
        try {
            String description = descriptionParam;
            if (!StringUtils.hasText(description) && body != null) {
                description = body.get("description");
            }
            return OmUpdownloadResponseSupport.success(
                    service.updateDescription(fileId, description == null ? "" : description));
        } catch (IllegalArgumentException e) {
            return OmUpdownloadResponseSupport.fail("E-UD-BIZ-0001", e.getMessage());
        }
    }

    @DeleteMapping("/{fileId}")
    public Map<String, Object> delete(@PathVariable("fileId") String fileId) {
        try {
            return OmUpdownloadResponseSupport.success(service.delete(fileId));
        } catch (IllegalArgumentException e) {
            return OmUpdownloadResponseSupport.fail("E-UD-BIZ-0001", e.getMessage());
        } catch (Exception e) {
            return OmUpdownloadResponseSupport.fail("E-UD-SYS-0001", "삭제 처리 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable("fileId") String fileId,
            @RequestParam(value = "userId", required = false, defaultValue = "GUEST") String userId,
            HttpServletRequest request) {
        return service.download(fileId, userId, request.getRemoteAddr());
    }
}
