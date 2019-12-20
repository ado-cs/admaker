package we.lcx.admaker.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import we.lcx.admaker.common.AdPackage;
import we.lcx.admaker.common.AdUnit;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.enums.ShowType;
import we.lcx.admaker.utils.WordsTool;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.TaskBuilder;
import javax.annotation.Resource;
import java.util.*;

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
        String body = HttpExecutor.doRequest(
                TaskBuilder.post(URL_MAISUI + URLs.YUNYING_URL + URLs.MAISUI_PLAN)
                        .cookie(COOKIE_MAISUI)
                        .param(Params.MAISUI_PLAN)
                        .param("adPlanName", Settings.PREFIX_NAME + WordsTool.randomSuffix(6)).build()).getBody();
        if (WordsTool.valid(body, "false", "success")) {
            throw new RuntimeException("failed to create new plan, body = " + body);
        }
        return WordsTool.parseBody(body, "adPlanId");
    }

    private String getPrice(String planId, String uid, BiddingMode mode) {
        String body = HttpExecutor.doRequest(
                TaskBuilder.post(URL_MAISUI + URLs.YUNYING_URL + URLs.MAISUI_PRICE)
                        .cookie(COOKIE_MAISUI)
                        .param(Params.MAISUI_PRICE)
                        .param("adPlanId", planId)
                        .param("campaignPackageUid", uid)
                        .param("billingMode", mode.getCode()).build()).getBody();
        if (WordsTool.valid(body, "false", "success")) {
            throw new RuntimeException("failed to get price, body = " + body);
        }
        return WordsTool.parseBody(body, "campaignPackagePrice");
    }

    public int createAd(String uid, BiddingMode mode, String adName, String begin, String end, int num) {
        AdPackage var0 = basic.getPackage(uid);
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
        var5.put("mainTitle", "auto creation");
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
                var8.put("desc", WordsTool.repeat(var7.getLimit()));
                var8.put("destinationUrl", "http://www.163.com");
            }
        }
        List<Map<String, Object>> var9 = new ArrayList<>();
        var9.add(var4);
        for (int var10 = 0; var10 < num; var10++) {
            var3.add(TaskBuilder.post(URL_MAISUI + URLs.YUNYING_URL + URLs.MAISUI_CREATE)
                    .cookie(COOKIE_MAISUI)
                    .param(Params.MAISUI_CREATE)
                    .param("adPlanId", var1)
                    .param("adformName", adName + WordsTool.randomSuffix(6))
                    .param("campaignPackageId", uid)
                    .param("bidAmountMin", var2)
                    .param("bidAmountMax", var2)
                    .param("billingMode", mode.getCode())
                    .param("adCreativeList materialList", var9)
                    .param("beginDate", WordsTool.parseDate(begin))
                    .param("endDate", WordsTool.parseDate(end))
                    .build());
        }
        List<ResponseEntity<String>> var11 = HttpExecutor.execute(var3);
        List<Task> var12 = new ArrayList<>();
        for (ResponseEntity<String> var13 : var11) {
            if (var13.getStatusCode() != HttpStatus.OK) continue;
            Map var14 = JSON.parseObject(var13.getBody(), Map.class);
            if (WordsTool.valid(var14, "false", "success")) continue;
            String var15 = WordsTool.parseBody(var13.getBody(), "result");
            if (var15 == null) continue;
            var12.add(TaskBuilder.post(URL_MAISUI + URLs.COMMON_APPROVE)
                    .param(Params.COMMON_APPROVE)
                    .param("creativeId", "MAISUI_" + (Integer.valueOf(var15) + 9500))
                    .build());
        }
        if (var12.size() < var11.size()) {
            ResponseEntity<String> var13 = var11.get(0);
            log.error("failed to create ads: {} / {}, code = {}, body = {}",
                    var12.size(), var11.size(), var13.getStatusCode(), var13.getBody());
        }
        if (var12.size() == 0) return 0;
        int var16 = 0;
        for (ResponseEntity<String> var13 : HttpExecutor.execute(var12)) {
            if (var13.getStatusCode() == HttpStatus.OK) var16++;
        }
        return var16;
    }
}
