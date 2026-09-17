package icu.samnyan.aqua.sega.general.service;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Resolves a human-readable display name for a card by looking at the player names
 * registered across the various games. A card has no name of its own, so we borrow
 * the first non-blank {@code userName} found in any game's user profile.
 */
@Service
public class CardNameService {

    private final icu.samnyan.aqua.sega.ongeki.dao.userdata.UserDataRepository ongekiRepo;
    private final icu.samnyan.aqua.sega.maimai2.dao.userdata.UserDataRepository maimai2Repo;
    private final icu.samnyan.aqua.sega.chusan.dao.userdata.UserDataRepository chusanRepo;
    private final icu.samnyan.aqua.sega.maimai.dao.userdata.UserDataRepository maimaiRepo;
    private final icu.samnyan.aqua.sega.chunithm.dao.userdata.UserDataRepository chunithmRepo;

    public CardNameService(
            icu.samnyan.aqua.sega.ongeki.dao.userdata.UserDataRepository ongekiRepo,
            icu.samnyan.aqua.sega.maimai2.dao.userdata.UserDataRepository maimai2Repo,
            icu.samnyan.aqua.sega.chusan.dao.userdata.UserDataRepository chusanRepo,
            icu.samnyan.aqua.sega.maimai.dao.userdata.UserDataRepository maimaiRepo,
            icu.samnyan.aqua.sega.chunithm.dao.userdata.UserDataRepository chunithmRepo) {
        this.ongekiRepo = ongekiRepo;
        this.maimai2Repo = maimai2Repo;
        this.chusanRepo = chusanRepo;
        this.maimaiRepo = maimaiRepo;
        this.chunithmRepo = chunithmRepo;
    }

    /**
     * @return the first non-blank player name found for the given card, if any.
     */
    public Optional<String> resolveName(long extId) {
        return Stream.of(
                ongekiRepo.findByCard_ExtId(extId).map(u -> u.getUserName()),
                maimai2Repo.findByCard_ExtId(extId).map(u -> u.getUserName()),
                chusanRepo.findByCard_ExtId(extId).map(u -> u.getUserName()),
                maimaiRepo.findByCard_ExtId(extId).map(u -> u.getUserName()),
                chunithmRepo.findByCard_ExtId(extId).map(u -> u.getUserName())
        ).filter(Optional::isPresent)
                .map(Optional::get)
                .filter(n -> n != null && !n.isBlank())
                .findFirst();
    }
}
