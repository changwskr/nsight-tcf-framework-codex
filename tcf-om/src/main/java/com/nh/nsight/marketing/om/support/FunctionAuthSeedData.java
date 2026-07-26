package com.nh.nsight.marketing.om.support;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** OM 화면 메뉴 × 권한그룹 기능권한 시드 (user-auth · 기능권한 탭). */
final class FunctionAuthSeedData {
    private static final Logger log = LoggerFactory.getLogger(FunctionAuthSeedData.class);
    private static final Set<String> STATIC_GROUPS = Set.of("ROLE_ADMIN", "ROLE_OPERATOR", "ROLE_VIEWER");

    private FunctionAuthSeedData() {}

    private record Entry(
            String authId,
            String authGroupId,
            String menuId,
            String canInquiry,
            String canRegister,
            String canUpdate,
            String canDelete,
            String canDownload) {}

    private static final String[] SCREEN_MENUS = {
            "OM_DASH", "OM_TX", "OM_TXC", "OM_TMO", "OM_SVC", "OM_MSG", "OM_AUTH", "OM_AUDIT", "OM_SES",
            "OM_ERR", "OM_BAT", "OM_HLT", "OM_RTM", "OM_RTM_GUIDE", "OM_RTM_WS", "OM_RTM_DASH", "OM_RTM_CARDS",
            "OM_RTM_THREAD", "OM_RTM_JVM", "OM_RTM_DBPOOL", "OM_RTM_DOM", "OM_RTM_OCC", "OM_RTM_ACTIVE",
            "OM_RTM_SQL", "OM_RTM_TXDET", "OM_RTM_CAUSE", "OM_RTM_FLOW", "OM_RTM_HIST", "OM_RTM_THR",
            "OM_CFG", "OM_FIL", "OM_DPL",
            "OM_CDC", "OM_FAU", "OM_DAU", "OM_AHT", "OM_CCH"
    };

    private static final Entry[] ALL = buildAll();

    static void mergeAll(JdbcTemplate jdbcTemplate) {
        int before = countRows(jdbcTemplate);
        for (Entry seed : ALL) {
            mergeEntry(jdbcTemplate, seed);
        }
        int customAdded = mergeCustomAuthGroups(jdbcTemplate);
        int after = countRows(jdbcTemplate);
        log.info(
                "OM_FUNCTION_AUTH seed merged: {} -> {} rows (base {}, custom groups +{})",
                before, after, ALL.length, customAdded);
    }

    private static void mergeEntry(JdbcTemplate jdbcTemplate, Entry seed) {
        jdbcTemplate.update("""
                MERGE INTO OM_FUNCTION_AUTH (
                    AUTH_ID, AUTH_GROUP_ID, MENU_ID,
                    CAN_INQUIRY, CAN_REGISTER, CAN_UPDATE, CAN_DELETE, CAN_DOWNLOAD
                ) KEY (AUTH_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                seed.authId(), seed.authGroupId(), seed.menuId(),
                seed.canInquiry(), seed.canRegister(), seed.canUpdate(),
                seed.canDelete(), seed.canDownload());
    }

    /** ROLE_ADMIN/OPERATOR/VIEWER 외 OM_AUTH_GROUP 에 대해 조회자 기본 권한을 보강한다. */
    private static int mergeCustomAuthGroups(JdbcTemplate jdbcTemplate) {
        List<String> groups;
        try {
            groups = jdbcTemplate.queryForList(
                    "SELECT AUTH_GROUP_ID FROM OM_AUTH_GROUP WHERE COALESCE(USE_YN, 'Y') = 'Y'",
                    String.class);
        } catch (DataAccessException ex) {
            log.debug("OM_AUTH_GROUP not ready for function-auth seed: {}", ex.getMessage());
            return 0;
        }
        int customMenus = 0;
        for (String groupId : groups) {
            if (STATIC_GROUPS.contains(groupId)) {
                continue;
            }
            for (String menuId : SCREEN_MENUS) {
                String[] permissions = viewerPermissions(menuId);
                Entry seed = entry(groupId, menuId,
                        permissions[0], permissions[1], permissions[2], permissions[3], permissions[4]);
                mergeEntry(jdbcTemplate, seed);
                customMenus++;
            }
        }
        return customMenus;
    }

    private static String[] viewerPermissions(String menuId) {
        String[] defaults = viewerPermissionsByMenu().get(menuId);
        return defaults != null ? defaults : new String[] {"N", "N", "N", "N", "N"};
    }

    private static Map<String, String[]> viewerPermissionsByMenu() {
        return Map.ofEntries(
                Map.entry("OM_DASH", new String[] {"Y", "N", "N", "N", "N"}),
                Map.entry("OM_TX", new String[] {"Y", "N", "N", "N", "N"}),
                Map.entry("OM_TXC", new String[] {"Y", "N", "N", "N", "N"}),
                Map.entry("OM_TMO", new String[] {"Y", "N", "N", "N", "N"}),
                Map.entry("OM_SVC", new String[] {"Y", "N", "N", "N", "N"}),
                Map.entry("OM_MSG", new String[] {"Y", "N", "N", "N", "N"}),
                Map.entry("OM_AUTH", new String[] {"N", "N", "N", "N", "N"}),
                Map.entry("OM_AUDIT", new String[] {"Y", "N", "N", "N", "N"}),
                Map.entry("OM_SES", new String[] {"N", "N", "N", "N", "N"}),
                Map.entry("OM_ERR", new String[] {"N", "N", "N", "N", "N"}),
                Map.entry("OM_BAT", new String[] {"N", "N", "N", "N", "N"}),
                Map.entry("OM_HLT", new String[] {"Y", "N", "N", "N", "N"}),
                Map.entry("OM_CFG", new String[] {"Y", "N", "N", "N", "N"}),
                Map.entry("OM_FIL", new String[] {"N", "N", "N", "N", "N"}),
                Map.entry("OM_DPL", new String[] {"N", "N", "N", "N", "N"}),
                Map.entry("OM_CDC", new String[] {"N", "N", "N", "N", "N"}),
                Map.entry("OM_FAU", new String[] {"N", "N", "N", "N", "N"}),
                Map.entry("OM_DAU", new String[] {"Y", "N", "N", "N", "N"}),
                Map.entry("OM_AHT", new String[] {"Y", "N", "N", "N", "N"}),
                Map.entry("OM_CCH", new String[] {"N", "N", "N", "N", "N"}));
    }

    private static int countRows(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate)) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM OM_FUNCTION_AUTH", Integer.class);
        return count != null ? count : 0;
    }

    static void ensureTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS OM_FUNCTION_AUTH (
                    AUTH_ID VARCHAR(64) NOT NULL,
                    AUTH_GROUP_ID VARCHAR(50) NOT NULL,
                    MENU_ID VARCHAR(50),
                    CAN_INQUIRY CHAR(1) DEFAULT 'N',
                    CAN_REGISTER CHAR(1) DEFAULT 'N',
                    CAN_UPDATE CHAR(1) DEFAULT 'N',
                    CAN_DELETE CHAR(1) DEFAULT 'N',
                    CAN_DOWNLOAD CHAR(1) DEFAULT 'N',
                    PRIMARY KEY (AUTH_ID)
                )
                """);
    }

    private static boolean tableExists(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'OM_FUNCTION_AUTH'",
                Integer.class);
        return count != null && count > 0;
    }

    private static Entry[] buildAll() {
        java.util.List<Entry> rows = new java.util.ArrayList<>();

        for (String menuId : SCREEN_MENUS) {
            rows.add(entry("ROLE_ADMIN", menuId, "Y", "Y", "Y", "Y", "Y"));
        }

        rows.add(entry("ROLE_OPERATOR", "OM_DASH", "Y", "N", "N", "N", "Y"));
        rows.add(entry("ROLE_OPERATOR", "OM_TX", "Y", "N", "N", "N", "Y"));
        rows.add(entry("ROLE_OPERATOR", "OM_TXC", "Y", "Y", "N", "Y", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_TMO", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_SVC", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_MSG", "Y", "Y", "Y", "Y", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_AUTH", "N", "N", "N", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_AUDIT", "Y", "N", "N", "N", "Y"));
        rows.add(entry("ROLE_OPERATOR", "OM_SES", "Y", "N", "Y", "Y", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_ERR", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_BAT", "Y", "N", "Y", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_HLT", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_CFG", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_FIL", "Y", "Y", "N", "N", "Y"));
        rows.add(entry("ROLE_OPERATOR", "OM_DPL", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_CDC", "N", "N", "N", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_FAU", "N", "N", "N", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_DAU", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_AHT", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_OPERATOR", "OM_CCH", "Y", "N", "Y", "N", "N"));

        rows.add(entry("ROLE_VIEWER", "OM_DASH", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_TX", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_TXC", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_TMO", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_SVC", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_MSG", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_AUTH", "N", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_AUDIT", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_SES", "N", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_ERR", "N", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_BAT", "N", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_HLT", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_CFG", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_FIL", "N", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_DPL", "N", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_CDC", "N", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_FAU", "N", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_DAU", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_AHT", "Y", "N", "N", "N", "N"));
        rows.add(entry("ROLE_VIEWER", "OM_CCH", "N", "N", "N", "N", "N"));

        return rows.toArray(Entry[]::new);
    }

    private static Entry entry(
            String authGroupId,
            String menuId,
            String inquiry,
            String register,
            String update,
            String delete,
            String download) {
        String authId = "FA-" + authGroupId + "-" + menuId;
        return new Entry(authId, authGroupId, menuId, inquiry, register, update, delete, download);
    }
}
