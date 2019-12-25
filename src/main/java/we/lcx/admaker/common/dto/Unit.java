package we.lcx.admaker.common.dto;

import lombok.Data;
import we.lcx.admaker.common.enums.ShowType;

/**
 * Created by LinChenxiao on 2019/12/20 16:49
 **/
@Data
public class Unit {
    private String id;
    private String name;
    private ShowType type;
    private String orderId;
    private String limit;
}
