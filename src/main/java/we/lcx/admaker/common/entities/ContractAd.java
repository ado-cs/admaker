package we.lcx.admaker.common.entities;

import lombok.Data;
import we.lcx.admaker.common.enums.ContractMode;
import we.lcx.admaker.common.enums.DealMode;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by LinChenxiao on 2020/01/03 20:08
 **/
@Data
public class ContractAd {
    private String name;
    private Integer positionId;
    private Integer adId;
    private Integer adVersion;
    private Boolean adStatus;
    private Integer dealId;
    private Integer dealItemId;
    private Integer reservationId;
    private Integer version;
    private Boolean status;
    private DealMode dealMode;
    private ContractMode contractMode;
    private Integer dspId;

    public Integer index() {
        return dealMode == DealMode.PDB ? contractMode == ContractMode.CPT ? 1 : 3 :
                dealMode == DealMode.PD ? 5 : 7;
    }
}
