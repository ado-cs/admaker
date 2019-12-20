package we.lcx.admaker.common.ad;

import lombok.Data;
import we.lcx.admaker.common.annotation.Level;

/**
 * Created by LinChenxiao on 2019/12/20 15:59
 **/
@Data
public class AdFlight {
    @Level("uid")
    private Integer id;
    private String name;
}
