package pl.guz.m1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

@SpringBootApplication
@EnableDiscoveryClient
public class M1Application {

    public static void main(String[] args) {
        SpringApplication.run(M1Application.class, args);
    }

    @Configuration
    class SimpleConfiguration {

        @LoadBalanced
        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @LoadBalanced
        @Bean
        AsyncRestTemplate asyncRestTemplate() {
            return new AsyncRestTemplate();
        }
    }


    @RestController
    class SimpleController {

        private final RestTemplate restTemplate;
        private final AsyncRestTemplate asyncRestTemplate;


        public SimpleController(RestTemplate restTemplate, AsyncRestTemplate asyncRestTemplate) {
            this.restTemplate = restTemplate;
            this.asyncRestTemplate = asyncRestTemplate;
        }

        @GetMapping("/hello-sync")
        String helloSync() {
            return "M2: " + restTemplate.getForObject("http://m2/hello", String.class);
        }

        @GetMapping("/hello-async")
        DeferredResult<String> helloAsync() {
            DeferredResult<String> result = new DeferredResult<>();
            asyncRestTemplate.getForEntity("http://m2/hello", String.class)
                    .addCallback(
                            stringResponseEntity -> result.setResult("M2 - async: " + stringResponseEntity.getBody()),
                            result::setErrorResult
                    );
            return result;
        }
    }
}