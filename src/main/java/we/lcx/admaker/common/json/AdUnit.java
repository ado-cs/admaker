package we.lcx.admaker.common.json;

import lombok.Data;
import we.lcx.admaker.common.enums.ShowType;

/**
 * Created by LinChenxiao on 2019/12/13 10:12
 **/
@Data
public class AdUnit {
    private String uid;
    private String name;
    private Integer need;
    private ShowType type;
    private String orderId;
    private Integer length;
    private Integer lowerLength;
    private String size;
}
