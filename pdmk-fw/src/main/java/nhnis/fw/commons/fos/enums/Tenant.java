package nhnis.fw.commons.fos.enums;

public enum Tenant {

    SHARE_OBSTENANT02("obstenant01"),
    BIZ_OBSTENANT02("obstenant02");

    private final String TENANT;

    Tenant(String tenant) {
        this.TENANT = tenant;
    }

    public String getTenant() {
        return this.TENANT;
    }
}
