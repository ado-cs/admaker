package we.lcx.admaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.Urls;
import we.lcx.admaker.common.entities.BiddingAd;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.common.enums.DealMode;
import we.lcx.admaker.utils.CommonUtil;
import we.lcx.admaker.utils.HttpExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Lin Chenxiao on 2020-01-04
 **/
@Slf4j
@Service
public class BiddingModify implements Modify {

    @Resource
    private BasicService basicService;

    @Value("${ad.url.maisui}")
    private String URL;


    private Map<Integer, BiddingAd> ads = new HashMap<>();
    private static final String PLAN_ID = "443817";

    private volatile boolean processing;

    @PostConstruct
    private void init() {
        refreshAds();
        //clean();
    }

    public void refreshAds() {
        if (processing) return;
        synchronized (this) {
            if (processing) return;
            processing = true;
        }
        try {
            Map<Integer, BiddingAd> allAds = new HashMap<>();
            int offset = 0;
            int total;
            do {
                Entity entity = HttpExecutor.doRequest(Task.post(URL + Urls.MAISUI_AD_LIST).param(fillDate(Params.MAISUI_AD_LIST)
                        .put("adPlanId", PLAN_ID)
                        .put("offset", offset)))
                        .valid("获取麦穗广告信息失败")
                        .getEntity();
                Integer page = (Integer) entity.get("result totalPager");
                if (page == null) {
                    log.error("获取麦穗计划返回非法内容");
                    break;
                }
                else total = page;
                entity.cd("result/list").each(v -> {
                    BiddingAd biddingAd = new BiddingAd();
                    biddingAd.setVersion((Integer) v.get("version"));
                    biddingAd.setId((Integer) v.get("adformId"));
                    biddingAd.setPositionId((Integer) v.get("campaignPackageId"));
                    biddingAd.setName(basicService.getFlightNameByPositionId(biddingAd.getPositionId()));
                    biddingAd.setBiddingMode(BiddingMode.valueOf((String) v.get("billingMode value")));
                    biddingAd.setStatus("1001".equals(String.valueOf(v.get("configuredStatus code"))));
                    allAds.put(biddingAd.getId(), biddingAd);
                });
                offset += 1;
            }
            while (offset < total);
            ads = allAds;
        } finally {
            processing = false;
        }
    }

    @Async
    @Override
    public void clean() {
        List<BiddingAd> plans = new ArrayList<>();
        int offset = 0;
        int total;
        do {
            Entity entity = HttpExecutor.doRequest(Task.post(URL + Urls.MAISUI_PLAN).param(fillDate(Params.MAISUI_PLAN)
                    .put("offset", offset)))
                    .valid("获取麦穗计划失败")
                    .getEntity();
            Integer page = (Integer) entity.get("result totalPager");
            if (page == null) {
                log.error("获取麦穗计划返回非法内容");
                break;
            }
            else total = page;
            entity.cd("result/list").each(v -> {
                Integer id = (Integer) v.get("adPlanId");
                if (!PLAN_ID.equals(String.valueOf(id))) {
                    BiddingAd ad = new BiddingAd();
                    ad.setId(id);
                    ad.setVersion((Integer) v.get("version"));
                    ad.setStatus("1011".equals(String.valueOf(v.get("configuredStatus code"))));
                    plans.add(ad);
                }
            });
            offset += 1;
        }
        while (offset < total);
        for (BiddingAd plan : plans) {
            if (plan.getStatus()) {
                if (HttpExecutor.doRequest(Task.post(URL + Urls.MAISUI_PLAN_PAUSE).param(Entity.of(Params.MAISUI_PLAN_MODIFY)
                        .put("adPlanId", plan.getId()).put("version", plan.getVersion())))
                        .logError("关闭计划失败，planId = {}", plan.getId()))
                    plan.setVersion(plan.getVersion() + 1);
            }
            HttpExecutor.doRequest(Task.post(URL + Urls.MAISUI_PLAN_DELETE).param(Entity.of(Params.MAISUI_PLAN_MODIFY)
                    .put("adPlanId", plan.getId()).put("version", plan.getVersion())))
                    .logError("删除计划失败，planId = {}", plan.getId());
        }
        log.info("麦穗后台清理工作完成");
    }

    private Entity fillDate(String param) {
        Date date = new Date();
        String begin = CommonUtil.parseDateString(CommonUtil.toDateString(date));
        date.setTime(date.getTime() + Settings.DAY);
        String end = CommonUtil.parseDateString(CommonUtil.toDateString(date));
        return Entity.of(param).put("beginDate", begin).put("endDate", end);
    }

    @Override
    public boolean update(Collection<Integer> adIds, boolean flag) {
        if (CollectionUtils.isEmpty(adIds)) return true;
        Set<BiddingAd> set = new HashSet<>();
        for (Integer id : adIds) {
            BiddingAd biddingAd = ads.get(id);
            if (biddingAd != null && biddingAd.getStatus() != flag) set.add(biddingAd);
        }
        if (HttpExecutor.doRequest(Task.post(URL + (flag ? Urls.MAISUI_OPEN : Urls.MAISUI_PAUSE))
                .param(composeEntity(set)))
                .logError("开启/关闭麦穗广告失败")) {
            for (BiddingAd ad : set) {
                ad.setStatus(flag);
                ad.setVersion(ad.getVersion() + 1);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Collection<Integer> adIds) {
        if (CollectionUtils.isEmpty(adIds)) return true;
        Set<BiddingAd> set = new HashSet<>();
        for (Integer id : adIds) {
            BiddingAd biddingAd = ads.get(id);
            if (biddingAd != null) set.add(biddingAd);
        }
        if (HttpExecutor.doRequest(Task.post(URL + Urls.MAISUI_DELETE)
                .param(composeEntity(set)))
                .logError("删除麦穗广告失败")) {
            for (BiddingAd ad : set) {
                ads.remove(ad.getId());
            }
            return true;
        }
        return false;
    }

    private Entity composeEntity(Collection<BiddingAd> list) {
        Entity entity = Entity.of(Params.MAISUI_UPDATE).newList("adFormUpdateList");
        for (BiddingAd ad : list) {
            entity.put("adformId", ad.getId())
                    .put("version", ad.getVersion())
                    .add();
        }
        return entity;
    }

    @Override
    public Collection<BiddingAd> getAds() {
        return ads.values();
    }
}
