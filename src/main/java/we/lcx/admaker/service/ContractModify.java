package we.lcx.admaker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.Urls;
import we.lcx.admaker.common.entities.ContractAd;
import we.lcx.admaker.common.enums.ContractMode;
import we.lcx.admaker.common.enums.DealMode;
import we.lcx.admaker.utils.CommonUtil;
import we.lcx.admaker.utils.HttpExecutor;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by LinChenxiao on 2020/01/03 16:20
 **/
@Service
public class ContractModify {

    @Resource
    private BasicService basicService;

    @Value("${ad.url.maitian}")
    private String URL;

    private Map<Integer, Map<String, List<ContractAd>>> ads = new HashMap<>();
    private ConcurrentHashMap<Integer, AtomicInteger> dealItemVersion = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Integer> dealItemMapReservationId = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, AtomicInteger> adVersion = new ConcurrentHashMap<>();

    private volatile boolean processing;

    public void refreshAds() {
        if (processing) return;
        synchronized (this) {
            if (processing) return;
            processing = true;
        }
        try {
            long time = CommonUtil.timeOfToday();
            Map<Integer, Map<String, List<ContractAd>>> allAds = new HashMap<>();
            Set<Integer> items = new HashSet<>();
            List<Integer> adIds = new ArrayList<>();
            HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_AD_LIST).cookie(basicService.getCookie()).param(Entity.of(Params.MAITIAN_AD_LIST)
                    .cd("execPeriod")
                    .put("startTime", time)
                    .put("endTime", time + Settings.DAY - 1000)))
                    .valid("获取今日麦田广告信息失败")
                    .getEntity().each(v -> {
                String flag = String.valueOf(v.get("activeStatus code"));
                boolean open = "1001".equals(flag);
                if (!open && !"411".equals(flag)) return;
                int id = (int) v.get("id");
                String name = (String) v.get("name");
                if (open && (StringUtils.isEmpty(name) || !name.contains("_" + Settings.SPECIAL_NAME))) {
                    items.add(Integer.valueOf(String.valueOf(v.get("scheduleItemId"))));
                    adIds.add(id);
                } else {
                    ContractAd contractAd = new ContractAd();
                    contractAd.setId(id);
                    contractAd.setActive(open);
                    String[] s = CommonUtil.splitName(name, "_", 3, 2);
                    contractAd.setName(s[0]);
                    contractAd.setDealItemId(String.valueOf(v.get("scheduleItemId")));
                    contractAd.setDealMode(DealMode.valueOf(s[1]));
                    contractAd.setVersion(new AtomicInteger((int) v.get("version")));
                    contractAd.setDealId((Integer) v.get("scheduleInfo id"));
                    contractAd.setContractMode(ContractMode.valueOf(s[2]));
                    Integer idx = getIndex(contractAd.getDealMode(), contractAd.getContractMode());
                    allAds.computeIfAbsent(idx, t -> new HashMap<>()).computeIfAbsent(s[0], t -> new ArrayList<>()).add(contractAd);
                    adVersion.put(id, contractAd.getVersion());
                }
            });
            ads = allAds;
            updateItems(items, false);
            updateAds(adIds, false);
        } finally {
            processing = false;
        }
    }

    private Integer getIndex(DealMode deal, ContractMode fee) {
        return DealMode.PDB == deal ? fee.getCode() : Math.abs(DealMode.PD.getCode() - deal.getCode()) * 4;
    }

    public void fillDealItemInfo(Integer dealId) {
        HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_DETAIL).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", String.valueOf(dealId))))
                .valid("获取排期条目详情失败").getEntity()
                .cd("result/items").each(v -> {
            Integer id = (Integer) v.get("uid");
            dealItemVersion.put(id, new AtomicInteger((int) v.get("version")));
            dealItemMapReservationId.put(id, (Integer) v.get("reserveItemUid"));
        });
    }

    public void updateItems(Collection<Integer> items, boolean flag) {
        for (Integer id : items) {
            AtomicInteger version = dealItemVersion.computeIfAbsent(id, t -> new AtomicInteger());
            if (HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_ITEM_CLOSE).cookie(basicService.getCookie()).param(Entity.of()
                    .put("uid", id).put("version", version.get()).put("trafficSwitch", flag ? "ON" : "CLOSE")))
                    .logError("开启/关闭排期条目流量失败，itemId = {}", id))
                version.incrementAndGet();
        }
    }

    public void updateAds(Collection<Integer> adIds, boolean flag) {
        for (Integer id : adIds) {
            AtomicInteger version = adVersion.computeIfAbsent(id, t -> new AtomicInteger());
            if (HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_AD_CLOSE).cookie(basicService.getCookie()).param(Entity.of()
                    .put("id", id).put("version", version.get()).put("status", flag ? "1001" : "411")))
                    .logError("开启/关闭麦田广告失败，adId = {}", id))
                version.incrementAndGet();
        }
    }

    public void removeReservations(Collection<Integer> ids) {
        for (Integer id : ids) {
            HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_RESERVATION_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                    .put("uid", id).put("version", 0)))
                    .logError("删除资源预定失败，预定id = {}", id);
        }
    }

    public void removeReservationsByDealItems(Collection<Integer> ids) {
        for (Integer itemId : ids) {
            Integer id = dealItemMapReservationId.get(itemId);
            HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_RESERVATION_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                    .put("uid", id).put("version", 0)))
                    .logError("删除资源预定失败，预定id = {}", id);
        }
    }

    public void removeItems(Collection<Integer> items) {
        for (Integer id : items) {
            AtomicInteger version = dealItemVersion.computeIfAbsent(id, t -> new AtomicInteger());
            if (HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_ITEM_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                    .put("uid", id).put("version", version.get())))
                    .logError("删除排期条目失败，itemId = {}", id))
                dealItemVersion.remove(id);
        }
    }

    public void removeAds(Collection<Integer> adIds) {
        for (Integer id : adIds) {
            AtomicInteger version = adVersion.computeIfAbsent(id, t -> new AtomicInteger());
            if (HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_AD_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                    .put("uid", id).put("version", version.get())))
                    .logError("删除麦田广告失败，adId = {}", id))
                adVersion.remove(id);
        }
    }

    public Map<Integer, Map<String, List<ContractAd>>> getAds() {
        return ads;
    }

    public List<ContractAd> getAds(String flightName, DealMode deal, ContractMode fee) {
        Map<String, List<ContractAd>> map = ads.get(getIndex(deal, fee));
        return map == null ? null : map.get(flightName);
    }
}
