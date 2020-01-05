package we.lcx.admaker.manager.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.consts.Urls;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.manager.AdManager;
import we.lcx.admaker.service.BasicService;
import we.lcx.admaker.service.BiddingCreate;
import we.lcx.admaker.service.BiddingModify;
import we.lcx.admaker.utils.CommonUtil;
import javax.annotation.Resource;
import java.util.*;

/**
 * Created by LinChenxiao on 2020/01/02 17:14
 **/
@Service
public class BiddingManager implements AdManager {
    @Resource
    private BasicService basicService;

    @Resource
    private BiddingCreate biddingCreate;

    @Resource
    private BiddingModify biddingModify;

    @Value("${ad.url.maisui}")
    private String URL;

    @Override
    public Result create(NewAds ads) {
        Entity entity = biddingCreate.composeEntity(ads);
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < ads.getAmount(); i++) {
            Entity e = entity.copy();
            tasks.add(Task.post(URL + Urls.MAISUI_CREATE)
                    .param(e.put("adformName", e.get("adformName") + CommonUtil.randomSuffix(6))));
        }
        List<Integer> adIds = basicService.executeAndApprove(tasks, 2);
        if (adIds == null) return Result.ok();
        rollback(adIds);
        return Result.fail("创建广告失败！");
    }

    private void rollback(List<Integer> adIds) {
        if (CollectionUtils.isEmpty(adIds)) return;
        biddingModify.update(adIds, false);
        biddingModify.remove(adIds);
    }
}
