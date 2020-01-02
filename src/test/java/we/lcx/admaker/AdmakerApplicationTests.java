package we.lcx.admaker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import we.lcx.admaker.service.BasicService;
import we.lcx.admaker.service.BiddingService;

import javax.annotation.Resource;

@SpringBootTest
class AdmakerApplicationTests {

    @Resource
    private BasicService basicService;

    @Resource
    private BiddingService biddingService;

    @Test
    void contextLoads() {
        //maiSui.createAd(2019201, BiddingMode.CPC, "test", "2019-12-13", "2019-12-14", 3);
        System.out.println(1);
    }

}
