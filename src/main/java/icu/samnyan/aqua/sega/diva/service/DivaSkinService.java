package icu.samnyan.aqua.sega.diva.service;

import icu.samnyan.aqua.sega.diva.model.gamedata.DivaSkin;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DivaSkinService {

    private final Resource skinCsv;
    private List<DivaSkin> skins = Collections.emptyList();
    private Map<Integer, DivaSkin> skinById = Collections.emptyMap();

    public DivaSkinService(org.springframework.core.io.ResourceLoader resourceLoader) {
        this.skinCsv = resourceLoader.getResource("classpath:diva/skin.csv");
    }

    @PostConstruct
    public void load() {
        List<DivaSkin> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(skinCsv.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",", 2);
                if (parts.length != 2) {
                    continue;
                }
                loaded.add(new DivaSkin(Integer.parseInt(parts[0].trim()), parts[1].trim()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load diva skin catalog", e);
        }
        this.skins = Collections.unmodifiableList(loaded);
        this.skinById = Collections.unmodifiableMap(loaded.stream().collect(Collectors.toMap(DivaSkin::getId, Function.identity(), (first, ignored) -> first)));
    }

    public List<DivaSkin> getSkins() {
        return skins;
    }

    public Optional<DivaSkin> getSkinById(int id) {
        return Optional.ofNullable(skinById.get(id));
    }
}