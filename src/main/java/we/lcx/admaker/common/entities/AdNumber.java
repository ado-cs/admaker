package we.lcx.admaker.common.entities;

import lombok.Data;

/**
 * Created by Lin Chenxiao on 2020-01-04
 **/
@Data
public class AdNumber {
    private String name;
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

    public void increase(int adType, int idx, boolean flag) {
        if (adType == 1) {
            if (idx == 1) {
                if (flag) pdbCptOn += 1;
                else pdbCptOff += 1;
            }
            else if (idx == 2) {
                if (flag) pdbCpmOn += 1;
                else pdbCpmOff += 1;
            }
            else if (idx == 3) {
                if (flag) pdOn += 1;
                else pdOff += 1;
            }
            else if (idx == 4) {
                if (flag) bottomOn += 1;
                else bottomOff += 1;
            }
        }
        else if (adType == 2) {
            if (idx == 1) {
                if (flag) cpcOn += 1;
                else cpcOff += 1;
            }
            else if (idx == 2) {
                if (flag) cpmOn += 1;
                else cpmOff += 1;
            }
        }
    }
}
