package com.pulsepay.webhook;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class WebhookUrlValidatorTest {
    @Test void rejects_private_or_non_https_destinations() {
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.publicHttpsUri("http://example.com/hook"));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.publicHttpsUri("https://localhost/hook"));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.publicHttpsUri("https://127.0.0.1/hook"));
    }

    @Test void accepts_a_public_literal_https_uri() {
        assertDoesNotThrow(() -> WebhookUrlValidator.publicHttpsUri("https://1.1.1.1/hooks/pulsepay"));
    }
}
