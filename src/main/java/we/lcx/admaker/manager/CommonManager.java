package we.lcx.admaker.manager;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
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

    public Result getAds(int refreshFlag) {//0: 不用刷新 1: 刷新麦田 2: 刷新麦穗 3: 全部刷新
        if ((refreshFlag & 1) == 1) contractModify.refreshAds();
        if (refreshFlag > 1) biddingModify.refreshAds();
        Map<String, Map<Integer, Map<Integer, Integer>>> result = new HashMap<>();
        for (Object v : contractModify.getAds()) {
            ContractAd ad = (ContractAd) v;
            Map<Integer, Map<Integer, Integer>> map1 = result.computeIfAbsent(ad.getName(), t -> new HashMap<>());
            Map<Integer, Integer> map2 = map1.computeIfAbsent(ad.index(), t -> new HashMap<>());
            Map<Integer, Integer> map3 = map1.computeIfAbsent(ad.index() + 1, t -> new HashMap<>());
            map2.put(ad.getDspId(), map2.getOrDefault(ad.getDspId(), 0) + 1);
            map3.putIfAbsent(ad.getDspId(), 0);
            if (ad.getStatus()) {
                map3.put(ad.getDspId(), map3.get(ad.getDspId()) + 1);
            }
        }
        for (Object v : biddingModify.getAds()) {
            BiddingAd ad = (BiddingAd) v;
            Map<Integer, Map<Integer, Integer>> map1 = result.computeIfAbsent(ad.getName(), t -> new HashMap<>());
            Map<Integer, Integer> map2 = map1.computeIfAbsent(ad.index(), t -> new HashMap<>());
            map2.put(0, map2.getOrDefault(0, 0) + 1);
            if (ad.getStatus()) {
                map2 = map1.computeIfAbsent(ad.index() + 1, t -> new HashMap<>());
                map2.put(0, map2.getOrDefault(0, 0) + 1);
            }
        }
        return Result.ok(result);
    }

    public Result modify(ModifyAd modifyAd) {
        boolean success = true;
        modifyAd.convert();
        Modify modify = modifyAd.getType() == 1 ? contractModify : biddingModify;
        if (modifyAd.getAmount() < 0) modifyAd.setAmount(0);
        Set<Integer> idsOn = new HashSet<>();
        Set<Integer> idsOff = new HashSet<>();
        for (Object ad : modify.getAds()) {
            if (mismatching(modifyAd, ad)) continue;
            if (modifyAd.getType() == 1) {
                ContractAd contractAd = (ContractAd) ad;
                if (contractAd.getStatus()) idsOn.add(contractAd.getDealItemId());
                else idsOff.add(contractAd.getDealItemId());
            }
            else {
                BiddingAd biddingAd = (BiddingAd) ad;
                if (biddingAd.getStatus()) idsOn.add(biddingAd.getId());
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
                success = modify.update(idsOn, false);
                idsOff.addAll(idsOn);
            }
            else if (idsOff.size() > num) {
                Iterator<Integer> idIter = idsOff.iterator();
                while (idsOff.size() > num) {
                    idIter.next();
                    idIter.remove();
                }
            }
            success = success && modify.remove(idsOff);
        }
        else if (modifyAd.getAmount() > 0) {
            if (idsOn.size() == modifyAd.getAmount()) return Result.ok();
            int num = Math.abs(modifyAd.getAmount() - idsOn.size());
            boolean flag = idsOn.size() < modifyAd.getAmount();
            if (flag) {
                Set<Integer> set = idsOn;
                idsOn = idsOff;
                idsOff = set;
            }
            idsOff.clear();
            Iterator<Integer> idIter = idsOn.iterator();
            for (int i = 0; i < num; i++) {
                idsOff.add(idIter.next());
            }
            success = modify.update(idsOff, flag);
        }
        else if (idsOn.size() > 0) {
            success = modify.update(idsOn, false);
        }
        return success ? Result.ok() : Result.fail("存在失败操作，请检查日志！");
    }

    private boolean mismatching(ModifyAd modifyAd, Object ad) {
        if (modifyAd.getType() == 1) {
            ContractAd contractAd = (ContractAd) ad;
            return !StringUtils.isEmpty(modifyAd.getName()) && !modifyAd.getName().equals(contractAd.getName()) ||
                    modifyAd.getDspId() != null && !modifyAd.getDspId().equals(contractAd.getDspId()) ||
                    modifyAd.getDealMode() != null && modifyAd.getDealMode() != contractAd.getDealMode() ||
                    modifyAd.getContractMode() != null && modifyAd.getContractMode() != contractAd.getContractMode();
        }
        else {
            BiddingAd biddingAd = (BiddingAd) ad;
            return !StringUtils.isEmpty(modifyAd.getName()) && !modifyAd.getName().equals(biddingAd.getName()) ||
                    modifyAd.getBiddingMode() != null && modifyAd.getBiddingMode() != biddingAd.getBiddingMode();
        }
    }
}
