package nhnis.mk.co.a.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 거래통제 목록 조회 출력 (mkcoa6666S0).
 */
public class mkcoa6666S0DTOout {

    @JsonProperty("mkcoa6666S0DTOSub0")
    private List<mkcoa6666S0DTOSub0> mkcoa6666S0DTOSub0 = new ArrayList<>();
    private int size;
    private int pageNo;
    private int pageSize;
    private long totalCount;
    private int totalPages;

    public List<mkcoa6666S0DTOSub0> getMkcoa6666S0DTOSub0() {
        return mkcoa6666S0DTOSub0;
    }

    public void setMkcoa6666S0DTOSub0(List<mkcoa6666S0DTOSub0> list) {
        this.mkcoa6666S0DTOSub0 = list != null ? list : new ArrayList<>();
    }

    public void addmkcoa6666S0DTOSub0(mkcoa6666S0DTOSub0 item) {
        if (mkcoa6666S0DTOSub0 == null) {
            mkcoa6666S0DTOSub0 = new ArrayList<>();
        }
        mkcoa6666S0DTOSub0.add(item);
    }

    public int sizemkcoa6666S0DTOSub0() {
        return mkcoa6666S0DTOSub0 == null ? 0 : mkcoa6666S0DTOSub0.size();
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

    @Override
    public String toString() {
        return "size=" + size + " totalCount=" + totalCount
                + " pageNo=" + pageNo + " pageSize=" + pageSize;
    }
}
