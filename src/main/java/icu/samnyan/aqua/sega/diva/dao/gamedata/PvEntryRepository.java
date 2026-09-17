package icu.samnyan.aqua.sega.diva.dao.gamedata;

import icu.samnyan.aqua.sega.diva.model.common.Difficulty;
import icu.samnyan.aqua.sega.diva.model.gamedata.PvEntry;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author samnyan (privateamusement@protonmail.com)
 */
@Repository
public interface PvEntryRepository extends JpaRepository<PvEntry, Integer> {
    List<PvEntry> findByDifficulty(Difficulty difficulty);

    List<PvEntry> findByPvId(int pvId);

    @Modifying
    @Query("DELETE FROM DivaPvEntry p WHERE p.pvId = :pvId")
    void deleteByPvId(@Param("pvId") int pvId);
}
