package com.example.service;

import com.example.db.CampaignRepository;
import com.example.db.CombatantRepository;
import com.example.db.EncounterRepository;
import com.example.domain.Campaign;
import com.example.domain.Combatant;
import com.example.domain.CombatantKind;
import com.example.domain.Encounter;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic for {@link Combatant}s and the turn order they form within an
 * {@link Encounter}.
 *
 * <p>This service is the single place where combatants are created, attached to an
 * encounter, damaged, and ordered into a round. It owns the turn-ordering rule:
 * {@link #buildTurnOrder} assigns each participant a 1-based position ordered by
 * initiative (higher initiative acts first, ties broken by id), and
 * {@link #nextTurn} advances the encounter's current turn around the round, skipping
 * defeated combatants. All mutations are persisted through the repository so combat
 * state reloads across application restarts within a campaign.</p>
 */
@Service
public class CombatantService {

    private final CampaignRepository campaigns;
    private final CombatantRepository combatants;
    private final EncounterRepository encounters;

    public CombatantService(CampaignRepository campaigns, CombatantRepository combatants,
                            EncounterRepository encounters) {
        this.campaigns = campaigns;
        this.combatants = combatants;
        this.encounters = encounters;
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private Combatant requireOwnedCombatant(Long campaignId, Long combatantId) {
        return combatants.findById(combatantId)
                .filter(c -> c.getCampaign() != null && c.getCampaign().getId() != null
                        && c.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No combatant with id " + combatantId));
    }

    private Encounter requireOwnedEncounter(Long campaignId, Long encounterId) {
        return encounters.findById(encounterId)
                .filter(e -> e.getCampaign() != null && e.getCampaign().getId() != null
                        && e.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No encounter with id " + encounterId));
    }

    /**
     * Creates a new combatant owned by the given campaign.
     *
     * @return the created combatant (never {@code null})
     */
    public Combatant createCombatant(Long campaignId, String name, CombatantKind kind,
                                     int hitPoints, int maxHitPoints) {
        Campaign campaign = requireCampaign(campaignId);
        Combatant combatant = new Combatant(campaign, name, kind, hitPoints, maxHitPoints);
        return combatants.save(combatant);
    }

    /**
     * Attaches an existing combatant to an encounter within the same campaign.
     *
     * @return the updated combatant (never {@code null})
     */
    public Combatant addToEncounter(Long campaignId, Long combatantId, Long encounterId) {
        Combatant combatant = requireOwnedCombatant(campaignId, combatantId);
        Encounter encounter = requireOwnedEncounter(campaignId, encounterId);
        combatant.setEncounter(encounter);
        combatant.setScene(encounter.getScene());
        return combatants.save(combatant);
    }

    /**
     * Applies hit-point damage to a combatant, marking it defeated when its hit points
     * reach zero.
     *
     * @return the updated combatant (never {@code null})
     */
    public Combatant applyDamage(Long campaignId, Long combatantId, int delta) {
        Combatant combatant = requireOwnedCombatant(campaignId, combatantId);
        combatant.takeDamage(delta);
        return combatants.save(combatant);
    }

    /**
     * Restores hit points to a combatant, capped at the maximum.
     *
     * @return the updated combatant (never {@code null})
     */
    public Combatant heal(Long campaignId, Long combatantId, int amount) {
        Combatant combatant = requireOwnedCombatant(campaignId, combatantId);
        combatant.heal(amount);
        return combatants.save(combatant);
    }

    /**
     * Builds the round's turn order for a combatant's encounter, assigning each
     * active participant a 1-based {@link Combatant#getOrder() position} sorted by
     * descending initiative (ties broken by id). Combatants with no initiative yet are
     * ordered last by id.
     *
     * <p>Defeated combatants are excluded from the turn order entirely: they are not
     * assigned a position and never appear in the returned list. The encounter's
     * {@code current_turn} is reset to the first turn of the freshly built round.</p>
     *
     * @return the active combatants in turn order (never {@code null})
     */
    public List<Combatant> buildTurnOrder(Long campaignId, Long encounterId) {
        Encounter encounter = requireOwnedEncounter(campaignId, encounterId);
        List<Combatant> ordered = combatants
                .findByCampaignIdAndEncounterIdOrderByOrderAsc(campaignId, encounterId);
        List<Combatant> active = ordered.stream()
                .filter(combatant -> !combatant.isDefeated())
                .collect(Collectors.toList());
        active.sort(Comparator
                .comparingInt((Combatant combatant) -> combatant.getInitiative() == null
                        ? Integer.MIN_VALUE
                        : combatant.getInitiative())
                .thenComparing(Combatant::getId));
        int position = 1;
        for (Combatant combatant : active) {
            combatant.setOrder(position++);
            combatants.save(combatant);
        }
        encounter.setCurrentTurn(1);
        encounters.save(encounter);
        return active;
    }

    /**
     * Advances the encounter to the next turn around the round. Because the turn order
     * excludes defeated combatants, advancing never lands on a defeated one. When no
     * active participant remains the round is resolved and the current turn is cleared
     * to {@code 0}.
     *
     * @return the updated encounter with its new current turn (never {@code null})
     */
    public Encounter nextTurn(Long campaignId, Long encounterId) {
        Encounter encounter = requireOwnedEncounter(campaignId, encounterId);
        List<Combatant> active = activeCombatants(encounter);
        if (active.isEmpty()) {
            encounter.setCurrentTurn(0);
            return encounters.save(encounter);
        }
        int current = encounter.getCurrentTurn() == null ? 0 : encounter.getCurrentTurn();
        int next = current + 1;
        if (next > active.size()) {
            next = 1;
        }
        encounter.setCurrentTurn(next);
        return encounters.save(encounter);
    }

    /**
     * @return the current turn's combatant for the encounter, or empty when the round
     *         has no active participant
     */
    public Optional<Combatant> currentCombatant(Long campaignId, Long encounterId) {
        Encounter encounter = requireOwnedEncounter(campaignId, encounterId);
        Integer turn = encounter.getCurrentTurn();
        if (turn == null || turn < 1) {
            return Optional.empty();
        }
        List<Combatant> active = activeCombatants(encounter);
        if (turn > active.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(active.get(turn - 1));
    }

    /**
     * The combatants of an encounter that are still fighting, ordered by their
     * assigned turn-order position (falls back to initiative when positions have not
     * yet been set). Defeated combatants are excluded.
     *
     * @return the active combatants in turn order (never {@code null})
     */
    private List<Combatant> activeCombatants(Encounter encounter) {
        List<Combatant> all = combatants
                .findByCampaignIdAndEncounterIdOrderByOrderAsc(encounter.getCampaign().getId(),
                        encounter.getId());
        List<Combatant> active = all.stream()
                .filter(combatant -> !combatant.isDefeated())
                .collect(Collectors.toList());
        active.sort(Comparator
                .comparingInt((Combatant combatant) -> combatant.getOrder() == null
                        ? Integer.MIN_VALUE
                        : combatant.getOrder())
                .thenComparing(Combatant::getId));
        return active;
    }

    /**
     * Detects whether the encounter is complete: it is complete once every combatant on
     * at least one side ({@link CombatantKind}) has been defeated. A side that still has
     * one combatant standing leaves the encounter unfinished, and an encounter where
     * both sides have been fully defeated is treated as unresolved.
     *
     * @return {@code true} when one side is fully defeated and the encounter is over
     */
    public boolean isEncounterComplete(Long campaignId, Long encounterId) {
        return winningSide(campaignId, encounterId).isPresent();
    }

    /**
     * Identifies the side that won an encounter, when one has been decided. The winner
     * is the side that still has at least one combatant fighting while the opposing
     * side has been fully defeated. Returns empty when the encounter is still running,
     * when neither side has any combatant, or when both sides have been fully defeated.
     *
     * @return the winning side, or empty when the encounter has no winner yet
     */
    public Optional<CombatantKind> winningSide(Long campaignId, Long encounterId) {
        Encounter encounter = requireOwnedEncounter(campaignId, encounterId);
        List<Combatant> all = combatants
                .findByCampaignIdAndEncounterIdOrderByOrderAsc(campaignId, encounterId);
        List<Combatant> players = combatantsOfKind(all, CombatantKind.PLAYER);
        List<Combatant> enemies = combatantsOfKind(all, CombatantKind.ENEMY);

        boolean playersDefeated = !players.isEmpty() && players.stream().allMatch(Combatant::isDefeated);
        boolean enemiesDefeated = !enemies.isEmpty() && enemies.stream().allMatch(Combatant::isDefeated);

        if (!playersDefeated && !enemiesDefeated) {
            return Optional.empty();
        }
        if (playersDefeated && enemiesDefeated) {
            return Optional.empty();
        }
        return Optional.of(playersDefeated ? CombatantKind.ENEMY : CombatantKind.PLAYER);
    }

    private List<Combatant> combatantsOfKind(List<Combatant> combatants, CombatantKind kind) {
        return combatants.stream().filter(combatant -> kind == combatant.getKind()).collect(Collectors.toList());
    }

    public Combatant getCombatant(Long campaignId, Long combatantId) {
        return requireOwnedCombatant(campaignId, combatantId);
    }

    public List<Combatant> listCombatants(Long campaignId) {
        requireCampaign(campaignId);
        return combatants.findByCampaignOrderByOrderAsc(requireCampaign(campaignId).getId());
    }

    public List<Combatant> listCombatantsOfEncounter(Long campaignId, Long encounterId) {
        requireOwnedEncounter(campaignId, encounterId);
        return combatants.findByCampaignIdAndEncounterIdOrderByOrderAsc(campaignId, encounterId);
    }
}
