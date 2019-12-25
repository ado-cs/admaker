package we.lcx.admaker.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.dto.Ad;
import we.lcx.admaker.common.dto.NewAds;
import we.lcx.admaker.common.dto.Unit;
import we.lcx.admaker.common.enums.*;
import we.lcx.admaker.service.AdCreateService;
import we.lcx.admaker.service.Basic;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.WordsTool;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by LinChenxiao on 2019/12/23 17:26
 **/
@Slf4j
@Service
public class MaiTian implements AdCreateService {
    private static final long DAY = 24 * 60 * 60 * 1000;
    @Resource
    private Basic basic;

    @Value("${ad.login.url}")
    private String LOGIN_URL;

    @Value("${ad.login.account}")
    private String LOGIN_ACCOUNT;

    @Value("${ad.maitian.url}")
    private String URL;

    //以下字段需在麦田提前建立对应条目

    @Value("${ad.maitian.resourceId}")
    private String RESOURCE_ID; //资源id

    @Value("${ad.maitian.customerId}")
    private String CUSTOMER_ID;

    @Value("${ad.maitian.mediaId}")
    private String MEDIA_ID;

    @Value("${ad.maitian.contractId}")
    private String CONTRACT_ID;

    @Value("${ad.maitian.firstIndustry}")
    private String FIRST_INDUSTRY;

    @Value("${ad.maitian.secondIndustry}")
    private String SECOND_INDUSTRY;

    @Value("${ad.maitian.brand}")
    private String BRAND;

    private String cookie;

    @PostConstruct
    private void login() {
        TaskResult result = HttpExecutor.doRequest(
                Task.get(LOGIN_URL + LOGIN_ACCOUNT));
        result.valid("麦田登录失败");
        List<String> list = result.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (!CollectionUtils.isEmpty(list)) cookie = list.get(0);
        else log.error("麦田cookie无效");
    }

    private int createItem(String packageId, String packageName, DealMode deal, ContractMode fee, FlowEnum flow) {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_ITEM)
                        .cookie(cookie).param(Entity.of(Params.MAITIAN_ITEM)
                        .put("resourceUid", RESOURCE_ID)
                        .put("mediaUid", MEDIA_ID)
                        .put("itemName", Settings.PREFIX_NAME + "_" + WordsTool.getNowDate() + WordsTool.randomSuffix(4))
                        .put("resourceTrafficType", deal.name())
                        .put("billingMode", "BILLING_MODE_" + fee.getValue())
                        .put("resourceTrafficRatio", flow.getValue())
                        .put("trafficSplitTypes", WordsTool.toList(ContractMode.CPT.getTraffic(), ContractMode.CPM.getTraffic()))
                        .put("positionIdList", WordsTool.toList(packageId))
                        .cd("positionList[0]")
                        .put("code", packageId)
                        .put("value", packageName)));
        result.valid("创建资源条目失败");
        return (int) result.getEntity().get("result");
    }

    private int createRevenue(int itemId, String begin, String end) {
        Date date = WordsTool.parseDate(begin);
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_REVENUE)
                        .cookie(cookie).param(Entity.of(Params.MAITIAN_REVENUE)
                        .put("resourceUid", RESOURCE_ID)
                        .put("resourceItemUid", itemId)
                        .put("revenueName", Settings.PREFIX_NAME + "_" + WordsTool.getNowDate() + WordsTool.randomSuffix(4))
                        .put("year", date.getYear())
                        .put("month", "Q" + (date.getMonth() / 3 + 1))
                        .cd("durationVO")
                        .put("begin", date.getTime())
                        .put("end", WordsTool.parseTime(end) + DAY - 1)));
        result.valid("创建资源条目失败");
        return (int) result.getEntity().get("result");
    }

    private int createReservation(int itemId, int revenueId, DealMode deal, ContractMode fee, String begin, String end) {
        Entity entity = Entity.of(Params.MAITIAN_RESERVE)
                .put("customerUid", CUSTOMER_ID)
                .put("resourceOwnerUid", MEDIA_ID)
                .put("resourceUid", RESOURCE_ID)
                .put("resourceItemUid", itemId)
                .put("billingModeCode", fee.getCode())
                .put("revenueUid", revenueId)
                .cd("trafficType")
                .put("code", String.valueOf(deal.getCode()))
                .put("value", deal.getValue())
                .cd("/resourceItem")
                .put("uid", itemId)
                .cd("trafficType")
                .put("code", String.valueOf(deal.getCode()))
                .put("value", deal.getValue())
                .cd("/mergeDurations");
        long m = WordsTool.parseTime(begin);
        long n = WordsTool.parseTime(end);
        if (m > n) throw new RuntimeException("结束时间必须在开始时间之后");
        while (m <= n) {
            entity.put("beginTime", m);
            m += DAY;
            entity.put("endTime", m - 1).add();
        }
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_RESERVE)
                        .cookie(cookie).param(entity));
        result.valid("创建资源条目失败");
        return (int) result.getEntity().get("result");
    }

    private int createDeal(DealMode deal, String begin, String end, CategoryEnum category) {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_DEAL)
                        .cookie(cookie).param(Entity.of(Params.MAITIAN_DEAL)
                        .put("name", Settings.PREFIX_NAME + "_" + WordsTool.getNowDate() + WordsTool.randomSuffix(4))
                        .put("scheduleTrafficType", deal.name())
                        .put("begin", WordsTool.parseTime(begin))
                        .put("end", WordsTool.parseTime(end) + DAY - 1000)
                        .put("contractUid", CONTRACT_ID)
                        .put("firstIndustryUid", FIRST_INDUSTRY)
                        .put("secondIndustryUid", SECOND_INDUSTRY)
                        .put("brandUid", BRAND)
                        .put("productName", WordsTool.randomSuffix(6))
                        .put("scheduleCategoryCode", String.valueOf(category.getCode()))));
        result.valid("创建排期失败");
        return (int) result.getEntity().get("result uid");
    }

    private int createDealItem(String reservationId, int dealId, String packageId, int revenueId,
                               String showAmount, double showRadio, DealMode deal, ContractMode fee) {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_QUERY)
                        .cookie(cookie).param(Entity.of(Params.MAITIAN_QUERY).put("uid", reservationId)));
        result.valid("获取预定信息失败");
        Object obj = result.getEntity().get("result reserveDatingVOs");
        Entity entity = Entity.of(Params.MAITIAN_DEAL_ITEM)
                .put("name", WordsTool.randomSuffix(6))
                .put("scheduleUid", dealId)
                .put("reserveItemUid", reservationId)
                .put("positionUids", WordsTool.toList(packageId))
                .put("trafficSplitType", fee.getTraffic())
                .put("realAmountDaily", fee == ContractMode.CPM ? showAmount : "")
                .put("realTrafficRatio", fee == ContractMode.CPT ? showRadio : 0)
                .put("refundStatus", deal == DealMode.PD ? "REFUND" : "NOT_REFUND")
                .put("refundAmountRatio", deal == DealMode.PD ? 1 : 0)
                .put("resourceRevenueUid", revenueId)
                .put("deliveryPeriods", obj)
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

    @Override
    public int createAd(NewAds ads) {
        Ad var0 = basic.getAdFlight(ads.getFlight());
        TaskResult result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_TEMPLATE).cookie(cookie)
                .param(Entity.of(Params.MAITIAN_TEMPLATE).put("uid", var0.getPackageId())));
        result.valid("获取版位模板信息失败");
        Object template = result.getEntity().get("result");
        Object mediaUnit = result.getEntity().cd("multiMediaUnits[0]").getCurrent();

        Entity creative = Entity.of(Params.MAITIAN_CREATIVE)
                .put("templateRefId", var0.getRefId())
                .put("template", template);
        for (Unit unit : var0.getUnits()) {
            if (unit.getType() == ShowType.TEXT) {
                creative.put(unit.getName(), unit.getName().equals("mediaSponsorId") ? MEDIA_ID : WordsTool.repeat(unit.getLimit()));
            }
        }
        creative.cd("multiMediaList");
        for (Unit unit : var0.getUnits()) {
            if (unit.getType() == ShowType.PICTURE) {
                creative.put("multiMediaType", 1)
                        .put("materialMd5", Settings.DEFAULT_MD5)
                        .put("materialSize", unit.getLimit())
                        .put("materialUrl", Settings.DEFAULT_URL)
                        .put("sortIndex", 0)
                        .put("backendMaterialUrl", "")
                        .put("resourceId", "")
                        .put("template", mediaUnit)
                        .add();
            }
        }
        List list = WordsTool.toList(creative.getHead());
        int itemId = createItem(String.valueOf(var0.getPackageId()), var0.getPackageName(), ads.getDealMode(), ads.getContractMode(), ads.getFlowEnum());
        int revenueId = createRevenue(itemId, ads.getBegin(), ads.getEnd());
        int reservationId = createReservation(itemId, revenueId, ads.getDealMode(), ads.getContractMode(), ads.getBegin(), ads.getEnd());
        int dealId = createDeal(ads.getDealMode(), ads.getBegin(), ads.getEnd(), ads.getCategoryEnum());
        int dealItemId = createDealItem(String.valueOf(reservationId), dealId, String.valueOf(var0.getPackageId()), revenueId, String.valueOf(ads.getShowAmount()),
                ads.getShowRadio(), ads.getDealMode(), ads.getContractMode());

        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < ads.getAmount(); i++) {
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
        return basic.approveAds(HttpExecutor.execute(tasks));
    }
}
