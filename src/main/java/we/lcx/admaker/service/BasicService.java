package we.lcx.admaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.common.json.*;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.entities.Ad;
import we.lcx.admaker.common.entities.Unit;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.enums.ShowType;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.CommonUtil;
import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Created by LinChenxiao on 2019/12/13 09:54
 **/
@Slf4j
@Service
public class BasicService {
    @Value("${ad.common.account}")
    private String ACCOUNT;

    @Value("${ad.url.maisui}")
    private String URL_MAISUI;

    @Value("${ad.url.maitian}")
    private String URL_MAITIAN;

    @Value("${ad.url.yunying}")
    private String URL_YUNYING;

    @Value("${ad.common.dspId}")
    private Integer DSP_ID;

    @Value("${ad.common.groupId}")
    private Integer GROUP_ID;

    private Map<Integer, Ad> ads = new HashMap<>();

    private volatile boolean processing;

    private String cookie;

    @PostConstruct
    private void login() {
        List<String> list = HttpExecutor.doRequest(Task.get(URL_MAITIAN + "mock?user=" + ACCOUNT)).valid("麦田登录失败")
                .getHeaders().get(HttpHeaders.SET_COOKIE);
        if (!CollectionUtils.isEmpty(list)) cookie = list.get(0);
        else log.error("麦田cookie无效");
        initAds();
    }

    public String getCookie() {
        return cookie;
    }

    private void initAds() {
        if (processing) return;
        synchronized (this) {
            if (processing) return;
            processing = true;
        }
        try {
            Map<Integer, Ad> map = new HashMap<>();
            for (AdPosition position : HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_POSITIONS).param(Entity.of(Params.YUNYING_POSITION)))
                    .valid("获取版位失败").getEntity().toList(AdPosition.class)) {
                if (position == null || !Objects.equals(position.getStatus(), 201) ||
                        Objects.equals(position.getPositionType(), 0) ||
                        CommonUtil.notSingle(position.getFlightIds(), position.getTemplateIds()) ||
                        CommonUtil.notContains(position.getGroupIds(), GROUP_ID) ||
                        CommonUtil.notContains(position.getProductTypes(), 101))
                    continue;
                Ad ad = new Ad();
                ad.setFlightId(position.getFlightIds().get(0));
                ad.setFlightName(position.getFlightNames().get(0));
                ad.setPositionId(position.getUid());
                ad.setPositionName(position.getName());
                ad.setRefId(String.valueOf(position.getTemplateIds().get(0)));
                ad.setMainType(Objects.equals(position.getPositionType(), 3) ? ShowType.TEXT : ShowType.PICTURE);
                HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_TEMPLATES)
                        .param(Entity.of(Params.COMMON_QUERY).put("flightId", ad.getFlightId())))
                        .valid("获取模板展示类型失败").getEntity().cd("result").each(v -> {
                    if (Objects.equals(v.get("id"), ad.getRefId())) {
                        ad.setShowType((String) v.get("showType"));
                        return true;
                    }
                    return false;
                });
                List<Unit> units = new ArrayList<>();
                ad.setUnits(units);
                HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_UNITS)
                        .param(Entity.of(Params.COMMON_QUERY).put("uid", ad.getRefId())))
                        .valid("获取模板单元信息失败").getEntity().cd("result/templateUnit").each(k -> {
                    k.each(v -> {
                        AdUnit adUnit = v.to(AdUnit.class);
                        Unit unit = new Unit();
                        unit.setId(adUnit.getUid());
                        unit.setName(CommonUtil.convertName(adUnit.getName()));
                        unit.setOrderId(adUnit.getOrderId());
                        unit.setType(adUnit.getType());
                        unit.setLimit(adUnit.getType() == ShowType.TEXT ?
                                CommonUtil.getLimitLength(adUnit.getLowerLength(), adUnit.getLength()) : adUnit.getSize());
                        units.add(unit);
                    });
                });
                map.put(ad.getFlightId(), ad);
            }
            ads = map;
        } finally {
            processing = false;
        }
    }

    public List<Map<String, String>> queryFlight(String keyword) {
        List<Map<String, String>> results = new ArrayList<>();
        List<FlightSearch> records = HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_FLIGHT_QUERY)
                .param(Entity.of(Params.YUNYING_FLIGHT_QUERY).put("nameLike", keyword)))
                .valid("获取广告位数据失败")
                .getEntity().toList(FlightSearch.class);
        if (records == null) return results;
        for (FlightSearch rec : records) {
            if ("E7D5508C".equals(rec.getMediaCode()) && !rec.getName().contains("废弃") && !rec.getName().contains("无效")) {
                Map<String, String> item = new HashMap<>();
                results.add(item);
                item.put("name", rec.getName());
                item.put("value", rec.getId() + "_" + rec.getAdType());
            }
        }
        return results;
    }

    public Ad getAdFlight(NewAds newAds) {
        Ad ad = ads.get(newAds.getFlightId());
        if (ad != null) return ad;
        Entity entity = Entity.of(Params.YUNYING_CREATE);
        HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_TEMPLATES)
                .param(Entity.of(Params.COMMON_QUERY).put("flightId", newAds.getFlightId())))
                .valid("获取模板单元失败")
                .getEntity().cd("result").each(t -> {
            String s = String.valueOf(t.get("mainShowType"));
            if ("PICTURE".equals(s) || "TEXT".equals(s)) {
                entity.put("templateUidList", Arrays.asList(t.get("id")));
                return true;
            }
            return false;
        });
        HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_CREATE)
                .param(entity.put("name", newAds.getFlightName() + CommonUtil.randomSuffix(4))
                        .put("flightUidList", Arrays.asList(newAds.getFlightId()))
                        .put("adType", newAds.getFlightType()))).valid("创建广告版位失败");
        initAds();
        while ((ad = ads.get(newAds.getFlightId())) == null)
            initAds();
        return ad;
    }

    public void checkFlight(int flightId) {
        Entity entity = HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_FLIGHT).param(Entity.of(Params.YUNYING_FLIGHT)
                .put("adFlightId", String.valueOf(flightId)).put("dspId", DSP_ID))).valid("查询广告位开关状态失败").getEntity();
        HttpExecutor.doRequest(Task.post(URL_YUNYING + URLs.YUNYING_STATUS).param(Entity.of(Params.YUNYING_STATUS)
                .put("id", entity.get("result list id")).put("adFlightId", flightId).put("dspId", DSP_ID)))
                .valid("运营平台开启广告位失败");
    }

    public boolean approveAds(NewAds ads, Set<Integer> adIds) {
        String url;
        String param;
        if (ads.getType() == 1) {
            url = URL_MAITIAN + URLs.MAITIAN_CREATIVE_QUERY;
            param = Params.MAITIAN_CREATIVE_QUERY;
        } else {
            url = URL_MAISUI + URLs.MAISUI_CREATIVE_QUERY;
            param = Params.MAISUI_CREATIVE_QUERY;
        }
        List<Task> tasks = new ArrayList<>();
        for (Integer id : adIds) {
            Task task = Task.post(url);
            if (ads.getType() == 1) task.cookie(cookie);
            TaskResult result = HttpExecutor.doRequest(task.param(Entity.of(param)
                    .put(ads.getType() == 1 ? "uid" : "adformId", id)));
            if (!result.isSuccess()) return false;
            tasks.add(Task.post(URL_MAITIAN + URLs.COMMON_APPROVE).tag(id)
                    .param(Entity.of(Params.COMMON_APPROVE)
                            .put("creativeId", "MAISUI_" + result.getEntity()
                                    .get(ads.getType() == 1 ? "result list id" : "result adCreativeList creativeId"))));
        }
        for (TaskResult r : HttpExecutor.execute(tasks)) {
            if (!r.isSuccess()) return false;
        }
        return true;
    }
}

