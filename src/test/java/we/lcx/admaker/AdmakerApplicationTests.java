package we.lcx.admaker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import we.lcx.admaker.common.AdPackage;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.service.BaseInfo;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.TaskBuilder;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
class AdmakerApplicationTests {

    @Resource
    private BaseInfo baseInfo;

    @Test
    void contextLoads() {
        List<AdPackage> list = baseInfo.getPackages();
        System.out.println(1);
    }

}
