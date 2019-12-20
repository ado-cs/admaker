package we.lcx.admaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.ad.AdFlight;
import we.lcx.admaker.common.ad.AdPackage;
import we.lcx.admaker.common.ad.AdUnit;
import we.lcx.admaker.common.ad.QueryFlight;
import we.lcx.admaker.common.basic.Entity;
import we.lcx.admaker.common.basic.Task;
import we.lcx.admaker.common.bo.Ad;
import we.lcx.admaker.common.bo.Unit;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.enums.ShowType;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.WordsTool;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Created by LinChenxiao on 2019/12/13 09:54
 **/
@Slf4j
@Service
public class Basic {
    @Value("${ad.maisui.url}")
    private String URL;

    @Value("${ad.maisui.cookie}")
    private String COOKIE;

    private Map<Integer, Ad> packages = new HashMap<>();

    @PostConstruct
    public void login() {
        if (!HttpExecutor.doRequest(
                Task.get(URL + URLs.MAISUI_LOGIN)
                        .cookie(COOKIE)).isValid()) {
            log.error("麦穗登录失败");
            return;
        }
        initPackages();
    }

    private void initPackages() {
        Map<Integer, Ad> var0 = new HashMap<>();
        Entity var1 = HttpExecutor.doRequest(Task.post(URL + URLs.YUNYING_URL + URLs.MAISUI_PACKAGES).cookie(COOKIE).param(Entity.of(Params.MAISUI_PACKAGES)));
        if (!var1.isValid()) {
            log.error("获取广告版位失败.");
            return;
        }
        List<AdPackage> var2 = var1.toList(AdPackage.class);
        if (var2 == null) {
            log.error("解析广告版位失败.");
            return;
        }
        for (AdPackage var3 : var2) {
            if (var3.getStatus() != 201) continue;
            if (CollectionUtils.isEmpty(var3.getFlights()) || var3.getFlights().size() != 1) continue;
            Ad var4 = new Ad();
            AdFlight var5 = var3.getFlights().get(0);
            var4.setFlightId(var5.getId());
            var4.setFlightName(var5.getName());
            var4.setPackageId(var3.getId());
            var4.setPackageName(var3.getName());
            var4.setRefId(var3.getRefId());
            var4.setMainType(var3.getMainType());
            var4.setShowType(var3.getShowType());
            List<Unit> var6 = new ArrayList<>();
            for (AdUnit var7 : var3.getUnits()) {
                Unit var8 = new Unit();
                var8.setId(var7.getId());
                var8.setOrderId(var7.getOrderId());
                var8.setType(var7.getType());
                if (var7.getType() == ShowType.TEXT)
                    var8.setLimit(String.valueOf(Math.floor((var7.getLength() + var7.getLowerLength()) / 2.0)));
                else var8.setLimit(var7.getSize());
                var6.add(var8);
            }
            var4.setUnits(var6);
            var0.put(var5.getId(), var4);
        }
        synchronized (this) {
            packages = var0;
        }
    }

    public Map<Integer, String> queryFlight(Integer type, String keyword) {
        Map<Integer, String> map = new HashMap<>();
        Entity entity = HttpExecutor.doRequest(Task.post(URL + URLs.YUNYING_LIST)
                .cookie(COOKIE).param(Entity.of(Params.YUNYING_LIST).put("nameLike", keyword)));
        if (!entity.isValid()) return map;
        List<QueryFlight> list = entity.toList(QueryFlight.class);
        if (list == null) return map;
        for (QueryFlight v : list) {
            if ("E7D5508C".equals(v.getMediaCode()) && (v.getAdType() == 3 || v.getAdType().equals(type)))
                map.put(v.getId(), v.getName());
        }
        return map;
    }

    public Ad getAdFlight(Integer id) {
        Ad var0 = packages.get(id);
        if (var0 != null) return var0;
        Entity var1 = HttpExecutor.doRequest(Task.post(URL + URLs.YUNYING_QUERY)
                .cookie(COOKIE).param(Entity.of(Params.YUNYING_QUERY).put("flightId", id)));
        var1.asset("获取广告位信息失败");
        Integer var2 = null;
        for (Entity var3 : var1.ofList("result")) {
            String var4 = String.valueOf(var3.get("mainShowType"));
            if ("PICTURE".equals(var4) || "TEXT".equals(var4)) {
                var2 = (Integer) var3.get("id");
                break;
            }
        }
        if (var2 == null) throw new RuntimeException("不支持该广告位的模板类型");
        var1 = Entity.of(Params.YUNYING_CREATE).put("name", Settings.PREFIX_NAME + "_" + id)
                .put("flightUidList", WordsTool.toList(id))
                .put("templateUidList", WordsTool.toList(var2));
        var1.asset("创建广告版位失败");
        initPackages();
        return packages.get(id);
    }
}

