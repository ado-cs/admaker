package we.lcx.admaker.entity.ad;

import lombok.Data;
import we.lcx.admaker.common.annotation.Level;
import we.lcx.admaker.common.enums.ShowType;

/**
 * Created by LinChenxiao on 2019/12/13 10:12
 **/
@Data
public class AdUnit {
    @Level("uid")
    private String id;
    private Integer need;
    private ShowType type;
    private String orderId;
    private Integer length;
    private Integer lowerLength;
    private String size;
}
