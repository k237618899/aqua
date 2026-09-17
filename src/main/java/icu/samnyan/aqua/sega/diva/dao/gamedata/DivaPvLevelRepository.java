package icu.samnyan.aqua.sega.diva.dao.gamedata;

import icu.samnyan.aqua.sega.diva.model.gamedata.Difficulty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for diva_pv_info_level, which stores per-song, per-difficulty level
 * codes such as PV_LV_08_5 (the .5-level format used by Project Diva Arcade).
 */
public interface DivaPvLevelRepository extends JpaRepository<Difficulty, Long> {

    List<Difficulty> findByPv_PvId(int pvId);

    @Modifying
    @Query("DELETE FROM DivaPvLevel d WHERE d.pv.pvId = :pvId")
    void deleteByPvId(@Param("pvId") int pvId);
}
