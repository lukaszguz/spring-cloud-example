package pl.guz.m1.domain.model;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController("/users")
@AllArgsConstructor
class UserController {

    private final UserService userService;

    @GetMapping
    DeferredResult<org.springframework.data.domain.Page<UserDto>> getUsers() {
        DeferredResult<org.springframework.data.domain.Page<UserDto>> result = new DeferredResult<>();
        userService.fetchUsers()
                .subscribe(result::setResult, result::setErrorResult);
        return result;
    }
}
