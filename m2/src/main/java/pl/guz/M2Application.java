package pl.guz;

import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

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
            log.info("Hello sync");
            return "hello";
        }

        @GetMapping("/hello-async-rx")
        DeferredResult<String> helloAsyncRx() {
            DeferredResult<String> result = new DeferredResult<>();
            log.info("Hello async rx");
            Single.just("hello-rx")
                  .subscribe(body -> result.setResult("M2 - async - rx: " + body),
                             result::setErrorResult);
            return result;
        }
    }
}
