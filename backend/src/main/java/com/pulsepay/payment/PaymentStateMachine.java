package com.pulsepay.payment;
import java.util.Set;
public final class PaymentStateMachine { private PaymentStateMachine(){} public static boolean canConfirm(String state){return "REQUIRES_CONFIRMATION".equals(state);} public static boolean canCancel(String state){return Set.of("REQUIRES_CONFIRMATION","REQUIRES_PAYMENT_METHOD").contains(state);} public static boolean canCapture(String authorizationState,long authorized,long captured,long requested){return Set.of("AUTHORIZED","PARTIALLY_CAPTURED").contains(authorizationState)&&requested>0&&captured+requested<=authorized;} }
