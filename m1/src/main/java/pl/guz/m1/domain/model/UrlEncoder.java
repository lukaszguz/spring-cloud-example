package pl.guz.m1.domain.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import io.reactivex.Observable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

public class UrlEncoder {

    private ObjectMapper objectMapper;

    public UrlEncoder() {
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new OptionalModule());
    }

    public UrlEncoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(Object object) {
        Map<String, Object> params = objectMapper.convertValue(object, new TypeReference<Map<String, Object>>() {
        });
        return addQuestionMark(join(params));
    }

    public String encode(Object object, Pageable pageable) {
        Map<String, Object> params = objectMapper.convertValue(object, new TypeReference<Map<String, Object>>() {
        });
        return addQuestionMark(String.join("&", join(params), encodePage(pageable)));
    }

    private String join(Map<String, Object> params) {
        return params.entrySet()
                .stream()
                .filter(e -> Objects.nonNull(e.getValue()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(joining("&"));
    }

    private String encodePage(Pageable pageable) {
        String pageNumber = "page=" + pageable.getPageNumber();
        String size = "size=" + pageable.getPageSize();
        return String.join("&", pageNumber, size, encodeSort(pageable.getSort()));
    }

    private String encodeSort(Sort sort) {
        return Optional.ofNullable(sort)
                .map(s -> Observable.fromIterable(s)
                        .map(order -> "sort=" + order.getProperty() + "," + order.getDirection())
                        .toList()
                        .map(orders -> String.join("&", orders))
                        .blockingGet())
                .orElse("");
    }

    private String addQuestionMark(String urlWithParams) {
        return Optional.of(urlWithParams)
                .filter(url -> !url.isEmpty())
                .map(url -> "?" + url)
                .orElse(urlWithParams);
    }

    private class OptionalModule extends Module {

        @Override
        public String getModuleName() {
            return "OptionalModule";
        }

        @Override
        public Version version() {
            return VersionUtil.packageVersionFor(OptionalModule.class);
        }

        @Override
        public void setupModule(SetupContext setupContext) {
            final SimpleSerializers serializers = new SimpleSerializers();
            serializers.addSerializer(Optional.class, new OptionalSerializer());
            setupContext.addSerializers(serializers);
        }
    }

    private class OptionalSerializer extends JsonSerializer<Optional> {
        @Override
        public void serialize(Optional optional, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            if (optional.isPresent()) {
                jsonGenerator.writeObject(optional.get());
            } else {
                jsonGenerator.writeNull();
            }
        }
    }
}
