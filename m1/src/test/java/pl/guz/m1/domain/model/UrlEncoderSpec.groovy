package pl.guz.m1.domain.model

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import spock.lang.Shared
import spock.lang.Specification

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC
import static org.springframework.data.domain.Sort.Order

class UrlEncoderSpec extends Specification {

    @Shared
    private UUID uuid = UUID.fromString('5c6b4a66-bdd8-47a7-9b4a-945339a8f9fd')
    private UrlEncoder urlEncoder = new UrlEncoder()

    def "Should encode param to url"() {
        expect:
        urlEncoder.encode(dto) == encodedUrl

        where:
        dto              | encodedUrl
        TempDto.builder()
                .uuid(uuid)
                .build() | '?uuid=5c6b4a66-bdd8-47a7-9b4a-945339a8f9fd'

        TempDto.builder()
                .uuid(uuid)
                .bigDecimal(new BigDecimal('10.01'))
                .build() | '?uuid=5c6b4a66-bdd8-47a7-9b4a-945339a8f9fd&bigDecimal=10.01'

        TempDto.builder()
                .uuid(uuid)
                .bigDecimal(new BigDecimal('10.01'))
                .stringOptional(Optional.of("so"))
                .build() | '?uuid=5c6b4a66-bdd8-47a7-9b4a-945339a8f9fd&stringOptional=so&bigDecimal=10.01'

        TempDto.builder()
                .uuid(uuid)
                .stringOptional(Optional.empty())
                .bigDecimalOptional(Optional.of(new BigDecimal("123.55")))
                .bigDecimal(new BigDecimal('10.01'))
                .build() | '?uuid=5c6b4a66-bdd8-47a7-9b4a-945339a8f9fd&bigDecimalOptional=123.55&bigDecimal=10.01'
    }


    def "Should encode param to url with page"() {
        given:
        Sort sort = new Sort([new Order(ASC, 'uuid'), new Order(DESC, 'bigDecimal')])
        Pageable pageable = new PageRequest(2, 15, sort)

        expect:
        urlEncoder.encode(dto, pageable) == encodedUrl

        where:
        dto              | encodedUrl
        TempDto.builder()
                .uuid(uuid)
                .build() | '?uuid=5c6b4a66-bdd8-47a7-9b4a-945339a8f9fd&page=2&size=15&sort=uuid,ASC&sort=bigDecimal,DESC'

        TempDto.builder()
                .uuid(uuid)
                .bigDecimal(new BigDecimal('10.01'))
                .build() | '?uuid=5c6b4a66-bdd8-47a7-9b4a-945339a8f9fd&bigDecimal=10.01&page=2&size=15&sort=uuid,ASC&sort=bigDecimal,DESC'

        TempDto.builder()
                .uuid(uuid)
                .bigDecimal(new BigDecimal('10.01'))
                .stringOptional(Optional.of("so"))
                .build() | '?uuid=5c6b4a66-bdd8-47a7-9b4a-945339a8f9fd&stringOptional=so&bigDecimal=10.01&page=2&size=15&sort=uuid,ASC&sort=bigDecimal,DESC'

        TempDto.builder()
                .uuid(uuid)
                .stringOptional(Optional.empty())
                .bigDecimalOptional(Optional.of(new BigDecimal("123.55")))
                .bigDecimal(new BigDecimal('10.01'))
                .build() | '?uuid=5c6b4a66-bdd8-47a7-9b4a-945339a8f9fd&bigDecimalOptional=123.55&bigDecimal=10.01&page=2&size=15&sort=uuid,ASC&sort=bigDecimal,DESC'
    }
}
