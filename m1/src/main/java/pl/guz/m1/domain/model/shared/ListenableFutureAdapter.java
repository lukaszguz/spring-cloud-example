package pl.guz.m1.domain.model.shared;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.springframework.util.concurrent.ListenableFuture;

public class ListenableFutureAdapter {

    public static <T> Single<T> toSingle(ListenableFuture<T> listenableFuture) {
        return Single.defer(() -> Single.create(singleEmitter -> listenableFuture.addCallback(singleEmitter::onSuccess, singleEmitter::onError)));
    }

    public static <T> Observable<T> toObservable(ListenableFuture<T> listenableFuture) {
        return Observable.defer(() -> Observable.create(observableEmitter -> listenableFuture.addCallback(result -> {
                                    observableEmitter.onNext(result);
                                    observableEmitter.onComplete();
                                }, observableEmitter::onError))
                               );
    }
}
