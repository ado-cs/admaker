package we.lcx.admaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import we.lcx.admaker.common.*;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.entities.*;
import we.lcx.admaker.common.enums.*;
import we.lcx.admaker.service.aop.Trace;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.CommonUtil;
import javax.annotation.Resource;
import java.util.*;

/**
 * Created by LinChenxiao on 2019/12/23 17:26
 **/
@Slf4j
@Service
public class ContractService {

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

    @Value("${ad.maitian.contractId}")
    private Long CONTRACT_ID; //合同id

    @Value("${ad.maitian.firstIndustry}")
    private Integer IND1; //第一行业id

    @Value("${ad.maitian.secondIndustry}")
    private Integer IND2; //第二行业id

    @Value("${ad.maitian.brand}")
    private Long BRAND; //品牌id
    
    @Resource
    private BasicService basicService;
    
    @Trace
    public int createResource(NewAds ads, ContractTag tag) {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_RESOURCE)
                        .cookie(basicService.getCookie()).param(Entity.of(Params.MAITIAN_RESOURCE)
                        .put("resourceName", tag.getAd().getFlightName() + Settings.SUFFIX_VERSION)))
                .valid("获取资源id失败");
        if ((int) result.getEntity().get("result total") > 0)
            return (int) result.getEntity().get("result list uid");
        return (int) HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_RESOURCE_NEW)
                        .cookie(basicService.getCookie()).param(Entity.of(Params.MAITIAN_RESOURCE_NEW)
                        .put("resourceName", tag.getAd().getFlightName() + Settings.SUFFIX_VERSION)))
                .valid("创建资源失败").getEntity().get("result");
    }

    @Trace
    public int createItem(NewAds ads, ContractTag tag) {
        final String name = ads.getDealMode().name() + "_" + ads.getContractMode().name()
                + (ads.getContractMode() == ContractMode.CPT ? "_" + ads.getFlowEnum().getCode() : "");

        HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_ITEM_LIST)
                        .cookie(basicService.getCookie()).param(Entity.of().put("uid", tag.getResourceId())))
                .valid("获取资源条目失败").getEntity().cd("result/items").each(e -> {
            if (name.equals(e.get("itemName"))) {
                tag.setResourceItemId((int) e.get("uid"));
                tag.setRevenueId((int) e.get("resourceRevenueVOList uid"));
                return true;
            }
            return false;
        });
        if (tag.getResourceItemId() != 0) return tag.getResourceItemId();

        return (int) HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_ITEM)
                        .cookie(basicService.getCookie()).param(Entity.of(Params.MAITIAN_ITEM)
                        .put("resourceUid", tag.getResourceId())
                        .put("mediaUid", MEDIA_ID)
                        .put("itemName", name)
                        .put("resourceTrafficType", ads.getDealMode().name())
                        .put("billingMode", "BILLING_MODE_" + ads.getContractMode().getValue())
                        .put("resourceTrafficRatio", ads.getContractMode() == ContractMode.CPM ? "ZERO" : ads.getFlowEnum().getValue())
                        .put("trafficSplitTypes", CommonUtil.toList(ContractMode.CPT.getTraffic(), ContractMode.CPM.getTraffic()))
                        .put("positionIdList", CommonUtil.toList(String.valueOf(tag.getAd().getPositionId())))
                        .cd("positionList[0]")
                        .put("code", String.valueOf(tag.getAd().getPositionId()))
                        .put("value", tag.getAd().getPositionName())))
                .valid("创建资源条目失败")
                .getEntity().get("result");
    }

    @Trace
    public int createRevenue(NewAds ads, ContractTag tag) {
        if (tag.getRevenueId() != 0) return tag.getRevenueId();
        Date date = new Date();
        return (int) HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_REVENUE)
                        .cookie(basicService.getCookie()).param(Entity.of(Params.MAITIAN_REVENUE)
                        .put("resourceUid", tag.getResourceId())
                        .put("resourceItemUid", tag.getResourceItemId())
                        .put("revenueName", CommonUtil.generateId().substring(0, 8))
                        .cd("durationVO")
                        .put("beginTime", date.getTime())))
                .valid("创建资源条目失败")
                .getEntity().get("result");
    }

    @Trace(value = "reservation", loop = true)
    public int createReservation(NewAds ads, ContractTag tag) {
        Entity entity = Entity.of(Params.MAITIAN_RESERVE)
                .put("customerUid", CUSTOMER_ID)
                .put("resourceOwnerUid", MEDIA_ID)
                .put("resourceUid", tag.getResourceId())
                .put("resourceItemUid", tag.getResourceItemId())
                .put("billingModeCode", ads.getContractMode().getCode())
                .put("revenueUid", tag.getRevenueId())
                .cd("trafficType")
                .put("code", String.valueOf(ads.getDealMode().getCode()))
                .put("value", ads.getDealMode().getValue())
                .cd("/resourceItem")
                .put("uid", tag.getResourceItemId())
                .cd("/mergeDurations");
        long m = CommonUtil.parseTime(ads.getBegin());
        long n = CommonUtil.parseTime(ads.getEnd());
        if (m > n) throw new VisibleException("结束时间必须在开始时间之后");
        while (m <= n) {
            entity.put("beginTime", m);
            m += Settings.DAY;
            entity.put("endTime", m - 1).add();
        }
        return (int) HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_RESERVE)
                        .cookie(basicService.getCookie()).param(entity))
                .valid("资源预定失败，请检查当日是否排期已满")
                .getEntity().get("result");
    }

    @Trace
    public int createDeal(NewAds ads, ContractTag tag) {
        final String name = ads.getDealMode().name() + ads.getCategoryEnum().getValue();
        HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_DEAL_LIST)
                        .cookie(basicService.getCookie()).param(Entity.of(Params.COMMON_PAGE).put("scheduleName", name)))
                .valid("获取排期失败").getEntity().cd("result/list").each(e -> {
            if (CONTRACT_ID.equals(Long.parseLong(String.valueOf(e.get("contractUid"))))) {
                tag.setDealId((int) e.get("uid"));
                return true;
            }
            return false;
        });
        if (tag.getDealId() != 0) return tag.getDealId();
        Date date = new Date();
        return (int) HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_DEAL)
                        .cookie(basicService.getCookie()).param(Entity.of(Params.MAITIAN_DEAL)
                        .put("name", name)
                        .put("scheduleTrafficType", ads.getDealMode().name())
                        .put("beginTime", date.getTime())
                        .put("contractUid", CONTRACT_ID)
                        .put("firstIndustryUid", IND1)
                        .put("secondIndustryUid", IND2)
                        .put("brandUid", BRAND)
                        .put("productName", CommonUtil.randomSuffix(6))
                        .put("scheduleCategoryCode", String.valueOf(ads.getCategory()))))
                .valid("创建排期失败")
                .getEntity().get("result uid");
    }

    @Trace(value = "dealItem", loop = true)
    public int createDealItem(NewAds ads, ContractTag tag) {
        TaskResult result = HttpExecutor.doRequest(
                Task.post(URL + URLs.MAITIAN_QUERY)
                        .cookie(basicService.getCookie()).param(Entity.of().put("uid", String.valueOf(tag.getReservationId()))));
        result.valid("获取预定信息失败");
        Object obj = result.getEntity().get("result reserveDatingVOs");
        Entity entity = Entity.of(Params.MAITIAN_DEAL_ITEM)
                .put("name", tag.getAd().getFlightName() + CommonUtil.randomSuffix(6))
                .put("scheduleUid", tag.getDealId())
                .put("reserveItemUid", String.valueOf(tag.getReservationId()))
                .put("positionUids", CommonUtil.toList(String.valueOf(tag.getAd().getPositionId())))
                .put("trafficSplitType", ads.getContractMode().getTraffic())
                .put("realAmountDaily", ads.getContractMode() == ContractMode.CPM ? String.valueOf(ads.getShowNumber()) : "")
                .put("realTrafficRatio", ads.getContractMode() == ContractMode.CPT ? ads.getShowRadio() : 0)
                .put("refundStatus", ads.getDealMode() == DealMode.PD ? "REFUND" : "NOT_REFUND")
                .put("refundAmountRatio", ads.getDealMode() == DealMode.PD ? 1 : 0)
                .put("resourceRevenueUid", tag.getRevenueId())
                .put("deliveryPeriods", obj)
                .put("dspUid", ads.getDspId())
                .put("costType", ads.getDealMode() == DealMode.PD ? "OFFLINE_SETTLE" : "FREE_TEST")
                .cd("deliveryPeriods").each(v -> {
                    v.put("serveBeginTime", v.get("beginTime"));
                    v.put("serveEndTime", v.get("endTime"));
                });
        return (int) HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_DEAL_ITEM)
                .cookie(basicService.getCookie()).param(entity))
                .valid("创建排期条目失败")
                .getEntity().get("result");
    }

    @Trace
    public List buildCreative(NewAds ads, ContractTag tag) {
        Ad ad = tag.getAd();
        TaskResult result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_TEMPLATE).cookie(basicService.getCookie())
                .param(Entity.of().put("uid", ad.getPositionId())));
        result.valid("获取版位模板信息失败");
        Object template = result.getEntity().get("result");
        List mediaUnits = (List) result.getEntity().cd("result/multiMediaUnits").getCurrent();
        Entity creative = Entity.of(Params.MAITIAN_CREATIVE)
                .put("templateRefId", ad.getRefId())
                .put("template", template);
        for (Unit unit : ad.getUnits()) {
            if (unit.getType() == ShowType.TEXT) {
                creative.put(unit.getName(), unit.getName().equals("mediaSponsorId") ? String.valueOf(MEDIA_ID) : CommonUtil.repeat(unit.getLimit()));
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
        return CommonUtil.toList(creative.getHead());
    }

    public void deleteReservation(int id) {
        TaskResult result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_RESERVATION_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", id).put("version", 0)));
        if (!result.isSuccess()) {
            result.error();
            log.error("删除资源预定失败，预定id = {}", id);
        }
    }

    public void modify(ModifyAd modifyAd) {
        Map<Integer, DealItem> items = new HashMap<>();

        for (Integer id : new HashSet<>(modifyAd.getIds())) {
            TaskResult result;
            DealItem item = items.get(id);
            if (item == null) {
                result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_PAGE).cookie(basicService.getCookie()).param(Entity.of(Params.COMMON_PAGE)
                        .put("scheduleItemUid", id)));
                if (!result.isSuccess()) {
                    result.error();
                    log.error("获取排期条目所在排期ID失败，itemId = {}", id);
                    continue;
                }
                Integer dealId = (Integer) result.getEntity().get("result list uid");
                result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_DETAIL).cookie(basicService.getCookie()).param(Entity.of()
                        .put("uid", String.valueOf(dealId))));
                if (!result.isSuccess()) {
                    result.error();
                    log.error("获取排期下所有排期条目详情失败，itemId = {}", id);
                    continue;
                }
                result.getEntity().cd("result/items").each(v -> {
                    DealItem t = new DealItem();
                    t.setReservationId((Integer) v.get("reserveItemUid"));
                    t.setVersion((Integer) v.get("version"));
                    t.setStatus("ON".equals(v.get("trafficSwitch code")));
                    items.put((Integer) v.get("uid"), t);
                });
                item = items.get(id);
            }
            if (item == null) {
                log.error("获取排期条目关联预约ID失败，itemId = {}", id);
                continue;
            }

            if ((modifyAd.getState() > 0) ^ item.getStatus()) {
                result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_ITEM_CLOSE).cookie(basicService.getCookie()).param(Entity.of()
                        .put("uid", id).put("version", item.getVersion()).put("trafficSwitch", item.getStatus() ? "CLOSE" : "ON")));
                if (!result.isSuccess()) {
                    result.error();
                    log.error("开启/关闭排期条目流量失败，itemId = {}", id);
                    continue;
                }
                item.setVersion(item.getVersion() + 1);
            }

            result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_AD_LIST).cookie(basicService.getCookie()).param(Entity.of(Params.MAITIAN_AD_LIST)
                    .put("scheduleItemId", id)));
            if (!result.isSuccess()) {
                result.error();
                log.error("获取排期条目下广告位失败，itemId = {}", id);
                continue;
            }
            result.getEntity().cd("result/list").each(v -> {
                Integer adId = (Integer) v.get("id");
                Integer version = (Integer) v.get("version");
                if ("1001".equals(v.get("activeStatus code")) ^ (modifyAd.getState() > 0)) {
                    TaskResult r = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_AD_CLOSE).cookie(basicService.getCookie()).param(Entity.of()
                            .put("id", adId).put("version", version).put("status", modifyAd.getState() > 0 ? "1001" : "411")));
                    if (!r.isSuccess()) log.error("关闭广告失败，itemId = {}, adId = {}", id, adId);
                    else version++;
                }
                if (modifyAd.getState() < 0) {
                    TaskResult r = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_AD_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                            .put("uid", adId).put("version", version)));
                    if (!r.isSuccess()) {
                        r.error();
                        log.error("删除广告失败，itemId = {}, adId = {}", id, adId);
                    }
                }
            });

            if (modifyAd.getState() < 0) {
                result = HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_ITEM_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                        .put("uid", id).put("version", item.getVersion())));
                if (!result.isSuccess()) {
                    result.error();
                    log.error("删除排期条目失败，itemId = {}", id);
                    continue;
                }
                deleteReservation(item.getReservationId());
            }
        }
    }
}
