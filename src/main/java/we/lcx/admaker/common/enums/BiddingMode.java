package we.lcx.admaker.common.enums;

/**
 * Created by LinChenxiao on 2019/12/12 20:15
 **/
public enum BiddingMode {
    CPC(3),
    CPM(2);

    private int code;

    BiddingMode(int code) {
        this.code = code;
    }

    public static BiddingMode of(int code) {
        for (BiddingMode mode : values())
            if (mode.code == code) return mode;
        return null;
    }

    public int getCode() {
        return code;
    }
}
