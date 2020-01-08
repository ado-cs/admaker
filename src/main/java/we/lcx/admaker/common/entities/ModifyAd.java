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
    private String name;
    private Integer type;
    private Integer deal;
    private Integer fee;
    private Integer dspId;
    private Boolean remove;
    private Integer amount;

    private DealMode dealMode;
    private BiddingMode biddingMode;
    private ContractMode contractMode;

    public void convert() {
        dealMode = DealMode.of(deal);
        if (type == 1) contractMode = ContractMode.of(fee);
        else biddingMode = BiddingMode.of(fee);
    }
}
