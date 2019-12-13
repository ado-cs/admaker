package we.lcx.admaker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import we.lcx.admaker.common.AdPackage;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.service.BaseInfo;
import we.lcx.admaker.service.MaiSui;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.TaskBuilder;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class AdmakerApplicationTests {

    @Resource
    private MaiSui maiSui;

    @Resource
    private BaseInfo baseInfo;

    @Test
    void contextLoads() {
        maiSui.createAd("2019201", BiddingMode.CPC, "test", "2019-12-13T12:05:30.258Z", "2019-12-14T12:05:30.258Z", 3);
        System.out.println(1);
    }

}
