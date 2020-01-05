package we.lcx.admaker.common.enums;

/**
 * Created by LinChenxiao on 2019/12/12 20:15
 **/
public enum ContractMode {
    CPT(1, "CPT", "SCHEDULE"),
    CPM(2, "EXPOSURE", "INDIVIDUATION");

    private int code;
    private String value;
    private String traffic;

    ContractMode(int code, String value, String traffic) {
        this.code = code;
        this.value = value;
        this.traffic = traffic;
    }

    public static ContractMode of(int code) {
        for (ContractMode mode : values())
            if (mode.code == code) return mode;
        return null;
    }

    public int getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public String getTraffic() {
        return traffic;
    }
}
