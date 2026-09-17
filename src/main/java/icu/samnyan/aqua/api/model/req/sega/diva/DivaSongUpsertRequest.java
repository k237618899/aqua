package icu.samnyan.aqua.api.model.req.sega.diva;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for one DIVA song metadata row and its playable entries.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DivaSongUpsertRequest {

    private Integer pvId;

    private Integer bpm;

    private String songName;

    private String songNameEng;

    private String songNameReading;

    private String arranger;

    private String lyrics;

    private String music;

    private Integer performerNumber;

    private String jacket;

    private List<DivaSongEntryUpsertRequest> entries = new ArrayList<>();
}
