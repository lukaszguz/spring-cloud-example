package pl.guz.domain.model.infrastructure;

import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.Tracer;

import java.util.List;

/**
 * {@link RxJavaPlugins} setup schedule handler into tracing for all schedulers
 * representation.
 * Based on Shivang Shah changes
 *
 * @author Łukasz Guz
 * @author Jakub Pyda
 */
class SleuthRxJava2SchedulersHandler {

    private static final Log log = LogFactory.getLog(SleuthRxJava2SchedulersHandler.class);

    private static final String RXJAVA_COMPONENT = "rxjava2";

    SleuthRxJava2SchedulersHandler(Tracer tracer, TraceKeys traceKeys,
                                   List<String> threadsToSample) {
        try {
            Function<? super Runnable, ? extends Runnable> delegate = RxJavaPlugins.getScheduleHandler();
            if (delegate instanceof SleuthRxJava2SchedulersHandler.ScheduleHandler) {
                return;
            }
            logCurrentStateOfRxJavaPlugins();
            RxJavaPlugins.reset();
            RxJavaPlugins.setScheduleHandler(new ScheduleHandler(tracer, traceKeys, threadsToSample, delegate));
        } catch (Exception e) {
            log.error("Failed to register Sleuth RxJava SchedulersHook", e);
        }
    }

    private void logCurrentStateOfRxJavaPlugins() {
        if (log.isDebugEnabled()) {
            log.debug("Registering Sleuth RxJava2 Schedulers Hook.");
        }
    }

    static class ScheduleHandler implements Function<Runnable, Runnable> {

        private final Tracer tracer;
        private final TraceKeys traceKeys;
        private final List<String> threadsToSample;
        private final Function<? super Runnable, ? extends Runnable> delegate;

        public ScheduleHandler(Tracer tracer, TraceKeys traceKeys, List<String> threadsToSample,
                               Function<? super Runnable, ? extends Runnable> delegate) {
            this.tracer = tracer;
            this.traceKeys = traceKeys;
            this.threadsToSample = threadsToSample;
            this.delegate = delegate;
        }

        @Override
        public Runnable apply(Runnable action) throws Exception {
            if (action instanceof SleuthRxJava2SchedulersHandler.TraceAction) {
                return action;
            }
            Runnable wrappedAction = this.delegate != null ? this.delegate.apply(action) : action;
            if (wrappedAction instanceof SleuthRxJava2SchedulersHandler.TraceAction) {
                return action;
            }
            return new SleuthRxJava2SchedulersHandler.TraceAction(this.tracer, this.traceKeys, wrappedAction, this.threadsToSample);
        }
    }

    static class TraceAction implements Runnable {

        private final Runnable actual;
        private final Tracer tracer;
        private final TraceKeys traceKeys;
        private final Span parent;
        private final List<String> threadsToIgnore;

        public TraceAction(Tracer tracer, TraceKeys traceKeys, Runnable actual,
                           List<String> threadsToIgnore) {
            this.tracer = tracer;
            this.traceKeys = traceKeys;
            this.threadsToIgnore = threadsToIgnore;
            this.parent = tracer.getCurrentSpan();
            this.actual = actual;
        }

        @Override
        public void run() {
            // don't create a span if the thread name is on a list of threads to ignore
            for (String threadToIgnore : this.threadsToIgnore) {
                String threadName = Thread.currentThread()
                                          .getName();
                if (threadName.matches(threadToIgnore)) {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format(
                                "Thread with name [%s] matches the regex [%s]. A span will not be created for this Thread.",
                                threadName, threadToIgnore));
                    }
                    this.actual.run();
                    return;
                }
            }
            Span span = this.parent;
            boolean created = false;
            if (span != null) {
                span = this.tracer.continueSpan(span);
            }
            else {
                span = this.tracer.createSpan(RXJAVA_COMPONENT);
                this.tracer.addTag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, RXJAVA_COMPONENT);
                this.tracer.addTag(this.traceKeys.getAsync()
                                                 .getPrefix()
                                   + this.traceKeys.getAsync()
                                                   .getThreadNameKey(),
                                   Thread.currentThread()
                                         .getName());
                created = true;
            }
            try {
                this.actual.run();
            } finally {
                if (created) {
                    this.tracer.close(span);
                }
                else if (this.tracer.isTracing()) {
                    this.tracer.detach(span);
                }
            }
        }
    }
}
