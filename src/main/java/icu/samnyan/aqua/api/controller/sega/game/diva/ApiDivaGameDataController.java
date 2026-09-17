package icu.samnyan.aqua.api.controller.sega.game.diva;

import icu.samnyan.aqua.api.model.resp.sega.diva.PvInfoDto;
import icu.samnyan.aqua.sega.diva.dao.gamedata.DivaCustomizeRepository;
import icu.samnyan.aqua.sega.diva.dao.gamedata.DivaModuleRepository;
import icu.samnyan.aqua.sega.diva.dao.gamedata.DivaPvRepository;
import icu.samnyan.aqua.sega.diva.model.gamedata.DivaCustomize;
import icu.samnyan.aqua.sega.diva.model.gamedata.DivaModule;
import icu.samnyan.aqua.sega.diva.model.gamedata.DivaSkin;
import icu.samnyan.aqua.sega.diva.service.DivaSkinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author samnyan (privateamusement@protonmail.com)
 */
@RestController
@RequestMapping("api/game/diva/data")
public class ApiDivaGameDataController {

    private static final Logger logger = LoggerFactory.getLogger(ApiDivaGameDataController.class);

    private final DivaModuleRepository divaModuleRepository;
    private final DivaCustomizeRepository divaCustomizeRepository;
    private final DivaPvRepository divaPvRepository;
    private final DivaSkinService divaSkinService;

    public ApiDivaGameDataController(DivaModuleRepository divaModuleRepository, DivaCustomizeRepository divaCustomizeRepository, DivaPvRepository divaPvRepository, DivaSkinService divaSkinService) {
        this.divaModuleRepository = divaModuleRepository;
        this.divaCustomizeRepository = divaCustomizeRepository;
        this.divaPvRepository = divaPvRepository;
        this.divaSkinService = divaSkinService;
    }

    @GetMapping(value = "musicList")
    public List<PvInfoDto> musicList() {
        long start = System.currentTimeMillis();
        // Select only the scalar columns the viewer needs (projection). This avoids
        // the lazy `difficulty` collection AND the heavy `jacket` TEXT column, so a
        // database that has not been upgraded with `jacket` still returns the song
        // list instead of failing with a 500.
        List<PvInfoDto> result = divaPvRepository.findAllInfo();
        // Request-level timing metric.
        logger.info("musicList: count={}, cost={}ms", result.size(), System.currentTimeMillis() - start);
        return result;
    }

    @GetMapping(value = "moduleList")
    public List<DivaModule> moduleList() {
        return divaModuleRepository.findAll();
    }

    @GetMapping(value = "customizeList")
    public List<DivaCustomize> customizeList() {
        return divaCustomizeRepository.findAll();
    }

    @GetMapping(value = "skinList")
    public List<DivaSkin> skinList() {
        return divaSkinService.getSkins();
    }

    @GetMapping(value = "skinList/{id}")
    public ResponseEntity<DivaSkin> skinById(@PathVariable int id) {
        return divaSkinService.getSkinById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
