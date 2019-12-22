package we.lcx.admaker.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.entity.web.NewAds;
import we.lcx.admaker.service.Basic;
import we.lcx.admaker.service.MaiSui;
import javax.annotation.Resource;

/**
 * Created by LinChenxiao on 2019/12/13 17:02
 **/
@Controller
public class Portal {
    @Resource
    private Basic basic;

    @Resource
    private MaiSui maiSui;

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
        int r = ads.getType() == 1 ? 0 :
                maiSui.createAd(ads.getFlight(), BiddingMode.of(ads.getFee()), ads.getName(), ads.getBegin(), ads.getEnd(), ads.getAmount());
        return r == ads.getAmount() ? Result.ok() : Result.fail((ads.getAmount() - r) + "个广告单创建失败");
    }
}
