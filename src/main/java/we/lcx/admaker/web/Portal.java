package we.lcx.admaker.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.dto.NewAds;
import we.lcx.admaker.service.AdCreateService;
import we.lcx.admaker.service.Basic;
import javax.annotation.Resource;

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

    @PostMapping("/j/create")
    @ResponseBody
    public Result create(NewAds ads) {
        ads.convert();
        int r = ads.getType() == 1 ? maiTian.createAd(ads) : maiSui.createAd(ads);
        return r == ads.getAmount() ? Result.ok() : Result.fail((ads.getAmount() - r) + "个广告单创建失败");
    }
}
