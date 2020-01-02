package we.lcx.admaker.manager;

import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.entities.ModifyAd;
import we.lcx.admaker.common.entities.NewAds;

/**
 * Created by LinChenxiao on 2019/12/23 17:27
 **/
public interface AdManager {
    Result create(NewAds ads);
    void cancel(String traceId);
    void modify(ModifyAd modifyAd);
}
