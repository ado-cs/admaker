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
import we.lcx.admaker.common.entities.Pair;
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
public class ContractModify implements Modify {

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
            List<Integer> openAdIds = new ArrayList<>();
            Set<Integer> dealIds = new HashSet<>();
            HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_AD_LIST).cookie(basicService.getCookie()).param(Entity.of(Params.MAITIAN_AD_LIST)
                    .cd("execPeriod")
                    .put("startTime", time)
                    .put("endTime", time + Settings.DAY - 1000)))
                    .valid("获取今日麦田广告信息失败")
                    .getEntity().cd("result/list").each(v -> {
                String flag = String.valueOf(v.get("execStatus code"));
                boolean open = "1001".equals(flag);
                int id = (int) v.get("id");
                adVersion.put(id, new AtomicInteger((int) v.get("version")));
                String name = (String) v.get("name");
                Integer dealId = Integer.valueOf(String.valueOf(v.get("scheduleInfo id")));
                dealIds.add(dealId);
                if (!StringUtils.isEmpty(name) && name.contains("_" + Settings.SPECIAL_NAME) && (open || "411".equals(flag))) {
                    ContractAd contractAd = new ContractAd();
                    contractAd.setId(id);
                    contractAd.setActive(open);
                    String[] s = CommonUtil.splitName(name, "_", 3, 2);
                    contractAd.setName(s[0]);
                    contractAd.setDealItemId(String.valueOf(v.get("scheduleItemId")));
                    contractAd.setDealMode(DealMode.valueOf(s[1]));
                    contractAd.setDealId(dealId);
                    contractAd.setContractMode(ContractMode.valueOf(s[2]));
                    Integer idx = getIndex(contractAd.getDealMode(), contractAd.getContractMode());
                    allAds.computeIfAbsent(idx, t -> new HashMap<>()).computeIfAbsent(s[0], t -> new ArrayList<>()).add(contractAd);
                }
                else {
                    Integer itemId = Integer.valueOf(String.valueOf(v.get("scheduleItemId")));
                    items.add(itemId);
                    adIds.add(id);
                    if (open) openAdIds.add(id);
                }
            });
            ads = allAds;
            for (Integer id : dealIds) fillDealItemInfo(id);
            updateAds(openAdIds, false);
            updateItems(items, false);
            removeAds(adIds);
            removeItems(items);
            removeReservationsByDealItems(items);
        } finally {
            processing = false;
        }
    }

    private Integer getIndex(DealMode deal, ContractMode fee) {
        return DealMode.PDB == deal ? fee.getCode() : Math.abs(DealMode.PD.getCode() - deal.getCode()) * 4;
    }

    private void fillDealItemInfo(Integer dealId) {
        if (dealId == null) return;
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
            updateItem(id, flag);
        }
    }

    private void updateItem(Integer id, boolean flag) {
        AtomicInteger version = dealItemVersion.computeIfAbsent(id, t -> new AtomicInteger());
        if (HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_ITEM_CLOSE).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", id).put("version", version.get()).put("trafficSwitch", flag ? "ON" : "CLOSE")))
                .logError("开启/关闭排期条目流量失败，itemId = {}", id)) {
            version.incrementAndGet();
        }
    }

    public void updateAds(Collection<Integer> adIds, boolean flag) {
        for (Integer id : adIds) {
            updateAd(id, flag);
        }
    }

    private void updateAd(Integer id, boolean flag) {
        AtomicInteger version = adVersion.computeIfAbsent(id, t -> new AtomicInteger());
        if (HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_AD_CLOSE).cookie(basicService.getCookie()).param(Entity.of()
                .put("id", id).put("version", version.get()).put("status", flag ? "1001" : "411")))
                .logError("开启/关闭麦田广告失败，adId = {}", id))
            version.incrementAndGet();
    }

    @Override
    public void update(Collection pairs, boolean flag) {
        for (Object pair : pairs) {
            updateAd((Integer) ((Pair) pair).getValue(), flag);
            updateItem((Integer) ((Pair) pair).getKey(), flag);
        }
    }

    public void removeReservations(Collection<Integer> ids) {
        for (Integer id : ids) {
            if (id == null) continue;
            HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_RESERVATION_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                    .put("uid", id).put("version", 0)))
                    .logError("删除资源预定失败，预定id = {}", id);
        }
    }

    private void removeReservationsByDealItems(Collection<Integer> ids) {
        for (Integer itemId : ids) {
            removeReservationByDealItem(itemId);
        }
    }

    private void removeReservationByDealItem(Integer itemId) {
        Integer id = dealItemMapReservationId.get(itemId);
        HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_RESERVATION_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", id).put("version", 0)))
                .logError("删除资源预定失败，预定id = {}", id);
    }

    public void removeItems(Collection<Integer> items) {
        for (Integer id : items) {
            removeItem(id);
        }
    }

    private void removeItem(Integer id) {
        int version = dealItemVersion.computeIfAbsent(id, t -> new AtomicInteger()).get();
        if (HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_ITEM_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", id).put("version", version))).isSuccess())
            dealItemVersion.remove(id);
        else if (HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_ITEM_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", id).put("version", version - 1)))
                .logError("删除排期条目失败，itemId = {}", id))
            dealItemVersion.remove(id);
    }

    public void removeAds(Collection<Integer> adIds) {
        for (Integer id : adIds) {
            removeAd(id);
        }
    }

    private void removeAd(Integer id) {
        AtomicInteger version = adVersion.computeIfAbsent(id, t -> new AtomicInteger());
        if (HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_AD_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", id).put("version", version.get())))
                .logError("删除麦田广告失败，adId = {}", id))
            adVersion.remove(id);
    }

    @Override
    public void remove(Collection pairs) {
        for (Object pair : pairs) {
            removeAd((Integer) ((Pair) pair).getValue());
            removeItem((Integer) ((Pair) pair).getKey());
            removeReservationByDealItem((Integer) ((Pair) pair).getKey());
        }
    }

    @Override
    public Map<Integer, Map<String, List<ContractAd>>> getAds() {
        return ads;
    }

    @Override
    public List<ContractAd> getAds(String flightName, DealMode deal, Integer fee) {
        Map<String, List<ContractAd>> map = ads.get(getIndex(deal, ContractMode.of(fee)));
        return map == null ? null : map.get(flightName);
    }
}
