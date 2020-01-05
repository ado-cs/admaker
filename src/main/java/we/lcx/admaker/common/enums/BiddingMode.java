package we.lcx.admaker.common.enums;

/**
 * Created by LinChenxiao on 2019/12/12 20:15
 **/
public enum BiddingMode {
    CPC(1, 3),
    CPM(2, 2);

    private int code;
    private int value;

    BiddingMode(int code, int value) {
        this.code = code;
        this.value = value;
    }

    public static BiddingMode of(int code) {
        for (BiddingMode mode : values())
            if (mode.code == code) return mode;
        return null;
    }

    public int getCode() {
        return code;
    }

    public int getValue() {
        return value;
    }
}
