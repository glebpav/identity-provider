package ru.mephi.identity.usecase.user;

import ru.mephi.identity.common.error.ApiException;
import ru.mephi.identity.api.user.UserResponse;
import ru.mephi.identity.repository.user.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserUseCase {

    private final UserRepository users;

    public GetUserUseCase(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse execute(UUID userId) {
        return users.findById(userId)
            .map(UserResponse::from)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }
}
