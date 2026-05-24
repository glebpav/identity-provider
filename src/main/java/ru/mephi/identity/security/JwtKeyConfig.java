package ru.mephi.identity.security;

import ru.mephi.identity.config.IdentitySecurityProperties;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtKeyConfig {

    @Bean
    RSAKey rsaKey(IdentitySecurityProperties properties) {
        if (hasText(properties.getPrivateKey()) && hasText(properties.getPublicKey())) {
            return fromPem(properties.getPrivateKey(), properties.getPublicKey());
        }
        return generatedDevKey();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaKey) {
        ImmutableJWKSet<SecurityContext> jwkSet = new ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSet);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey, IdentitySecurityProperties properties) {
        try {
            var decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.getIssuer()));
            return decoder;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create JWT decoder", exception);
        }
    }

    private RSAKey fromPem(String privateKey, String publicKey) {
        try {
            var factory = KeyFactory.getInstance("RSA");
            var privateSpec = new PKCS8EncodedKeySpec(decodePem(privateKey));
            var publicSpec = new X509EncodedKeySpec(decodePem(publicKey));
            return new RSAKey.Builder((RSAPublicKey) factory.generatePublic(publicSpec))
                .privateKey((RSAPrivateKey) factory.generatePrivate(privateSpec))
                .keyID(UUID.randomUUID().toString())
                .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid RSA key configuration", exception);
        }
    }

    private RSAKey generatedDevKey() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate development RSA key", exception);
        }
    }

    private byte[] decodePem(String value) {
        var normalized = value
            .replace("\\n", "\n")
            .replaceAll("-----BEGIN (.*)-----", "")
            .replaceAll("-----END (.*)-----", "")
            .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
