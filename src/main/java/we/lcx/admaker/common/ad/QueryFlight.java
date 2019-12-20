package we.lcx.admaker.common.ad;

import lombok.Data;
import we.lcx.admaker.common.annotation.Level;

/**
 * Created by LinChenxiao on 2019/12/20 18:16
 **/
@Data
@Level("result list")
public class QueryFlight {
    private Integer id;
    private String name;
    private String mediaCode;
    private Integer adType;
}
