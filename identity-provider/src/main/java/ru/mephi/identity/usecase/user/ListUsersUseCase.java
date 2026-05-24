package ru.mephi.identity.usecase.user;

import ru.mephi.identity.api.user.UserResponse;
import ru.mephi.identity.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUsersUseCase {

    private final UserRepository users;

    public ListUsersUseCase(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> execute(Pageable pageable) {
        return users.findAll(pageable).map(UserResponse::from);
    }
}
