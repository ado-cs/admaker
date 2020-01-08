package we.lcx.admaker.common.entities;

import lombok.Data;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.common.enums.ContractMode;
import we.lcx.admaker.common.enums.DealMode;

/**
 * Created by Lin Chenxiao on 2020-01-04
 **/
@Data
public class AdNumber {
    private String name;
    private int dspId;
    private int pdbCptOn;
    private int pdbCpmOn;
    private int pdOn;
    private int bottomOn;
    private int cpcOn;
    private int cpmOn;
    private int pdbCptOff;
    private int pdbCpmOff;
    private int pdOff;
    private int bottomOff;
    private int cpcOff;
    private int cpmOff;

    public AdNumber(String name) {
        this.name = name;
    }

    public AdNumber(String name, int dspId) {
        this.name = name;
        this.dspId = dspId;
    }

    public void increase(ContractAd ad) {
        if (ad.getDealMode() == DealMode.PDB) {
            if (ad.getContractMode() == ContractMode.CPT) {
                if (ad.getStatus()) pdbCptOn += 1;
                else pdbCptOff += 1;
            }
            else {
                if (ad.getStatus()) pdbCpmOn += 1;
                else pdbCpmOff += 1;
            }
        }
        else if (ad.getDealMode() == DealMode.PD) {
            if (ad.getStatus()) pdOn += 1;
            else pdOff += 1;
        }
        else {
            if (ad.getStatus()) bottomOn += 1;
            else bottomOff += 1;
        }
    }

    public void increase(BiddingAd ad) {
        if (ad.getBiddingMode() == BiddingMode.CPC) {
            if (ad.getStatus()) cpcOn += 1;
            else cpcOff += 1;
        }
        else {
            if (ad.getStatus()) cpmOn += 1;
            else cpmOff += 1;
        }
    }
}
