package com.pulsepay.security;
import java.util.UUID;
public record MerchantPrincipal(UUID userId, UUID merchantId, String role) { }

