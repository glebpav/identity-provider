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
        var privateKey = normalizeConfiguredValue(properties.getPrivateKey());
        var publicKey = normalizeConfiguredValue(properties.getPublicKey());
        if (hasText(privateKey) != hasText(publicKey)) {
            throw new IllegalStateException("JWT_PRIVATE_KEY and JWT_PUBLIC_KEY must be configured together, or both must be empty for local development key generation");
        }
        if (hasText(privateKey)) {
            return fromPem(privateKey, publicKey);
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
            throw new IllegalStateException(
                "Invalid RSA key configuration. Expected JWT_PRIVATE_KEY as PKCS#8 PEM '-----BEGIN PRIVATE KEY-----' "
                    + "and JWT_PUBLIC_KEY as X.509 PEM '-----BEGIN PUBLIC KEY-----'. "
                    + "For env files, keep keys on one line with escaped newlines '\\n'.",
                exception
            );
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
        var normalized = normalizeConfiguredValue(value)
            .replace("\\n", "\n")
            .replace("\\r", "")
            .replaceAll("-----BEGIN (.*)-----", "")
            .replaceAll("-----END (.*)-----", "")
            .replaceAll("\\s", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("RSA key value is empty after PEM normalization");
        }
        return Base64.getDecoder().decode(normalized);
    }

    private String normalizeConfiguredValue(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\"")) || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.isBlank() || normalized.equalsIgnoreCase("null")) {
            return null;
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
