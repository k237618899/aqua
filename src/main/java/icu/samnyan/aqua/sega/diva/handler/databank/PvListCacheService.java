package icu.samnyan.aqua.sega.diva.handler.databank;

import icu.samnyan.aqua.sega.diva.dao.gamedata.PvEntryRepository;
import icu.samnyan.aqua.sega.diva.model.common.Difficulty;
import icu.samnyan.aqua.sega.diva.model.gamedata.PvEntry;
import icu.samnyan.aqua.sega.util.URIEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds and caches the pv_list content string (the four difficulty blocks plus
 * the trailing terminator). The content is identical for every request, so it is
 * computed once and reused until the admin updates the song list (which evicts
 * the cache via {@code @CacheEvict} in ApiDivaManageController).
 *
 * @see PvListHandler
 */
@Component
public class PvListCacheService {

    private static final Logger logger = LoggerFactory.getLogger(PvListCacheService.class);

    private static final String CACHE_NAME = "divaPvList";

    private final PvEntryRepository pvEntryRepository;

    private final DateTimeFormatter df;

    public PvListCacheService(PvEntryRepository pvEntryRepository) {
        this.pvEntryRepository = pvEntryRepository;
        this.df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    @Cacheable(cacheNames = CACHE_NAME, unless = "#result == null")
    public PvListContent getPvListContent() {
        List<PvEntry> easyList = pvEntryRepository.findByDifficulty(Difficulty.EASY);
        List<PvEntry> normalList = pvEntryRepository.findByDifficulty(Difficulty.NORMAL);
        List<PvEntry> hardList = pvEntryRepository.findByDifficulty(Difficulty.HARD);
        List<PvEntry> extremeList = pvEntryRepository.findByDifficulty(Difficulty.EXTREME);

        StringBuilder sb = new StringBuilder();
        sb.append(URIEncoder.encode(difficultyString(easyList))).append(",");
        sb.append(URIEncoder.encode(difficultyString(normalList))).append(",");
        sb.append(URIEncoder.encode(difficultyString(hardList))).append(",");
        sb.append(URIEncoder.encode(difficultyString(extremeList))).append(",");
        sb.append("%2A%2A%2A");

        return new PvListContent(sb.toString(), easyList.size(), normalList.size(), hardList.size(), extremeList.size());
    }

    private String entryString(PvEntry entry) {
        return "" + entry.getPvId() + "," +
                entry.getVersion() + "," +
                entry.getEdition().getValue() + "," +
                df.format(entry.getDemoStart()) + "," +
                df.format(entry.getDemoEnd()) + "," +
                df.format(entry.getPlayableStart()) + "," +
                df.format(entry.getPlayableEnd());
    }

    private String difficultyString(List<PvEntry> list) {
        StringBuilder sb = new StringBuilder();
        list.forEach(x -> sb.append(URIEncoder.encode(entryString(x))).append(","));
        if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
