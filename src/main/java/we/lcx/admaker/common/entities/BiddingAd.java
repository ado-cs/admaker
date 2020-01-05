package we.lcx.admaker.common.entities;

import lombok.Data;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.common.enums.ContractMode;
import we.lcx.admaker.common.enums.DealMode;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by LinChenxiao on 2020/01/03 20:08
 **/
@Data
public class BiddingAd {
    private Integer id;
    private String name;
    private Boolean active;
    private BiddingMode biddingMode;
}
