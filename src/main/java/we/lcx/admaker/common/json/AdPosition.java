package we.lcx.admaker.common.json;

import lombok.Data;
import we.lcx.admaker.common.annotation.Level;

import java.util.List;

/**
 * Created by LinChenxiao on 2019/12/12 20:53
 **/
@Data
@Level("result list")
public class AdPosition {
    private Integer uid;
    private String name;
    private Integer status;
    private Integer adType;
    private Integer positionType;
    @Level(value = "flightUidList")
    private List<Integer> flightIds;
    @Level(value = "flightNameList", type = String.class)
    private List<String> flightNames;
    @Level(value = "templateUidList")
    private List<Integer> templateIds;
    @Level(value = "positionGroupUidList")
    private List<Integer> groupIds;
    @Level(value = "productTypeList")
    private List<Integer> productTypes;
}
