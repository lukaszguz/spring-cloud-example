package pl.guz.m1.domain.model;

import io.reactivex.Single;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.AsyncRestTemplate;

import static org.springframework.http.HttpMethod.GET;

@Service
@AllArgsConstructor
@Slf4j
class UserService {

    private final UrlEncoder urlEncoder = new UrlEncoder();
    private AsyncRestTemplate asyncRestTemplate;

    Single<Page<UserDto>> fetchUsers() {
        String encodeParams = urlEncoder.encode(UserFilter.builder().firstName("Jan").build(), new PageRequest(0, 100));
        String url = "http://m2/users" + encodeParams;
        return Single.fromFuture(asyncRestTemplate.exchange(url,
                GET,
                null,
                new ParameterizedTypeReference<Page<UserDto>>() {
                }))
                .map(HttpEntity::getBody)
                .doOnSuccess(page -> log.info("Success: {}", page))
                .doOnError(throwable -> log.error("Problem", throwable));
    }
}
