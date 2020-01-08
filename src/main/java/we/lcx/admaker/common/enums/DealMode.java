package we.lcx.admaker.common.enums;

/**
 * Created by LinChenxiao on 2019/12/12 20:15
 **/
public enum DealMode {
    PDB(1, "保价保量"),
    PD(3, "保价不保量"),
    BOTTOM(2, "抄底排期");

    private int code;
    private String value;

    DealMode(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public static DealMode of(Integer code) {
        if (code == null) return null;
        for (DealMode mode : values())
            if (mode.code == code) return mode;
        return null;
    }

    public static DealMode of(String value) {
        for (DealMode mode : values())
            if (mode.value.equals(value)) return mode;
        return null;
    }

    public int getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }
}
