package pl.guz.m1.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Builder
@EqualsAndHashCode
@Getter
public class TempDto {

    private UUID uuid;
    private Optional<String> stringOptional;
    private Optional<BigDecimal> bigDecimalOptional;
    private BigDecimal bigDecimal;
    private Long aLong;
}
