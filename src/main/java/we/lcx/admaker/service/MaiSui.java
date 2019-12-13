package we.lcx.admaker.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import we.lcx.admaker.common.AdPackage;
import we.lcx.admaker.common.AdUnit;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.consts.Cookies;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.enums.ShowType;
import we.lcx.admaker.utils.DataKeeper;
import we.lcx.admaker.utils.Helper;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.TaskBuilder;

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
    private BaseInfo baseInfo;

    private String newPlan() {
        return Helper.parseBody(HttpExecutor.doRequest(
                TaskBuilder.post(URLs.URL + URLs.MAISUI_PLAN)
                        .cookie(Cookies.MAISUI)
                        .param(Params.MAISUI_PLAN)
                        .param("adPlanName", Settings.PLAN_NAME + Helper.randomSuffix(6)).build()).getBody(), "adPlanId");
    }

    private String getPrice(String planId, String uid, BiddingMode mode) {
        return Helper.parseBody(HttpExecutor.doRequest(
                TaskBuilder.post(URLs.URL + URLs.MAISUI_PRICE)
                        .cookie(Cookies.MAISUI)
                        .param(Params.MAISUI_PRICE)
                        .param("adPlanId", planId)
                        .param("campaignPackageUid", uid)
                        .param("billingMode", mode.getCode()).build()).getBody(), "campaignPackagePrice");
    }

    public int createAd(String uid, BiddingMode mode, String adName, String begin, String end, int num) {
        AdPackage var0 = baseInfo.getPackage(uid);
        if (var0 == null) return 0;
        String var1 = newPlan();
        String var2 = getPrice(var1, uid, mode);
        var2 = String.valueOf((int) Math.ceil(Double.parseDouble(var2) / 100));
        List<Task> var3 = new ArrayList<>();
        Map<String, Object> var4 = new HashMap<>();
        var4.put("templateRefId", var0.getRefId());
        var4.put("materialName", null);
        Map<String, Object> var5 = new HashMap<>();
        var4.put("material", var5);
        var5.put("showType", var0.getShowType());
        var5.put("mainShowType", var0.getMainType().getCode());
        var5.put("content", "auto creation");
        List<Map<String, Object>> var6 = new ArrayList<>();
        var5.put("resUrlDetailList", var6);
        for (AdUnit var7 : var0.getUnits()) {
            Map<String, Object> var8 = new HashMap<>();
            var6.add(var8);
            var8.put("templateUnitId", var7.getId());
            var8.put("need", 1);
            var8.put("orderId", var7.getOrder());
            if (var7.getType() == ShowType.PICTURE) {
                var8.put("materialSize", var7.getLimit());
                var8.put("materialUrl", var7.getContent());
                var8.put("materialMd5", var7.getAppendix());
                var8.put("type", 1);
                var8.put("desc", null);
                var8.put("destinationUrl", null);
            }
            else {
                var8.put("materialSize", null);
                var8.put("materialUrl", null);
                var8.put("materialMd5", null);
                var8.put("type", 3);
                var8.put("desc", Helper.repeat(var7.getLimit()));
                var8.put("destinationUrl", "http://www.163.com");
            }
        }
        List<Map<String, Object>> var9 = new ArrayList<>();
        var9.add(var4);
        for (int var10 = 0; var10 < num; var10++) {
            var3.add(TaskBuilder.post(URLs.URL + URLs.MAISUI_CREATE)
                    .cookie(Cookies.MAISUI)
                    .param(Params.MAISUI_CREATE)
                    .param("adPlanId", var1)
                    .param("adformName", adName + Helper.randomSuffix(6))
                    .param("campaignPackageId", uid)
                    .param("bidAmountMin", var2)
                    .param("bidAmountMax", var2)
                    .param("billingMode", mode.getCode())
                    .param("adCreativeList materialList", var9)
                    .param("beginDate", begin)
                    .param("endDate", end)
                    .build());
        }
        int var11 = 0;
        List<Task> var12 = new ArrayList<>();
        for (Result var13 : HttpExecutor.execute(var3)) {
            if (!var13.isSuccess()) continue;
            Map var14 = JSON.parseObject(var13.getBody(), Map.class);
            if (Helper.valid(var14, "false", "success")) continue;
            String var15 = Helper.parseBody(var13.getBody(), "result");
            if (var15 == null) continue;
            try {
                var12.add(TaskBuilder.post(URLs.BASE_URL + URLs.COMMON_APPROVE)
                        .param(Params.COMMON_APPROVE)
                        .param("creativeId", "MAISUI_" + (Integer.valueOf(var15) + 9500))
                        .build());
                //todo 添加记录
            }
            catch (NumberFormatException e) {
                log.error("failed to parse adId.");
            }
        }
        if (var12.size() == 0) return 0;
        for (Result var13 : HttpExecutor.execute(var12)) {
            if (var13.isSuccess()) var11++;
        }
        return var11;
    }
}
