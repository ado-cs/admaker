package we.lcx.admaker.common.entities;

import lombok.Data;
import java.util.List;

/**
 * Created by Lin Chenxiao on 2019-12-31
 **/
@Data
public class ContractLog {
    private Ad ad;
    private List creative;
    private int resourceId;
    private int resourceItemId;
    private int revenueId;
    private int dealId;
    private int reservationId;
    private int dealItemId;
}
