package ru.mephi.identity;

import ru.mephi.identity.security.TokenHasher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHasherTest {

    @Test
    void sha256ReturnsStableNonRawTokenValue() {
        var hasher = new TokenHasher();

        var first = hasher.sha256("refresh-token");
        var second = hasher.sha256("refresh-token");

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo("refresh-token");
        assertThat(first).doesNotContain("=");
    }
}
