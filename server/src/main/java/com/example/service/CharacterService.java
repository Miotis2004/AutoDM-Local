package com.example.service;

import com.example.domain.Campaign;
import com.example.domain.PlayerCharacter;
import com.example.db.CampaignRepository;
import com.example.db.PlayerCharacterRepository;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for persistent player characters owned by a campaign.
 *
 * <p>This service is the single place where player characters are created, updated,
 * consulted, and removed. Every mutation resolves its owning campaign, applies the
 * change to a managed {@link PlayerCharacter}, and relies on the repository to persist
 * it, so identity, combat statistics, and resource values such as hit points reload
 * across sessions.</p>
 */
@Service
public class CharacterService {

    private final CampaignRepository campaigns;
    private final PlayerCharacterRepository characters;

    public CharacterService(CampaignRepository campaigns,
                            PlayerCharacterRepository characters) {
        this.campaigns = campaigns;
        this.characters = characters;
    }

    // ------------------------------------------------------------------
    // Campaign / character lookup
    // ------------------------------------------------------------------

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private PlayerCharacter requireCharacter(Long campaignId, Long characterId) {
        PlayerCharacter character = characters.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("No character with id " + characterId));
        Campaign owner = character.getCampaign();
        if (owner == null || !owner.getId().equals(campaignId)) {
            throw new IllegalArgumentException(
                    "Character " + characterId + " does not belong to campaign " + campaignId);
        }
        return character;
    }

    // ------------------------------------------------------------------
    // Creation and listing
    // ------------------------------------------------------------------

    /**
     * Creates a new player character owned by the given campaign. Every identity and
     * combat-statistic field is required so a character is never created half-formed.
     *
     * @param campaignId the owning campaign
     * @param name the character's name
     * @param ancestry the character's ancestry
     * @param characterClass the character's class
     * @param level the character's level
     * @param background the character's background
     * @param alignment the character's alignment
     * @param hitPoints the current hit points
     * @param maxHitPoints the maximum hit points
     * @param armorClass the armor class
     * @param movement the movement speed
     * @param proficiencyBonus the proficiency bonus
     * @return the created, persisted player character
     */
    public PlayerCharacter create(Long campaignId, String name, String ancestry,
                                  String characterClass, int level, String background,
                                  String alignment, int hitPoints, int maxHitPoints,
                                  int armorClass, int movement, int proficiencyBonus) {
        Campaign campaign = requireCampaign(campaignId);
        PlayerCharacter character = new PlayerCharacter(
                campaign, name, ancestry, characterClass, level, background, alignment,
                hitPoints, maxHitPoints, armorClass, movement);
        character.setProficiencyBonus(proficiencyBonus);
        return characters.save(character);
    }

    /**
     * Returns every character owned by the given campaign.
     *
     * @param campaignId the owning campaign
     * @return all player characters owned by the campaign (never {@code null})
     */
    public List<PlayerCharacter> list(Long campaignId) {
        return characters.findByCampaign(requireCampaign(campaignId));
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    /**
     * Returns a single character, verified to belong to the given campaign.
     *
     * @param campaignId the owning campaign
     * @param characterId the character to look up
     * @return the player character
     */
    public PlayerCharacter get(Long campaignId, Long characterId) {
        return requireCharacter(campaignId, characterId);
    }

    // ------------------------------------------------------------------
    // Identity updates
    // ------------------------------------------------------------------

    /**
     * Replaces the mutable identity fields of an existing character.
     *
     * @param campaignId the owning campaign
     * @param characterId the character to update
     * @param name the new name, or {@code null} to leave unchanged
     * @param ancestry the new ancestry, or {@code null} to leave unchanged
     * @param characterClass the new class, or {@code null} to leave unchanged
     * @param level the new level, or {@code null} to leave unchanged
     * @param background the new background, or {@code null} to leave unchanged
     * @param alignment the new alignment, or {@code null} to leave unchanged
     * @return the updated player character
     */
    public PlayerCharacter updateIdentity(Long campaignId, Long characterId, String name,
                                          String ancestry, String characterClass, Integer level,
                                          String background, String alignment) {
        PlayerCharacter character = requireCharacter(campaignId, characterId);
        if (name != null) {
            character.setName(name);
        }
        if (ancestry != null) {
            character.setAncestry(ancestry);
        }
        if (characterClass != null) {
            character.setCharacterClass(characterClass);
        }
        if (level != null) {
            character.setLevel(level);
        }
        if (background != null) {
            character.setBackground(background);
        }
        if (alignment != null) {
            character.setAlignment(alignment);
        }
        return characters.save(character);
    }

    // ------------------------------------------------------------------
    // Combat statistics and resource updates
    // ------------------------------------------------------------------

    /**
     * Replaces the combat-statistic fields of an existing character.
     *
     * @param campaignId the owning campaign
     * @param characterId the character to update
     * @param hitPoints the new hit points, or {@code null} to leave unchanged
     * @param maxHitPoints the new maximum hit points, or {@code null} to leave unchanged
     * @param armorClass the new armor class, or {@code null} to leave unchanged
     * @param movement the new movement, or {@code null} to leave unchanged
     * @param proficiencyBonus the new proficiency bonus, or {@code null} to leave unchanged
     * @return the updated player character
     */
    public PlayerCharacter updateCombatStats(Long campaignId, Long characterId, Integer hitPoints,
                                             Integer maxHitPoints, Integer armorClass,
                                             Integer movement, Integer proficiencyBonus) {
        PlayerCharacter character = requireCharacter(campaignId, characterId);
        if (hitPoints != null) {
            character.setHitPoints(hitPoints);
        }
        if (maxHitPoints != null) {
            character.setMaxHitPoints(maxHitPoints);
        }
        if (armorClass != null) {
            character.setArmorClass(armorClass);
        }
        if (movement != null) {
            character.setMovement(movement);
        }
        if (proficiencyBonus != null) {
            character.setProficiencyBonus(proficiencyBonus);
        }
        return characters.save(character);
    }

    /**
     * Sets the character's current hit points directly. Persisted so the value
     * survives across requests and sessions.
     *
     * @param campaignId the owning campaign
     * @param characterId the character to update
     * @param hitPoints the new hit points
     * @return the updated player character
     */
    public PlayerCharacter setHitPoints(Long campaignId, Long characterId, int hitPoints) {
        PlayerCharacter character = requireCharacter(campaignId, characterId);
        character.setHitPoints(hitPoints);
        return characters.save(character);
    }

    /**
     * Sets the character's maximum hit points directly.
     *
     * @param campaignId the owning campaign
     * @param characterId the character to update
     * @param maxHitPoints the new maximum hit points
     * @return the updated player character
     */
    public PlayerCharacter setMaxHitPoints(Long campaignId, Long characterId, int maxHitPoints) {
        PlayerCharacter character = requireCharacter(campaignId, characterId);
        character.setMaxHitPoints(maxHitPoints);
        return characters.save(character);
    }

    // ------------------------------------------------------------------
    // Removal
    // ------------------------------------------------------------------

    /**
     * Deletes a character, verified to belong to the given campaign.
     *
     * @param campaignId the owning campaign
     * @param characterId the character to remove
     */
    public void delete(Long campaignId, Long characterId) {
        requireCharacter(campaignId, characterId);
        characters.deleteById(characterId);
    }
}
