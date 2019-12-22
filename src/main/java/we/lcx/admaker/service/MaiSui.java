package we.lcx.admaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.entity.bo.Ad;
import we.lcx.admaker.entity.bo.Unit;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.common.enums.ShowType;
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
public class MaiSui {
    @Resource
    private Basic basic;

    @Value("${ad.maisui.url}")
    private String URL_MAISUI;

    @Value("${ad.maisui.cookie}")
    private String COOKIE_MAISUI;

    private String newPlan() {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL_MAISUI + URLs.YUNYING_URL + URLs.MAISUI_PLAN)
                        .cookie(COOKIE_MAISUI)
                        .param(Entity.of(Params.MAISUI_PLAN).put("adPlanName", Settings.PREFIX_NAME + WordsTool.randomSuffix(6))));
        result.valid("创建广告计划失败");
        return String.valueOf(result.getEntity().get("adPlanId"));
    }

    private String getPrice(String planId, Integer uid, BiddingMode mode) {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL_MAISUI + URLs.YUNYING_URL + URLs.MAISUI_PRICE)
                        .cookie(COOKIE_MAISUI)
                        .param(Entity.of(Params.MAISUI_PRICE)
                                .put("adPlanId", planId).put("campaignPackageUid", uid).put("billingMode", mode.getCode())));
        result.valid("获取广告报价失败");
        return String.valueOf(result.getEntity().get("campaignPackagePrice"));
    }

    public int createAd(Integer id, BiddingMode mode, String adName, String begin, String end, int num) {
        Ad var0 = basic.getAdFlight(id);
        String var1 = newPlan();
        String var2 = getPrice(var1, var0.getPackageId(), mode);

        var2 = String.valueOf((int) Math.ceil(Double.parseDouble(var2) / 10));
        List<Task> var3 = new ArrayList<>();
        for (int var4 = 0; var4 < num; var4++) {
            Entity var5 = Entity.of(Params.MAISUI_CREATE);
            var3.add(Task.post(URL_MAISUI + URLs.YUNYING_URL + URLs.MAISUI_CREATE)
                    .cookie(COOKIE_MAISUI)
                    .param(var5));
            var5.put("adPlanId", var1)
                    .put("adformName", adName + WordsTool.randomSuffix(6))
                    .put("campaignPackageId", var0.getPackageId())
                    .put("bidAmountMin", var2)
                    .put("bidAmountMax", var2)
                    .put("billingMode", mode.getCode())
                    .put("beginDate", WordsTool.parseDate(begin))
                    .put("endDate", WordsTool.parseDate(end))
                    .cd("adCreativeList[0]").newList("materialList")
                    .put("templateRefId", var0.getRefId())
                    .put("materialName", null)
                    .cd("material")
                    .put("showType", var0.getShowType())
                    .put("mainShowType", var0.getMainType().getCode())
                    .put("mainTitle", "auto creation")
                    .newList("resUrlDetailList");
            for (Unit var6 : var0.getUnits()) {
                var5.put("templateUnitId", var6.getId())
                        .put("need", 1)
                        .put("orderId", var6.getOrderId());
                if (var6.getType() == ShowType.PIC) {
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
        List<TaskResult> var4 = HttpExecutor.execute(var3);
        List<Task> var5 = new ArrayList<>();
        boolean var6 = true;
        for (TaskResult var7 : var4) {
            if (!var7.isSuccess()) {
                if (var6) {
                    var6 = false;
                    var7.error();
                }
                continue;
            }
            var5.add(Task.post(URL_MAISUI + URLs.COMMON_APPROVE)
                    .param(Entity.of(Params.COMMON_APPROVE)
                            .put("creativeId", "MAISUI_" + (Integer.valueOf(String.valueOf(var7.getEntity().get("result"))) + 9500))));
        }
        if (var5.size() == 0) return 0;
        int var7 = 0;
        for (TaskResult var8 : HttpExecutor.execute(var5)) {
            if (var8.isSuccess()) var7++;
        }
        return var7;
    }
}
