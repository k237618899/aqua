package icu.samnyan.aqua.sega.diva.dao.userdata;

import icu.samnyan.aqua.sega.diva.model.userdata.PlayerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author samnyan (privateamusement@protonmail.com)
 */
@Repository
public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {

    Optional<PlayerProfile> findByPdId(int pdId);

    Page<PlayerProfile> findByPlayerNameContainingIgnoreCaseOrderByPdIdAsc(String playerName, Pageable pageable);

    Page<PlayerProfile> findAllByOrderByPdIdAsc(Pageable pageable);
}
