package icu.samnyan.aqua.api.model.resp.sega.diva;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Projection/DTO for the diva musicList API.
 *
 * The original endpoint returned the {@code Pv} JPA entity directly. The entity
 * carries a lazy {@code @OneToMany} {@code difficulty} map (mapped against an
 * enum) which is both broken and expensive to serialize under load. This DTO
 * returns only the scalar columns the viewer actually needs, avoiding the
 * lazy-loading pitfall and reducing the serialized payload (proposal B1).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PvInfoDto {

    private Integer pvId;

    private Integer bpm;

    private String songName;

    private String songNameEng;

    private String songNameReading;

    private String arranger;

    private String lyrics;

    private String music;

    private Integer performerNumber;
}
