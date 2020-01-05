package we.lcx.admaker.common.entities;

import lombok.Data;
import we.lcx.admaker.common.enums.*;

/**
 * Created by Lin Chenxiao on 2019-12-22
 **/
@Data
public class NewAds {
    private Integer flightId;
    private String flightName;
    private String flightType;
    private Integer type;
    private Integer amount;
    private Integer deal;
    private Integer fee;
    private Integer flow;
    private Integer dspId;
    private String begin;
    private String end;
    private Integer showNumber;
    private Double showRadio;

    private DealMode dealMode;
    private BiddingMode biddingMode;
    private ContractMode contractMode;
    private FlowEnum flowEnum;

    public void convert() {
        dealMode = DealMode.of(deal);
        if (type == 1) contractMode = ContractMode.of(fee);
        else biddingMode = BiddingMode.of(fee);
        flowEnum = FlowEnum.of(flow);
    }
}
