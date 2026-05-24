package ru.mephi.identity.config;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity.service-clients")
public class IdentityServiceClientProperties {

    private Client fxDealing = new Client();
    private Client positioner = new Client();
    private Client reports = new Client();

    public Optional<ResolvedClient> findByClientId(String clientId) {
        return Set.of(
                new ResolvedClient("fx-dealing", fxDealing.getClientId(), fxDealing.getClientSecret(), fxDealing.scopesAsSet()),
                new ResolvedClient("positioner", positioner.getClientId(), positioner.getClientSecret(), positioner.scopesAsSet()),
                new ResolvedClient("reports", reports.getClientId(), reports.getClientSecret(), reports.scopesAsSet())
            )
            .stream()
            .filter(client -> client.clientId().equals(clientId))
            .findFirst();
    }

    public Client getFxDealing() {
        return fxDealing;
    }

    public void setFxDealing(Client fxDealing) {
        this.fxDealing = fxDealing;
    }

    public Client getPositioner() {
        return positioner;
    }

    public void setPositioner(Client positioner) {
        this.positioner = positioner;
    }

    public Client getReports() {
        return reports;
    }

    public void setReports(Client reports) {
        this.reports = reports;
    }

    public static class Client {
        private String clientId;
        private String clientSecret;
        private String scopes;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getScopes() {
            return scopes;
        }

        public void setScopes(String scopes) {
            this.scopes = scopes;
        }

        private Set<String> scopesAsSet() {
            if (scopes == null || scopes.isBlank()) {
                return Set.of();
            }
            return Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toSet());
        }
    }

    public record ResolvedClient(String serviceName, String clientId, String clientSecret, Set<String> scopes) {
    }
}
