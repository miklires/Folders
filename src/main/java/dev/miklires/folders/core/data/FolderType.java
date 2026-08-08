package dev.miklires.folders.core.data;

/**
 * The kinds of vanilla list a folder can group.
 *
 * <p>Adding a fourth content type means adding a constant here plus one
 * integration class; nothing in {@code core} needs to change. See §79 of the
 * spec.
 */
public enum FolderType {
    WORLDS("worlds", "world"),
    SERVERS("servers", "server"),
    RESOURCE_PACKS("resource_packs", "pack");

    private final String configName;
    private final String idPrefix;

    FolderType(String configName, String idPrefix) {
        this.configName = configName;
        this.idPrefix = idPrefix;
    }

    /** File this type is persisted to, e.g. {@code worlds.json}. */
    public String fileName() {
        return configName + ".json";
    }

    public String configName() {
        return configName;
    }

    /**
     * Prefix every item id of this type carries, e.g. {@code world:}. Lets the
     * repository reject an id that belongs to a different list outright instead
     * of silently storing a reference nothing will ever resolve.
     */
    public String idPrefix() {
        return idPrefix + ":";
    }

    public boolean ownsId(String itemId) {
        return itemId != null && itemId.startsWith(idPrefix());
    }

    /** Translation key for this type's "N items" line. */
    public String countTranslationKey() {
        return "folders.count." + configName;
    }
}
