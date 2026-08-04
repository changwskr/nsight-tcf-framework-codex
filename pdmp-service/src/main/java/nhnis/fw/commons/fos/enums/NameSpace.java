package nhnis.fw.commons.fos.enums;

public enum NameSpace {

    // FOS
    OBS_DAY(""),
    OBS_3DAY(""),
    OBS_WEEK(""),
    OBS_2WEEK(""),
    OBS_MON(""),
    OBS_BUNGI(""),
    OBS_BANGI(""),
    OBS_YEAR(""),
    OBS_11YEAR(""),
    OBS_COMM_DAY(""),
    OBS_COMM_3DAY("");

    private final String NAMESPACE;

    NameSpace(String nameSpace) {
        this.NAMESPACE = nameSpace;
    }

    public String getNameSpace() {
        return this.NAMESPACE;
    }
}
