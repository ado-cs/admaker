package we.lcx.admaker.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.entities.Ad;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.common.entities.Unit;
import we.lcx.admaker.common.enums.*;
import we.lcx.admaker.service.AdCreateService;
import we.lcx.admaker.service.Basic;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.WordsTool;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * Created by LinChenxiao on 2019/12/23 17:26
 **/
@Slf4j
@Service
public class MaiTian implements AdCreateService {
    private static final long DAY = 24 * 60 * 60 * 1000;
    @Resource
    private Basic basic;

    @Value("${ad.common.account}")
    private String ACCOUNT;

    @Value("${ad.url.maitian}")
    private String URL;

    @Value("${ad.common.dspId}")
    private Integer DSP_ID;

    //以下字段需在麦田提前建立对应条目

    @Value("${ad.maitian.customerId}")
    private Long CUSTOMER_ID; //客户id

    @Value("${ad.maitian.mediaId}")
    private Integer MEDIA_ID; //资源媒体id

    @Value("${ad.maitian.dealId}")
    private Integer DEAL_ID; //排期第一个id

    private String cookie;

    @PostConstruct
    private void login() {
        TaskResult result = HttpExecutor.doRequest(
                Task.get(URL + "mock?user=" + ACCOUNT));
        result.valid("麦田登录失败");
        List<String> list = result.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (!CollectionUtils.isEmpty(list)) cookie = list.get(0);
        else log.error("麦田cookie无效");
    }

    private int getResourceId(NewAds ads) {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_RESOURCE)
                        .cookie(cookie).param(Entity.of(Params.MAITIAN_RESOURCE)
                        .put("resourceName", ads.getAd().getPackageName() + Settings.SUFFIX_VERSION)));
        result.valid("获取资源id失败");
        if ((int) result.getEntity().get("result total") > 0)
            return (int) result.getEntity().cd("result/list[0]").get("uid");
        result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_RESOURCE_NEW)
                        .cookie(cookie).param(Entity.of(Params.MAITIAN_RESOURCE_NEW)
                        .put("resourceName", ads.getAd().getPackageName() + Settings.SUFFIX_VERSION)));
        result.valid("创建资源失败");
        return (int) result.getEntity().get("result");
    }

    private int createItem(NewAds ads) {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_ITEM)
                        .cookie(cookie).param(Entity.of(Params.MAITIAN_ITEM)
                        .put("resourceUid", ads.getResourceId())
                        .put("mediaUid", MEDIA_ID)
                        .put("itemName", ads.getDealMode().name() + "_" + ads.getContractMode().name() + WordsTool.randomSuffix(4))
                        .put("resourceTrafficType", ads.getDealMode().name())
                        .put("billingMode", "BILLING_MODE_" + ads.getContractMode().getValue())
                        .put("resourceTrafficRatio", ads.getContractMode() == ContractMode.CPM ? "ZERO" : ads.getFlowEnum().getValue())
                        .put("trafficSplitTypes", WordsTool.toList(ContractMode.CPT.getTraffic(), ContractMode.CPM.getTraffic()))
                        .put("positionIdList", WordsTool.toList(String.valueOf(ads.getAd().getPackageId())))
                        .cd("positionList[0]")
                        .put("code", String.valueOf(ads.getAd().getPackageId()))
                        .put("value", ads.getAd().getPackageName())));
        result.valid("创建资源条目失败");
        return (int) result.getEntity().get("result");
    }

    private int createRevenue(NewAds ads) {
        Date date = WordsTool.parseDate(ads.getBegin());
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_REVENUE)
                        .cookie(cookie).param(Entity.of(Params.MAITIAN_REVENUE)
                        .put("resourceUid", ads.getResourceId())
                        .put("resourceItemUid", ads.getResourceItemId())
                        .put("revenueName", ads.getResourceItemId() + WordsTool.randomSuffix(4))
                        .put("year", date.getYear() + 1900)
                        .put("month", "Q" + (date.getMonth() / 3 + 1))
                        .cd("durationVO")
                        .put("beginTime", date.getTime())
                        .put("endTime", WordsTool.parseTime(ads.getEnd()) + DAY - 1)));
        result.valid("创建资源条目失败");
        return (int) result.getEntity().get("result");
    }

    private int createReservation(NewAds ads) {
        Entity entity = Entity.of(Params.MAITIAN_RESERVE)
                .put("customerUid", CUSTOMER_ID)
                .put("resourceOwnerUid", MEDIA_ID)
                .put("resourceUid", ads.getResourceId())
                .put("resourceItemUid", ads.getResourceItemId())
                .put("billingModeCode", ads.getContractMode().getCode())
                .put("revenueUid", ads.getRevenueId())
                .cd("trafficType")
                .put("code", String.valueOf(ads.getDealMode().getCode()))
                .put("value", ads.getDealMode().getValue())
                .cd("/resourceItem")
                .put("uid", ads.getResourceItemId())
                .cd("/mergeDurations");
        long m = WordsTool.parseTime(ads.getBegin());
        long n = WordsTool.parseTime(ads.getEnd());
        if (m > n) throw new RuntimeException("结束时间必须在开始时间之后");
        while (m <= n) {
            entity.put("beginTime", m);
            m += DAY;
            entity.put("endTime", m - 1).add();
        }
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_RESERVE)
                        .cookie(cookie).param(entity));
        result.valid("资源预定失败，请检查当日是否排期已满");
        return (int) result.getEntity().get("result");
    }

    private int createDeal(DealMode deal, String begin, String end, CategoryEnum category) {
//        contractId: 152512
//        firstIndustry: 3
//        secondIndustry: 10
//        brand: 2153992469
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_DEAL)
                        .cookie(cookie).param(Entity.of(Params.MAITIAN_DEAL)
                        .put("name", deal.name() + category.getCode() + WordsTool.randomSuffix(4))
                        .put("scheduleTrafficType", deal.name())
                        .put("beginTime", WordsTool.parseTime(begin))
                        .put("endTime", WordsTool.parseTime(end) + DAY - 1000)
//         .put("contractUid", CONTRACT_ID)
//         .put("firstIndustryUid", FIRST_INDUSTRY)
//         .put("secondIndustryUid", SECOND_INDUSTRY)
//         .put("brandUid", BRAND)
                        .put("productName", WordsTool.randomSuffix(6))
                        .put("scheduleCategoryCode", String.valueOf(category.getCode()))));
        result.valid("创建排期失败");
        return (int) result.getEntity().get("result uid");
    }

    private int getDealId(NewAds ads) {
        if (ads.getDealMode() == DealMode.PDB) return DEAL_ID + ads.getCategoryEnum().getCode() - 1;
        else if (ads.getDealMode() == DealMode.PD) return DEAL_ID + ads.getCategoryEnum().getCode() + 1;
        else return DEAL_ID + ads.getCategoryEnum().getCode() + 3;
    }

    private int createDealItem(NewAds ads, String reservationId) {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_QUERY)
                        .cookie(cookie).param(Entity.of(Params.MAITIAN_QUERY).put("uid", reservationId)));
        result.valid("获取预定信息失败");
        Object obj = result.getEntity().get("result reserveDatingVOs");
        Entity entity = Entity.of(Params.MAITIAN_DEAL_ITEM)
                .put("name", ads.getAd().getFlightName() + WordsTool.randomSuffix(6))
                .put("scheduleUid", ads.getDealId())
                .put("reserveItemUid", reservationId)
                .put("positionUids", WordsTool.toList(String.valueOf(ads.getAd().getPackageId())))
                .put("trafficSplitType", ads.getContractMode().getTraffic())
                .put("realAmountDaily", ads.getContractMode() == ContractMode.CPM ? String.valueOf(ads.getShowNumber()) : "")
                .put("realTrafficRatio", ads.getContractMode() == ContractMode.CPT ? ads.getShowRadio() : 0)
                .put("refundStatus", ads.getDealMode() == DealMode.PD ? "REFUND" : "NOT_REFUND")
                .put("refundAmountRatio", ads.getDealMode() == DealMode.PD ? 1 : 0)
                .put("resourceRevenueUid", ads.getRevenueId())
                .put("deliveryPeriods", obj)
                .put("dspUid", ads.getDspId())
                .put("costType", ads.getDealMode() == DealMode.PD ? "OFFLINE_SETTLE" : "FREE_TEST")
                .cd("deliveryPeriods").each(v -> {
                    v.put("serveBeginTime", v.get("beginTime"));
                    v.put("serveEndTime", v.get("endTime"));
                    return false;
                });
        result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_DEAL_ITEM)
                        .cookie(cookie).param(entity));
        result.valid("创建排期条目失败");
        return (int) result.getEntity().get("result");
    }

    private List buildCreative(NewAds ads) {
        Ad ad = ads.getAd();
        TaskResult result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_TEMPLATE).cookie(cookie)
                .param(Entity.of(Params.MAITIAN_TEMPLATE).put("uid", ad.getPackageId())));
        result.valid("获取版位模板信息失败");
        Object template = result.getEntity().get("result");
        List mediaUnits = (List) result.getEntity().cd("result/multiMediaUnits").getCurrent();
        Entity creative = Entity.of(Params.MAITIAN_CREATIVE)
                .put("templateRefId", ad.getRefId())
                .put("template", template);
        for (Unit unit : ad.getUnits()) {
            if (unit.getType() == ShowType.TEXT) {
                creative.put(unit.getName(), unit.getName().equals("mediaSponsorId") ? String.valueOf(MEDIA_ID) : WordsTool.repeat(unit.getLimit()));
            }
        }
        creative.cd("multiMediaList");
        int idx = 0;
        for (Unit unit : ad.getUnits()) {
            if (unit.getType() == ShowType.PICTURE) {
                creative.put("multiMediaType", 1)
                        .put("materialMd5", Settings.DEFAULT_MD5)
                        .put("materialSize", unit.getLimit())
                        .put("materialUrl", Settings.DEFAULT_URL)
                        .put("sortIndex", ((Map) mediaUnits.get(idx)).get("sortIndex"))
                        .put("backendMaterialUrl", "")
                        .put("resourceId", "")
                        .put("template", mediaUnits.get(idx++))
                        .add();
            }
        }
        return WordsTool.toList(creative.getHead());
    }

    @Override
    public Result createAd(NewAds ads) {
        Ad var0 = basic.getAdFlight(ads.getFlight());
        ads.setAd(var0);
        List list = buildCreative(ads);
        int resourceId = getResourceId(ads);
        ads.setResourceId(resourceId);
        int itemId = createItem(ads);
        ads.setResourceItemId(itemId);
        int revenueId = createRevenue(ads);
        ads.setRevenueId(revenueId);
        int dealId = getDealId(ads);
        ads.setDealId(dealId);
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < ads.getAmount(); i++) {
            int reservationId = createReservation(ads);
            int dealItemId = createDealItem(ads, String.valueOf(reservationId));
            if (!DSP_ID.equals(ads.getDspId())) continue;
            Entity entity = Entity.of(Params.MAITIAN_CREATE);
            entity.put("name", ads.getName() + "_" + i + WordsTool.randomSuffix(4))
                    .put("execPeriods", WordsTool.toList(WordsTool.parseTime(ads.getBegin()), WordsTool.parseTime(ads.getEnd()) + DAY - 1))
                    .put("creatives", list)
                    .cd("scheduleItemInfo")
                    .put("positionId", String.valueOf(var0.getPackageId()))
                    .put("scheduleId", dealId)
                    .put("scheduleItemId", dealItemId);
            tasks.add(Task.post(URL + URLs.MAITIAN_CREATE).cookie(cookie).param(entity));
        }
        if (!DSP_ID.equals(ads.getDspId())) return Result.ok();
        List<Integer> adIds = new ArrayList<>();
        for (TaskResult result : HttpExecutor.execute(tasks)) {
            if (result.isSuccess()) {
                adIds.add((Integer) result.getEntity().get("result"));
            }
        }
        List<Integer> failed = basic.approveAds(adIds);
        return CollectionUtils.isEmpty(failed) ? Result.ok() :
                Result.fail(String.format("%d个广告创建失败，%d个创意审核失败！", ads.getAmount() - adIds.size(), failed.size()));
    }

    @Override
    public void closeItems(List<Integer> itemIds, boolean delete) {
        Map<Integer, Integer> dealItems = new HashMap<>();
        Map<Integer, Integer> versions = new HashMap<>();
        Map<Integer, Boolean> status = new HashMap<>();

        for (Integer id : new HashSet<>(itemIds)) {
            TaskResult result;
            Integer resId = dealItems.get(id);
            Integer version = versions.get(id);
            Boolean flag = status.get(id);

            if (resId == null) {
                result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_PAGE).cookie(cookie).param(Entity.of(Params.MAITIAN_PAGE)
                        .put("scheduleItemUid", id)));
                if (!result.isSuccess()) {
                    log.error("获取排期条目所在排期ID失败，itemId = {}", id);
                    continue;
                }
                Integer dealId = (Integer) result.getEntity().get("result list uid");
                result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_DETAIL).cookie(cookie).param(Entity.of(Params.MAITIAN_DETAIL)
                        .put("uid", String.valueOf(dealId))));
                if (!result.isSuccess()) {
                    log.error("获取排期下所有排期条目详情失败，itemId = {}", id);
                    continue;
                }
                result.getEntity().cd("result/items").each(v -> {
                    Integer uid = (Integer) v.get("uid");
                    dealItems.put(uid, (Integer) v.get("reserveItemUid"));
                    versions.put(uid, (Integer) v.get("version"));
                    status.put(uid, "ON".equals(v.get("trafficSwitch code")));
                    return false;
                });
                resId = dealItems.get(id);
                version = versions.get(id);
                flag = status.get(id);
            }
            if (resId == null || version == null || flag == null) {
                log.error("获取排期条目关联预约ID失败，itemId = {}", id);
                continue;
            }
            if (flag) {
                result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_ITEM_CLOSE).cookie(cookie).param(Entity.of(Params.MAITIAN_ITEM_CLOSE)
                        .put("uid", id).put("version", version)));
                if (!result.isSuccess()) {
                    log.error("关闭排期条目流量失败，itemId = {}", id);
                    continue;
                }
                version++;
            }

            result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_AD_LIST).cookie(cookie).param(Entity.of(Params.MAITIAN_AD_LIST)
                    .put("scheduleItemId", id)));
            if (!result.isSuccess()) {
                log.error("获取排期条目下广告位失败，itemId = {}", id);
                continue;
            }
            result.getEntity().cd("result/list").each(v -> {
                Integer adId = (Integer) v.get("id");
                Integer vs = (Integer) v.get("version");
                if ("1001".equals(v.get("activeStatus code"))) {
                    TaskResult r = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_AD_CLOSE).cookie(cookie).param(Entity.of(Params.MAITIAN_AD_CLOSE)
                            .put("id", adId).put("version", vs)));
                    if (!r.isSuccess()) log.error("关闭广告失败，itemId = {}, adId = {}", id, adId);
                    else vs++;
                }
                if (delete &&
                        !HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_AD_DELETE).cookie(cookie).param(Entity.of(Params.MAITIAN_AD_DELETE)
                                .put("uid", adId).put("version", vs))).isSuccess())
                    log.error("删除广告失败，itemId = {}, adId = {}", id, adId);
                return false;
            });

            if (delete) {
                result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_ITEM_DELETE).cookie(cookie).param(Entity.of(Params.MAITIAN_ITEM_DELETE)
                        .put("uid", id).put("version", version)));
                if (!result.isSuccess()) {
                    log.error("删除排期条目失败，itemId = {}", id);
                    continue;
                }

                result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_RESERVATION_DELETE).cookie(cookie).param(Entity.of(Params.MAITIAN_RESERVATION_DELETE)
                        .put("uid", resId)));
                if (!result.isSuccess())
                    log.error("删除资源预定失败，itemId = {}, 预定id = {}", id, resId);

            }
        }
    }

    @Override
    public void cancel(String traceId) {

    }
}
