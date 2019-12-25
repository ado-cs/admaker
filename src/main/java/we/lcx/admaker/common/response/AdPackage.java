package we.lcx.admaker.common.response;

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
    @Level("status code")
    private Integer status;
    @Level(value = "flightList", type = AdFlight.class)
    private List<AdFlight> flights;
    @Level(value = "templateList", type = AdTemplate.class)
    private List<AdTemplate> templates;
}
