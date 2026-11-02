package com.pulsepay.payment;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class PaymentStateMachineTest {
 @Test void only_confirmation_ready_payments_can_be_confirmed(){assertTrue(PaymentStateMachine.canConfirm("REQUIRES_CONFIRMATION"));assertFalse(PaymentStateMachine.canConfirm("SUCCEEDED"));}
 @Test void capture_never_exceeds_authorization(){assertTrue(PaymentStateMachine.canCapture("AUTHORIZED",1000,0,1000));assertTrue(PaymentStateMachine.canCapture("PARTIALLY_CAPTURED",1000,400,600));assertFalse(PaymentStateMachine.canCapture("PARTIALLY_CAPTURED",1000,400,601));assertFalse(PaymentStateMachine.canCapture("CANCELED",1000,0,1));}
}
