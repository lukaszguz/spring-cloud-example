package pl.guz.m1;

import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import pl.guz.m1.domain.model.shared.ListenableFutureAdapter;

import java.util.concurrent.Executors;

@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class M1Application {

    public static void main(String[] args) {
        SpringApplication.run(M1Application.class, args);
    }

    @Configuration
    class SimpleConfiguration {

        private static final int MAX_CONCURRENCY = 1000;
        private static final int TIMEOUT = 20 * 1000;

        @LoadBalanced
        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        @LoadBalanced
        AsyncRestTemplate traceAsyncRestTemplate(Tracer tracer, HttpSpanInjector httpSpanInjector, HttpTraceKeysInjector httpTraceKeysInjector,
                                                 ErrorParser errorParser) {
            IOReactorConfig reactorConfig = IOReactorConfig.custom()
                                                           .setConnectTimeout(TIMEOUT)
                                                           .setSoTimeout(TIMEOUT)
                                                           .build();
            HttpAsyncClientBuilder asyncClientBuilder = HttpAsyncClientBuilder.create();
            asyncClientBuilder.setDefaultIOReactorConfig(reactorConfig);
            asyncClientBuilder.setMaxConnPerRoute(MAX_CONCURRENCY)
                              .setMaxConnTotal(MAX_CONCURRENCY);
            final CloseableHttpAsyncClient httpAsyncClient = asyncClientBuilder.build();
            TraceAsyncClientHttpRequestFactoryWrapper traceAsyncClientHttpRequestFactoryWrapper = new TraceAsyncClientHttpRequestFactoryWrapper(tracer,
                                                                                                                                                httpSpanInjector,
                                                                                                                                                new
                                                                                                                                                        HttpComponentsAsyncClientHttpRequestFactory(
                                                                                                                                                        httpAsyncClient),
                                                                                                                                                httpTraceKeysInjector);
            return new TraceAsyncRestTemplate(traceAsyncClientHttpRequestFactoryWrapper, tracer, errorParser);
        }
    }

    @RestController
    class SimpleController {

        private final RestTemplate restTemplate;
        private final AsyncRestTemplate asyncRestTemplate;
        private final Scheduler scheduler;
        private final Scheduler scheduler2;

        public SimpleController(RestTemplate restTemplate,
                                AsyncRestTemplate traceAsyncRestTemplate) {
            this.restTemplate = restTemplate;
            this.asyncRestTemplate = traceAsyncRestTemplate;
            this.scheduler = Schedulers.from(Executors.newFixedThreadPool(4,
                                                                          new CustomizableThreadFactory("myPool-")));
            this.scheduler2 = Schedulers.from(Executors.newFixedThreadPool(4,
                                                                           new CustomizableThreadFactory("second-myPool-")));
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
                                     stringResponseEntity -> result
                                             .setResult("M2 - async: " + stringResponseEntity.getBody()),
                                     result::setErrorResult);
            return result;
        }

        @GetMapping("/hello-async-rx")
        DeferredResult<String> helloAsyncRx() {
            DeferredResult<String> result = new DeferredResult<>();
            log.info("Hello async rx");
            ListenableFutureAdapter
                    .toSingle(asyncRestTemplate.getForEntity("http://m2/hello-async-rx",
                                                             String.class))
                    .doOnSuccess(body -> log.info("Send request hello-async-rx"))
                    .observeOn(scheduler)
                    .map(HttpEntity::getBody)
                    .doOnSuccess(body -> log.info("Got body: {}", body))

                    .observeOn(scheduler2)
                    .doOnSuccess(body -> log.info("hello 2: {}", body))
                    .observeOn(scheduler)
                    .doOnSuccess(body -> log.info("hello 1: {}", body))

                    .doOnSubscribe(disposable -> log.info("Subscribed"))
                    .subscribe(body -> result.setResult("M1 - async - rx: " + body),
                               result::setErrorResult);
            return result;
        }

        @GetMapping("/hello-async-rx-2")
        DeferredResult<String> helloAsyncRx2() {
            DeferredResult<String> result = new DeferredResult<>();
            log.info("Hello async rx");
            Single.just("Elo")
                  .doOnSuccess(body -> log.info("Send request hello-async-rx"))
                  .observeOn(scheduler)
                  .doOnSuccess(body -> log.info("Got body: {}", body))
                  .doOnSubscribe(disposable -> log.info("Subscribed"))
                  .subscribe(body -> result.setResult("M1 - async - rx: " + body),
                             result::setErrorResult);
            return result;
        }
    }

}
