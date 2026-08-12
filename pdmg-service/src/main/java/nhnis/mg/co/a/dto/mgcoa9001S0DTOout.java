package nhnis.mg.co.a.dto;

import java.util.ArrayList;
import java.util.List;

/** 거래통제 목록 조회 출력 (mgcoa9001S0). */
public class mgcoa9001S0DTOout {

    private List<mgcoa9001S0DTOSub0> rows = new ArrayList<>();
    private int size;
    private int pageNo;
    private int pageSize;
    private long totalCount;
    private int totalPages;

    public List<mgcoa9001S0DTOSub0> getRows() { return rows; }
    public void setRows(List<mgcoa9001S0DTOSub0> rows) { this.rows = rows; }
    public void addRow(mgcoa9001S0DTOSub0 row) {
        if (rows == null) {
            rows = new ArrayList<>();
        }
        rows.add(row);
    }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getPageNo() { return pageNo; }
    public void setPageNo(int pageNo) { this.pageNo = pageNo; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
