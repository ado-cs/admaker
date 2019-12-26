package we.lcx.admaker.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.dto.Ad;
import we.lcx.admaker.common.dto.NewAds;
import we.lcx.admaker.common.dto.Unit;
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

    private String getPrice(String planId, Integer uid, BiddingMode mode) {
//        TaskResult result = HttpExecutor.doRequest(
//                Task.post(URL + URLs.MAISUI_PRICE)
//                        .param(Entity.of(Params.MAISUI_PRICE)
//                                .put("adPlanId", planId).put("campaignPackageUid", uid).put("billingMode", mode.getValue())));
//        result.valid("获取广告报价失败");
//        return String.valueOf(result.getEntity().get("result campaignPackagePrice"));
        return "";
    }
    @Override
    public int createAd(NewAds ads) {
        basic.checkFlight(ads.getFlight());
        Ad var0 = basic.getAdFlight(ads.getFlight());
//        String var2 = getPrice(PLAN_ID, var0.getPackageId(), ads.getBiddingMode());
//
//        var2 = String.valueOf((int) Math.ceil(Double.parseDouble(var2) / 10));
        List<Task> var3 = new ArrayList<>();
        for (int var4 = 0; var4 < ads.getAmount(); var4++) {
            Entity var5 = Entity.of(Params.MAISUI_CREATE);
            var3.add(Task.post(URL + URLs.MAISUI_CREATE)
                    .param(var5));
            var5.put("adPlanId", PLAN_ID)
                    .put("adformName", ads.getName() + "_" + var4 + WordsTool.randomSuffix(4))
                    .put("campaignPackageId", var0.getPackageId())
                    .put("bidAmountMin", "100")
                    .put("bidAmountMax", "100")
                    .put("billingMode", ads.getBiddingMode().getValue())
                    .put("beginDate", WordsTool.parseDateString(ads.getBegin()))
                    .put("endDate", WordsTool.parseDateString(ads.getEnd()))
                    .cd("adCreativeList[0]").newList("materialList")
                    .put("templateRefId", var0.getRefId())
                    .put("materialName", null)
                    .cd("material")
                    .put("content", "1234567890")
                    .put("showType", var0.getShowType())
                    .put("mainShowType", var0.getMainType().getCode())
                    .put("mainTitle", "auto creation")
                    .newList("resUrlDetailList");
            for (Unit var6 : var0.getUnits()) {
                var5.put("templateUnitId", var6.getId())
                        .put("need", 1)
                        .put("orderId", var6.getOrderId());
                if (var6.getType() == ShowType.PICTURE) {
                    var5.put("materialSize", var6.getLimit())
                            .put("materialUrl", Settings.DEFAULT_URL)
                            .put("materialMd5", Settings.DEFAULT_MD5)
                            .put("type", 1)
                            .put("desc", null)
                            .put("destinationUrl", null);
                } else {
                    var5.put("materialSize", null)
                            .put("materialUrl", null)
                            .put("materialMd5", null)
                            .put("type", 3)
                            .put("desc", WordsTool.repeat(var6.getLimit()))
                            .put("destinationUrl", "http://www.163.com");
                }
                var5.add();
            }
        }
        return basic.approveAds(HttpExecutor.execute(var3));
    }
}
