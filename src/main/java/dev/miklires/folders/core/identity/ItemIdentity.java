package dev.miklires.folders.core.identity;

import dev.miklires.folders.core.data.FolderType;

import java.util.Locale;

/**
 * Builds the stable ids folders store (§6).
 *
 * <p>Kept out of the Minecraft-facing resolvers on purpose: the rules for what
 * counts as "the same server" are worth testing, and they should not change when
 * the mappings do.
 *
 * <p>Never index-based, never name-based (§77.8, §77.9).
 */
public final class ItemIdentity {

    public static final int DEFAULT_SERVER_PORT = 25565;

    private ItemIdentity() {
    }

    /**
     * A world is identified by its save directory name, which is what Minecraft
     * itself uses as the unique key inside {@code saves/} and which survives the
     * player renaming the world in-game.
     */
    public static String world(String directoryName) {
        String normalized = normalizeDirectory(directoryName);
        return normalized.isEmpty() ? null : FolderType.WORLDS.idPrefix() + normalized;
    }

    /**
     * Strips separators and case-folds nothing: save directory names are
     * case-sensitive on Linux and macOS, so lowercasing here would merge two
     * genuinely different worlds.
     */
    static String normalizeDirectory(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.replace('\\', '/').trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0) {
            normalized = normalized.substring(lastSlash + 1);
        }
        return normalized;
    }

    /**
     * A server is identified by host and port only, so renaming it in the
     * multiplayer screen keeps it in its folder (§6).
     */
    public static String server(String address) {
        HostPort parsed = parseAddress(address);
        return parsed == null ? null : server(parsed.host(), parsed.port());
    }

    public static String server(String host, int port) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
        while (normalizedHost.endsWith(".")) {
            // "example.com." and "example.com" are the same host.
            normalizedHost = normalizedHost.substring(0, normalizedHost.length() - 1);
        }
        if (normalizedHost.isEmpty()) {
            return null;
        }
        int normalizedPort = (port <= 0 || port > 65535) ? DEFAULT_SERVER_PORT : port;
        return FolderType.SERVERS.idPrefix() + normalizedHost + ":" + normalizedPort;
    }

    public record HostPort(String host, int port) {
    }

    /** Handles {@code host}, {@code host:port} and {@code [::1]:port}. */
    public static HostPort parseAddress(String address) {
        if (address == null) {
            return null;
        }
        String trimmed = address.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.startsWith("[")) {
            int close = trimmed.indexOf(']');
            if (close < 0) {
                return null;
            }
            String host = trimmed.substring(1, close);
            int port = DEFAULT_SERVER_PORT;
            if (close + 1 < trimmed.length() && trimmed.charAt(close + 1) == ':') {
                port = parsePort(trimmed.substring(close + 2));
            }
            return host.isEmpty() ? null : new HostPort(host, port);
        }

        int colon = trimmed.indexOf(':');
        if (colon < 0) {
            return new HostPort(trimmed, DEFAULT_SERVER_PORT);
        }
        // More than one colon and no brackets: a bare IPv6 literal.
        if (trimmed.indexOf(':', colon + 1) >= 0) {
            return new HostPort(trimmed, DEFAULT_SERVER_PORT);
        }
        String host = trimmed.substring(0, colon);
        return host.isEmpty() ? null : new HostPort(host, parsePort(trimmed.substring(colon + 1)));
    }

    private static int parsePort(String raw) {
        try {
            int port = Integer.parseInt(raw.trim());
            return (port <= 0 || port > 65535) ? DEFAULT_SERVER_PORT : port;
        } catch (NumberFormatException e) {
            return DEFAULT_SERVER_PORT;
        }
    }

    /**
     * A resource pack is identified by its profile id — the pack source, e.g.
     * {@code file/shaders.zip} — not its display name, so two packs that happen to
     * call themselves the same thing stay distinct (§6).
     */
    public static String resourcePack(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return null;
        }
        return FolderType.RESOURCE_PACKS.idPrefix() + profileId.trim();
    }
}
