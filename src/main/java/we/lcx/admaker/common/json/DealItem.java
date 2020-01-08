package we.lcx.admaker.common.json;

import lombok.Data;
import we.lcx.admaker.common.annotation.Ignore;
import we.lcx.admaker.common.annotation.Level;
import we.lcx.admaker.common.enums.ContractMode;

/**
 * Created by LinChenxiao on 2020/01/06 11:21
 **/
@Data
public class DealItem {
    @Level("uid")
    private Integer id;
    @Level("positionCVs code")
    private Integer positionId;
    @Level("reserveItemUid")
    private Integer reservationId;
    private Integer version;
    @Level("dspCV code")
    private Integer dspId;
    @Ignore
    private Boolean status;
    @Level("billingMode value")
    private ContractMode contractMode;
}
