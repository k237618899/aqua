package icu.samnyan.aqua.api.controller.sega.manage;

import icu.samnyan.aqua.api.model.MessageResponse;
import icu.samnyan.aqua.api.model.req.sega.diva.DivaSongEntryUpsertRequest;
import icu.samnyan.aqua.api.model.req.sega.diva.DivaSongUpsertRequest;
import icu.samnyan.aqua.api.model.resp.sega.diva.DivaSongBatchValidateResponse;
import icu.samnyan.aqua.api.model.resp.sega.diva.PvInfoDto;
import icu.samnyan.aqua.api.model.req.sega.diva.ModuleEntry;
import icu.samnyan.aqua.api.model.req.sega.diva.PvListEntry;
import icu.samnyan.aqua.api.model.req.sega.diva.PvListRequest;
import icu.samnyan.aqua.sega.diva.dao.gamedata.*;
import icu.samnyan.aqua.sega.diva.model.common.Difficulty;
import icu.samnyan.aqua.sega.diva.model.common.Edition;
import icu.samnyan.aqua.sega.diva.model.gamedata.*;
import icu.samnyan.aqua.sega.diva.service.DivaSkinService;
import icu.samnyan.aqua.sega.general.dao.PropertyEntryRepository;
import icu.samnyan.aqua.sega.general.model.PropertyEntry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author samnyan (privateamusement@protonmail.com)
 */
@RestController
@RequestMapping("api/manage/diva/")
public class ApiDivaManageController {

    private static final Logger logger = LoggerFactory.getLogger(ApiDivaManageController.class);

    private final PvEntryRepository pvEntryRepository;
    private final DivaModuleRepository moduleRepository;
    private final DivaCustomizeRepository customizeRepository;
    private final FestaRepository festaRepository;
    private final ContestRepository contestRepository;
    private final PropertyEntryRepository propertyEntryRepository;
    private final DivaPvRepository divaPvRepository;
    private final DivaPvLevelRepository divaPvLevelRepository;
    private final DivaSkinService divaSkinService;

    public ApiDivaManageController(PvEntryRepository pvEntryRepository, DivaModuleRepository moduleRepository, DivaCustomizeRepository customizeRepository, FestaRepository festaRepository, ContestRepository contestRepository, PropertyEntryRepository propertyEntryRepository, DivaPvRepository divaPvRepository, DivaPvLevelRepository divaPvLevelRepository, DivaSkinService divaSkinService) {
        this.pvEntryRepository = pvEntryRepository;
        this.moduleRepository = moduleRepository;
        this.customizeRepository = customizeRepository;
        this.festaRepository = festaRepository;
        this.contestRepository = contestRepository;
        this.propertyEntryRepository = propertyEntryRepository;
        this.divaPvRepository = divaPvRepository;
        this.divaPvLevelRepository = divaPvLevelRepository;
        this.divaSkinService = divaSkinService;
    }

    @CacheEvict(cacheNames = "divaPvList", allEntries = true)
    @PostMapping("pvList")
    public List<PvEntry> updatePvList(@RequestBody PvListRequest request) {
        // C2: soft consistency guard. Every pv_entry must reference an existing
        // diva_pv_info row. Missing references are logged (not blocked) so an
        // incomplete "add song" update is surfaced instead of silently shipping
        // inconsistent data to cabinets.
        Set<Integer> knownPvIds = new HashSet<>(divaPvRepository.findAllPvIds());
        if (!knownPvIds.isEmpty()) {
            Stream.of(request.getEasy(), request.getNormal(), request.getHard(), request.getExtreme())
                    .flatMap(List::stream)
                    .map(PvListEntry::getPVID)
                    .filter(id -> id != null && !knownPvIds.contains(id))
                    .forEach(id -> logger.warn("pvList update references pvId={} absent from diva_pv_info; pv_list/pv_info may be inconsistent", id));
        }

        request.getEasy().forEach(x -> savePv(x, Difficulty.EASY));
        request.getNormal().forEach(x -> savePv(x, Difficulty.NORMAL));
        request.getHard().forEach(x -> savePv(x, Difficulty.HARD));
        request.getExtreme().forEach(x -> savePv(x, Difficulty.EXTREME));
        return pvEntryRepository.findAll();
    }

    @Transactional
    @CacheEvict(cacheNames = "divaPvList", allEntries = true)
    @PostMapping("song")
    public ResponseEntity<?> upsertSong(@RequestBody DivaSongUpsertRequest request) {
        List<String> validationErrors = validateSongRequest(request);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse(validationErrors.get(0)));
        }

        Pv pv = upsertSongInternal(request);
        return ResponseEntity.ok(pv);
    }

    /**
     * Lightweight song list ({pvId, names, ...}) used by the add-song UI to detect
     * duplicate pvIds and to pick an existing song for editing. Reuses the same
     * projection as the game musicList endpoint so the heavy jacket column is
     * never loaded.
     */
    @GetMapping("song")
    public List<PvInfoDto> listSongs() {
        return divaPvRepository.findAllInfo();
    }

    /**
     * Full song detail (metadata + jacket + per-difficulty entries/levels) so the
     * UI can load an existing song into the edit form and round-trip it back
     * through the upsert endpoint unchanged.
     */
    @GetMapping("song/{pvId}")
    public ResponseEntity<?> getSong(@PathVariable int pvId) {
        return divaPvRepository.findById(pvId)
                .<ResponseEntity<?>>map(pv -> ResponseEntity.ok(toSongResponse(pv)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("pvId not found: " + pvId)));
    }

    private DivaSongUpsertRequest toSongResponse(Pv pv) {
        DivaSongUpsertRequest response = new DivaSongUpsertRequest();
        response.setPvId(pv.getPvId());
        response.setBpm(pv.getBpm());
        response.setSongName(pv.getSongName());
        response.setSongNameEng(pv.getSongNameEng());
        response.setSongNameReading(pv.getSongNameReading());
        response.setArranger(pv.getArranger());
        response.setLyrics(pv.getLyrics());
        response.setMusic(pv.getMusic());
        response.setPerformerNumber(pv.getPerformerNumber());
        response.setJacket(pv.getJacket());

        // diva_pv_info_level is keyed by a difficulty name (easy/normal/hard/extreme/extreme_extra),
        // so map it back to the (difficulty, edition) pair carried by each entry.
        Map<String, String> levelByDiff = new HashMap<>();
        for (icu.samnyan.aqua.sega.diva.model.gamedata.Difficulty level : divaPvLevelRepository.findByPv_PvId(pv.getPvId())) {
            levelByDiff.putIfAbsent(level.getDiff(), level.getLevel());
        }

        List<DivaSongEntryUpsertRequest> entries = new ArrayList<>();
        for (PvEntry entry : pvEntryRepository.findByPvId(pv.getPvId())) {
            int difficultyValue = entry.getDifficulty().getValue();
            int editionValue = entry.getEdition().getValue();
            entries.add(new DivaSongEntryUpsertRequest(
                    difficultyValue,
                    entry.getVersion(),
                    editionValue,
                    levelByDiff.get(difficultyName(difficultyValue, editionValue)),
                    entry.getDemoStart(),
                    entry.getDemoEnd(),
                    entry.getPlayableStart(),
                    entry.getPlayableEnd()
            ));
        }
        response.setEntries(entries);
        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = "divaPvList", allEntries = true)
    @PostMapping("song/batch")
    public ResponseEntity<?> upsertSongBatch(@RequestBody List<DivaSongUpsertRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Request body must be a non-empty array."));
        }

        for (int i = 0; i < requests.size(); i++) {
            List<String> validationErrors = validateSongRequest(requests.get(i));
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("song[" + i + "]: " + validationErrors.get(0)));
            }
        }

        requests.forEach(this::upsertSongInternal);
        return ResponseEntity.ok(new MessageResponse("Upserted songs: " + requests.size()));
    }

    @PostMapping("song/batch/validate")
    public ResponseEntity<DivaSongBatchValidateResponse> validateSongBatch(@RequestBody List<DivaSongUpsertRequest> requests) {
        List<String> errors = new ArrayList<>();
        int validCount = 0;

        if (requests == null || requests.isEmpty()) {
            errors.add("Request body must be a non-empty array.");
            return ResponseEntity.ok(new DivaSongBatchValidateResponse(false, 0, 0, errors.size(), errors));
        }

        Set<Integer> seenPvIds = new HashSet<>();
        for (int i = 0; i < requests.size(); i++) {
            final int index = i;
            DivaSongUpsertRequest request = requests.get(i);
            List<String> validationErrors = validateSongRequest(request);

            if (request != null && request.getPvId() != null && request.getPvId() > 0 && !seenPvIds.add(request.getPvId())) {
                validationErrors.add("pvId is duplicated in this batch: " + request.getPvId());
            }

            if (validationErrors.isEmpty()) {
                validCount++;
            } else {
                validationErrors.forEach(err -> errors.add("song[" + index + "]: " + err));
            }
        }

        return ResponseEntity.ok(new DivaSongBatchValidateResponse(
                errors.isEmpty(),
                requests.size(),
                validCount,
                errors.size(),
                errors
        ));
    }

    @PostMapping("module")
    public List<DivaModule> updateModuleList(@RequestBody List<ModuleEntry> request) {
        List<DivaModule> moduleList = new ArrayList<>();
        request.forEach(x -> moduleList.add(new DivaModule(x.getID(), x.getName(), x.getPrice(), x.getReleaseDate(), x.getEndDate(), x.getSortOrder())));
        return moduleRepository.saveAll(moduleList);
    }

    @PostMapping("item")
    public List<DivaCustomize> updateItemList(@RequestBody List<ModuleEntry> request) {
        List<DivaCustomize> itemList = new ArrayList<>();
        request.forEach(x -> itemList.add(new DivaCustomize(x.getID(), x.getName(), x.getPrice(), x.getReleaseDate(), x.getEndDate(), x.getSortOrder())));
        return customizeRepository.saveAll(itemList);
    }

    private void savePv(PvListEntry x, Difficulty difficulty) {
        pvEntryRepository.save(new PvEntry(x.getPVID(),
                difficulty,
                x.getVersion(),
                Edition.fromValue(x.getEdition()),
                x.getAdvDemo().getStart(),
                x.getAdvDemo().getEnd(),
                x.getPlayable().getStart(),
                x.getPlayable().getEnd()
        ));
    }

    private Pv upsertSongInternal(DivaSongUpsertRequest request) {
        Pv pv = divaPvRepository.findById(request.getPvId()).orElseGet(Pv::new);

        pv.setPvId(request.getPvId());
        pv.setBpm(request.getBpm());
        pv.setSongName(request.getSongName());
        pv.setSongNameEng(request.getSongNameEng());
        pv.setSongNameReading(request.getSongNameReading());
        pv.setArranger(request.getArranger());
        pv.setLyrics(request.getLyrics());
        pv.setMusic(request.getMusic());
        pv.setPerformerNumber(request.getPerformerNumber());
        pv.setJacket(request.getJacket());

        Pv savedPv = divaPvRepository.save(pv);

        pvEntryRepository.deleteByPvId(savedPv.getPvId());
        // EXTRA EXTREME (the slide chart added to older songs) is the EXTREME difficulty
        // played with the EXTRA edition, matching diva_pv_entry's (pv_id, edition,
        // difficulty) key and how results/rankings are recorded.
        for (DivaSongEntryUpsertRequest entry : request.getEntries()) {
            pvEntryRepository.save(new PvEntry(
                    savedPv.getPvId(),
                    Difficulty.fromValue(entry.getDifficulty()),
                    entry.getVersion(),
                    Edition.fromValue(entry.getEdition()),
                    entry.getDemoStart(),
                    entry.getDemoEnd(),
                    entry.getPlayableStart(),
                    entry.getPlayableEnd()
            ));
        }

        // Persist per-difficulty level codes (e.g. PV_LV_08_5) into diva_pv_info_level.
        // This table was previously only seeded by migrations; wiring it here lets the
        // add-song UI record .5 levels (and any PV_LV_XX_X value) for each difficulty.
        divaPvLevelRepository.deleteByPvId(savedPv.getPvId());
        for (DivaSongEntryUpsertRequest entry : request.getEntries()) {
            String level = entry.getLevel();
            if (level != null && !level.trim().isEmpty()) {
                icu.samnyan.aqua.sega.diva.model.gamedata.Difficulty dl = new icu.samnyan.aqua.sega.diva.model.gamedata.Difficulty();
                dl.setPv(savedPv);
                dl.setEdition(entry.getEdition());
                dl.setLevel(level.trim());
                dl.setVersion(entry.getVersion());
                dl.setDiff(difficultyName(entry.getDifficulty(), entry.getEdition()));
                divaPvLevelRepository.save(dl);
            }
        }

        return savedPv;
    }

    private static String difficultyName(int difficulty, int edition) {
        // EXTRA EXTREME = EXTREME with the EXTRA edition; diva_pv_info_level names it
        // "extreme_extra" (see the V10 migration's seed data).
        if (difficulty == Difficulty.EXTREME.getValue() && edition == Edition.EXTRA.getValue()) {
            return "extreme_extra";
        }
        switch (difficulty) {
            case 0: return "easy";
            case 1: return "normal";
            case 2: return "hard";
            case 3: return "extreme";
            default: return "unknown";
        }
    }

    private List<String> validateSongRequest(DivaSongUpsertRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Request body is required.");
            return errors;
        }
        if (request.getPvId() == null || request.getPvId() <= 0) {
            errors.add("pvId must be a positive integer.");
        }
        if (!StringUtils.hasText(request.getSongName())) {
            errors.add("songName is required.");
        }
        if (request.getPerformerNumber() == null || request.getPerformerNumber() < 0) {
            errors.add("performerNumber must be >= 0.");
        }
        if (request.getEntries() == null || request.getEntries().isEmpty()) {
            errors.add("entries must contain at least one playable difficulty.");
            return errors;
        }

        // A song may hold both EXTREME and EXTRA EXTREME: they share the EXTREME
        // difficulty but differ by edition, so uniqueness is keyed on (difficulty,
        // edition) rather than difficulty alone.
        Set<String> seenDifficulties = new HashSet<>();
        for (DivaSongEntryUpsertRequest entry : request.getEntries()) {
            if (Difficulty.fromValue(entry.getDifficulty()) == Difficulty.UNDEFINED) {
                errors.add("entries[].difficulty has invalid value: " + entry.getDifficulty());
            }
            if (!seenDifficulties.add(entry.getDifficulty() + ":" + entry.getEdition())) {
                errors.add("entries[] has duplicated difficulty/edition: "
                        + entry.getDifficulty() + "/" + entry.getEdition());
            }
            if (entry.getLevel() != null && !entry.getLevel().trim().isEmpty()
                    && !entry.getLevel().trim().matches("^PV_LV_\\d+_\\d$")) {
                errors.add("entries[] level must match PV_LV_XX_X format: " + entry.getLevel());
            }

            if (entry.getDemoStart() == null || entry.getDemoEnd() == null ||
                    entry.getPlayableStart() == null || entry.getPlayableEnd() == null) {
                errors.add("entries[] requires demoStart/demoEnd/playableStart/playableEnd.");
                continue;
            }
            if (entry.getDemoStart().isAfter(entry.getDemoEnd())) {
                errors.add("entries[] demoStart must be <= demoEnd.");
            }
            if (entry.getPlayableStart().isAfter(entry.getPlayableEnd())) {
                errors.add("entries[] playableStart must be <= playableEnd.");
            }
        }

        return errors;
    }

    @GetMapping("festa")
    public List<Festa> getFesta() {
        return festaRepository.findAll();
    }

    @PutMapping("festa")
    public Festa updateFesta(@RequestBody Festa festa) {
        return festaRepository.save(festa);
    }

    @DeleteMapping("festa/{id}")
    public MessageResponse getFesta(@PathVariable int id) {
        festaRepository.deleteById(id);
        return new MessageResponse("Deleted " + id);
    }

    @GetMapping("contest")
    public List<Contest> getContest() {
        return contestRepository.findAll();
    }

    @PutMapping("contest")
    public Contest updateContest(@RequestBody Contest contest) {
        return contestRepository.save(contest);
    }

    @DeleteMapping("contest/{id}")
    public MessageResponse deleteContest(@PathVariable int id) {
        contestRepository.deleteById(id);
        return new MessageResponse("Deleted " + id);
    }

    @GetMapping("news")
    public Optional<PropertyEntry> getNews() {
        return propertyEntryRepository.findByPropertyKey("diva_news");
    }

    @PutMapping("news")
    public PropertyEntry updateNews(@RequestBody PropertyEntry property) {
        PropertyEntry entry = propertyEntryRepository.findByPropertyKey("diva_news")
                .orElseGet(() -> new PropertyEntry("diva_news"));
        entry.setPropertyValue(property.getPropertyValue());
        return propertyEntryRepository.save(entry);
    }

    @GetMapping("warning")
    public Optional<PropertyEntry> getWarning() {
        return propertyEntryRepository.findByPropertyKey("diva_warning");
    }

    @PutMapping("warning")
    public PropertyEntry updateWarning(@RequestBody PropertyEntry property) {
        PropertyEntry entry = propertyEntryRepository.findByPropertyKey("diva_warning")
                .orElseGet(() -> new PropertyEntry("diva_warning"));
        entry.setPropertyValue(property.getPropertyValue());
        return propertyEntryRepository.save(entry);
    }

    @GetMapping("store-name")
    public Optional<PropertyEntry> getStoreName() {
        return propertyEntryRepository.findByPropertyKey("diva_store_name");
    }

    @PutMapping("store-name")
    public PropertyEntry updateStoreName(@RequestBody PropertyEntry property) {
        PropertyEntry entry = propertyEntryRepository.findByPropertyKey("diva_store_name")
                .orElseGet(() -> new PropertyEntry("diva_store_name"));
        entry.setPropertyValue(property.getPropertyValue());
        return propertyEntryRepository.save(entry);
    }

    @GetMapping("module")
    public List<DivaModule> getModule() {
        return moduleRepository.findAll();
    }

    @GetMapping("customize")
    public List<DivaCustomize> getCustomize() {
        return customizeRepository.findAll();
    }

    @GetMapping("skin")
    public List<DivaSkin> getSkin() {
        return divaSkinService.getSkins();
    }

    @GetMapping("skin/{id}")
    public ResponseEntity<DivaSkin> getSkinById(@PathVariable int id) {
        return divaSkinService.getSkinById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
