package ngo.cong.thao.s2o_pro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class S2oProApplication {

    public static void main(String[] args) {
        SpringApplication.run(S2oProApplication.class, args);
    }

}
