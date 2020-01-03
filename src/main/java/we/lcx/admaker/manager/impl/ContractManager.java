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
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.entities.ContractLog;
import we.lcx.admaker.common.entities.ModifyAd;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.common.entities.Pair;
import we.lcx.admaker.manager.AdManager;
import we.lcx.admaker.service.BasicService;
import we.lcx.admaker.service.ContractCreate;
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
        Set<Pair<Integer, Integer>> created = new HashSet<>();
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
                cancel(created);
                throw e;
            }
            if (!DSP_ID.equals(ads.getDspId())) continue;
            Entity entity = Entity.of(Params.MAITIAN_CREATE);
            entity.put("name", ads.getFlightName() + "_" + ads.getDealMode().name() + "_" + ads.getContractMode().name()
                    + "_" + Settings.SPECIAL_NAME + CommonUtil.randomSuffix(4))
                    .put("execPeriods", Arrays.asList(CommonUtil.parseTime(ads.getBegin()), CommonUtil.parseTime(ads.getEnd()) + Settings.DAY - 1))
                    .put("creatives", tag.getCreative())
                    .cd("scheduleItemInfo")
                    .put("positionId", String.valueOf(tag.getAd().getPositionId()))
                    .put("scheduleId", tag.getDealId())
                    .put("scheduleItemId", tag.getDealItemId());
            tasks.add(Task.post(URL + URLs.MAITIAN_CREATE).cookie(basicService.getCookie()).param(entity));
        }
        if (!DSP_ID.equals(ads.getDspId())) return Result.ok();
        Set<Integer> adIds = new HashSet<>();
        for (TaskResult result : HttpExecutor.execute(tasks)) {
            if (result.isSuccess()) {
                adIds.add((Integer) result.getEntity().get("result"));
            }
            else {
                cancel(created);
                return Result.fail("创建失败，请查看日志！");
            }
        }
        return basicService.approveAds(ads, adIds) ? Result.ok() : Result.fail("创建失败，请查看日志！");
    }

    private void cancel(Set<Pair<Integer, Integer>> created) {
        List<Integer> dealItems = new ArrayList<>();
        for (Pair<Integer, Integer> v : created) {
            if (v.getValue() != null) dealItems.add(v.getValue());
            else if (v.getKey() != null) contractCreate.deleteReservation(v.getKey());
        }
        if (CollectionUtils.isEmpty(dealItems)) return;
        //todo: delete deal items
    }

    @Override
    public Result modify(ModifyAd modifyAd) {
        modifyAd.convert();
        return contractCreate.modify(modifyAd);
    }
}
