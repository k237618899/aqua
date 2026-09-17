package icu.samnyan.aqua.util;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Helpers for deciding whether a request originated from a private/local network.
 * Used to gate endpoints (e.g. the card list) so they are only exposed on the LAN
 * and never to clients coming from the public internet.
 */
public final class IpUtil {

    private IpUtil() {
    }

    /**
     * @return true when the request comes from loopback, a private RFC1918 range,
     *         a link-local range, or from behind a trusted proxy that reports such
     *         an address in {@code X-Forwarded-For}.
     */
    public static boolean isPrivate(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            // X-Forwarded-For: client, proxy1, proxy2 ... -> take the original client.
            ip = ip.split(",")[0].trim();
        }
        if (ip.isEmpty() || "localhost".equalsIgnoreCase(ip)) {
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
