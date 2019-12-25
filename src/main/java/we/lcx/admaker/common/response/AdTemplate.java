package we.lcx.admaker.common.response;

import lombok.Data;
import we.lcx.admaker.common.annotation.Level;
import we.lcx.admaker.common.enums.ShowType;

import java.util.List;

/**
 * Created by LinChenxiao on 2019/12/24 14:25
 **/
@Data
public class AdTemplate {
    @Level("uid")
    private String refId;
    @Level("mainShowType code")
    private ShowType mainType;
    private String showType;
    @Level(value = "locationTypeJsonList", type = AdUnit.class)
    private List<AdUnit> units;
}
