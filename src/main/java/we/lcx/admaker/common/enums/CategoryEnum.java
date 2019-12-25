package we.lcx.admaker.common.enums;

/**
 * Created by LinChenxiao on 2019/12/12 20:15
 **/
public enum CategoryEnum {
    NORMAL(1, "普通类别广告"),
    BUSINESS(2, "运营广告");

    private int code;
    private String value;

    CategoryEnum(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public static CategoryEnum of(int code) {
        for (CategoryEnum mode : values())
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
