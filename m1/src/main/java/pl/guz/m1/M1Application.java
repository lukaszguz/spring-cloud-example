package pl.guz.m1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanInjector;
import org.springframework.cloud.sleuth.instrument.web.HttpTraceKeysInjector;
import org.springframework.cloud.sleuth.instrument.web.client.TraceAsyncClientHttpRequestFactoryWrapper;
import org.springframework.cloud.sleuth.instrument.web.client.TraceAsyncListenableTaskExecutor;
import org.springframework.cloud.sleuth.instrument.web.client.TraceAsyncRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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

        @Autowired
        Tracer tracer;
        @Autowired
        private HttpTraceKeysInjector httpTraceKeysInjector;
        @Autowired
        private HttpSpanInjector spanInjector;
        @Autowired(required = false)
        private ClientHttpRequestFactory clientHttpRequestFactory;
        @Autowired(required = false)
        private AsyncClientHttpRequestFactory asyncClientHttpRequestFactory;

        private TraceAsyncClientHttpRequestFactoryWrapper traceAsyncClientHttpRequestFactory() {
            ClientHttpRequestFactory clientFactory = this.clientHttpRequestFactory;
            AsyncClientHttpRequestFactory asyncClientFactory = this.asyncClientHttpRequestFactory;
            if (clientFactory == null) {
                clientFactory = defaultClientHttpRequestFactory(this.tracer);
            }
            if (asyncClientFactory == null) {
                asyncClientFactory = clientFactory instanceof AsyncClientHttpRequestFactory ?
                                     (AsyncClientHttpRequestFactory) clientFactory : defaultClientHttpRequestFactory(this.tracer);
            }
            return new TraceAsyncClientHttpRequestFactoryWrapper(this.tracer, this.spanInjector,
                                                                 asyncClientFactory, clientFactory, this.httpTraceKeysInjector);
        }

        private SimpleClientHttpRequestFactory defaultClientHttpRequestFactory(Tracer tracer) {
            SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
            simpleClientHttpRequestFactory.setTaskExecutor(asyncListenableTaskExecutor(tracer));
            return simpleClientHttpRequestFactory;
        }

        private AsyncListenableTaskExecutor asyncListenableTaskExecutor(Tracer tracer) {
            ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
            threadPoolTaskScheduler.initialize();
            return new TraceAsyncListenableTaskExecutor(threadPoolTaskScheduler, tracer);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(value = "spring.sleuth.web.async.client.template.enabled", matchIfMissing = true)
        public AsyncRestTemplate traceAsyncRestTemplate(ErrorParser errorParser) {
            return new TraceAsyncRestTemplate(traceAsyncClientHttpRequestFactory(), this.tracer, errorParser);
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
