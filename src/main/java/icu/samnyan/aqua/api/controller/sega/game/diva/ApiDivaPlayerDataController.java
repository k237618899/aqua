package icu.samnyan.aqua.api.controller.sega.game.diva;

import icu.samnyan.aqua.api.model.MessageResponse;
import icu.samnyan.aqua.api.model.ReducedPageResponse;
import icu.samnyan.aqua.api.model.resp.sega.diva.PvRankRecord;
import icu.samnyan.aqua.sega.diva.dao.userdata.*;
import icu.samnyan.aqua.sega.diva.dao.gamedata.DivaPvRepository;
import icu.samnyan.aqua.sega.diva.model.common.Difficulty;
import icu.samnyan.aqua.sega.diva.model.common.Edition;
import icu.samnyan.aqua.sega.diva.model.common.SortMode;
import icu.samnyan.aqua.sega.diva.model.userdata.*;
import icu.samnyan.aqua.sega.diva.service.PlayerProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author samnyan (privateamusement@protonmail.com)
 */
@RestController
@RequestMapping("api/game/diva")
public class ApiDivaPlayerDataController {

    private static final Logger logger = LoggerFactory.getLogger(ApiDivaPlayerDataController.class);

    private final PlayerProfileService playerProfileService;

    private final GameSessionRepository gameSessionRepository;
    private final PlayLogRepository playLogRepository;
    private final PlayerPvRecordRepository playerPvRecordRepository;
    private final PlayerPvCustomizeRepository playerPvCustomizeRepository;
    private final PlayerModuleRepository playerModuleRepository;
    private final PlayerCustomizeRepository playerCustomizeRepository;
    private final PlayerScreenShotRepository playerScreenShotRepository;
    private final PlayerInventoryRepository playerInventoryRepository;
    private final DivaPvRepository divaPvRepository;

    public ApiDivaPlayerDataController(PlayerProfileService playerProfileService, GameSessionRepository gameSessionRepository, PlayLogRepository playLogRepository, PlayerPvRecordRepository playerPvRecordRepository, PlayerPvCustomizeRepository playerPvCustomizeRepository, PlayerModuleRepository playerModuleRepository, PlayerCustomizeRepository playerCustomizeRepository, PlayerScreenShotRepository playerScreenShotRepository, PlayerInventoryRepository playerInventoryRepository, DivaPvRepository divaPvRepository) {
        this.playerProfileService = playerProfileService;
        this.gameSessionRepository = gameSessionRepository;
        this.playLogRepository = playLogRepository;
        this.playerPvRecordRepository = playerPvRecordRepository;
        this.playerPvCustomizeRepository = playerPvCustomizeRepository;
        this.playerModuleRepository = playerModuleRepository;
        this.divaPvRepository = divaPvRepository;
        this.playerCustomizeRepository = playerCustomizeRepository;
        this.playerScreenShotRepository = playerScreenShotRepository;
        this.playerInventoryRepository = playerInventoryRepository;
    }

    @PostMapping("forceUnlock")
    public ResponseEntity<MessageResponse> forceUnlock(@RequestParam int pdId) {
        PlayerProfile profile = playerProfileService.findByPdId(pdId).orElseThrow();
        Optional<GameSession> session = gameSessionRepository.findByPdId(profile);
        if(session.isPresent()) {
            gameSessionRepository.delete(session.get());
            return ResponseEntity.ok(new MessageResponse("Session deleted."));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse("Session doesn't exist."));
        }
    }

    @GetMapping("playerInfo")
    public Optional<PlayerProfile> getPlayerInfo(@RequestParam int pdId) {
        return playerProfileService.findByPdId(pdId);
    }

    @GetMapping("playerInfo/search")
    public ReducedPageResponse<PlayerProfile> searchPlayerInfo(@RequestParam(required = false, defaultValue = "") String q,
                                                               @RequestParam(required = false, defaultValue = "0") int page,
                                                               @RequestParam(required = false, defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (q == null || q.isBlank()) {
            Page<PlayerProfile> allProfiles = playerProfileService.findAll(pageable);
            return new ReducedPageResponse<>(allProfiles.getContent(), allProfiles.getPageable().getPageNumber(), allProfiles.getTotalPages(), allProfiles.getTotalElements());
        }

        String keyword = q.trim();
        if (keyword.matches("^\\d+$")) {
            Optional<PlayerProfile> exact = playerProfileService.findByPdId(Integer.parseInt(keyword));
            List<PlayerProfile> content = exact.map(List::of).orElseGet(Collections::emptyList);
            return new ReducedPageResponse<>(content, 0, content.isEmpty() ? 0 : 1, (long) content.size());
        }

        Page<PlayerProfile> profiles = playerProfileService.findByPlayerName(keyword, pageable);
        return new ReducedPageResponse<>(profiles.getContent(), profiles.getPageable().getPageNumber(), profiles.getTotalPages(), profiles.getTotalElements());
    }

    @GetMapping("playerInfo/plateOptions")
    public List<Integer> getPlayerPlateOptions(@RequestParam int pdId) {
        return playerInventoryRepository.findByPdId_PdIdAndType(pdId, "PLATE")
                .stream()
                .map(PlayerInventory::getValue)
                .filter(x -> x != null && x.matches("^-?\\d+$"))
                .map(Integer::parseInt)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @GetMapping("playerInfo/rival")
    public Map<String, String> getRivalInfo(@RequestParam int pdId) {
        int rId = playerProfileService.findByPdId(pdId).orElseThrow().getRivalPdId();
        Map<String, String> result = new HashMap<>();
        if (rId == -1) {
            result.put("rival", "Not Set");
        } else {
            Optional<PlayerProfile> profile = playerProfileService.findByPdId(rId);
            if (profile.isPresent()) {
                result.put("rival", profile.get().getPlayerName());
            } else {
                result.put("rival", "Player Not Found");
            }
        }
        return result;
    }

    @PutMapping("playerInfo/rival")
    public PlayerProfile updateRivalWithId(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId((Integer) request.get("pdId")).orElseThrow();
        profile.setRivalPdId((Integer) request.get("rivalId"));
        return playerProfileService.save(profile);
    }

    @PutMapping("playerInfo/rival/byRecord")
    public PlayerProfile updateRivalWithRecord(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId((Integer) request.get("pdId")).orElseThrow();
        PlayerPvRecord record = playerPvRecordRepository.findById(((Integer) request.get("recordId")).longValue()).orElseThrow();
        profile.setRivalPdId(record.getPdId().getPdId());
        return playerProfileService.save(profile);
    }

    @PutMapping("playerInfo/playerName")
    public PlayerProfile updateName(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId((Integer) request.get("pdId")).orElseThrow();
        profile.setPlayerName((String) request.get("playerName"));
        return playerProfileService.save(profile);
    }

    @PutMapping("playerInfo/title")
    public PlayerProfile updateTitle(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId((Integer) request.get("pdId")).orElseThrow();
        profile.setLevelTitle((String) request.get("title"));
        return playerProfileService.save(profile);
    }

    @PutMapping("playerInfo/plate")
    public PlayerProfile updatePlate(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId(((Number) request.get("pdId")).intValue()).orElseThrow();
        profile.setPlateId(((Number) request.get("plateId")).intValue());
        profile.setPlateEffectId(((Number) request.get("plateEffectId")).intValue());
        return playerProfileService.save(profile);
    }

    @PutMapping("playerInfo/commonModule")
    public PlayerProfile updateModule(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId((Integer) request.get("pdId")).orElseThrow();
        profile.setCommonModule((String) request.get("commonModule"));
        return playerProfileService.save(profile);
    }

    @PutMapping("playerInfo/commonCustomize")
    public PlayerProfile updateCustomize(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId((Integer) request.get("pdId")).orElseThrow();
        profile.setCommonCustomizeItems((String) request.get("commonCustomize"));
        return playerProfileService.save(profile);
    }

    @PutMapping("playerInfo/commonSkin")
    public PlayerProfile updateSkin(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId((Integer) request.get("pdId")).orElseThrow();
        profile.setCommonSkin((Integer) request.get("skinId"));
        return playerProfileService.save(profile);
    }

    @PutMapping("playerInfo/myList")
    public PlayerProfile updateMyList(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId((Integer) request.get("pdId")).orElseThrow();
        switch ((Integer) request.get("myListId")) {
            case 0:
                profile.setMyList0((String) request.get("myListData"));
                break;
            case 1:
                profile.setMyList1((String) request.get("myListData"));
                break;
            case 2:
                profile.setMyList2((String) request.get("myListData"));
                break;
        }
        return playerProfileService.save(profile);
    }

    @PutMapping("playerInfo/se")
    public PlayerProfile updateSe(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId((Integer) request.get("pdId")).orElseThrow();
        profile.setButtonSe((Integer) request.get("buttonSe"));
        profile.setChainSlideSe((Integer) request.get("chainSlideSe"));
        profile.setSlideSe((Integer) request.get("slideSe"));
        profile.setSliderTouchSe((Integer) request.get("sliderTouchSe"));
        return playerProfileService.save(profile);
    }

    @PutMapping("playerInfo/display")
    public PlayerProfile updateDisplay(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId((Integer) request.get("pdId")).orElseThrow();
        profile.setShowInterimRanking((Boolean) request.get("showInterimRanking"));
        profile.setShowClearStatus((Boolean) request.get("showClearStatus"));
        profile.setShowGreatBorder((Boolean) request.get("showGreatBorder"));
        profile.setShowExcellentBorder((Boolean) request.get("showExcellentBorder"));
        profile.setShowRivalBorder((Boolean) request.get("showRivalBorder"));
        profile.setShowRgoSetting((Boolean) request.get("showRgoSetting"));
        return playerProfileService.save(profile);
    }

    @PutMapping("playerInfo/settings")
    public PlayerProfile updateSettings(@RequestBody Map<String, Object> request) {
        PlayerProfile profile = playerProfileService.findByPdId(((Number) request.get("pdId")).intValue()).orElseThrow();

        if (request.containsKey("playerName")) profile.setPlayerName((String) request.get("playerName"));
        if (request.containsKey("title")) profile.setLevelTitle((String) request.get("title"));
        if (request.containsKey("plateId")) profile.setPlateId(((Number) request.get("plateId")).intValue());
        if (request.containsKey("plateEffectId")) profile.setPlateEffectId(((Number) request.get("plateEffectId")).intValue());
        if (request.containsKey("commonSkin")) profile.setCommonSkin(((Number) request.get("commonSkin")).intValue());

        if (request.containsKey("preferPerPvModule")) profile.setPreferPerPvModule((Boolean) request.get("preferPerPvModule"));
        if (request.containsKey("preferCommonModule")) profile.setPreferCommonModule((Boolean) request.get("preferCommonModule"));
        if (request.containsKey("usePerPvSkin")) profile.setUsePerPvSkin((Boolean) request.get("usePerPvSkin"));
        if (request.containsKey("usePerPvButtonSe")) profile.setUsePerPvButtonSe((Boolean) request.get("usePerPvButtonSe"));
        if (request.containsKey("usePerPvSliderSe")) profile.setUsePerPvSliderSe((Boolean) request.get("usePerPvSliderSe"));
        if (request.containsKey("usePerPvChainSliderSe")) profile.setUsePerPvChainSliderSe((Boolean) request.get("usePerPvChainSliderSe"));
        if (request.containsKey("usePerPvTouchSliderSe")) profile.setUsePerPvTouchSliderSe((Boolean) request.get("usePerPvTouchSliderSe"));

        if (request.containsKey("headphoneVolume")) profile.setHeadphoneVolume(((Number) request.get("headphoneVolume")).intValue());
        if (request.containsKey("buttonSeOn")) profile.setButtonSeOn((Boolean) request.get("buttonSeOn"));
        if (request.containsKey("buttonSeVolume")) profile.setButtonSeVolume(((Number) request.get("buttonSeVolume")).intValue());
        if (request.containsKey("sliderSeVolume")) profile.setSliderSeVolume(((Number) request.get("sliderSeVolume")).intValue());
        if (request.containsKey("buttonSe")) profile.setButtonSe(((Number) request.get("buttonSe")).intValue());
        if (request.containsKey("slideSe")) profile.setSlideSe(((Number) request.get("slideSe")).intValue());
        if (request.containsKey("chainSlideSe")) profile.setChainSlideSe(((Number) request.get("chainSlideSe")).intValue());
        if (request.containsKey("sliderTouchSe")) profile.setSliderTouchSe(((Number) request.get("sliderTouchSe")).intValue());

        if (request.containsKey("sortMode")) profile.setSortMode(SortMode.fromValue(((Number) request.get("sortMode")).intValue()));

        if (request.containsKey("showInterimRanking")) profile.setShowInterimRanking((Boolean) request.get("showInterimRanking"));
        if (request.containsKey("showClearStatus")) profile.setShowClearStatus((Boolean) request.get("showClearStatus"));
        if (request.containsKey("showGreatBorder")) profile.setShowGreatBorder((Boolean) request.get("showGreatBorder"));
        if (request.containsKey("showExcellentBorder")) profile.setShowExcellentBorder((Boolean) request.get("showExcellentBorder"));
        if (request.containsKey("showRivalBorder")) profile.setShowRivalBorder((Boolean) request.get("showRivalBorder"));
        if (request.containsKey("showRgoSetting")) profile.setShowRgoSetting((Boolean) request.get("showRgoSetting"));

        return playerProfileService.save(profile);
    }

    @GetMapping("playLog")
    public ReducedPageResponse<PlayLog> getPlayLogs(@RequestParam int pdId,
                                                    @RequestParam(required = false, defaultValue = "0") int page,
                                                    @RequestParam(required = false, defaultValue = "10") int size) {
        Page<PlayLog> playLogs = playLogRepository.findByPdId_PdIdOrderByDateTimeDesc(pdId, PageRequest.of(page, size));
        return new ReducedPageResponse<>(playLogs.getContent(), playLogs.getPageable().getPageNumber(), playLogs.getTotalPages(), playLogs.getTotalElements());
    }

    // Returns jacket image (base64 data URL) for a song, used by the result-picture export.
    @GetMapping("pv/{pvId}")
    public Map<String, Object> getPv(@PathVariable int pvId) {
        Map<String, Object> result = new HashMap<>();
        result.put("pvId", pvId);
        result.put("jacket", null);
        try {
            divaPvRepository.findById(pvId).ifPresent(pv -> result.put("jacket", pv.getJacket()));
        } catch (Exception e) {
            // A database that predates the jacket column must not break the viewer;
            // the client simply falls back to its procedural cover.
            logger.warn("Unable to read jacket for pv {}: {}", pvId, e.getMessage());
        }
        return result;
    }

    /**
     * PvRecord
     */

    @GetMapping("pvRecord")
    public ReducedPageResponse<PlayerPvRecord> getPvRecords(@RequestParam int pdId,
                                                            @RequestParam(required = false, defaultValue = "0") int page,
                                                            @RequestParam(required = false, defaultValue = "10") int size) {
        Page<PlayerPvRecord> pvRecords = playerPvRecordRepository.findByPdId_PdIdOrderByPvId(pdId, PageRequest.of(page, size));
        return new ReducedPageResponse<>(pvRecords.getContent(), pvRecords.getPageable().getPageNumber(), pvRecords.getTotalPages(), pvRecords.getTotalElements());
    }

    @GetMapping("pvRecord/{pvId}")
    public Map<String, Object> getPvRecord(@RequestParam int pdId, @PathVariable int pvId) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("records", playerPvRecordRepository.findByPdId_PdIdAndPvId(pdId, pvId));
        playerPvCustomizeRepository.findByPdId_PdIdAndPvId(pdId, pvId).ifPresent(x -> resultMap.put("customize", x));
        return resultMap;
    }

    @PutMapping("pvRecord/{pvId}")
    public PlayerPvCustomize updatePvCustomize(@RequestBody Map<String, Object> request, @PathVariable int pvId) {
        PlayerProfile profile = playerProfileService.findByPdId((Integer) request.get("pdId")).orElseThrow();
        PlayerPvCustomize playerPvCustomize = playerPvCustomizeRepository.findByPdIdAndPvId(profile, pvId)
                .orElseGet(() -> new PlayerPvCustomize(profile, pvId));
        playerPvCustomize.setModule((String) request.get("module"));
        playerPvCustomize.setCustomize((String) request.get("customize"));
        playerPvCustomize.setCustomizeFlag((String) request.get("customizeFlag"));
        playerPvCustomize.setSkin((Integer) request.get("skin"));
        playerPvCustomize.setButtonSe((Integer) request.get("buttonSe"));
        playerPvCustomize.setSlideSe((Integer) request.get("slideSe"));
        playerPvCustomize.setChainSlideSe((Integer) request.get("chainSlideSe"));
        playerPvCustomize.setSliderTouchSe((Integer) request.get("sliderTouchSe"));
        return playerPvCustomizeRepository.save(playerPvCustomize);
    }

    @GetMapping("pvRecord/{pvId}/ranking/{difficulty}")
    public ReducedPageResponse<PvRankRecord> getPvRanking(@PathVariable int pvId,
                                                          @PathVariable String difficulty,
                                                          @RequestParam(required = false, defaultValue = "0") int page,
                                                          @RequestParam(required = false, defaultValue = "10") int size) {
        Difficulty diff = null;
        Edition edition = Edition.ORIGINAL;
        switch (difficulty) {
            case "EASY":
                diff = Difficulty.EASY;
                break;
            case "NORMAL":
                diff = Difficulty.NORMAL;
                break;
            case "HARD":
                diff = Difficulty.HARD;
                break;
            case "EXTREME":
                diff = Difficulty.EXTREME;
                break;
            case "EXTRA_EXTREME": {
                diff = Difficulty.EXTREME;
                edition = Edition.EXTRA;
                break;
            }
        }
        if(diff != null) {
            Page<PlayerPvRecord> pvRecords = playerPvRecordRepository.findByPvIdAndEditionAndDifficultyOrderByMaxScoreDesc(pvId, edition,diff, PageRequest.of(page, size));

            List<PvRankRecord> rankList = new LinkedList<>();

            pvRecords.forEach(x ->{
                rankList.add(new PvRankRecord(x.getId(),x.getPdId().getPlayerName(),x.getMaxScore(),x.getMaxAttain()));
            });

            return new ReducedPageResponse<>(rankList, pvRecords.getPageable().getPageNumber(), pvRecords.getTotalPages(), pvRecords.getTotalElements());
        }
        return null;
    }

    @GetMapping("module")
    public ReducedPageResponse<PlayerModule> getModules(@RequestParam int pdId,
                                                        @RequestParam(required = false, defaultValue = "0") int page,
                                                        @RequestParam(required = false, defaultValue = "10") int size) {
        Page<PlayerModule> modules = playerModuleRepository.findByPdId_PdId(pdId, PageRequest.of(page, size));
        return new ReducedPageResponse<>(modules.getContent(), modules.getPageable().getPageNumber(), modules.getTotalPages(), modules.getTotalElements());
    }

    @GetMapping("customize")
    public ReducedPageResponse<PlayerCustomize> getCustomizes(@RequestParam int pdId,
                                                              @RequestParam(required = false, defaultValue = "0") int page,
                                                              @RequestParam(required = false, defaultValue = "10") int size) {
        Page<PlayerCustomize> customizes = playerCustomizeRepository.findByPdId_PdId(pdId, PageRequest.of(page, size));
        return new ReducedPageResponse<>(customizes.getContent(), customizes.getPageable().getPageNumber(), customizes.getTotalPages(), customizes.getTotalElements());
    }

    @GetMapping("screenshot")
    public List<PlayerScreenShot> getScreenshotList(@RequestParam int pdId) {
        return playerScreenShotRepository.findByPdId_PdId(pdId);
    }

}
