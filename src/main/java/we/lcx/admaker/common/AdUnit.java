package we.lcx.admaker.common;

import lombok.Data;
import we.lcx.admaker.common.enums.ShowType;

/**
 * Created by LinChenxiao on 2019/12/13 10:12
 **/
@Data
public class AdUnit {
    private String id;
    private ShowType type;
    private String order;
    private String content; //图片url
    private String appendix; //图片md5
    private String limit; //尺寸或字数
}
