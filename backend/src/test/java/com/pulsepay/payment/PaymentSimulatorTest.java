package com.pulsepay.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class PaymentSimulatorTest {
    @Test void explicit_test_mode_outcome_overrides_the_default() {
        PaymentSimulator simulator = new PaymentSimulator(null, "success", false);
        assertEquals("temporary_failure", simulator.outcome("normal", "{\"test_outcome\":\"temporary_failure\"}"));
    }
    @Test void legacy_decline_description_remains_deterministic() {
        PaymentSimulator simulator = new PaymentSimulator(null, "success", false);
        assertEquals("decline", simulator.outcome("Please decline this test payment", "{}"));
    }
}
