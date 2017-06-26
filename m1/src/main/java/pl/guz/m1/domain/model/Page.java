package pl.guz.m1.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.reactivex.Observable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.reactivex.Observable.fromIterable;

public class Page<T> extends PageImpl<T> {

    @JsonCreator
    public Page(
            @JsonProperty("content") List<T> content,
            @JsonProperty("number") long page,
            @JsonProperty("size") long size,
            @JsonProperty("totalElements") long totalElements) {
        super(content, new PageRequest((int) page, (int) size), totalElements);
    }

    List<T> mapNull(List<T> content) {
        return Optional.ofNullable(content)
                .orElse(Collections.emptyList());
    }

    public Observable<T> toObservable() {
        return fromIterable(getContent());
    }

    public <U> Page<U> withContent(List<U> content) {
        return new Page<>(content, getNumber(), getSize(), getTotalElements());
    }
}