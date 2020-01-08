package we.lcx.admaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Urls;
import we.lcx.admaker.common.entities.ContractAd;
import we.lcx.admaker.common.entities.Pair;
import we.lcx.admaker.common.enums.DealMode;
import we.lcx.admaker.common.json.DealItem;
import we.lcx.admaker.utils.HttpExecutor;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * Created by LinChenxiao on 2020/01/03 16:20
 **/
@Slf4j
@Service
public class ContractModify implements Modify {

    @Resource
    private BasicService basicService;

    @Value("${ad.url.maitian}")
    private String URL;

    @Value("${ad.common.dspId}")
    private Integer DSP_ID;

    private Map<Integer, ContractAd> ads = new HashMap<>();

    private volatile boolean processing;

    @PostConstruct
    private void init() {
        refreshAds();
        //clean();
    }

    @Override
    public void refreshAds() {
        if (processing) return;
        synchronized (this) {
            if (processing) return;
            processing = true;
        }
        try {
            Map<Integer, ContractAd> allAds = new HashMap<>();
            for (Map.Entry<DealMode, Integer> entry : basicService.getDeals().entrySet()) {
                for (DealItem item : getDealItemInfo(entry.getValue())) {
                    ContractAd contractAd = new ContractAd();
                    contractAd.setPositionId(item.getPositionId());
                    contractAd.setDealItemId(item.getId());
                    contractAd.setDspId(item.getDspId());
                    contractAd.setDealId(entry.getValue());
                    contractAd.setName(basicService.getFlightNameByPositionId(item.getPositionId()));
                    contractAd.setStatus(item.getStatus());
                    contractAd.setVersion(item.getVersion());
                    contractAd.setReservationId(item.getReservationId());
                    contractAd.setContractMode(item.getContractMode());
                    contractAd.setDealMode(entry.getKey());
                    allAds.put(item.getId(), contractAd);
                }
            }
            ads = allAds;
        } finally {
            processing = false;
        }
    }

    @Async
    @Override
    public void clean() {
        Collection<Integer> deals = basicService.getDeals().values();
        List<Integer> usedDealIds = new ArrayList<>();
        List<Pair<Integer, Integer>> dealIds = new ArrayList<>();
        int offset = 0;
        int total;
        do {
            Entity entity = HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_DEAL_LIST)
                    .cookie(basicService.getCookie()).param(Entity.of(Params.COMMON_PAGE).put("offset", offset).put("limit", 100)))
                    .valid("获取排期失败").getEntity();
            Integer count = (Integer) entity.get("result total");
            if (count == null) {
                log.error("获取麦田广告返回非法内容");
                break;
            }
            else total = count;
            offset += 100;
            entity.cd("result/list").each(v -> {
                Integer id = (Integer) v.get("uid");
                if (deals.contains(id)) return;
                Integer num = (Integer) v.get("scheduleItemNum");
                if (num == null || num > 0) usedDealIds.add(id);
                dealIds.add(new Pair<>(id, (Integer) v.get("version")));
            });
        }
        while (offset < total);
        for (Integer id : usedDealIds) {
            for (DealItem item : getDealItemInfo(id)) {
                for (ContractAd ad : listAds(item.getId())){
                    if (ad.getAdStatus() == null) continue;
                    if (ad.getAdStatus()) {
                        updateAd(ad.getAdId(), ad.getAdVersion(), false);
                        ad.setAdVersion(ad.getAdVersion() + 1);
                    }
                    removeAd(ad.getAdId(), ad.getAdVersion());
                }
                if (item.getStatus()) {
                    updateItem(item.getId(), item.getVersion(), false);
                    item.setVersion(item.getVersion() + 1);
                }
                removeItem(item.getId(), item.getVersion());
                removeReservation(item.getReservationId());
            }
        }
        for (Pair<Integer, Integer> pair : dealIds) {
            HttpExecutor.doRequest(Task.post(URL + Urls.MAISUI_DEAL_DELETE)
                    .cookie(basicService.getCookie()).param(Entity.of().put("uid", pair.getKey()).put("version", pair.getValue())))
                    .logError("删除排期失败，dealId = {}", pair.getKey());
        }
        log.info("麦田后台清理工作完成");
    }

    private List<DealItem> getDealItemInfo(Integer dealId) {
        List<DealItem> list = new ArrayList<>();
        if (dealId == null) return list;
        TaskResult result = HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_DETAIL).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", String.valueOf(dealId))));
        if (result.isSuccess()) {
            result.getEntity()
                    .cd("result/items").each(v -> {
                DealItem item = v.to(DealItem.class);
                item.setStatus("ON".equals(v.get("trafficSwitch code")));
                list.add(item);
            });
        }
        else result.logError("获取排期条目详情失败，dealId = {}", dealId);
        return list;
    }

    private boolean updateItem(Integer id, Integer version, boolean flag) {
        if (id == null) return false;
        return HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_ITEM_CLOSE).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", id).put("version", version).put("trafficSwitch", flag ? "ON" : "CLOSE")))
                .logError("开启/关闭排期条目流量失败，itemId = {}", id);
    }

    private boolean updateAd(Integer id, Integer version, boolean flag) {
        if (id == null) return false;
        return HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_AD_CLOSE).cookie(basicService.getCookie()).param(Entity.of()
                .put("id", id).put("version", version).put("status", flag ? "1001" : "411")))
                .logError("开启/关闭麦田广告失败，adId = {}", id);
    }

    private boolean removeItem(Integer id, Integer version) {
        if (id == null) return false;
        return HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_ITEM_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", id).put("version", version))).isSuccess() ||
                HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_ITEM_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                        .put("uid", id).put("version", version - 1)))
                        .logError("删除排期条目失败，itemId = {}", id);
    }

    private boolean removeAd(Integer id, Integer version) {
        if (id == null) return false;
        return HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_AD_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", id).put("version", version)))
                .isSuccess() ||
                HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_AD_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                        .put("uid", id).put("version", version - 1)))
                        .logError("删除麦田广告失败，adId = {}", id);
    }

    private boolean removeReservation(Integer id) {
        if (id == null) return false;
        return HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_RESERVATION_DELETE).cookie(basicService.getCookie()).param(Entity.of()
                .put("uid", id).put("version", 0)))
                .logError("删除资源预定失败，预定id = {}", id);
    }

    public void rollback(Collection<Integer> adIds, List<Pair<Integer, Integer>> items) {
        if (!CollectionUtils.isEmpty(adIds)) {
            for (Integer id : adIds) {
                updateAd(id, 0, false);
                removeAd(id, 1);
            }
        }
        List<Integer> ids = new ArrayList<>();
        List<Integer> dealItems = new ArrayList<>();
        for (Pair<Integer, Integer> v : items) {
            if (v.getKey() != null) ids.add(v.getKey());
            if (v.getValue() != null) dealItems.add(v.getValue());
        }
        if (CollectionUtils.isEmpty(dealItems)) return;
        for (Integer id : dealItems) {
            updateItem(id, 0, false);
            removeItem(id, 1);
        }
        for (Integer id : ids) removeReservation(id);
    }

    @Override
    public boolean update(Collection<Integer> list, boolean flag) {
        boolean success = true;
        for (Integer id : list) {
            ContractAd contractAd = fillAd(ads.get(id));
            if (contractAd == null) continue;
            Integer version = contractAd.getAdVersion();
            if (version != null && flag != contractAd.getAdStatus()) {
                if (updateAd(contractAd.getAdId(), version, flag)) {
                    contractAd.setAdStatus(flag);
                    contractAd.setAdVersion(version + 1);
                }
                else success = false;
            }
            version = contractAd.getVersion();
            if (flag != contractAd.getStatus()) {
                if (updateItem(contractAd.getDealItemId(), version, flag)) {
                    contractAd.setStatus(flag);
                    contractAd.setVersion(version + 1);
                }
                else success = false;
            }
        }
        return success;
    }

    @Override
    public boolean remove(Collection<Integer> list) {
        boolean success = true;
        for (Integer id : list) {
            ContractAd contractAd = fillAd(ads.get(id));
            if (contractAd == null) continue;
            Integer version = contractAd.getAdVersion();
            if (contractAd.getAdId() != null && !removeAd(contractAd.getAdId(), version)) { success = false; continue;}
            version = contractAd.getVersion();
            if (!removeItem(contractAd.getDealItemId(), version)) { success = false; continue;}
            if (!removeReservation(contractAd.getReservationId())) { success = false; continue;}
            ads.remove(id);
        }
        return success;
    }

    private ContractAd fillAd(ContractAd ad) {
        if (ad == null) return null;
        if (ad.getAdId() != null) return ad;
        Integer id = ad.getDealItemId();
        TaskResult result = HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_AD_LIST).cookie(basicService.getCookie()).param(Entity.of(Params.MAITIAN_AD_LIST)
                .put("scheduleItemId", id)));
        if (result.isSuccess()) {
            Entity entity = result.getEntity();
            int total = (int) entity.get("result total");
            if (total == 0) log.error("排期条目下无广告，itemId = {}", id);
            else {
                if (total > 1) log.error("排期条目下广告数量大于1，itemId = {}", id);
                entity.cd("result/list[0]");
                ad.setAdId((Integer) entity.get("id"));
                ad.setAdVersion((Integer) entity.get("version"));
                ad.setAdStatus("1001".equals(entity.get("activeStatus code")));
            }
        }
        else result.logError("获取排期条目下广告列表失败，itemId = {}", id);
        return ad;
    }

    private List<ContractAd> listAds(Integer dealItem) {
        List<ContractAd> list = new ArrayList<>();
        if (dealItem == null) return list;
        TaskResult result = HttpExecutor.doRequest(Task.post(URL + Urls.MAITIAN_AD_LIST).cookie(basicService.getCookie()).param(Entity.of(Params.MAITIAN_AD_LIST)
                .put("scheduleItemId", dealItem)));
        if (result.isSuccess()) {
            result.getEntity().cd("result/list").each(t -> {
                ContractAd ad = new ContractAd();
                ad.setAdId((Integer) t.get("id"));
                ad.setAdVersion((Integer) t.get("version"));
                ad.setAdStatus("1001".equals(t.get("activeStatus code")));
                list.add(ad);
            });
        }
        else result.logError("获取排期条目下广告列表失败，itemId = {}", dealItem);
        return list;
    }

    @Override
    public Collection<ContractAd> getAds() {
        return ads.values();
    }
}
