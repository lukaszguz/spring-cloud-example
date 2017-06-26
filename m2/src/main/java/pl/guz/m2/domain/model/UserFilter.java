package pl.guz.m2.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.Optional;

@Value
public class UserFilter {

    private Optional<String> firstName;
    private Optional<String> lastName;

    @JsonCreator
    public UserFilter(
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName) {
        this.firstName = Optional.ofNullable(firstName);
        this.lastName = Optional.ofNullable(lastName);
    }
}
