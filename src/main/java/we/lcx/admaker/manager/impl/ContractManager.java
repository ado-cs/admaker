package we.lcx.admaker.manager.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.Urls;
import we.lcx.admaker.common.entities.*;
import we.lcx.admaker.manager.AdManager;
import we.lcx.admaker.service.BasicService;
import we.lcx.admaker.service.ContractCreate;
import we.lcx.admaker.service.ContractModify;
import we.lcx.admaker.utils.CommonUtil;
import we.lcx.admaker.utils.HttpExecutor;
import javax.annotation.Resource;
import java.util.*;

/**
 * Created by LinChenxiao on 2020/01/02 17:14
 **/
@Service
public class ContractManager implements AdManager {
    @Resource
    private BasicService basicService;

    @Resource
    private ContractCreate contractCreate;

    @Resource
    private ContractModify contractModify;

    @Value("${ad.url.maitian}")
    private String URL;

    @Value("${ad.common.dspId}")
    private Integer DSP_ID;

    @Override
    public Result create(NewAds ads) {
        ContractLog tag = new ContractLog();
        tag.setAd(basicService.getAdFlight(ads));
        tag.setCreative(contractCreate.buildCreative(tag));
        tag.setResourceId(contractCreate.createResource(tag));
        tag.setResourceItemId(contractCreate.createItem(ads, tag));
        tag.setRevenueId(contractCreate.createRevenue(tag));
        tag.setDealId(contractCreate.createDeal(ads, tag));
        List<Task> tasks = new ArrayList<>();
        List<Pair<Integer, Integer>> created = new ArrayList<>();
        for (int i = 0; i < ads.getAmount(); i++) {
            Pair<Integer, Integer> pair = new Pair<>();
            try {
                tag.setReservationId(contractCreate.createReservation(ads, tag));
                pair.setKey(tag.getReservationId());
                created.add(pair);
                tag.setDealItemId(contractCreate.createDealItem(ads, tag));
                pair.setValue(tag.getDealItemId());
            }
            catch (Exception e) {
                rollback(created, null);
                throw e;
            }
            if (!DSP_ID.equals(ads.getDspId())) continue;
            Entity entity = Entity.of(Params.MAITIAN_CREATE);
            entity.put("name", ads.getFlightName() + "_" + ads.getDealMode().name() + "_" + ads.getContractMode().name()
                    + "_" + Settings.SPECIAL_NAME + CommonUtil.randomSuffix(6))
                    .put("execPeriods", Arrays.asList(CommonUtil.parseTime(ads.getBegin()), CommonUtil.parseTime(ads.getEnd()) + Settings.DAY - 1))
                    .put("creatives", tag.getCreative())
                    .cd("scheduleItemInfo")
                    .put("positionId", String.valueOf(tag.getAd().getPositionId()))
                    .put("scheduleId", tag.getDealId())
                    .put("scheduleItemId", tag.getDealItemId());
            tasks.add(Task.post(URL + Urls.MAITIAN_CREATE).cookie(basicService.getCookie()).param(entity));
        }
        if (!DSP_ID.equals(ads.getDspId())) return Result.ok();
        List<Integer> adIds = basicService.executeAndApprove(tasks, 1);
        if (adIds == null) return Result.ok();
        rollback(created, adIds);
        return Result.fail("创建广告失败！");
    }

    private void rollback(List<Pair<Integer, Integer>> created, List<Integer> adIds) {
        if (!CollectionUtils.isEmpty(adIds)) {
            contractModify.updateAds(adIds, false);
            contractModify.removeAds(adIds);
        }
        List<Integer> ids = new ArrayList<>();
        List<Integer> dealItems = new ArrayList<>();
        for (Pair<Integer, Integer> v : created) {
            if (v.getKey() != null) ids.add(v.getKey());
            if (v.getValue() != null) dealItems.add(v.getValue());
        }
        if (CollectionUtils.isEmpty(dealItems)) return;
        contractModify.updateItems(dealItems, false);
        contractModify.removeItems(dealItems);
        contractModify.removeReservations(ids);
    }

    @Override
    public Result modify(ModifyAd modifyAd) {
        if (modifyAd.getAmount() < 0) return Result.fail("广告数量无效");
        List<ContractAd> ads = contractModify.getAds(modifyAd.getFlightName(), modifyAd.getDealMode(), modifyAd.getContractMode());
        if (ads == null) return Result.fail("获取麦田广告列表信息失败");
        Set<Integer> itemsOn = new HashSet<>();
        Set<Integer> idsOn = new HashSet<>();
        Set<Integer> itemsOff = new HashSet<>();
        Set<Integer> idsOff = new HashSet<>();
        Integer dealId = null;
        for (ContractAd ad : ads) {
            dealId = ad.getDealId();
            if (ad.getActive()) {
                itemsOn.add(Integer.valueOf(ad.getDealItemId()));
                idsOn.add(ad.getId());
            }
            else {
                itemsOff.add(Integer.valueOf(ad.getDealItemId()));
                idsOff.add(ad.getId());
            }
        }
        if (itemsOn.size() + itemsOff.size() < modifyAd.getAmount())
            return Result.fail("麦田广告单总数不足");
        if (modifyAd.getRemove()) {
            int num = itemsOn.size() + itemsOff.size() - modifyAd.getAmount();
            if (num == 0) return Result.ok();
            contractModify.fillDealItemInfo(dealId);
            if (itemsOff.size() < num) {
                Iterator<Integer> itemIter = itemsOn.iterator();
                Iterator<Integer> idIter = idsOn.iterator();
                while (itemsOn.size() + itemsOff.size() > num) {
                    itemIter.next();
                    idIter.next();
                    itemIter.remove();
                    idIter.remove();
                }
                contractModify.updateAds(idsOn, false);
                contractModify.updateItems(itemsOn, false);
                itemsOff.addAll(itemsOn);
                idsOff.addAll(idsOn);
            }
            else if (itemsOff.size() > num) {
                Iterator<Integer> itemIter = itemsOff.iterator();
                Iterator<Integer> idIter = idsOff.iterator();
                while (itemsOff.size() > num) {
                    itemIter.next();
                    idIter.next();
                    itemIter.remove();
                    idIter.remove();
                }
            }
            contractModify.removeAds(idsOff);
            contractModify.removeItems(itemsOff);
            contractModify.removeReservationsByDealItems(itemsOff);
        }
        else if (modifyAd.getAmount() > 0) {
            if (itemsOn.size() == modifyAd.getAmount()) return Result.ok();
            contractModify.fillDealItemInfo(dealId);
            int num = Math.abs(modifyAd.getAmount() - itemsOn.size());
            if (itemsOn.size() < modifyAd.getAmount()) {
                Set<Integer> set = itemsOn;
                itemsOn = itemsOff;
                itemsOff = set;
                set = idsOn;
                idsOn = idsOff;
                idsOff = set;
            }
            itemsOff.clear();
            idsOff.clear();
            Iterator<Integer> itemIter = itemsOn.iterator();
            Iterator<Integer> idIter = idsOn.iterator();
            for (int i = 0; i < num; i++) {
                itemsOff.add(itemIter.next());
                idsOff.add(idIter.next());
            }
            contractModify.updateItems(itemsOff, itemsOn.size() < modifyAd.getAmount());
            contractModify.updateAds(idsOff, itemsOn.size() < modifyAd.getAmount());
        }
        else if (itemsOn.size() > 0) {
            contractModify.fillDealItemInfo(dealId);
            contractModify.updateAds(idsOn, false);
            contractModify.updateItems(itemsOn, false);
        }
        return Result.ok();
    }
}
