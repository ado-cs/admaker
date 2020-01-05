package we.lcx.admaker.service;

import org.springframework.beans.factory.annotation.Value;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Lin Chenxiao on 2020-01-04
 **/
@Service
public class BiddingModify implements Modify {

    @Value("${ad.url.maisui}")
    private String URL;


    private Map<Integer, Map<String, List<BiddingAd>>> ads = new HashMap<>();
    private ConcurrentHashMap<Integer, AtomicInteger> adVersion = new ConcurrentHashMap<>();

    private volatile boolean processing;

    public void refreshAds() {
        if (processing) return;
        synchronized (this) {
            if (processing) return;
            processing = true;
        }
        try {
            Date date = new Date();
            String begin = CommonUtil.parseDateString(CommonUtil.toDateString(date));
            date.setTime(date.getTime() + Settings.DAY);
            String end = CommonUtil.parseDateString(CommonUtil.toDateString(date));
            List<Integer> adIds = new ArrayList<>();
            List<Integer> openAdIds = new ArrayList<>();
            Map<Integer, Map<String, List<BiddingAd>>> allAds = new HashMap<>();
            HttpExecutor.doRequest(Task.post(URL + Urls.MAISUI_AD_LIST).param(Entity.of(Params.MAISUI_AD_LIST)
                    .put("beginDate", begin)
                    .put("endDate", end)))
                    .valid("获取今日麦穗广告信息失败")
                    .getEntity().cd("result/list").each(v -> {
                BiddingAd ad = buildAd(v);
                if (ad == null) {
                    Integer id = (Integer) v.get("adformId");
                    adIds.add(id);
                    openAdIds.add(id);
                }
                else {
                    ad.setActive(true);
                    allAds.computeIfAbsent(getIndex(ad.getBiddingMode()), t -> new HashMap<>()).computeIfAbsent(ad.getName(), t -> new ArrayList<>()).add(ad);
                }
            });
            HttpExecutor.doRequest(Task.post(URL + Urls.MAISUI_AD_LIST).param(Entity.of(Params.MAISUI_AD_LIST)
                    .put("beginDate", begin)
                    .put("endDate", end)
                    .put("queryStates", 2)
                    .put("adInfo", "_" + Settings.SPECIAL_NAME)))
                    .valid("获取今日麦穗广告信息失败")
                    .getEntity().cd("result/list").each(v -> {
                BiddingAd ad = buildAd(v);
                if (ad == null) adIds.add((Integer) v.get("adformId"));
                else {
                    ad.setActive(false);
                    allAds.computeIfAbsent(getIndex(ad.getBiddingMode()), t -> new HashMap<>()).computeIfAbsent(ad.getName(), t -> new ArrayList<>()).add(ad);
                }
            });
            ads = allAds;
            update(openAdIds, false);
            remove(adIds);
        } finally {
            processing = false;
        }
    }

    private BiddingAd buildAd(Entity v) {
        Integer id = (Integer) v.get("adformId");
        String name = (String) v.get("adformName");
        adVersion.put(id, new AtomicInteger((int) v.get("version")));
        if (StringUtils.isEmpty(name) || !name.contains("_" + Settings.SPECIAL_NAME)) return null;
        BiddingAd biddingAd = new BiddingAd();
        biddingAd.setId(id);
        String[] s = CommonUtil.splitName(name, "_", 2, 2);
        biddingAd.setName(s[0]);
        biddingAd.setBiddingMode(BiddingMode.valueOf(s[1]));
        return biddingAd;
    }

    private Integer getIndex(BiddingMode biddingMode) {
        return biddingMode == null ? 0 : biddingMode.getCode();
    }

    @Override
    public void update(Collection adIds, boolean flag) {
        if (CollectionUtils.isEmpty(adIds)) return;
        if (!HttpExecutor.doRequest(Task.post(URL + (flag ? Urls.MAISUI_OPEN : Urls.MAISUI_PAUSE))
                .param(composeEntity(adIds)))
                .logError("开启/关闭麦穗广告失败")) {
            for (Object id : adIds) {
                adVersion.computeIfAbsent((Integer) id, t -> new AtomicInteger()).decrementAndGet();
            }
        }
    }

    @Override
    public void remove(Collection adIds) {
        if (CollectionUtils.isEmpty(adIds)) return;
        if (HttpExecutor.doRequest(Task.post(URL + Urls.MAISUI_DELETE)
                .param(composeEntity(adIds)))
                .logError("删除麦穗广告失败")) {
            for (Object id : adIds) {
                adVersion.remove(id);
            }
        }
    }

    private Entity composeEntity(Collection adIds) {
        Entity entity = Entity.of(Params.MAISUI_UPDATE).newList("adFormUpdateList");
        for (Object id : adIds) {
            entity.put("adformId", id)
                    .put("version", adVersion.computeIfAbsent((Integer) id, t -> new AtomicInteger()).getAndIncrement())
                    .add();
        }
        return entity;
    }

    @Override
    public Map<Integer, Map<String, List<BiddingAd>>> getAds() {
        return ads;
    }

    @Override
    public List<BiddingAd> getAds(String flightName, DealMode deal, Integer fee) {
        Map<String, List<BiddingAd>> map = ads.get(getIndex(BiddingMode.of(fee)));
        return map == null ? null : map.get(flightName);
    }
}
