package we.lcx.admaker.common.enums;

/**
 * Created by LinChenxiao on 2019/12/13 10:13
 **/
public enum ShowType {
    PICTURE(1, "图片"),
    VIDEO(2, "视频"),
    TEXT(3, "文字"),
    AUDIO(4, "音频");

    private int code;
    private String description;
    ShowType(int code, String description) {
        this.code = code;
        this.description = description;
    }
    public static ShowType of(String code) {
        if (code == null) return null;
        for (ShowType type : values()) {
            if (code.equals(String.valueOf(type.code))) return type;
        }
        return null;
    }
    public int getCode() {
        return code;
    }
    public String getDescription() {
        return description;
    }
}
