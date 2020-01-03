package we.lcx.admaker.common.json;

import lombok.Data;
import we.lcx.admaker.common.annotation.Ignore;
import we.lcx.admaker.common.annotation.Level;
import we.lcx.admaker.common.enums.ContractMode;

/**
 * Created by Lin Chenxiao on 2020-01-01
 **/
@Data
public class DealItem {
    private Integer uid;
    @Level("resourceCV value")
    private String resourceName;
    @Level("reserveItemUid")
    private Integer reservationId;
    private Integer version;
    @Ignore
    private Boolean status;
    @Ignore
    private ContractMode fee;
}
