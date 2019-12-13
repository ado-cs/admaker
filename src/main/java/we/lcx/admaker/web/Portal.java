package we.lcx.admaker.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import we.lcx.admaker.common.AdPackage;
import we.lcx.admaker.service.BaseInfo;
import javax.annotation.Resource;
import java.util.List;

/**
 * Created by LinChenxiao on 2019/12/13 17:02
 **/
@Controller
public class Portal {
    @Resource
    private BaseInfo baseInfo;

    @GetMapping("/")
    public String index() {
        return "index.html";
    }

    @GetMapping("/list")
    @ResponseBody
    public List<AdPackage> list() {
        return baseInfo.getPackages();
    }
}
