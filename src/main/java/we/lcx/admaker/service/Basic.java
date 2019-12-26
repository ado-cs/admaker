package we.lcx.admaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.response.*;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.dto.Ad;
import we.lcx.admaker.common.dto.Unit;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.enums.ShowType;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.WordsTool;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by LinChenxiao on 2019/12/13 09:54
 **/
@Slf4j
@Service
public class Basic {
    @Value("${ad.url.maisui}")
    private String URL_MAISUI;

    @Value("${ad.url.maitian}")
    private String URL_MAITIAN;

    @Value("${ad.url.yunying}")
    private String URL_YUNYING;

    @Value("${ad.common.dspId}")
    private Integer DSP_ID;

    private Map<Integer, Ad> flights = new HashMap<>();
    private volatile boolean processing;

    @PostConstruct
    private void initFlights() {
        if (processing) return;
        synchronized (this) {
            if (processing) return;
            processing = true;
        }
        try {
            Map<Integer, Ad> var0 = new HashMap<>();
            TaskResult var1 = HttpExecutor.doRequest(Task.post(URL_MAISUI + URLs.MAISUI_PACKAGES).param(Entity.of(Params.MAISUI_PACKAGES)));
            var1.valid("获取广告版位失败");
            List<AdPackage> var2 = var1.getEntity().toList(AdPackage.class);
            if (var2 == null) {
                log.error("解析广告版位失败.");
                return;
            }
            v:
            for (AdPackage var3 : var2) {
                if (var3.getStatus() != 201) continue;
                if (CollectionUtils.isEmpty(var3.getFlights()) || var3.getFlights().size() != 1 ||
                        CollectionUtils.isEmpty(var3.getTemplates()) || var3.getTemplates().size() != 1 ||
                        var3.getTemplates().get(0).getMainType().getCode() % 2 == 0 ||
                        CollectionUtils.isEmpty(var3.getTemplates().get(0).getUnits())) continue;
                AdTemplate var4 = var3.getTemplates().get(0);
                AdFlight var5 = var3.getFlights().get(0);
                Ad var6 = new Ad();
                var6.setFlightId(var5.getId());
                var6.setFlightName(var5.getName());
                var6.setPackageId(var3.getId());
                var6.setPackageName(var3.getName());
                var6.setRefId(var4.getRefId());
                var6.setMainType(var4.getMainType());
                var6.setShowType(var4.getShowType());
                List<Unit> var7 = new ArrayList<>();
                for (AdUnit var8 : var4.getUnits()) {
                    if (!var8.getNeed().equals(1)) continue;
                    Unit var9 = new Unit();
                    var9.setId(var8.getId());
                    var9.setName(WordsTool.convertName(var8.getName()));
                    var9.setOrderId(var8.getOrderId());
                    var9.setType(var8.getType());
                    if (var8.getType() == ShowType.TEXT)
                        var9.setLimit(String.valueOf((int) Math.floor((var8.getLength() + var8.getLowerLength()) / 2.0)));
                    else if (var8.getType() == ShowType.PICTURE) var9.setLimit(var8.getSize());
                    else continue v;
                    var7.add(var9);
                }
                var6.setUnits(var7);
                var0.put(var5.getId(), var6);
            }
            flights = var0;
        } finally {
            processing = false;
        }
    }

    public List<Map<String, String>> queryFlight(String keyword) {
        List<Map<String, String>> var0 = new ArrayList<>();
        TaskResult var1 = HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_LIST)
                .param(Entity.of(Params.YUNYING_LIST).put("nameLike", keyword)));
        var1.valid("获取广告位数据失败");
        List<QueryFlight> var2 = var1.getEntity().toList(QueryFlight.class);
        if (var2 == null) return var0;
        for (QueryFlight var3 : var2) {
            if ("E7D5508C".equals(var3.getMediaCode()) && !var3.getName().contains("废弃") && !var3.getName().contains("无效")) {
                Map<String, String> var4 = new HashMap<>();
                var0.add(var4);
                var4.put("name", var3.getName());
                var4.put("value", var3.getId() + "_" + var3.getAdType());
            }
        }
        return var0;
    }

    public Ad getAdFlight(Integer id) {
        Ad var0 = flights.get(id);
        if (var0 != null) return var0;
        TaskResult var1 = HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_QUERY)
                .param(Entity.of(Params.YUNYING_QUERY).put("flightId", id)));
        var1.valid("获取广告位信息失败");
        final AtomicInteger var2 = new AtomicInteger(-1);
        var1.getEntity().cd("result").each(var3 -> {
            String var4 = String.valueOf(var3.get("mainShowType"));
            if ("PICTURE".equals(var4) || "TEXT".equals(var4)) {
                var2.set((Integer) var3.get("id"));
                return true;
            }
            return false;
        });
        int var3 = var2.get();
        if (var3 == -1) throw new RuntimeException("不支持该广告位的模板类型");
        HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_CREATE)
                .param(Entity.of(Params.YUNYING_CREATE)
                        .put("name", Settings.PREFIX_NAME + "_" + id + WordsTool.randomSuffix(4))
                        .put("flightUidList", WordsTool.toList(id))
                        .put("templateUidList", WordsTool.toList(var3)))).valid("创建广告版位失败");
        var3 = 3;
        initFlights();
        while ((var0 = flights.get(id)) == null && var3-- > 0)
            initFlights();
        return var0;
    }

    public void checkFlight(int flightId) {
        TaskResult result = HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_FLIGHT).param(Entity.of(Params.YUNYING_FLIGHT)
                .put("adFlightId", String.valueOf(flightId)).put("dspId", DSP_ID)));
        result.valid("查询广告位开关状态失败");
        HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_STATUS).param(Entity.of(Params.YUNYING_STATUS)
                .put("id", result.getEntity().get("result list id")).put("adFlightId", flightId).put("dspId", DSP_ID)))
                .valid("运营平台开启广告位失败");
    }

    public int approveAds(List<TaskResult> list) {
        List<Task> var0 = new ArrayList<>();
        boolean var1 = true;
        for (TaskResult var2 : list) {
            if (!var2.isSuccess()) {
                if (var1) {
                    var1 = false;
                    var2.error();
                }
                continue;
            }
            var0.add(Task.post(URL_MAITIAN + URLs.COMMON_APPROVE)
                    .param(Entity.of(Params.COMMON_APPROVE)
                            .put("creativeId", "MAISUI_" + (Integer.valueOf(String.valueOf(var2.getEntity().get("result"))) + 9500))));
        }
        if (var0.size() == 0) return 0;
        int var2 = 0;
        for (TaskResult var3 : HttpExecutor.execute(var0)) {
            if (var3.isSuccess()) var2++;
        }
        return var2;
    }
}

