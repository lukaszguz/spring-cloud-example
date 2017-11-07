package pl.guz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class M2Application {

    public static void main(String[] args) {
        SpringApplication.run(M2Application.class, args);
    }

    @RestController
    class SimpleController {


        @GetMapping("/hello")
        String hello() {
            log.info("Send hello");
            return "hello";
        }
    }
}
