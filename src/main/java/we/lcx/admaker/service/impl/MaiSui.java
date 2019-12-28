package we.lcx.admaker.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.entities.Ad;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.common.entities.Unit;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.common.enums.ShowType;
import we.lcx.admaker.service.AdCreateService;
import we.lcx.admaker.service.Basic;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.WordsTool;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by LinChenxiao on 2019/12/12 19:21
 **/
@Slf4j
@Service
public class MaiSui implements AdCreateService {
    @Resource
    private Basic basic;

    @Value("${ad.url.maisui}")
    private String URL;

    @Value("${ad.maisui.adPlanId}")
    private String PLAN_ID;

    private ConcurrentHashMap<String, List<Object>> traceMap = new ConcurrentHashMap<>();

    //单位分，bidAmountMin和bidAmountMax单位为元
    private String getPrice(String planId, Integer uid, BiddingMode mode) {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAISUI_PRICE)
                        .param(Entity.of(Params.MAISUI_PRICE)
                                .put("adPlanId", planId).put("campaignPackageUid", uid).put("billingMode", mode.getValue())));
        result.valid("获取广告报价失败");
        return String.valueOf(result.getEntity().get("result campaignPackagePrice"));
    }
    
    private Entity composeEntity(NewAds ads) {
        Ad ad = basic.getAdFlight(ads.getFlight());
        Entity entity = Entity.of(Params.MAISUI_CREATE);
        entity.put("adPlanId", PLAN_ID)
                //.put("adformName", ads.getName() + "_" + var4 + WordsTool.randomSuffix(4))
                .put("campaignPackageId", ad.getPackageId())
                .put("bidAmountMin", "100")
                .put("bidAmountMax", "100")
                .put("billingMode", ads.getBiddingMode().getValue())
                .put("beginDate", WordsTool.parseDateString(ads.getBegin()))
                .put("endDate", WordsTool.parseDateString(ads.getEnd()))
                .cd("adCreativeList[0]").newList("materialList")
                .put("templateRefId", ad.getRefId())
                .put("materialName", null)
                .cd("material")
                .put("showType", ad.getShowType())
                .put("mainShowType", ad.getMainType().getCode());
        for (Unit unit : ad.getUnits()) {
            if (unit.getType() == ShowType.TEXT) {
                entity.put(unit.getName(), WordsTool.repeat(unit.getLimit()));
            }
        }
        entity.newList("resUrlDetailList");
        for (Unit unit : ad.getUnits()) {
            if (unit.getType() == ShowType.PICTURE) {
                entity.put("templateUnitId", unit.getId())
                        .put("need", 1)
                        .put("orderId", unit.getOrderId())
                        .put("materialSize", unit.getLimit())
                        .put("materialUrl", Settings.DEFAULT_URL)
                        .put("materialMd5", Settings.DEFAULT_MD5)
                        .put("type", 1)
                        .put("desc", null)
                        .put("destinationUrl", null)
                        .add();
            }
        }
        return entity;
    }

    @Override
    public Result createAd(NewAds ads) {
        String id = ads.getTraceId();
        List<Object> list = traceMap.computeIfAbsent(id, k -> new ArrayList<>());

        if (list.size() == 0) {
            basic.checkFlight(ads.getFlight());
            list.add(true);
        }
        Entity entity;
        if (list.size() == 1) {
            entity = composeEntity(ads);
            list.add(entity);
        }
        else entity = (Entity) list.get(1);
        int amount;
        if (list.size() == 2) amount = ads.getAmount();
        else amount = (int) list.get(2);
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            tasks.add(Task.post(URL + URLs.MAISUI_CREATE)
                    .param(entity.copy().put("adformName", ads.getName() + WordsTool.randomSuffix(6))));
        }
        List<Integer> adIds = new ArrayList<>();
        for (TaskResult result : HttpExecutor.execute(tasks)) {
            if (result.isSuccess()) {
                amount--;
                adIds.add((Integer) result.getEntity().get("result"));
            }
        }
        if (list.size() > 2) list.set(2, amount); else list.add(amount);
        if (list.size() > 3) adIds.addAll((List)list.get(3));
        List<Integer> failed = basic.approveAds(adIds);
        if (CollectionUtils.isEmpty(failed)) {
            traceMap.remove(id);
            return Result.ok();
        }
        if (list.size() > 3) list.set(3, failed); else list.add(failed);
        return Result.fail(String.format("%d个广告创建失败，%d个创意审核失败！", amount, failed.size()));
    }

    @Override
    public void closeItems(List<Integer> itemIds, boolean delete) {

    }

    @Override
    public void cancel(String traceId) {
        traceMap.remove(traceId);
    }
}
