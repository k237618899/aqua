package icu.samnyan.aqua.api.model;

/**
 * Lightweight card description returned by the card-list endpoint. The {@code name}
 * is a best-effort display name borrowed from a game profile; it may be null when
 * the card has no associated player name in any game.
 */
public class CardSummary {
    private long extId;
    private String luid;
    private String name;

    public CardSummary(long extId, String luid, String name) {
        this.extId = extId;
        this.luid = luid;
        this.name = name;
    }

    public long getExtId() {
        return extId;
    }

    public String getLuid() {
        return luid;
    }

    public String getName() {
        return name;
    }
}
