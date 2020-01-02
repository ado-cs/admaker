package we.lcx.admaker.common.enums;

/**
 * Created by LinChenxiao on 2019/12/12 20:15
 **/
public enum FlowEnum {
    F1(1, "ALL"),
    F1_2(2, "HALF"),
    F1_3(3, "ONE_OVER_THREE"),
    F2_3(4, "TWO_OVER_THREE"),
    F1_4(5, "ONE_OVER_FOUR"),
    F3_4(6, "THREE_OVER_FOUR"),
    F1_5(7, "ONE_OVER_FIVE"),
    F2_5(8, "TWO_OVER_FIVE"),
    F3_5(9, "THREE_OVER_FIVE"),
    F4_5(10, "FOUR_OVER_FIVE");

    private int code;
    private String value;

    FlowEnum(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public static FlowEnum of(int code) {
        for (FlowEnum mode : values())
            if (mode.code == code) return mode;
        return null;
    }

    public int getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }
}
