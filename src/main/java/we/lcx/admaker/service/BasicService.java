package we.lcx.admaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.VisibleException;
import we.lcx.admaker.common.enums.DealMode;
import we.lcx.admaker.common.json.DealItem;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.common.json.*;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.entities.Ad;
import we.lcx.admaker.common.entities.Unit;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Urls;
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

    @Value("${ad.setting.suffix}")
    private String SUFFIX;

    private static final int CONTRACT_ID = 152512; //合同id
    private static final int IND1 = 3; //第一行业id
    private static final int IND2 = 10; //第二行业id
    private static final long BRAND = 2153992469L; //品牌id

    private Map<Integer, Ad> ads = new HashMap<>();
    private Map<DealMode, Integer> envDeals = new HashMap<>();
    private Map<Integer, Ad> positions = new HashMap<>();

    private volatile boolean processing;

    private String cookie;

    @PostConstruct
    private void login() {
        List<String> list = HttpExecutor.doRequest(Task.get(URL_MAITIAN + "mock?user=" + ACCOUNT)).valid("麦田登录失败")
                .getHeaders().get(HttpHeaders.SET_COOKIE);
        if (!CollectionUtils.isEmpty(list)) cookie = list.get(0);
        else log.error("麦田cookie无效");
        refreshDeals();
        refreshPositions();
    }

    public String getCookie() {
        return cookie;
    }

    public Map<DealMode, Integer> getDeals() { return envDeals; }

    private void refreshDeals() {
        Map<DealMode, Integer> map = new HashMap<>();
        HttpExecutor.doRequest(
                Task.post(URL_MAITIAN + Urls.MAITIAN_DEAL_LIST)
                        .cookie(cookie).param(Entity.of(Params.COMMON_PAGE).put("scheduleName", SUFFIX)))
                .valid("获取排期失败").getEntity().cd("result/list").each(e -> {
            String[] s = ((String) e.get("name")).split("_");
            if (s.length != 2) return;
            for (DealMode mode : DealMode.values()) {
                if (mode.name().equalsIgnoreCase(s[0])) {
                    map.put(mode, (Integer) e.get("uid"));
                    break;
                }
            }
        });
        for (DealMode mode : DealMode.values()) {
            if (!map.containsKey(mode)) {
                Date date = new Date();
                map.put(mode, (int) HttpExecutor.doRequest(
                        Task.post(URL_MAITIAN + Urls.MAITIAN_DEAL)
                                .cookie(cookie).param(Entity.of(Params.MAITIAN_DEAL)
                                .put("name", mode.name() + SUFFIX)
                                .put("scheduleTrafficType", mode.name())
                                .put("beginTime", date.getTime())
                                .put("contractUid", CONTRACT_ID)
                                .put("firstIndustryUid", IND1)
                                .put("secondIndustryUid", IND2)
                                .put("brandUid", BRAND)
                                .put("productName", CommonUtil.randomSuffix(6))))
                        .valid("创建排期失败")
                        .getEntity().get("result uid"));
            }
        }
        envDeals = map;
    }

    private void refreshPositions() {
        if (processing) return;
        synchronized (this) {
            if (processing) return;
            processing = true;
        }
        try {
            Map<Integer, Ad> map1 = new HashMap<>();
            Map<Integer, Ad> map2 = new HashMap<>();
            for (AdPosition position : HttpExecutor.doRequest(Task.post(URL_YUNYING + Urls.YUNYING_POSITIONS).param(Entity.of(Params.YUNYING_POSITION)))
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
                HttpExecutor.doRequest(Task.post(URL_YUNYING + Urls.YUNYING_TEMPLATES)
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
                HttpExecutor.doRequest(Task.post(URL_YUNYING + Urls.YUNYING_UNITS)
                        .param(Entity.of(Params.COMMON_QUERY).put("uid", ad.getRefId())))
                        .valid("获取模板单元信息失败").getEntity().cd("result/templateUnit")
                        .each(k -> {
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
                map1.put(ad.getFlightId(), ad);
                map2.put(ad.getPositionId(), ad);
            }
            ads = map1;
            positions = map2;
        } finally {
            processing = false;
        }
    }

    public String getFlightNameByPositionId(Integer id) {
        Ad ad = positions.get(id);
        return ad == null ? "" : ad.getFlightName();
    }

//    public List<DealItem> getActiveDealItems() {
//        List<DealItem> dealItems = new ArrayList<>();
//        for (Ad ad : ads.values()) {
//            HttpExecutor.doRequest(Task.post(URL_YUNYING + Urls.YUNYING_ITEM_LIST)
//                    .param(Entity.of(Params.YUNYING_ITEM_LIST).put("flightId", ad.getFlightId()).put("time", CommonUtil.timeOfNow())))
//                    .valid("获取广告位对应排期条目失败").getEntity().cd("result/list").each(k -> {
//                DealItem item = k.to(DealItem.class);
//                item.setFlightId(ad.getFlightId());
//                item.setFlightName(ad.getFlightName());
//                dealItems.add(item);
//            });
//        }
//        return dealItems;
//    }

    public List<Map<String, String>> queryFlight(String keyword) {
        List<FlightSearch> records = HttpExecutor.doRequest(Task.post(URL_YUNYING + Urls.YUNYING_FLIGHT_QUERY)
                .param(Entity.of(Params.YUNYING_FLIGHT_QUERY).put("nameLike", keyword)))
                .valid("获取广告位数据失败")
                .getEntity().toList(FlightSearch.class);
        if (records == null) return null;
        List<Map<String, String>> results = new ArrayList<>();
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
        HttpExecutor.doRequest(Task.post(URL_YUNYING + Urls.YUNYING_TEMPLATES)
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
        HttpExecutor.doRequest(Task.post(URL_YUNYING + Urls.YUNYING_CREATE)
                .param(entity.put("name", newAds.getFlightName() + CommonUtil.randomSuffix(4))
                        .put("flightUidList", Arrays.asList(newAds.getFlightId()))
                        .put("adType", newAds.getFlightType()))).valid("创建广告版位失败");
        refreshPositions();
        while ((ad = ads.get(newAds.getFlightId())) == null)
            refreshPositions();
        return ad;
    }

    public void checkFlight(int flightId) {
        Object id = HttpExecutor.doRequest(Task.post(URL_YUNYING + Urls.YUNYING_FLIGHT).param(Entity.of(Params.YUNYING_FLIGHT)
                .put("adFlightId", String.valueOf(flightId)).put("dspId", DSP_ID))).valid("查询广告位开关状态失败").getEntity().get("result list id");
        if (id == null) throw new VisibleException("内部DSP未接入该广告位");
        HttpExecutor.doRequest(Task.post(URL_YUNYING + Urls.YUNYING_STATUS).param(Entity.of(Params.YUNYING_STATUS)
                .put("id", id).put("adFlightId", flightId).put("dspId", DSP_ID)))
                .valid("运营平台开启广告位失败");
    }

    public List<Integer> executeAndApprove(List<Task> tasks, int adType) {
        List<Integer> adIds = new ArrayList<>();
        boolean flag = false;
        for (TaskResult result : HttpExecutor.execute(tasks)) {
            if (result.isSuccess())
                adIds.add((Integer) result.getEntity().get("result"));
            else flag = true;
        }
        if (flag) return adIds;
        return (adType == 1 ? approveContractAds(adIds) : approveBiddingAds(adIds)) ? null : adIds;
    }

    private boolean approveContractAds(List<Integer> adIds) {
        List<Task> tasks = new ArrayList<>();
        for (Integer id : adIds) {
            TaskResult result = HttpExecutor.doRequest(Task.post(URL_MAITIAN + Urls.MAITIAN_CREATIVE_QUERY)
                    .cookie(cookie)
                    .param(Entity.of(Params.MAITIAN_CREATIVE_QUERY)
                            .put("uid", id)));
            if (!result.isSuccess()) return false;
            tasks.add(Task.post(URL_MAITIAN + Urls.COMMON_APPROVE).tag(id)
                    .param(Entity.of(Params.COMMON_APPROVE)
                            .put("creativeId", "MAISUI_" + result.getEntity()
                                    .get("result list id"))));
        }
        for (TaskResult r : HttpExecutor.execute(tasks)) {
            if (!r.isSuccess()) return false;
        }
        return true;
    }

    private boolean approveBiddingAds(List<Integer> adIds) {
        List<Task> tasks = new ArrayList<>();
        for (Integer id : adIds) {
            TaskResult result = HttpExecutor.doRequest(Task.post(URL_MAISUI + Urls.MAISUI_CREATIVE_QUERY)
                    .param(Entity.of(Params.MAISUI_CREATIVE_QUERY)
                            .put("adformId", id)));
            if (!result.isSuccess()) return false;
            tasks.add(Task.post(URL_MAITIAN + Urls.COMMON_APPROVE).tag(id)
                    .param(Entity.of(Params.COMMON_APPROVE)
                            .put("creativeId", "MAISUI_" + result.getEntity()
                                    .get("result adCreativeList creativeId"))));
        }
        for (TaskResult r : HttpExecutor.execute(tasks)) {
            if (!r.isSuccess()) return false;
        }
        return true;
    }
}

