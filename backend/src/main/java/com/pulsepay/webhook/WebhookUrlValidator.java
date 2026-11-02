package com.pulsepay.webhook;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

/** Rejects non-public webhook destinations both at registration and delivery time. */
public final class WebhookUrlValidator {
    private WebhookUrlValidator() { }

    public static URI publicHttpsUri(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null || uri.getPort() > 0 && uri.getPort() != 443) throw new IllegalArgumentException("Webhook URL must be a public HTTPS URL.");
            String normalized = host.toLowerCase(Locale.ROOT);
            if (normalized.equals("localhost") || normalized.endsWith(".localhost") || normalized.endsWith(".local")) throw new IllegalArgumentException("Webhook URL must not target localhost.");
            for (InetAddress address : InetAddress.getAllByName(host)) if (privateAddress(address)) throw new IllegalArgumentException("Webhook URL must not target a private network.");
            return uri;
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalArgumentException("Webhook URL host could not be validated.", e); }
    }

    private static boolean privateAddress(InetAddress address) {
        byte[] raw = address.getAddress();
        return address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()
            || (raw.length == 4 && (raw[0] & 0xff) == 100 && (raw[1] & 0xff) >= 64 && (raw[1] & 0xff) <= 127)
            || (raw.length == 4 && (raw[0] & 0xff) == 198 && ((raw[1] & 0xff) == 18 || (raw[1] & 0xff) == 19));
    }
}
