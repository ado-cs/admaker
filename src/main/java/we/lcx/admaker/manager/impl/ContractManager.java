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
                contractModify.rollback(null, created);
                throw e;
            }
            if (!DSP_ID.equals(ads.getDspId())) continue;
            Entity entity = Entity.of(Params.MAITIAN_CREATE);
            entity.put("name", ads.getFlightName() + "_" + ads.getDealMode().name() + "_" + ads.getContractMode().name()
                    + CommonUtil.randomSuffix(6))
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
        contractModify.rollback(adIds, created);
        return Result.fail("创建广告失败！");
    }
}
