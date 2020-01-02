package we.lcx.admaker.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.entities.ModifyAd;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.common.enums.ContractMode;
import we.lcx.admaker.manager.impl.BiddingManager;
import we.lcx.admaker.manager.impl.ContractManager;
import we.lcx.admaker.service.BasicService;
import we.lcx.admaker.service.BiddingService;
import we.lcx.admaker.service.ContractService;
import we.lcx.admaker.service.aop.TraceAop;
import we.lcx.admaker.utils.CommonUtil;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by LinChenxiao on 2019/12/13 17:02
 **/
@Controller
public class Portal {
    @Resource
    private BasicService basicService;

    @Resource
    private BiddingManager biddingManager;

    @Resource
    private ContractManager contractManager;

    @Resource
    private TraceAop traceAop;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/j/flight/{query}")
    @ResponseBody
    public Result query(@PathVariable String query) {
        return Result.ok(basicService.queryFlight(query));
    }

    @GetMapping("/j/modify")
    @ResponseBody
    public Result modify(ModifyAd modifyAd) {
        contractManager.modify(modifyAd);
        return Result.ok();
    }

    @PostMapping("/j/create")
    @ResponseBody
    public Result create(NewAds ads, HttpServletRequest request) {
        ads.setTraceId(ads.getTraceId() == null ? CommonUtil.generateId() : ads.getTraceId());
        request.setAttribute("traceId", ads.getTraceId());

        return ads.getType() == 1 ? contractManager.create(ads) : biddingManager.create(ads);
    }

    @PostMapping("/j/cancel")
    @ResponseBody
    public Result cancel(String traceId) {
        NewAds ad = traceAop.getAd(traceId);
        if (ad == null) return Result.fail("记录不存在！");
        if (ad.getType() == 1) contractManager.cancel(traceId);
        else biddingManager.cancel(traceId);
        return Result.ok();
    }
}
