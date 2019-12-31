package we.lcx.admaker.service;

import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.entities.ModifyAd;
import we.lcx.admaker.common.entities.NewAds;

import java.util.List;

/**
 * Created by LinChenxiao on 2019/12/23 17:27
 **/
public interface AdCreateService {
    Result createAd(NewAds ads);
    void cancel(String traceId);
    void modify(ModifyAd modifyAd);
}
