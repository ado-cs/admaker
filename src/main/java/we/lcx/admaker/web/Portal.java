package we.lcx.admaker.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import we.lcx.admaker.common.AdPackage;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.enums.AdType;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.service.BaseInfo;
import we.lcx.admaker.service.MaiSui;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by LinChenxiao on 2019/12/13 17:02
 **/
@Controller
public class Portal {
    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    @Resource
    private BaseInfo baseInfo;
    @Resource
    private MaiSui maiSui;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/j/list")
    @ResponseBody
    public List<AdPackage> list() {
        return baseInfo.getPackages();
    }

    @PostMapping("/j/create")
    @ResponseBody
    public Result create(Integer type, Integer fee, Integer deal, String camp, String name, String begin, String end, Integer amount) {
        if (amount <= 0) return Result.fail("广告单个数不能小于1！");
        try {
            if (!FORMAT.parse(end).after(FORMAT.parse(begin)))
                return Result.fail("结束时间需晚于开始时间！");
            int r;
            if (AdType.isContract(type)) {
                r = 1;
            }
            else if (AdType.isBidding(type)){
                r = maiSui.createAd(camp, BiddingMode.of(fee), name, begin, end, amount);
            }
            else {
                return Result.fail("不支持的广告单类型！");
            }
            return r == amount ? Result.ok() : Result.fail(amount - r);
        }
        catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}
