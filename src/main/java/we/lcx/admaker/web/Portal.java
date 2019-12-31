package we.lcx.admaker.web;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.entities.ModifyAd;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.service.AdCreateService;
import we.lcx.admaker.service.Basic;
import we.lcx.admaker.utils.WordsTool;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * Created by LinChenxiao on 2019/12/13 17:02
 **/
@Controller
public class Portal {
    @Resource
    private Basic basic;

    @Resource
    private AdCreateService maiSui;

    @Resource
    private AdCreateService maiTian;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/j/flight/{query}")
    @ResponseBody
    public Result query(@PathVariable String query) {
        return Result.ok(basic.queryFlight(query));
    }

    @GetMapping("/j/modify")
    @ResponseBody
    public Result modify(ModifyAd modifyAd) {
        maiTian.modify(modifyAd);
        return Result.ok();
    }

    @PostMapping("/j/create")
    @ResponseBody
    public Result create(NewAds ads, HttpServletRequest request) {
        ads.setTraceId(ads.getTraceId() == null ? WordsTool.generateId() : ads.getTraceId());
        request.setAttribute("traceId", ads.getTraceId());
        ads.convert();
        return ads.getType() == 1 ? maiTian.createAd(ads) : maiSui.createAd(ads);
    }
}
