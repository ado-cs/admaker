package we.lcx.admaker.manager;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.entities.AdNumber;
import we.lcx.admaker.common.entities.BiddingAd;
import we.lcx.admaker.common.entities.ContractAd;
import we.lcx.admaker.common.entities.Pair;
import we.lcx.admaker.service.BasicService;
import we.lcx.admaker.service.BiddingModify;
import we.lcx.admaker.service.ContractModify;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Lin Chenxiao on 2020-01-03
 **/
@Service
public class CommonManager {
    @Resource
    private BasicService basicService;

    @Resource
    private ContractModify contractModify;

    @Resource
    private BiddingModify biddingModify;

    public Result queryFlightByKeyword(String keyword) {
        List<Map<String, String>> results = basicService.queryFlight(keyword);
        return CollectionUtils.isEmpty(results) ? Result.fail() : Result.ok(results);
    }

    @SuppressWarnings("unchecked")
    public Result getAds(int refreshFlag) {//0: 不用刷新 1: 刷新麦田 2: 刷新麦穗 3: 全部刷新
        if ((refreshFlag & 1) == 1) contractModify.refreshAds();
        if (refreshFlag > 1) biddingModify.refreshAds();
        Map<String, AdNumber> result = new HashMap<>();
        Map map = contractModify.getAds();
        fillNumbers(map, result, 1);
        map = biddingModify.getAds();
        fillNumbers(map, result, 2);
        return Result.ok(result);
    }

    private void fillNumbers(Map<Integer, Map<String, List>> map, Map<String, AdNumber> result, int adType) {
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<Integer, Map<String, List>> entry : map.entrySet()) {
                if (CollectionUtils.isEmpty(entry.getValue())) continue;
                for (Map.Entry<String, List> e : entry.getValue().entrySet()) {
                    if (CollectionUtils.isEmpty(e.getValue())) continue;
                    AdNumber adNumber = result.computeIfAbsent(e.getKey(), t -> new AdNumber(e.getKey()));
                    for (Object ad : e.getValue()) {
                        adNumber.increase(adType, entry.getKey(), adType == 1 ? ((ContractAd) ad).getActive() : ((BiddingAd) ad).getActive());
                    }
                }
            }
        }
    }
}
