package we.lcx.admaker.common.enums;

/**
 * Created by LinChenxiao on 2019/12/16 13:42
 **/
public enum AdType {
    CONTRACT(1),
    BIDDING(2);

    private int code;

    AdType(int code) {
        this.code = code;
    }

    public static boolean isContract(int code) {
        return code == 1;
    }

    public static boolean isBidding(int code) {
        return code == 2;
    }

    public int getCode() {
        return code;
    }
}
