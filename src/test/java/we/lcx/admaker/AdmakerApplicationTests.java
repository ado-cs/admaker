package we.lcx.admaker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.service.Basic;
import we.lcx.admaker.service.impl.MaiSui;

import javax.annotation.Resource;

@SpringBootTest
class AdmakerApplicationTests {

    @Resource
    private Basic basic;

    @Resource
    private MaiSui maiSui;

    @Test
    void contextLoads() {
        //maiSui.createAd(2019201, BiddingMode.CPC, "test", "2019-12-13", "2019-12-14", 3);
        System.out.println(1);
    }

}
