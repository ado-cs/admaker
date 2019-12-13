package we.lcx.admaker.common;

import lombok.Data;
import we.lcx.admaker.common.enums.ShowType;

import java.util.List;

/**
 * Created by LinChenxiao on 2019/12/12 20:53
 **/
@Data
public class AdPackage {
    private String id;
    private String name;
    private String refId;
    private ShowType mainType;
    private String showType;
    private List<AdUnit> units;
}
