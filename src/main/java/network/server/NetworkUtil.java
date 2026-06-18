package network.server;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

/**
 * Network helpers shared between server and client mains.
 * <p>
 * In particular {@link #detectLocalIPv4()} resolves the local IPv4 address
 * that should be advertised via {@code java.rmi.server.hostname}, so that
 * RMI stubs serialised by either side carry a LAN-reachable address rather
 * than {@code 127.0.0.1} (the default from {@code InetAddress.getLocalHost()}
 * on macOS and many other setups).
 */
public final class NetworkUtil {

    /**
     * Returns the best-effort local IPv4 address to expose to remote peers.
     * Resolution order:
     * <ol>
     *   <li>the system property {@code mesos.host}, if set;</li>
     *   <li>the environment variable {@code MESOS_HOST}, if set;</li>
     *   <li>the first IPv4 address bound to a non-loopback, non-virtual,
     *       non-link-local interface that is currently up (typically the
     *       WiFi/Ethernet interface);</li>
     *   <li>{@code 127.0.0.1} as a last-resort fallback.</li>
     * </ol>
     *
     * @return the IPv4 address to advertise to remote peers, never {@code null}
     */
    public static String detectLocalIPv4() {
        String override = System.getProperty("mesos.host");
        if (override == null) override = System.getenv("MESOS_HOST");
        if (override != null && !override.isBlank()) return override.trim();

        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                // Skip Docker interfaces, virtual bridges, VPN tunnels and VM adapters:
                // these pass the isLoopback/isVirtual checks but are not reachable from
                // other hosts on the LAN/hotspot, and would otherwise be returned before
                // the real WiFi/Ethernet interface.
                String name = ni.getName().toLowerCase();
                if (name.startsWith("docker") || name.startsWith("br-")   ||
                    name.startsWith("veth")   || name.startsWith("virbr") ||
                    name.startsWith("tun")    || name.startsWith("utun")   ||
                    name.startsWith("vmnet")  || name.startsWith("vboxnet")) {
                    continue;
                }
                for (var addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address
                            && !addr.isLoopbackAddress()
                            && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {}
        return "127.0.0.1";
    }
}
