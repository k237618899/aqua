package icu.samnyan.aqua.api.controller.sega;

import icu.samnyan.aqua.api.model.CardSummary;
import icu.samnyan.aqua.api.model.MessageResponse;
import icu.samnyan.aqua.sega.general.model.Card;
import icu.samnyan.aqua.sega.general.service.CardNameService;
import icu.samnyan.aqua.sega.general.service.CardService;
import icu.samnyan.aqua.util.IpUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * General Aime actions endpoint
 * @author samnyan (privateamusement@protonmail.com)
 */
@RestController
@RequestMapping("api/sega/aime")
public class ApiAimeController {

    private static final String ACCESS_CODE_PATTERN = "^[0-9A-Fa-f]{20}$";

    private final CardService cardService;
    private final CardNameService cardNameService;

    public ApiAimeController(CardService cardService, CardNameService cardNameService) {
        this.cardService = cardService;
        this.cardNameService = cardNameService;
    }

    @PostMapping("getByAccessCode")
    public Optional<Card> getByAccessCode(@RequestBody Map<String, String> request) {
        return cardService.getCardByAccessCode(request.get("accessCode").replaceAll("-", "").replaceAll(" ", ""));
    }

    /**
     * List every registered card. This endpoint exposes the access codes (which are
     * effectively credentials), so it is only served to clients on the local network.
     * Requests coming from a public address are rejected with 403 and the WebUI then
     * falls back to manual access-code entry.
     */
    @GetMapping("cards")
    public ResponseEntity<?> listCards(HttpServletRequest request) {
        if (!IpUtil.isPrivate(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Card list is only available on the local network."));
        }
        List<CardSummary> cards = cardService.getAllCards().stream()
                .map(c -> new CardSummary(
                        c.getExtId(),
                        c.getLuid(),
                        cardNameService.resolveName(c.getExtId()).orElse(null)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(cards);
    }

    @PutMapping("rebindAccessCode")
    public ResponseEntity<MessageResponse> rebindAccessCode(@RequestBody Map<String, Object> request) {
        Object extIdObj = request.get("extId");
        Object accessCodeObj = request.get("accessCode");
        if (!(extIdObj instanceof Number) || !(accessCodeObj instanceof String)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("extId (number) and accessCode (string) are required."));
        }

        long extId = ((Number) extIdObj).longValue();
        String normalizedAccessCode = ((String) accessCodeObj).replaceAll("-", "").replaceAll(" ", "").trim();

        if (!normalizedAccessCode.matches(ACCESS_CODE_PATTERN)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Invalid accessCode format. Expected 20 hex characters."));
        }

        Optional<Card> duplicatedCard = cardService.getCardByAccessCode(normalizedAccessCode);
        if (duplicatedCard.isPresent() && !duplicatedCard.get().getExtId().equals(extId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new MessageResponse("Access code is already in use by another card."));
        }

        Optional<Card> cardOptional = cardService.getCardByExtId(extId);
        if (cardOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Card not found for extId " + extId));
        }

        Card card = cardOptional.get();
        card.setLuid(normalizedAccessCode);
        cardService.save(card);

        return ResponseEntity.ok(new MessageResponse("Access code updated."));
    }
}
