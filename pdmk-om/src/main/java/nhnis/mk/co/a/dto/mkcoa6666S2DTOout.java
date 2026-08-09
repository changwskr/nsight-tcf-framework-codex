package nhnis.mk.co.a.dto;

/**
 * Service Catalog 상태 집계 (mkcoa6666S2) — 대시보드 Card.
 */
public class mkcoa6666S2DTOout {

    private int totalCount;
    private int normalCount;
    private int maintenanceCount;
    private int stopCount;
    private int disabledCount;

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getNormalCount() { return normalCount; }
    public void setNormalCount(int normalCount) { this.normalCount = normalCount; }
    public int getMaintenanceCount() { return maintenanceCount; }
    public void setMaintenanceCount(int maintenanceCount) { this.maintenanceCount = maintenanceCount; }
    public int getStopCount() { return stopCount; }
    public void setStopCount(int stopCount) { this.stopCount = stopCount; }
    public int getDisabledCount() { return disabledCount; }
    public void setDisabledCount(int disabledCount) { this.disabledCount = disabledCount; }

    @Override
    public String toString() {
        return "total=" + totalCount + " normal=" + normalCount
                + " maint=" + maintenanceCount + " stop=" + stopCount
                + " disabled=" + disabledCount;
    }
}
