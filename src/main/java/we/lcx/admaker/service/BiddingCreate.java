package we.lcx.admaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.entities.Ad;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.common.entities.Unit;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.Urls;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.common.enums.ShowType;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.CommonUtil;
import javax.annotation.Resource;

/**
 * Created by LinChenxiao on 2019/12/12 19:21
 **/
@Slf4j
@Service
public class BiddingCreate {
    @Resource
    private BasicService basicService;

    @Value("${ad.url.maisui}")
    private String URL;

    private static final String PLAN_ID = "443817"; //计划id

    //单位分，bidAmountMin和bidAmountMax单位为元
    private String getPrice(String planId, Integer uid, BiddingMode mode) {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + Urls.MAISUI_PRICE)
                        .param(Entity.of(Params.MAISUI_PRICE)
                                .put("adPlanId", planId).put("campaignPackageUid", uid).put("billingMode", mode.getValue())));
        result.valid("获取广告报价失败");
        return String.valueOf(result.getEntity().get("result campaignPackagePrice"));
    }

    public Entity composeEntity(NewAds ads) {
        basicService.checkFlight(ads.getFlightId());
        Ad ad = basicService.getAdFlight(ads);
        Entity entity = Entity.of(Params.MAISUI_CREATE);
        entity.put("adPlanId", PLAN_ID)
                .put("adformName", ad.getFlightName() + "_" + ads.getBiddingMode().name())
                .put("campaignPackageId", ad.getPositionId())
                .put("bidAmountMin", "100")
                .put("bidAmountMax", "100")
                .put("billingMode", ads.getBiddingMode().getValue())
                .put("beginDate", CommonUtil.parseDateString(ads.getBegin()))
                .put("endDate", CommonUtil.parseDateString(ads.getEnd()))
                .cd("adCreativeList[0]").newList("materialList")
                .put("templateRefId", ad.getRefId())
                .put("materialName", null)
                .cd("material")
                .put("showType", ad.getShowType())
                .put("mainShowType", ad.getMainType().getCode());
        for (Unit unit : ad.getUnits()) {
            if (unit.getType() == ShowType.TEXT) {
                entity.put(unit.getName(), CommonUtil.repeat(unit.getLimit()));
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
}
