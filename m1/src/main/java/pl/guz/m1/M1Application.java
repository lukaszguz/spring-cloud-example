package pl.guz.m1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanInjector;
import org.springframework.cloud.sleuth.instrument.web.HttpTraceKeysInjector;
import org.springframework.cloud.sleuth.instrument.web.client.TraceAsyncClientHttpRequestFactoryWrapper;
import org.springframework.cloud.sleuth.instrument.web.client.TraceAsyncRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
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

        @Bean
        @LoadBalanced
        AsyncRestTemplate traceAsyncRestTemplate(Tracer tracer, HttpSpanInjector httpSpanInjector, HttpTraceKeysInjector httpTraceKeysInjector,
                                                 ErrorParser errorParser) {
            TraceAsyncClientHttpRequestFactoryWrapper traceAsyncClientHttpRequestFactoryWrapper = new TraceAsyncClientHttpRequestFactoryWrapper(tracer,
                                                                                                                                                httpSpanInjector,
                                                                                                                                                new AsyncRestTemplate()
                                                                                                                                                        .getAsyncRequestFactory(),
                                                                                                                                                httpTraceKeysInjector);

            return new TraceAsyncRestTemplate(traceAsyncClientHttpRequestFactoryWrapper, tracer, errorParser);
        }

    }

    @RestController
    class SimpleController {

        private final RestTemplate restTemplate;
        private final AsyncRestTemplate asyncRestTemplate;

        public SimpleController(RestTemplate restTemplate, AsyncRestTemplate traceAsyncRestTemplate) {
            this.restTemplate = restTemplate;
            this.asyncRestTemplate = traceAsyncRestTemplate;
        }

        @GetMapping("/hello-sync")
        String helloSync() {
            log.info("Hello sync");
            return "M2: " + restTemplate.getForObject("http://m2/hello", String.class);
        }

        @GetMapping("/hello-async")
        DeferredResult<String> helloAsync() {
            DeferredResult<String> result = new DeferredResult<>();
            log.info("Hello async");
            asyncRestTemplate.getForEntity("http://m2/hello", String.class)
                             .addCallback(
                                     stringResponseEntity -> result.setResult("M2 - async: " + stringResponseEntity.getBody()),
                                     result::setErrorResult
                                         );
            return result;
        }
    }
}
