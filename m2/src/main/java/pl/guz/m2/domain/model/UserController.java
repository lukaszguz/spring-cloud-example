package pl.guz.m2.domain.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("/users")
@AllArgsConstructor
@Slf4j
class UserController {

    private final ObjectMapper objectMapper;

    @GetMapping
    Page<UserDto> getUsers(
            @PageableDefault(sort = "firstName", direction = Direction.DESC) Pageable pageable,
            @RequestParam Map<String, Object> allRequestParams) {
        UserFilter userFilter = objectMapper.convertValue(allRequestParams, UserFilter.class);
        log.info("Page: {}, Filter: {}", pageable, userFilter);

        List<UserDto> users = new ArrayList<>(Arrays.asList(
                UserDto.builder().firstName("Jan").lastName("Kowalski").build(),
                UserDto.builder().firstName("Zbyszek").lastName("Nowak").build()
        ));
        List<UserDto> finalUsers = userFilter.getFirstName()
                .map(firstName -> users.stream()
                        .filter(userDto -> userDto.getFirstName().equals(firstName))
                        .collect(Collectors.toList()))
                .orElse(users);

        return new PageImpl<>(finalUsers, pageable, users.size());
    }
}
