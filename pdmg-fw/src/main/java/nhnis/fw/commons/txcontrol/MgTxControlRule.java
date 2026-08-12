package nhnis.fw.commons.txcontrol;

/**
 * 매칭된 거래통제 규칙.
 */
public final class MgTxControlRule {

    private final String controlType;
    private final String blockYn;

    public MgTxControlRule(String controlType, String blockYn) {
        this.controlType = controlType;
        this.blockYn = blockYn;
    }

    public String getControlType() {
        return controlType;
    }

    public String getBlockYn() {
        return blockYn;
    }

    public boolean isBlocking() {
        return "Y".equalsIgnoreCase(blockYn);
    }
}
