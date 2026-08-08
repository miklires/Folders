package dev.miklires.folders.core.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemIdentityTest {

    @Test
    @DisplayName("the default port is implied, so host and host:25565 are one server (§6)")
    void defaultPortIsCanonical() {
        assertEquals(ItemIdentity.server("example.com"), ItemIdentity.server("example.com:25565"));
        assertEquals("server:example.com:25565", ItemIdentity.server("example.com"));
    }

    @Test
    @DisplayName("host case and a trailing dot do not change identity")
    void hostIsNormalised() {
        assertEquals(ItemIdentity.server("Example.COM"), ItemIdentity.server("example.com"));
        assertEquals(ItemIdentity.server("example.com."), ItemIdentity.server("example.com"));
    }

    @Test
    @DisplayName("a different port is a different server")
    void portMatters() {
        assertNotEquals(ItemIdentity.server("example.com:25566"), ItemIdentity.server("example.com"));
    }

    @Test
    @DisplayName("bracketed IPv6 keeps its port, bare IPv6 does not lose its address")
    void ipv6() {
        assertEquals("server:[::1]:25566", "server:[" + ItemIdentity.parseAddress("[::1]:25566").host() + "]:"
                + ItemIdentity.parseAddress("[::1]:25566").port());
        assertEquals("::1", ItemIdentity.parseAddress("::1").host());
        assertEquals(ItemIdentity.DEFAULT_SERVER_PORT, ItemIdentity.parseAddress("::1").port());
    }

    @Test
    @DisplayName("a nonsense port falls back to the default rather than failing")
    void malformedPort() {
        assertEquals(ItemIdentity.server("example.com"), ItemIdentity.server("example.com:notaport"));
        assertEquals(ItemIdentity.server("example.com"), ItemIdentity.server("example.com:99999"));
    }

    @Test
    void blankInputsYieldNoId() {
        assertNull(ItemIdentity.server("   "));
        assertNull(ItemIdentity.server(null));
        assertNull(ItemIdentity.world(""));
        assertNull(ItemIdentity.resourcePack(" "));
    }

    @Test
    @DisplayName("a world is keyed by its directory name, not the path it was found at")
    void worldUsesDirectoryName() {
        assertEquals("world:My World", ItemIdentity.world("My World"));
        assertEquals("world:My World", ItemIdentity.world("/home/p/.minecraft/saves/My World"));
        assertEquals("world:My World", ItemIdentity.world("C:\\games\\.minecraft\\saves\\My World\\"));
    }

    @Test
    @DisplayName("world directory names stay case-sensitive")
    void worldCaseIsSignificant() {
        assertNotEquals(ItemIdentity.world("world"), ItemIdentity.world("World"));
    }

    @Test
    @DisplayName("packs are keyed by profile id, so two packs named alike stay distinct (§6)")
    void packUsesProfileId() {
        assertEquals("pack:file/shaders.zip", ItemIdentity.resourcePack("file/shaders.zip"));
        assertNotEquals(ItemIdentity.resourcePack("file/a/pack.zip"), ItemIdentity.resourcePack("file/b/pack.zip"));
    }
}
