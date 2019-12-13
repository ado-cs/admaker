package we.lcx.admaker.common;

import lombok.Data;

/**
 * Created by LinChenxiao on 2019/12/13 13:15
 **/
@Data
public class Cache {
    Object data;
    long expired;
}
