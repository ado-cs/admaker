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
import we.lcx.admaker.service.ContractService;
import we.lcx.admaker.service.aop.TraceAop;
import we.lcx.admaker.utils.CommonUtil;
import we.lcx.admaker.utils.HttpExecutor;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by LinChenxiao on 2020/01/02 17:14
 **/
@Service
public class ContractManager implements AdManager {
    @Resource
    private BasicService basicService;

    @Resource
    private ContractService contractService;

    @Resource
    private TraceAop traceAop;

    @Value("${ad.url.maitian}")
    private String URL;

    @Value("${ad.common.dspId}")
    private Integer DSP_ID;

    @Override
    public Result create(NewAds ads) {
        ContractLog tag = new ContractLog();
        tag.setAd(basicService.getAdFlight(ads));
        tag.setCreative(contractService.buildCreative(ads, tag));
        tag.setResourceId(contractService.createResource(ads, tag));
        tag.setResourceItemId(contractService.createItem(ads, tag));
        tag.setRevenueId(contractService.createRevenue(ads, tag));
        tag.setDealId(contractService.createDeal(ads, tag));
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < ads.getRealAmount(); i++) {
            Pair<Integer, Integer> pair = new Pair<>();
            tag.setReservationId(contractService.createReservation(ads, tag));
            pair.setKey(tag.getReservationId());
            tag.setDealItemId(contractService.createDealItem(ads, tag));
            pair.setValue(tag.getDealItemId());
            if (!DSP_ID.equals(ads.getDspId())) continue;
            Entity entity = Entity.of(Params.MAITIAN_CREATE);
            entity.put("name", ads.getName() + CommonUtil.randomSuffix(4))
                    .put("execPeriods", CommonUtil.toList(CommonUtil.parseTime(ads.getBegin()), CommonUtil.parseTime(ads.getEnd()) + Settings.DAY - 1))
                    .put("creatives", tag.getCreative())
                    .cd("scheduleItemInfo")
                    .put("positionId", String.valueOf(tag.getAd().getPositionId()))
                    .put("scheduleId", tag.getDealId())
                    .put("scheduleItemId", tag.getDealItemId());
            tasks.add(Task.post(URL + URLs.MAITIAN_CREATE).cookie(basicService.getCookie()).param(entity).tag(pair));
        }
        if (!DSP_ID.equals(ads.getDspId())) return Result.ok();
        Set<Integer> adIds = new HashSet<>();
        for (TaskResult result : HttpExecutor.execute(tasks)) {
            if (result.isSuccess()) {
                adIds.add((Integer) result.getEntity().get("result"));
                Pair pair = (Pair) result.getTag();
                traceAop.use(ads.getTraceId(), "reservation", pair.getKey());
                traceAop.use(ads.getTraceId(), "dealItem", pair.getValue());
            }
        }
        return Result.ok(adIds);
    }

    @Override
    public Result cancel(String traceId) {
        NewAds ads = traceAop.getAd(traceId);
        List<Integer> dealItems = new ArrayList<>();
        int id = -1;
        boolean even = false;
        for (Object v : traceAop.cancel(traceId)) {
            if (even) dealItems.add((Integer) v);
            else id = (int) v;
            even = !even;
        }
        if (even) contractService.deleteReservation(id);
        if (CollectionUtils.isEmpty(dealItems)) return Result.ok();
        ModifyAd modifyAd = new ModifyAd();
        modifyAd.setFlightName(ads.getFlightName());
        modifyAd.setCategoryEnum(ads.getCategoryEnum());
        modifyAd.setContractMode(ads.getContractMode());
        modifyAd.setDealMode(ads.getDealMode());
        modifyAd.setState(-1);
        return contractService.modify(modifyAd, dealItems);
    }

    @Override
    public Result modify(ModifyAd modifyAd) {
        modifyAd.convert();
        return contractService.modify(modifyAd);
    }
}
