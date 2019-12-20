package we.lcx.admaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import we.lcx.admaker.common.basic.Entity;
import we.lcx.admaker.common.basic.Task;
import we.lcx.admaker.common.bo.Ad;
import we.lcx.admaker.common.bo.Unit;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.common.enums.ShowType;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.WordsTool;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Entity entity = HttpExecutor.doRequest(
                Task.post(URL_MAISUI + URLs.YUNYING_URL + URLs.MAISUI_PLAN)
                        .cookie(COOKIE_MAISUI)
                        .param(Entity.of(Params.MAISUI_PLAN).put("adPlanName", Settings.PREFIX_NAME + WordsTool.randomSuffix(6))));
        if (!entity.isValid()) {
            throw new RuntimeException("创建广告计划失败");
        }
        return String.valueOf(entity.get("adPlanId"));
    }

    private String getPrice(String planId, Integer uid, BiddingMode mode) {
        Entity entity = HttpExecutor.doRequest(
                Task.post(URL_MAISUI + URLs.YUNYING_URL + URLs.MAISUI_PRICE)
                        .cookie(COOKIE_MAISUI)
                        .param(Entity.of(Params.MAISUI_PRICE)
                                .put("adPlanId", planId).put("campaignPackageUid", uid).put("billingMode", mode.getCode())));
        if (!entity.isValid()) {
            throw new RuntimeException("获取广告报价失败");
        }
        return String.valueOf(entity.get("campaignPackagePrice"));
    }

    public int createAd(Integer id, BiddingMode mode, String adName, String begin, String end, int num) {
        Ad var0 = basic.getAdFlight(id);
        String var1 = newPlan();
        String var2 = getPrice(var1, var0.getPackageId(), mode);
        var2 = String.valueOf((int) Math.ceil(Double.parseDouble(var2) / 10));
        List<Task> var3 = new ArrayList<>();
        Map<String, Object> var4 = new HashMap<>();
        var4.put("templateRefId", var0.getRefId());
        var4.put("materialName", null);
        Map<String, Object> var5 = new HashMap<>();
        var4.put("material", var5);
        var5.put("showType", var0.getShowType());
        var5.put("mainShowType", var0.getMainType().getCode());
        var5.put("mainTitle", "auto creation");
        List<Map<String, Object>> var6 = new ArrayList<>();
        var5.put("resUrlDetailList", var6);
        for (Unit var7 : var0.getUnits()) {
            Map<String, Object> var8 = new HashMap<>();
            var6.add(var8);
            var8.put("templateUnitId", var7.getId());
            var8.put("need", 1);
            var8.put("orderId", var7.getOrderId());
            if (var7.getType() == ShowType.PIC) {
                var8.put("materialSize", var7.getLimit());
                var8.put("materialUrl", Settings.DEFAULT_URL);
                var8.put("materialMd5", Settings.DEFAULT_MD5);
                var8.put("type", 1);
                var8.put("desc", null);
                var8.put("destinationUrl", null);
            }
            else {
                var8.put("materialSize", null);
                var8.put("materialUrl", null);
                var8.put("materialMd5", null);
                var8.put("type", 3);
                var8.put("desc", WordsTool.repeat(var7.getLimit()));
                var8.put("destinationUrl", "http://www.163.com");
            }
        }
        List<Map<String, Object>> var9 = new ArrayList<>();
        var9.add(var4);
        for (int var10 = 0; var10 < num; var10++) {
            var3.add(Task.post(URL_MAISUI + URLs.YUNYING_URL + URLs.MAISUI_CREATE)
                    .cookie(COOKIE_MAISUI)
                    .param(Entity.of(Params.MAISUI_CREATE)
                    .put("adPlanId", var1)
                    .put("adformName", adName + WordsTool.randomSuffix(6))
                    .put("campaignPackageId", var0.getPackageId())
                    .put("bidAmountMin", var2)
                    .put("bidAmountMax", var2)
                    .put("billingMode", mode.getCode())
                    .put("adCreativeList materialList", var9)
                    .put("beginDate", WordsTool.parseDate(begin))
                    .put("endDate", WordsTool.parseDate(end))));
        }
        List<Entity> var11 = HttpExecutor.execute(var3);
        List<Task> var12 = new ArrayList<>();
        for (Entity var10 : var11) {
            if (!var10.isValid()) continue;
            String var14 = var10.get("result").toString();
            if (var14 == null) continue;
            var12.add(Task.post(URL_MAISUI + URLs.COMMON_APPROVE)
                    .param(Entity.of(Params.COMMON_APPROVE).put("creativeId", "MAISUI_" + (Integer.valueOf(var14) + 9500))));
        }
        if (var12.size() < var11.size()) {
            Entity var10 = var11.get(0);
            log.error("failed to create ads: {} / {}, body = {}",
                    var12.size(), var11.size(), var10.get());
        }
        if (var12.size() == 0) return 0;
        int var13 = 0;
        for (Entity var10 : HttpExecutor.execute(var12)) {
            if (var10.isValid()) var13++;
        }
        return var13;
    }
}
