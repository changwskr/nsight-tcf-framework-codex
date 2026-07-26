package com.nh.nsight.gateway.application.service;

import com.nh.nsight.gateway.persistence.dao.UserSessionDao;
import com.nh.nsight.gateway.support.UserSession;
import com.nh.nsight.gateway.support.GatewaySessionIdResolver;
import com.nh.nsight.gateway.support.UserSessionViewMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GatewayUserSessionAdminService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserSessionDao userSessionDao;

    public GatewayUserSessionAdminService(UserSessionDao userSessionDao) {
        this.userSessionDao = userSessionDao;
    }

    public Map<String, Object> inquiry(String userId, boolean activeOnly, int pageNo, int pageSize) {
        int normalizedPageNo = pageNo < 1 ? 1 : pageNo;
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;

        List<UserSession> sessions = userSessionDao.search(userId, activeOnly, offset, normalizedPageSize);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (UserSession session : sessions) {
            rows.add(UserSessionViewMapper.toView(session));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("screen", "Gateway 세션 관리");
        result.put("pageNo", normalizedPageNo);
        result.put("pageSize", normalizedPageSize);
        result.put("totalCount", userSessionDao.count(userId, activeOnly));
        result.put("activeCount", userSessionDao.countActive());
        result.put("rows", rows);
        return result;
    }

    public Optional<Map<String, Object>> get(String sessionId) {
        return findSession(sessionId).map(UserSessionViewMapper::toView);
    }

    public Map<String, Object> forceLogout(String sessionId, String deleteReason) {
        if (!StringUtils.hasText(deleteReason) || deleteReason.trim().length() < 5) {
            throw new IllegalArgumentException("종료 사유를 5자 이상 입력해야 합니다.");
        }
        Optional<UserSession> target = findSession(sessionId);
        if (target.isEmpty()) {
            throw new IllegalArgumentException("종료할 세션을 찾을 수 없습니다.");
        }
        int updated = 0;
        for (String candidate : GatewaySessionIdResolver.lookupCandidates(sessionId)) {
            updated += userSessionDao.forceLogout(candidate, deleteReason.trim());
        }
        if (updated == 0) {
            throw new IllegalArgumentException("활성 세션이 아니거나 이미 종료되었습니다.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("screen", "Gateway 세션 강제 종료");
        result.put("deleted", true);
        result.put("sessionId", target.get().sessionId());
        result.put("userId", target.get().userId());
        return result;
    }

    private Optional<UserSession> findSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Optional.empty();
        }
        for (String candidate : GatewaySessionIdResolver.lookupCandidates(sessionId)) {
            Optional<UserSession> found = userSessionDao.findBySessionId(candidate);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
