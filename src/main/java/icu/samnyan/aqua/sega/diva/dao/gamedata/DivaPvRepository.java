package icu.samnyan.aqua.sega.diva.dao.gamedata;

import icu.samnyan.aqua.api.model.resp.sega.diva.PvInfoDto;
import icu.samnyan.aqua.sega.diva.model.gamedata.Pv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author  samnyan (privateamusement@protonmail.com)
 */
public interface DivaPvRepository extends JpaRepository<Pv, Integer> {

    @Query("SELECT p.pvId FROM DivaPvInfo p")
    List<Integer> findAllPvIds();

    /**
     * Projection used by the viewer's musicList endpoint.
     *
     * <p>It selects only the scalar columns the viewer actually needs and
     * deliberately excludes the heavy {@code jacket} TEXT column (and the lazy
     * {@code difficulty} collection). Because the generated SQL never references
     * {@code jacket}, the endpoint keeps working on databases whose schema has not
     * been upgraded with that column yet, instead of failing the whole request and
     * leaving the viewer with an empty song list.</p>
     */
    @Query("SELECT new icu.samnyan.aqua.api.model.resp.sega.diva.PvInfoDto(" +
            "p.pvId, p.bpm, p.songName, p.songNameEng, p.songNameReading, " +
            "p.arranger, p.lyrics, p.music, p.performerNumber) " +
            "FROM DivaPvInfo p ORDER BY p.pvId")
    List<PvInfoDto> findAllInfo();
}
