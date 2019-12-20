package we.lcx.admaker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import we.lcx.admaker.service.Basic;
import we.lcx.admaker.service.MaiSui;
import javax.annotation.Resource;

@SpringBootTest
class AdmakerApplicationTests {

    @Resource
    private MaiSui maiSui;

    @Resource
    private Basic basic;

    @Test
    void contextLoads() {
        //maiSui.createAd("2019201", BiddingMode.CPC, "test", "2019-12-13T12:05:30.258Z", "2019-12-14T12:05:30.258Z", 3);
        System.out.println(1);
    }

}
