package pl.guz.m1.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
class UserFilter {
    private String firstName;
    private String lastName;
}
