package ru.mephi.identity.api.auth;

import ru.mephi.identity.usecase.client.IssueClientTokenUseCase;
import ru.mephi.identity.common.web.WebRequestMetadata;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oauth")
public class OAuthController {

    private final IssueClientTokenUseCase issueClientToken;

    public OAuthController(IssueClientTokenUseCase issueClientToken) {
        this.issueClientToken = issueClientToken;
    }

    @Operation(summary = "Issue service-to-service access token using client_credentials")
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public IssueClientTokenUseCase.ClientTokenResult token(
        @RequestParam("grant_type") String grantType,
        @RequestParam("client_id") String clientId,
        @RequestParam("client_secret") String clientSecret,
        @RequestParam(value = "scope", required = false) String scope,
        HttpServletRequest servletRequest
    ) {
        return issueClientToken.execute(grantType, clientId, clientSecret, scope, WebRequestMetadata.from(servletRequest));
    }
}
