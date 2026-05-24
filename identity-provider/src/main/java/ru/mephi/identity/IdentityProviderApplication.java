package ru.mephi.identity;

import ru.mephi.identity.config.IdentitySecurityProperties;
import ru.mephi.identity.config.IdentityServiceClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({IdentitySecurityProperties.class, IdentityServiceClientProperties.class})
public class IdentityProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityProviderApplication.class, args);
    }
}
