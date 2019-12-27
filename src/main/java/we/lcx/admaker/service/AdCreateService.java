package we.lcx.admaker.service;

import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.dto.NewAds;

import java.util.List;

/**
 * Created by LinChenxiao on 2019/12/23 17:27
 **/
public interface AdCreateService {
    Result createAd(NewAds ads);
    void closeItems(List<Integer> itemIds, boolean delete);
    void cancel(String traceId);
}
