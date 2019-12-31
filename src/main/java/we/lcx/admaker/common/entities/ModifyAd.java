package we.lcx.admaker.common.entities;

import lombok.Data;

import java.util.List;

/**
 * Created by Lin Chenxiao on 2019-12-30
 **/
@Data
public class ModifyAd {
    private List<Integer> ids;
    private Integer state; //-1: del, 0: close, 1: open
}
