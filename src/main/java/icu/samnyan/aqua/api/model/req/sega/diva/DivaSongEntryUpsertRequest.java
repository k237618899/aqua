package icu.samnyan.aqua.api.model.req.sega.diva;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request payload for one DIVA playable-entry row.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DivaSongEntryUpsertRequest {

    private int difficulty;

    private int version;

    private int edition;

    /**
     * Per-difficulty level code (e.g. "PV_LV_08_5"). Supports the .5 level format.
     * Optional: left blank when the level is unknown.
     */
    private String level;

    private LocalDateTime demoStart;

    private LocalDateTime demoEnd;

    private LocalDateTime playableStart;

    private LocalDateTime playableEnd;
}
