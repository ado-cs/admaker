package we.lcx.admaker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import we.lcx.admaker.common.Entity;
import we.lcx.admaker.common.Task;
import we.lcx.admaker.common.TaskResult;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.entities.ContractAd;
import we.lcx.admaker.common.entities.ModifyAd;
import we.lcx.admaker.common.enums.ContractMode;
import we.lcx.admaker.common.enums.DealMode;
import we.lcx.admaker.utils.CommonUtil;
import we.lcx.admaker.utils.HttpExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * Created by LinChenxiao on 2020/01/03 16:20
 **/
@Service
public class ContractModify {

    @Resource
    private BasicService basicService;

    @Value("${ad.url.maitian}")
    private String URL;

    @PostConstruct
    private void init() {
        long time = CommonUtil.timeOfToday();
        Map<Integer, ContractAd> ads = new HashMap<>();
        Set<Integer> removeItems = new HashSet<>();
        HttpExecutor.doRequest(Task.post(URL + URLs.MAITIAN_AD_LIST).cookie(basicService.getCookie()).param(Entity.of(Params.MAITIAN_AD_LIST)
                .cd("execPeriod")
                .put("startTime", time)
                .put("endTime", time + Settings.DAY - 1000)))
                .valid("获取今日麦田广告信息失败")
                .getEntity().each(v -> {
                    int id = (int) v.get("id");
                    String name = (String) v.get("name");
                    boolean open = "1001".equals(v.get("activeStatus code"));
                    if (open && (StringUtils.isEmpty(name) || !name.contains("_" + Settings.SPECIAL_NAME))) {
                        removeItems.add(id);
                    }
                    else {
                        ContractAd contractAd = new ContractAd();
                        contractAd.setId(id);
                        contractAd.setActive(open);
                        String[] s = CommonUtil.splitName(name, "_", 3, 2);
                        contractAd.setName(s[0]);
                        contractAd.setDealItemId(String.valueOf(v.get("scheduleItemId")));
                        contractAd.setDealMode(DealMode.of(s[1]));
                        contractAd.setContractMode(ContractMode.of(s[2]));
                        ads.put(id, contractAd);
                    }
        });

    }


    public Entity queryAds(ModifyAd ad) {

        return null;
    }

}
