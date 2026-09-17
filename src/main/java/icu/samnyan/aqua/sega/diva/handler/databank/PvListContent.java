package icu.samnyan.aqua.sega.diva.handler.databank;

/**
 * Immutable holder for the expensive-to-build pv_list content.
 * Caching this avoids re-running the four difficulty queries + URI encoding
 * on every request (see proposal A2). The per-request fields (req_id, timestamp)
 * are NOT part of this content and are added later by PvListHandler.
 */
public class PvListContent {

    private final String content;

    private final int easyCount;

    private final int normalCount;

    private final int hardCount;

    private final int extremeCount;

    public PvListContent(String content, int easyCount, int normalCount, int hardCount, int extremeCount) {
        this.content = content;
        this.easyCount = easyCount;
        this.normalCount = normalCount;
        this.hardCount = hardCount;
        this.extremeCount = extremeCount;
    }

    public String getContent() {
        return content;
    }

    public int getEasyCount() {
        return easyCount;
    }

    public int getNormalCount() {
        return normalCount;
    }

    public int getHardCount() {
        return hardCount;
    }

    public int getExtremeCount() {
        return extremeCount;
    }
}
