package ru.mephi.identity.api.auth;

import ru.mephi.identity.usecase.auth.LoginUserUseCase;
import ru.mephi.identity.usecase.auth.RefreshTokenUseCase;
import ru.mephi.identity.usecase.auth.RegisterUserUseCase;
import ru.mephi.identity.common.web.WebRequestMetadata;
import ru.mephi.identity.api.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUser;
    private final LoginUserUseCase loginUser;
    private final RefreshTokenUseCase refreshToken;

    public AuthController(
        RegisterUserUseCase registerUser,
        LoginUserUseCase loginUser,
        RefreshTokenUseCase refreshToken
    ) {
        this.registerUser = registerUser;
        this.loginUser = loginUser;
        this.refreshToken = refreshToken;
    }

    @Operation(summary = "Register a user")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        return registerUser.execute(request, WebRequestMetadata.from(servletRequest));
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return loginUser.execute(request, WebRequestMetadata.from(servletRequest));
    }

    @Operation(summary = "Rotate refresh token and issue a new access token")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest) {
        return refreshToken.execute(request, WebRequestMetadata.from(servletRequest));
    }
}
