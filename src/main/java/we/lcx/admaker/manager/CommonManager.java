package we.lcx.admaker.manager;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.entities.*;
import we.lcx.admaker.service.BasicService;
import we.lcx.admaker.service.BiddingModify;
import we.lcx.admaker.service.ContractModify;
import we.lcx.admaker.service.Modify;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by Lin Chenxiao on 2020-01-03
 **/
@Service
public class CommonManager {
    @Resource
    private BasicService basicService;

    @Resource
    private Modify contractModify;

    @Resource
    private Modify biddingModify;

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

    @SuppressWarnings("unchecked")
    public Result modify(ModifyAd modifyAd) {
        modifyAd.convert();
        Modify modify = modifyAd.getType() == 1 ? contractModify : biddingModify;
        if (modifyAd.getAmount() < 0) return Result.fail("广告数量无效");
        List ads = modify.getAds(modifyAd.getFlightName(), modifyAd.getDealMode(), modifyAd.getFee());
        if (ads == null) return Result.fail("无广告位数据");
        Set idsOn = new HashSet<>();
        Set idsOff = new HashSet<>();
        for (Object ad : ads) {
            if (modifyAd.getType() == 1) {
                ContractAd contractAd = (ContractAd) ad;
                if (contractAd.getActive()) idsOn.add(new Pair<>(Integer.valueOf(contractAd.getDealItemId()), contractAd.getId()));
                else idsOff.add(new Pair<>(Integer.valueOf(contractAd.getDealItemId()), contractAd.getId()));
            }
            else {
                BiddingAd biddingAd = (BiddingAd) ad;
                if (biddingAd.getActive()) idsOn.add(biddingAd.getId());
                else idsOff.add(biddingAd.getId());
            }
        }
        if (idsOn.size() + idsOff.size() < modifyAd.getAmount())
            return Result.fail("广告单总数不足");
        if (modifyAd.getRemove()) {
            int num = idsOn.size() + idsOff.size() - modifyAd.getAmount();
            if (num == 0) return Result.ok();
            if (idsOff.size() < num) {
                Iterator<Integer> idIter = idsOn.iterator();
                while (idsOn.size() + idsOff.size() > num) {
                    idIter.next();
                    idIter.remove();
                }
                modify.update(idsOn, false);
                idsOff.addAll(idsOn);
            }
            else if (idsOff.size() > num) {
                Iterator<Integer> idIter = idsOff.iterator();
                while (idsOff.size() > num) {
                    idIter.next();
                    idIter.remove();
                }
            }
            modify.remove(idsOff);
        }
        else if (modifyAd.getAmount() > 0) {
            if (idsOn.size() == modifyAd.getAmount()) return Result.ok();
            int num = Math.abs(modifyAd.getAmount() - idsOn.size());
            if (idsOn.size() < modifyAd.getAmount()) {
                Set<Integer> set = idsOn;
                idsOn = idsOff;
                idsOff = set;
            }
            idsOff.clear();
            Iterator<Integer> idIter = idsOn.iterator();
            for (int i = 0; i < num; i++) {
                idsOff.add(idIter.next());
            }
            modify.update(idsOff, idsOn.size() < modifyAd.getAmount());
        }
        else if (idsOn.size() > 0) {
            modify.update(idsOn, false);
        }
        return Result.ok();
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
