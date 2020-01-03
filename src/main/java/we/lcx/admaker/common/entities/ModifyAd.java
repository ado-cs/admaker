package we.lcx.admaker.common.entities;

import lombok.Data;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.common.enums.ContractMode;
import we.lcx.admaker.common.enums.DealMode;

/**
 * Created by Lin Chenxiao on 2019-12-30
 **/
@Data
public class ModifyAd {
    private String flightName;
    private Integer type;
    private Integer deal;
    private Integer fee;
    private Integer state; //-1: del, 0: close, 1: open
    private Integer amount;

    private DealMode dealMode;
    private BiddingMode biddingMode;
    private ContractMode contractMode;

    public void convert() {
        dealMode = DealMode.of(deal);
        biddingMode = BiddingMode.of(fee);
        contractMode = ContractMode.of(fee);
    }
}
