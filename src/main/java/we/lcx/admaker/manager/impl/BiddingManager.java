package we.lcx.admaker.manager.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.entities.ModifyAd;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.manager.AdManager;
import we.lcx.admaker.service.BasicService;
import we.lcx.admaker.service.BiddingService;
import we.lcx.admaker.service.ContractService;
import we.lcx.admaker.service.aop.TraceAop;
import we.lcx.admaker.utils.CommonUtil;
import we.lcx.admaker.utils.HttpExecutor;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by LinChenxiao on 2020/01/02 17:14
 **/
@Service
public class BiddingManager implements AdManager {
    @Resource
    private BiddingService biddingService;

    @Resource
    private TraceAop traceAop;

    @Value("${ad.url.maisui}")
    private String URL;

    @Override
    public Result create(NewAds ads) {
        Entity entity = biddingService.composeEntity(ads);
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < ads.getRealAmount(); i++) {
            Entity e = entity.copy();
            tasks.add(Task.post(URL + URLs.MAISUI_CREATE)
                    .param(e.put("adformName", e.get("adformName") + CommonUtil.randomSuffix(6))));
        }
        Set<Integer> adIds = new HashSet<>();
        for (TaskResult result : HttpExecutor.execute(tasks)) {
            if (result.isSuccess())
                adIds.add((Integer) result.getEntity().get("result"));
        }
        return Result.ok(adIds);
    }

    @Override
    public void cancel(String traceId) {
        List ids = new ArrayList<>(traceAop.cancel(traceId));
        ModifyAd modifyAd = new ModifyAd();
        modifyAd.setIds(ids);
        modifyAd.setState(-1);
        modify(modifyAd);
    }

    @Override
    public void modify(ModifyAd modifyAd) {

    }
}
