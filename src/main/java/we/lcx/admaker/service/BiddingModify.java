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
import we.lcx.admaker.utils.CommonUtil;
import we.lcx.admaker.utils.HttpExecutor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Lin Chenxiao on 2020-01-04
 **/
@Service
public class BiddingModify {

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
            Map<Integer, Map<String, List<BiddingAd>>> allAds = new HashMap<>();
            HttpExecutor.doRequest(Task.post(URL + Urls.MAISUI_AD_LIST).param(Entity.of(Params.MAISUI_AD_LIST)
                    .put("beginDate", begin)
                    .put("endDate", end)))
                    .valid("获取今日麦穗广告信息失败")
                    .getEntity().each(v -> {
                String flag = String.valueOf(v.get("configuredStatus code")); //todo 待验证是否投放过期所在 / compositeStatus
                boolean open = "1001".equals(flag);
                if (!open && !"411".equals(flag)) return;
                int id = (int) v.get("adformId");
                String name = (String) v.get("adformName");
                if (open && (StringUtils.isEmpty(name) || !name.contains("_" + Settings.SPECIAL_NAME)))
                    adIds.add(id);
                else {
                    BiddingAd biddingAd = new BiddingAd();
                    biddingAd.setId(id);
                    biddingAd.setVersion(new AtomicInteger((int) v.get("version")));
                    biddingAd.setActive(open);
                    String[] s = CommonUtil.splitName(name, "_", 2, 2);
                    biddingAd.setName(s[0]);
                    biddingAd.setBiddingMode(BiddingMode.valueOf(s[1]));
                    Integer idx = getIndex(biddingAd.getBiddingMode());
                    allAds.computeIfAbsent(idx, t -> new HashMap<>()).computeIfAbsent(s[0], t -> new ArrayList<>()).add(biddingAd);
                    adVersion.put(id, biddingAd.getVersion());
                }
            });
            ads = allAds;
            updateAds(adIds, false);
        } finally {
            processing = false;
        }
    }

    private Integer getIndex(BiddingMode biddingMode) {
        return biddingMode.getCode();
    }

    public void updateAds(Collection<Integer> adIds, boolean flag) {
        if (CollectionUtils.isEmpty(adIds)) return;
        if (!HttpExecutor.doRequest(Task.post(URL + (flag ? Urls.MAISUI_OPEN : Urls.MAISUI_PAUSE))
                .param(composeEntity(adIds)))
                .logError("开启/关闭麦穗广告失败")) {
            //回退版本 todo 验证 麦田和麦穗 失败时版本号是否增加
            for (Integer id : adIds) {
                adVersion.computeIfAbsent(id, t -> new AtomicInteger()).decrementAndGet();
            }
        }
    }

    public void removeAds(Collection<Integer> adIds) {
        if (CollectionUtils.isEmpty(adIds)) return;
        if (HttpExecutor.doRequest(Task.post(URL + Urls.MAISUI_DELETE)
                .param(composeEntity(adIds)))
                .logError("删除麦穗广告失败")) {
            for (Integer id : adIds) {
                adVersion.remove(id);
            }
        }
    }

    private Entity composeEntity(Collection<Integer> adIds) {
        Entity entity = Entity.of(Params.MAISUI_UPDATE).newList("adFormUpdateList");
        for (Integer id : adIds) {
            entity.put("adformId", id)
                    .put("version", adVersion.computeIfAbsent(id, t -> new AtomicInteger()).getAndIncrement())
                    .add();
        }
        return entity;
    }

    public Map<Integer, Map<String, List<BiddingAd>>> getAds() {
        return ads;
    }

    public List<BiddingAd> getAds(String flightName, BiddingMode fee) {
        Map<String, List<BiddingAd>> map = ads.get(getIndex(fee));
        return map == null ? null : map.get(flightName);
    }
}
