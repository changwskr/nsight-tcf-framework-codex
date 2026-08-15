package nhnis.infra.in.a.application.service;

import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.ExportHelper;
import nhnis.infra.in.a.dto.ifina9400E0DTOin;
import nhnis.infra.in.a.dto.ifina9400E0DTOout;
import nhnis.infra.in.a.dto.ifina9400S0DTOin;
import nhnis.infra.in.a.dto.ifina9400S0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina9400DAO;

@Service
public class ifina9400Service {
    private static final Map<Integer, String> TABLE_NAMES = Map.ofEntries(
            Map.entry(1, "현행 시스템별 서버군"),
            Map.entry(2, "서버군별 자원·Peak"),
            Map.entry(3, "Middleware·DB 제품"),
            Map.entry(4, "EOL/EOS 위험"),
            Map.entry(5, "HA·DR·RTO/RPO"),
            Map.entry(6, "성능·용량 Snapshot"),
            Map.entry(7, "7R·목표 플랫폼 적합성"),
            Map.entry(8, "목표 서버군·Node"),
            Map.entry(9, "현행→목표 전환 매핑"),
            Map.entry(10, "TCO 비교"),
            Map.entry(0, "서버 인벤토리"));

    private final ifina9400DAO dao;
    private final ExportHelper exportHelper;

    public ifina9400Service(ifina9400DAO dao, ExportHelper exportHelper) {
        this.dao = dao;
        this.exportHelper = exportHelper;
    }

    public ifina9400S0DTOout ifina9400S0(ifina9400S0DTOin input) throws Exception {
        return loadTable(input == null ? null : input.getTableId());
    }

    public ifina9400E0DTOout ifina9400E0(ifina9400E0DTOin input) throws Exception {
        ifina9400E0DTOout out = new ifina9400E0DTOout();
        String format = exportHelper.normalizeFormat(input == null ? null : input.getFormatCd());
        List<Integer> ids = resolveExportIds(input);
        if (ids.isEmpty()) {
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("export 대상 tableId 없음");
            return out;
        }

        boolean multi = ids.size() > 1;
        if (multi && "CSV".equals(format)) {
            format = "XLSX";
        }

        if (multi || "XLSX".equals(format)) {
            List<ExportHelper.SheetData> sheets = new ArrayList<>();
            int totalRows = 0;
            for (Integer id : ids) {
                ifina9400S0DTOout data = loadTable(id);
                if (!"0000".equals(data.getRSLT_CD())) {
                    out.setRSLT_CD(data.getRSLT_CD());
                    out.setRSLT_MSG(data.getRSLT_MSG());
                    return out;
                }
                String sheet = "T" + id + "_" + sanitizeSheet(data.getTableName());
                sheets.add(new ExportHelper.SheetData(sheet, data.getColumns(), data.getRows()));
                totalRows += data.getRows() == null ? 0 : data.getRows().size();
            }
            ExportHelper.Result file = exportHelper.writeXlsxMulti(
                    multi ? "proposal-multi" : "proposal-t" + ids.get(0), sheets);
            fillOk(out, ids, multi ? "제안 현황표 " + ids.size() + "종" : loadTable(ids.get(0)).getTableName(),
                    "XLSX", file, totalRows);
            return out;
        }

        ifina9400S0DTOout data = loadTable(ids.get(0));
        if (!"0000".equals(data.getRSLT_CD())) {
            out.setRSLT_CD(data.getRSLT_CD());
            out.setRSLT_MSG(data.getRSLT_MSG());
            return out;
        }

        ExportHelper.Result file;
        if ("PDF".equals(format)) {
            file = exportHelper.writeSimplePdf(
                    "proposal-t" + data.getTableId(),
                    "Table " + data.getTableId() + " - " + data.getTableName(),
                    data.getColumns(), data.getRows());
            fillOk(out, ids, data.getTableName(), "PDF", file, file.rowCount());
            return out;
        }

        file = exportHelper.writeCsv("proposal-t" + data.getTableId(), data.getColumns(), data.getRows());
        fillOk(out, ids, data.getTableName(), "CSV", file, file.rowCount());
        return out;
    }

    private static void fillOk(
            ifina9400E0DTOout out, List<Integer> ids, String tableName, String format,
            ExportHelper.Result file, int rowCount) {
        out.setTableId(ids.size() == 1 ? ids.get(0) : -1);
        out.setTableName(tableName);
        out.setTableCount(ids.size());
        out.setExportedTableIds(ids);
        out.setFormatCd(format);
        out.setFileName(file.fileName());
        out.setDownloadUri(file.downloadUri());
        out.setRowCount(rowCount);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
    }

    private static List<Integer> resolveExportIds(ifina9400E0DTOin input) {
        if (input != null && "Y".equalsIgnoreCase(trim(input.getAllTablesYn()))) {
            return List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
        if (input != null && input.getTableIdList() != null && !input.getTableIdList().isEmpty()) {
            LinkedHashSet<Integer> set = new LinkedHashSet<>();
            for (Integer id : input.getTableIdList()) {
                if (id != null && id >= 0 && id <= 10) {
                    set.add(id);
                }
            }
            return new ArrayList<>(set);
        }
        Integer tid = input == null ? null : input.getTableId();
        if (tid != null && tid == -1) {
            return List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
        return List.of(tid != null ? tid : 9);
    }

    private ifina9400S0DTOout loadTable(Integer tableIdIn) throws Exception {
        int tableId = tableIdIn != null ? tableIdIn : 9;
        ifina9400S0DTOout out = new ifina9400S0DTOout();
        out.setTableId(tableId);
        out.setTableName(TABLE_NAMES.getOrDefault(tableId, "Unknown"));

        if (tableId < 0 || tableId > 10) {
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("tableId는 0..10 (0=인벤토리, 1..9=제안표, 10=TCO)");
            return out;
        }

        List<Map<String, Object>> raw = switch (tableId) {
            case 0 -> dao.tableInventory(Map.of());
            case 1 -> dao.table1_systemGroups(Map.of());
            case 2 -> dao.table2_groupCapacity(Map.of());
            case 3 -> dao.table3_mwDb(Map.of());
            case 4 -> dao.table4_eol(Map.of());
            case 5 -> dao.table5_ha(Map.of());
            case 6 -> dao.table6_capacity(Map.of());
            case 7 -> dao.table7_strategy(Map.of());
            case 8 -> dao.table8_target(Map.of());
            case 9 -> dao.table9_mapping(Map.of());
            case 10 -> dao.table10_tco(Map.of());
            default -> List.of();
        };

        List<Map<String, Object>> rows = new ArrayList<>();
        LinkedHashSet<String> cols = new LinkedHashSet<>();
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                Map<String, Object> m = camelize(row);
                rows.add(m);
                cols.addAll(m.keySet());
            }
        }
        out.setColumns(new ArrayList<>(cols));
        out.setRows(rows);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static String sanitizeSheet(String name) {
        if (name == null || name.isBlank()) {
            return "Sheet";
        }
        String s = name.replaceAll("[\\\\/*?:\\[\\]]", "_");
        return s.length() > 20 ? s.substring(0, 20) : s;
    }

    private static String trim(String v) {
        return v == null ? null : v.trim();
    }

    private static Map<String, Object> camelize(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey() == null) continue;
            m.put(toCamel(e.getKey()), e.getValue());
        }
        return m;
    }

    private static String toCamel(String key) {
        String k = key.contains("_") ? key.toLowerCase(Locale.ROOT) : key;
        if (!k.contains("_")) {
            if (Character.isUpperCase(k.charAt(0)) && k.equals(k.toUpperCase(Locale.ROOT))) {
                k = k.toLowerCase(Locale.ROOT);
            }
            return k;
        }
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : k.toCharArray()) {
            if (c == '_') { up = true; continue; }
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
        }
        return sb.toString();
    }
}
