package nhnis.infra.in.a.application.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Component;

/**
 * 제안/현황 Export — CSV / XLSX(OOXML) / 간단 PDF 파일 생성 후 /exports URI 반환.
 */
@Component
public class ExportHelper {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final Path exportDir;

    public ExportHelper() throws IOException {
        this.exportDir = Paths.get(System.getProperty("user.dir"), "data", "exports");
        Files.createDirectories(exportDir);
    }

    public Result writeCsv(String prefix, List<String> columns, List<Map<String, Object>> rows) throws IOException {
        String fileName = newFileName(prefix, "csv");
        Path file = exportDir.resolve(fileName);
        Files.writeString(file, buildCsv(columns, rows), StandardCharsets.UTF_8);
        return result(fileName, rows == null ? 0 : rows.size());
    }

    public Result writeXlsx(String prefix, String sheetName, List<String> columns, List<Map<String, Object>> rows)
            throws IOException {
        return writeXlsxMulti(prefix, List.of(new SheetData(sheetName, columns, rows)));
    }

    public Result writeXlsxMulti(String prefix, List<SheetData> sheets) throws IOException {
        String fileName = newFileName(prefix, "xlsx");
        Path file = exportDir.resolve(fileName);
        int totalRows = 0;
        try (OutputStream os = Files.newOutputStream(file);
                ZipOutputStream zos = new ZipOutputStream(os)) {
            List<String> shared = new ArrayList<>();
            List<SheetBuilt> built = new ArrayList<>();
            int idx = 1;
            for (SheetData s : sheets) {
                if (s == null) {
                    continue;
                }
                String name = blank(s.sheetName(), "Sheet" + idx);
                SheetBuiltXml xml = buildSheetXml(s.columns(), s.rows(), shared);
                built.add(new SheetBuilt(name, "sheet" + idx + ".xml", xml.xml()));
                totalRows += s.rows() == null ? 0 : s.rows().size();
                idx++;
            }
            if (built.isEmpty()) {
                built.add(new SheetBuilt("Empty", "sheet1.xml", buildSheetXml(List.of(), List.of(), shared).xml()));
            }
            putStored(zos, "[Content_Types].xml", contentTypesXml(built));
            putStored(zos, "_rels/.rels", RELS_ROOT);
            putStored(zos, "xl/workbook.xml", workbookXml(built));
            putStored(zos, "xl/_rels/workbook.xml.rels", workbookRels(built));
            putStored(zos, "xl/styles.xml", STYLES_XML);
            putStored(zos, "xl/sharedStrings.xml", sharedStringsXml(shared));
            for (SheetBuilt b : built) {
                putStored(zos, "xl/worksheets/" + b.fileName(), b.xml());
            }
        }
        return result(fileName, totalRows);
    }

    /**
     * 간단 텍스트 PDF (표 제목·컬럼·행 미리보기). 외부 PDF 라이브러리 없이 생성.
     */
    public Result writeSimplePdf(String prefix, String title, List<String> columns, List<Map<String, Object>> rows)
            throws IOException {
        String fileName = newFileName(prefix, "pdf");
        Path file = exportDir.resolve(fileName);
        List<String> lines = new ArrayList<>();
        lines.add(blank(title, "Export"));
        lines.add("");
        if (columns != null && !columns.isEmpty()) {
            lines.add(String.join(" | ", columns));
            lines.add("-".repeat(Math.min(80, String.join(" | ", columns).length())));
        }
        int count = 0;
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                if (count >= 40) {
                    lines.add("... (" + (rows.size() - 40) + " more rows)");
                    break;
                }
                List<String> cells = new ArrayList<>();
                if (columns != null) {
                    for (String c : columns) {
                        Object v = row == null ? null : row.get(c);
                        cells.add(v == null ? "" : String.valueOf(v));
                    }
                }
                lines.add(String.join(" | ", cells));
                count++;
            }
        }
        Files.write(file, buildMinimalPdf(lines));
        return result(fileName, rows == null ? 0 : rows.size());
    }

    public String normalizeFormat(String formatCd) {
        String f = formatCd == null ? "CSV" : formatCd.trim().toUpperCase(Locale.ROOT);
        if ("XLSX".equals(f) || "EXCEL".equals(f) || "XLS".equals(f)) {
            return "XLSX";
        }
        if ("PDF".equals(f)) {
            return "PDF";
        }
        return "CSV";
    }

    private String buildCsv(List<String> columns, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        if (columns != null && !columns.isEmpty()) {
            sb.append(String.join(",", columns.stream().map(this::csv).toList()));
            sb.append('\n');
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    for (int i = 0; i < columns.size(); i++) {
                        if (i > 0) {
                            sb.append(',');
                        }
                        Object v = row == null ? null : row.get(columns.get(i));
                        sb.append(csv(v == null ? "" : String.valueOf(v)));
                    }
                    sb.append('\n');
                }
            }
        }
        return sb.toString();
    }

    private SheetBuiltXml buildSheetXml(List<String> columns, List<Map<String, Object>> rows, List<String> shared) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<sheetData>");
        int r = 1;
        if (columns != null && !columns.isEmpty()) {
            sb.append("<row r=\"").append(r).append("\">");
            for (int c = 0; c < columns.size(); c++) {
                sb.append(cellXml(r, c, columns.get(c), shared));
            }
            sb.append("</row>");
            r++;
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    sb.append("<row r=\"").append(r).append("\">");
                    for (int c = 0; c < columns.size(); c++) {
                        Object v = row == null ? null : row.get(columns.get(c));
                        sb.append(cellXml(r, c, v == null ? "" : String.valueOf(v), shared));
                    }
                    sb.append("</row>");
                    r++;
                }
            }
        }
        sb.append("</sheetData></worksheet>");
        return new SheetBuiltXml(sb.toString());
    }

    private static String cellXml(int row, int col, String value, List<String> shared) {
        String ref = colName(col) + row;
        int idx = shared.size();
        shared.add(value == null ? "" : value);
        return "<c r=\"" + ref + "\" t=\"s\"><v>" + idx + "</v></c>";
    }

    private static String colName(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        while (n >= 0) {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        }
        return sb.toString();
    }

    private static String sharedStringsXml(List<String> shared) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"")
                .append(shared.size()).append("\" uniqueCount=\"").append(shared.size()).append("\">");
        for (String s : shared) {
            sb.append("<si><t xml:space=\"preserve\">").append(xml(s)).append("</t></si>");
        }
        sb.append("</sst>");
        return sb.toString();
    }

    private static String contentTypesXml(List<SheetBuilt> sheets) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
        sb.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
        sb.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
        sb.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        sb.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        sb.append("<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>");
        for (SheetBuilt s : sheets) {
            sb.append("<Override PartName=\"/xl/worksheets/").append(s.fileName())
                    .append("\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        sb.append("</Types>");
        return sb.toString();
    }

    private static String workbookXml(List<SheetBuilt> sheets) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ");
        sb.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        sb.append("<sheets>");
        for (int i = 0; i < sheets.size(); i++) {
            sb.append("<sheet name=\"").append(xml(sheets.get(i).name())).append("\" sheetId=\"")
                    .append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        sb.append("</sheets></workbook>");
        return sb.toString();
    }

    private static String workbookRels(List<SheetBuilt> sheets) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 0; i < sheets.size(); i++) {
            sb.append("<Relationship Id=\"rId").append(i + 1)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/")
                    .append(sheets.get(i).fileName()).append("\"/>");
        }
        int next = sheets.size() + 1;
        sb.append("<Relationship Id=\"rId").append(next)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>");
        next++;
        sb.append("<Relationship Id=\"rId").append(next)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        sb.append("</Relationships>");
        return sb.toString();
    }

    private static final String RELS_ROOT = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
            """;

    private static final String STYLES_XML = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
              <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
              <borders count="1"><border/></borders>
              <cellStyleXfs count="1"><xf/></cellStyleXfs>
              <cellXfs count="1"><xf/></cellXfs>
            </styleSheet>
            """;

    private static byte[] buildMinimalPdf(List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT /F1 10 Tf 40 800 Td 14 TL\n");
        for (String line : lines) {
            content.append("(").append(pdfEscape(line)).append(") '\n");
        }
        content.append("ET\n");
        byte[] stream = content.toString().getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        writePdf(out, "%PDF-1.4\n");
        offsets.add(out.size());
        writePdf(out, "1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n");
        offsets.add(out.size());
        writePdf(out, "2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n");
        offsets.add(out.size());
        writePdf(out, "3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources<< /Font<< /F1 5 0 R >> >> >>endobj\n");
        offsets.add(out.size());
        writePdf(out, "4 0 obj<< /Length " + stream.length + " >>stream\n");
        out.writeBytes(stream);
        writePdf(out, "endstream\nendobj\n");
        offsets.add(out.size());
        writePdf(out, "5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n");
        int xref = out.size();
        writePdf(out, "xref\n0 " + (offsets.size() + 1) + "\n");
        writePdf(out, "0000000000 65535 f \n");
        for (int off : offsets) {
            writePdf(out, String.format("%010d 00000 n \n", off));
        }
        writePdf(out, "trailer<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static void writePdf(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }

    private static String pdfEscape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\\' || c == '(' || c == ')') {
                sb.append('\\').append(c);
            } else if (c < 32 || c > 126) {
                sb.append('?');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void putStored(ZipOutputStream zos, String name, String content) throws IOException {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        ZipEntry e = new ZipEntry(name);
        e.setMethod(ZipEntry.STORED);
        e.setSize(data.length);
        e.setCompressedSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        e.setCrc(crc.getValue());
        zos.putNextEntry(e);
        zos.write(data);
        zos.closeEntry();
    }

    private String newFileName(String prefix, String ext) {
        String safe = (prefix == null || prefix.isBlank() ? "export" : prefix)
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return safe + "-" + LocalDateTime.now().format(TS) + "-"
                + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
    }

    private Result result(String fileName, int rowCount) {
        return new Result(fileName, "/exports/" + fileName,
                exportDir.resolve(fileName).toAbsolutePath().toString(), rowCount);
    }

    private String csv(String v) {
        if (v == null) {
            return "";
        }
        boolean q = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        String s = v.replace("\"", "\"\"");
        return q ? "\"" + s + "\"" : s;
    }

    private static String xml(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String blank(String v, String d) {
        return v == null || v.isBlank() ? d : v.trim();
    }

    public record Result(String fileName, String downloadUri, String absolutePath, int rowCount) {}

    public record SheetData(String sheetName, List<String> columns, List<Map<String, Object>> rows) {}

    private record SheetBuilt(String name, String fileName, String xml) {}

    private record SheetBuiltXml(String xml) {}
}
