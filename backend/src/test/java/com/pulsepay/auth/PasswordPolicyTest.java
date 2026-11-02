package com.pulsepay.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {
    @Test void requires_length_case_and_number() {
        assertTrue(AuthController.strongPassword("SecurePayment7"));
        assertFalse(AuthController.strongPassword("lowercaseonly7"));
        assertFalse(AuthController.strongPassword("UPPERCASEONLY7"));
        assertFalse(AuthController.strongPassword("NoDigitsHere"));
        assertFalse(AuthController.strongPassword("Short7Aa"));
    }
}
