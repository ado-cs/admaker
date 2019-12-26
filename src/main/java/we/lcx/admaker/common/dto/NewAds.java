package we.lcx.admaker.common.dto;

import lombok.Data;
import we.lcx.admaker.common.enums.*;

/**
 * Created by Lin Chenxiao on 2019-12-22
 **/
@Data
public class NewAds {
    private Integer dspId;
    private Integer flight;
    private String flightName;
    private Integer type;
    private Integer amount;
    private Integer deal;
    private Integer fee;
    private Integer flow;
    private Integer category;
    private String name;
    private String begin;
    private String end;
    private Integer showNumber;
    private Double showRadio;

    private DealMode dealMode;
    private BiddingMode biddingMode;
    private ContractMode contractMode;
    private FlowEnum flowEnum;
    private CategoryEnum categoryEnum;

    public void convert() {
        dealMode = DealMode.of(deal);
        biddingMode = BiddingMode.of(fee);
        contractMode = ContractMode.of(fee);
        flowEnum = FlowEnum.of(flow);
        categoryEnum = CategoryEnum.of(category);
    }
}
