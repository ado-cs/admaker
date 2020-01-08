package we.lcx.admaker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync(proxyTargetClass = true)
@SpringBootApplication
public class AdmakerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdmakerApplication.class, args);
    }

}
