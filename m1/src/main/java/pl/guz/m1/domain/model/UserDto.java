package pl.guz.m1.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class UserDto {

    private String firstName;
    private String lastName;

    @JsonCreator
    public UserDto(
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
