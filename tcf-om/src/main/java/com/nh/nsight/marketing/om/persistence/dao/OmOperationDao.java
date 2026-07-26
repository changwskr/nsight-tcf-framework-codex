package com.nh.nsight.marketing.om.persistence.dao;

import com.nh.nsight.marketing.om.persistence.mapper.OmOperationMapper;
import java.sql.Clob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class OmOperationDao {
    private final OmOperationMapper mapper;

    public OmOperationDao(OmOperationMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> selectTxSummary(String baseDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("baseDate", baseDate);
        return mapper.selectTxSummary(params);
    }

    public List<Map<String, Object>> selectErrorTop(String fromTime, String toTime, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("fromTime", fromTime);
        params.put("toTime", toTime);
        params.put("limit", limit);
        return mapper.selectErrorTop(params);
    }

    public List<Map<String, Object>> selectSlowTransactionsTop(String fromTime, String toTime, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("fromTime", fromTime);
        params.put("toTime", toTime);
        params.put("limit", limit);
        return mapper.selectSlowTransactionsTop(params);
    }

    public List<Map<String, Object>> selectApStatus() {
        return mapper.selectApStatus();
    }

    public List<Map<String, Object>> selectDbStatus() {
        return mapper.selectDbStatus();
    }

    public List<Map<String, Object>> selectSessionStatus() {
        return mapper.selectSessionStatus();
    }

    public int sumSessionStatusActiveCount() {
        return mapper.sumSessionStatusActiveCount();
    }

    public int sumSessionStatusExpiredCount() {
        return mapper.sumSessionStatusExpiredCount();
    }

    public int sumSessionStatusUniqueUsers() {
        return mapper.sumSessionStatusUniqueUsers();
    }

    public List<Map<String, Object>> selectDeployStatus() {
        return mapper.selectDeployStatus();
    }

    public int deleteAllApStatus() {
        return mapper.deleteAllApStatus();
    }

    public int deleteAllDbStatus() {
        return mapper.deleteAllDbStatus();
    }

    public int deleteAllSessionStatus() {
        return mapper.deleteAllSessionStatus();
    }

    public int deleteAllDeployStatus() {
        return mapper.deleteAllDeployStatus();
    }

    public int selectActiveSessionCount() {
        Integer count = mapper.selectActiveSessionCount();
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> searchTransactionLogs(Map<String, Object> criteria) {
        return mapper.searchTransactionLogs(criteria);
    }

    public int countTransactionLogs(Map<String, Object> criteria) {
        return mapper.countTransactionLogs(criteria);
    }

    public Map<String, Object> summarizeTransactionLogs(Map<String, Object> criteria) {
        Map<String, Object> summary = mapper.summarizeTransactionLogs(criteria);
        return summary == null ? Map.of() : summary;
    }

    public int deleteAllTransactionLogs() {
        return mapper.deleteAllTransactionLogs();
    }

    public List<Map<String, Object>> searchServiceCatalog(Map<String, Object> criteria) {
        return mapper.searchServiceCatalog(criteria);
    }

    public int countServiceCatalog(Map<String, Object> criteria) {
        return mapper.countServiceCatalog(criteria);
    }

    public Map<String, Object> selectServiceCatalogByKey(Map<String, Object> key) {
        return mapper.selectServiceCatalogByKey(key);
    }

    public Map<String, Object> selectServiceCatalogByServiceId(String serviceId) {
        return mapper.selectServiceCatalogByServiceId(serviceId);
    }

    public int insertServiceCatalog(Map<String, Object> row) {
        return mapper.insertServiceCatalog(row);
    }

    public int updateServiceCatalog(Map<String, Object> row) {
        return mapper.updateServiceCatalog(row);
    }

    public int disableServiceCatalog(Map<String, Object> row) {
        return mapper.disableServiceCatalog(row);
    }

    public List<Map<String, Object>> searchUsers(Map<String, Object> criteria) {
        return mapper.searchUsers(criteria);
    }

    public int countUsers(Map<String, Object> criteria) {
        return mapper.countUsers(criteria);
    }

    public Map<String, Object> selectUserById(String userId) {
        return mapper.selectUserById(userId);
    }

    public int insertUser(Map<String, Object> row) {
        return mapper.insertUser(row);
    }

    public int updateUser(Map<String, Object> row) {
        return mapper.updateUser(row);
    }

    public int updateUserWithPassword(Map<String, Object> row) {
        return mapper.updateUserWithPassword(row);
    }

    public int disableUser(Map<String, Object> row) {
        return mapper.disableUser(row);
    }

    public List<Map<String, Object>> searchMenus(Map<String, Object> criteria) {
        return mapper.searchMenus(criteria);
    }

    public Map<String, Object> selectMenuById(String menuId) {
        return mapper.selectMenuById(menuId);
    }

    public int countChildMenus(String menuId) {
        return mapper.countChildMenus(menuId);
    }

    public int insertMenu(Map<String, Object> row) {
        return mapper.insertMenu(row);
    }

    public int updateMenu(Map<String, Object> row) {
        return mapper.updateMenu(row);
    }

    public int disableMenu(Map<String, Object> row) {
        return mapper.disableMenu(row);
    }

    public List<Map<String, Object>> searchAuthGroups(Map<String, Object> criteria) {
        return mapper.searchAuthGroups(criteria);
    }

    public Map<String, Object> selectAuthGroupById(String authGroupId) {
        return mapper.selectAuthGroupById(authGroupId);
    }

    public int countFunctionAuthByGroup(String authGroupId) {
        return mapper.countFunctionAuthByGroup(authGroupId);
    }

    public int countDataAuthByGroup(String authGroupId) {
        return mapper.countDataAuthByGroup(authGroupId);
    }

    public int insertAuthGroup(Map<String, Object> row) {
        return mapper.insertAuthGroup(row);
    }

    public int updateAuthGroup(Map<String, Object> row) {
        return mapper.updateAuthGroup(row);
    }

    public int disableAuthGroup(Map<String, Object> row) {
        return mapper.disableAuthGroup(row);
    }

    public List<Map<String, Object>> searchAuditLogs(Map<String, Object> criteria) {
        return mapper.searchAuditLogs(criteria);
    }

    public int countAuditLogs(Map<String, Object> criteria) {
        return mapper.countAuditLogs(criteria);
    }

    public int deleteAllAuditLogs() {
        return mapper.deleteAllAuditLogs();
    }

    public List<Map<String, Object>> searchErrorCodes(Map<String, Object> criteria) {
        return mapper.searchErrorCodes(criteria);
    }

    public int countErrorCodes(Map<String, Object> criteria) {
        return mapper.countErrorCodes(criteria);
    }

    public List<Map<String, Object>> searchBatchJobs(Map<String, Object> criteria) {
        return mapper.searchBatchJobs(criteria);
    }

    public List<Map<String, Object>> searchBatchHistories(Map<String, Object> criteria) {
        return mapper.searchBatchHistories(criteria);
    }

    public int countBatchHistories(Map<String, Object> criteria) {
        return mapper.countBatchHistories(criteria);
    }

    public int deleteAllBatchHistories() {
        return mapper.deleteAllBatchHistories();
    }

    public List<Map<String, Object>> searchSystemConfigs(Map<String, Object> criteria) {
        return mapper.searchSystemConfigs(criteria);
    }

    public List<Map<String, Object>> searchFileDownloadLogs(Map<String, Object> criteria) {
        return mapper.searchFileDownloadLogs(criteria);
    }

    public int countFileDownloadLogs(Map<String, Object> criteria) {
        return mapper.countFileDownloadLogs(criteria);
    }

    public int insertFileDownloadLog(Map<String, Object> row) {
        return mapper.insertFileDownloadLog(row);
    }

    public List<Map<String, Object>> searchCommonCodes(Map<String, Object> criteria) {
        return mapper.searchCommonCodes(criteria);
    }

    public int countCommonCodes(Map<String, Object> criteria) {
        return mapper.countCommonCodes(criteria);
    }

    public List<String> selectDistinctCodeGroups() {
        return mapper.selectDistinctCodeGroups();
    }

    public Map<String, Object> selectCommonCodeByKey(Map<String, Object> key) {
        return mapper.selectCommonCodeByKey(key);
    }

    public int insertCommonCode(Map<String, Object> row) {
        return mapper.insertCommonCode(row);
    }

    public int updateCommonCode(Map<String, Object> row) {
        return mapper.updateCommonCode(row);
    }

    public int disableCommonCode(Map<String, Object> row) {
        return mapper.disableCommonCode(row);
    }

    public List<Map<String, Object>> searchFunctionAuths(Map<String, Object> criteria) {
        return mapper.searchFunctionAuths(criteria);
    }

    public Map<String, Object> selectFunctionAuthById(String authId) {
        return mapper.selectFunctionAuthById(authId);
    }

    public int countFunctionAuthByGroupAndMenu(String authGroupId, String menuId, String excludeAuthId) {
        Map<String, Object> params = new HashMap<>();
        params.put("authGroupId", authGroupId);
        params.put("menuId", menuId);
        if (excludeAuthId != null) {
            params.put("excludeAuthId", excludeAuthId);
        }
        return mapper.countFunctionAuthByGroupAndMenu(params);
    }

    public int insertFunctionAuth(Map<String, Object> row) {
        return mapper.insertFunctionAuth(row);
    }

    public int updateFunctionAuth(Map<String, Object> row) {
        return mapper.updateFunctionAuth(row);
    }

    public int deleteFunctionAuthById(String authId) {
        return mapper.deleteFunctionAuthById(authId);
    }

    public List<Map<String, Object>> searchDataAuths(Map<String, Object> criteria) {
        return mapper.searchDataAuths(criteria);
    }

    public List<Map<String, Object>> searchAuthHistories(Map<String, Object> criteria) {
        return mapper.searchAuthHistories(criteria);
    }

    public int countAuthHistories(Map<String, Object> criteria) {
        return mapper.countAuthHistories(criteria);
    }

    public int insertAuthHistory(Map<String, Object> row) {
        return mapper.insertAuthHistory(row);
    }

    public int deleteAllAuthHistories() {
        return mapper.deleteAllAuthHistories();
    }

    public Map<String, Object> selectErrorCodeByCode(String errorCode) {
        return mapper.selectErrorCodeByCode(errorCode);
    }

    public int mergeErrorCode(Map<String, Object> row) {
        return mapper.mergeErrorCode(row);
    }

    public int insertErrorCode(Map<String, Object> row) {
        return mapper.insertErrorCode(row);
    }

    public int updateErrorCode(Map<String, Object> row) {
        return mapper.updateErrorCode(row);
    }

    public int disableErrorCode(Map<String, Object> row) {
        return mapper.disableErrorCode(row);
    }

    public List<Map<String, Object>> searchTransactionControls(Map<String, Object> criteria) {
        return mapper.searchTransactionControls(criteria);
    }

    public int countTransactionControls(Map<String, Object> criteria) {
        return mapper.countTransactionControls(criteria);
    }

    public Map<String, Object> selectTransactionControlByKey(Map<String, Object> key) {
        return mapper.selectTransactionControlByKey(key);
    }

    public int insertTransactionControl(Map<String, Object> row) {
        return mapper.insertTransactionControl(row);
    }

    public int updateTransactionControl(Map<String, Object> row) {
        return mapper.updateTransactionControl(row);
    }

    public int deleteTransactionControl(Map<String, Object> key) {
        return mapper.deleteTransactionControl(key);
    }

    public List<Map<String, Object>> searchTimeoutPolicies(Map<String, Object> criteria) {
        return mapper.searchTimeoutPolicies(criteria);
    }

    public int countTimeoutPolicies(Map<String, Object> criteria) {
        return mapper.countTimeoutPolicies(criteria);
    }

    public Map<String, Object> selectTimeoutPolicyByKey(Map<String, Object> key) {
        return mapper.selectTimeoutPolicyByKey(key);
    }

    public int insertTimeoutPolicy(Map<String, Object> row) {
        return mapper.insertTimeoutPolicy(row);
    }

    public int updateTimeoutPolicy(Map<String, Object> row) {
        return mapper.updateTimeoutPolicy(row);
    }

    public int deleteTimeoutPolicy(Map<String, Object> key) {
        return mapper.deleteTimeoutPolicy(key);
    }

    public Map<String, Object> selectBatchJobById(String jobId) {
        return mapper.selectBatchJobById(jobId);
    }

    public int insertBatchHistory(Map<String, Object> row) {
        return mapper.insertBatchHistory(row);
    }

    public List<Map<String, Object>> searchCacheStatus(Map<String, Object> criteria) {
        return mapper.searchCacheStatus(criteria);
    }

    public int deleteCache(Map<String, Object> criteria) {
        return mapper.deleteCache(criteria);
    }

    public int insertAuditLog(Map<String, Object> row) {
        return mapper.insertAuditLog(row);
    }

    public Map<String, Object> selectUserForLogin(String userId) {
        return mapper.selectUserForLogin(userId);
    }

    public int updateUserLastLoginTime(Map<String, Object> row) {
        return mapper.updateUserLastLoginTime(row);
    }

    public List<Map<String, Object>> selectUsersWithoutPasswordHash() {
        return mapper.selectUsersWithoutPasswordHash();
    }

    public int updateUserPasswordHash(Map<String, Object> row) {
        return mapper.updateUserPasswordHash(row);
    }

    public List<Map<String, Object>> searchSpringSessions(Map<String, Object> criteria) {
        return mapper.searchSpringSessions(criteria);
    }

    public int countSpringSessions(Map<String, Object> criteria) {
        return mapper.countSpringSessions(criteria);
    }

    public int countActiveSpringSessions(long now) {
        return mapper.countActiveSpringSessions(now);
    }

    public Map<String, Object> selectSpringSessionById(String sessionId) {
        return mapper.selectSpringSessionById(sessionId);
    }

    public int deleteSpringSession(String sessionId) {
        return mapper.deleteSpringSession(sessionId);
    }

    public int countExpiredSpringSessions(long now) {
        return mapper.countExpiredSpringSessions(now);
    }

    public int deleteExpiredSpringSessions(long now) {
        return mapper.deleteExpiredSpringSessions(now);
    }

    public List<Map<String, Object>> searchMessageStructs(Map<String, Object> criteria) {
        return mapper.searchMessageStructs(criteria);
    }

    public int countMessageStructs(Map<String, Object> criteria) {
        return mapper.countMessageStructs(criteria);
    }

    public Map<String, Object> selectMessageStructById(String structId) {
        return normalizeMessageStructRow(mapper.selectMessageStructById(structId));
    }

    public Map<String, Object> selectMessageStructByCode(String structCode) {
        return normalizeMessageStructRow(mapper.selectMessageStructByCode(structCode));
    }

    public List<Map<String, Object>> searchMessageFieldsByStructId(String structId) {
        return mapper.searchMessageFieldsByStructId(structId).stream()
                .map(this::normalizeMessageFieldRow)
                .toList();
    }

    public int insertMessageStruct(Map<String, Object> row) {
        return mapper.insertMessageStruct(row);
    }

    public int updateMessageStruct(Map<String, Object> row) {
        return mapper.updateMessageStruct(row);
    }

    public int disableMessageStruct(Map<String, Object> key) {
        return mapper.disableMessageStruct(key);
    }

    public int deleteMessageFieldsByStructId(String structId) {
        return mapper.deleteMessageFieldsByStructId(structId);
    }

    public int insertMessageField(Map<String, Object> row) {
        return mapper.insertMessageField(row);
    }

    private Map<String, Object> normalizeMessageStructRow(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        row.put("sampleJson", toJsonSafeString(row.get("sampleJson")));
        return row;
    }

    private Map<String, Object> normalizeMessageFieldRow(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        row.put("maxLength", toInteger(row.get("maxLength")));
        row.put("sortOrder", toInteger(row.get("sortOrder")));
        return row;
    }

    private static String toJsonSafeString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Clob clob) {
            return readClob(clob);
        }
        if (value instanceof String text) {
            return text.isBlank() ? null : text;
        }
        return String.valueOf(value);
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String readClob(Clob clob) {
        try {
            long length = clob.length();
            if (length <= 0) {
                return null;
            }
            int size = length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) length;
            return clob.getSubString(1, size);
        } catch (Exception e) {
            return null;
        }
    }
}


