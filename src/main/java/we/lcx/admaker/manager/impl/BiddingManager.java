package we.lcx.admaker.manager.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.consts.Urls;
import we.lcx.admaker.common.entities.BiddingAd;
import we.lcx.admaker.common.entities.ContractAd;
import we.lcx.admaker.common.entities.ModifyAd;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.manager.AdManager;
import we.lcx.admaker.service.BasicService;
import we.lcx.admaker.service.BiddingCreate;
import we.lcx.admaker.service.BiddingModify;
import we.lcx.admaker.utils.CommonUtil;
import we.lcx.admaker.utils.HttpExecutor;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by LinChenxiao on 2020/01/02 17:14
 **/
@Service
public class BiddingManager implements AdManager {
    @Resource
    private BasicService basicService;

    @Resource
    private BiddingCreate biddingCreate;

    @Resource
    private BiddingModify biddingModify;

    @Value("${ad.url.maisui}")
    private String URL;

    @Override
    public Result create(NewAds ads) {
        Entity entity = biddingCreate.composeEntity(ads);
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < ads.getAmount(); i++) {
            Entity e = entity.copy();
            tasks.add(Task.post(URL + Urls.MAISUI_CREATE)
                    .param(e.put("adformName", e.get("adformName") + CommonUtil.randomSuffix(6))));
        }
        List<Integer> adIds = basicService.executeAndApprove(tasks, 2);
        if (adIds == null) return Result.ok();
        rollback(adIds);
        return Result.fail("创建广告失败！");
    }

    private void rollback(List<Integer> adIds) {
        if (CollectionUtils.isEmpty(adIds)) return;
        biddingModify.updateAds(adIds, false);
        biddingModify.removeAds(adIds);
    }

    @Override
    public Result modify(ModifyAd modifyAd) {
        if (modifyAd.getAmount() < 0) return Result.fail("广告数量无效");
        List<BiddingAd> ads = biddingModify.getAds(modifyAd.getFlightName(), modifyAd.getBiddingMode());
        if (ads == null) return Result.fail("获取麦穗广告列表信息失败");
        Set<Integer> idsOn = new HashSet<>();
        Set<Integer> idsOff = new HashSet<>();
        for (BiddingAd ad : ads) {
            if (ad.getActive()) {
                idsOn.add(ad.getId());
            } else {
                idsOff.add(ad.getId());
            }
        }
        if (idsOn.size() + idsOff.size() < modifyAd.getAmount())
            return Result.fail("麦穗广告单总数不足");
        if (modifyAd.getRemove()) {
            int num = idsOn.size() + idsOff.size() - modifyAd.getAmount();
            if (num == 0) return Result.ok();
            if (idsOff.size() < num) {
                Iterator<Integer> idIter = idsOn.iterator();
                while (idsOn.size() + idsOff.size() > num) {
                    idIter.next();
                    idIter.remove();
                }
                biddingModify.updateAds(idsOn, false);
                idsOff.addAll(idsOn);
            }
            else if (idsOff.size() > num) {
                Iterator<Integer> idIter = idsOff.iterator();
                while (idsOff.size() > num) {
                    idIter.next();
                    idIter.remove();
                }
            }
            biddingModify.removeAds(idsOff);
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
            biddingModify.updateAds(idsOff, idsOn.size() < modifyAd.getAmount());
        }
        else if (idsOn.size() > 0) {
            biddingModify.updateAds(idsOn, false);
        }
        return Result.ok();
    }
}
