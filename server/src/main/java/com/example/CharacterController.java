package com.example;

import com.example.domain.PlayerCharacter;
import com.example.service.CharacterService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST surface for persistent player characters.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link CharacterService} call. All character construction, identity and resource
 * management logic lives in the service, and persistence is what lets characters and
 * their resource values reload across sessions.</p>
 *
 * <p>Characters are always scoped to a campaign: the {@code campaignId} path segment is
 * required on every endpoint, and reads and writes refuse to cross campaigns.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class CharacterController {

    private final CharacterService characters;

    public CharacterController(CharacterService characters) {
        this.characters = characters;
    }

    // ------------------------------------------------------------------
    // Creation and listing
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/characters")
    public PlayerCharacter createCharacter(@PathVariable Long campaignId,
                                           @RequestBody CharacterRequest request) {
        return characters.create(campaignId, request.name, request.ancestry,
                request.characterClass, request.level, request.background, request.alignment,
                request.hitPoints, request.maxHitPoints, request.armorClass,
                request.movement, request.proficiencyBonus);
    }

    @GetMapping("/{campaignId}/characters")
    public List<PlayerCharacter> listCharacters(@PathVariable Long campaignId) {
        return characters.list(campaignId);
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    @GetMapping("/{campaignId}/characters/{characterId}")
    public PlayerCharacter getCharacter(@PathVariable Long campaignId,
                                        @PathVariable Long characterId) {
        return characters.get(campaignId, characterId);
    }

    // ------------------------------------------------------------------
    // Identity updates
    // ------------------------------------------------------------------

    @PutMapping("/{campaignId}/characters/{characterId}")
    public PlayerCharacter updateIdentity(@PathVariable Long campaignId,
                                          @PathVariable Long characterId,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) String ancestry,
                                          @RequestParam(required = false) String characterClass,
                                          @RequestParam(required = false) Integer level,
                                          @RequestParam(required = false) String background,
                                          @RequestParam(required = false) String alignment) {
        return characters.updateIdentity(campaignId, characterId, name, ancestry, characterClass,
                level, background, alignment);
    }

    // ------------------------------------------------------------------
    // Combat statistics and resource updates
    // ------------------------------------------------------------------

    @PatchMapping("/{campaignId}/characters/{characterId}/stats")
    public PlayerCharacter updateCombatStats(@PathVariable Long campaignId,
                                             @PathVariable Long characterId,
                                             @RequestParam(required = false) Integer hitPoints,
                                             @RequestParam(required = false) Integer maxHitPoints,
                                             @RequestParam(required = false) Integer armorClass,
                                             @RequestParam(required = false) Integer movement,
                                             @RequestParam(required = false) Integer proficiencyBonus) {
        return characters.updateCombatStats(campaignId, characterId, hitPoints, maxHitPoints,
                armorClass, movement, proficiencyBonus);
    }

    @PatchMapping("/{campaignId}/characters/{characterId}/hit-points")
    public PlayerCharacter setHitPoints(@PathVariable Long campaignId,
                                        @PathVariable Long characterId,
                                        @RequestParam int hitPoints) {
        return characters.setHitPoints(campaignId, characterId, hitPoints);
    }

    @PatchMapping("/{campaignId}/characters/{characterId}/max-hit-points")
    public PlayerCharacter setMaxHitPoints(@PathVariable Long campaignId,
                                           @PathVariable Long characterId,
                                           @RequestParam int maxHitPoints) {
        return characters.setMaxHitPoints(campaignId, characterId, maxHitPoints);
    }

    // ------------------------------------------------------------------
    // Removal
    // ------------------------------------------------------------------

    @DeleteMapping("/{campaignId}/characters/{characterId}")
    public void deleteCharacter(@PathVariable Long campaignId, @PathVariable Long characterId) {
        characters.delete(campaignId, characterId);
    }

    // ------------------------------------------------------------------
    // Request bodies (HTTP concern: request shapes)
    // ------------------------------------------------------------------

    /**
     * The full set of required fields for creating a player character.
     */
    record CharacterRequest(String name, String ancestry, String characterClass, int level,
                            String background, String alignment, int hitPoints, int maxHitPoints,
                            int armorClass, int movement, int proficiencyBonus) {
    }
}
