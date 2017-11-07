package pl.guz.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
class UserDto {

    private String firstName;
    private String lastName;
}
