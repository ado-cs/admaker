package we.lcx.admaker.common.ad;

import lombok.Data;
import we.lcx.admaker.common.annotation.Level;
import we.lcx.admaker.common.enums.ShowType;
import java.util.List;

/**
 * Created by LinChenxiao on 2019/12/12 20:53
 **/
@Data
@Level("result list")
public class AdPackage {
    @Level("uid")
    private Integer id;
    private String name;
    @Level("templateList uid")
    private String refId;
    @Level("status code")
    private Integer status;
    @Level("templateList mainShowType code")
    private ShowType mainType;
    @Level("templateList showType")
    private String showType;
    @Level(value = "flightList", type = AdFlight.class)
    private List<AdFlight> flights;
    @Level(value = "templateList locationTypeJsonList", type = AdUnit.class)
    private List<AdUnit> units;
}
