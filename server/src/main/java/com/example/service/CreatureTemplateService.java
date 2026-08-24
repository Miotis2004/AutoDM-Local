package com.example.service;

import com.example.domain.Campaign;
import com.example.domain.CreatureTemplate;
import com.example.domain.Disposition;
import com.example.domain.Npc;
import com.example.domain.NpcRelationship;
import com.example.db.CampaignRepository;
import com.example.db.CreatureTemplateRepository;
import com.example.db.NpcRepository;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for reusable creature and enemy templates owned by a campaign, and
 * for the enemies instantiated from them.
 *
 * <p>This service is the single place where templates are created, listed, and read,
 * and where a template is instantiated into a concrete {@link Npc} enemy. Every
 * mutation resolves its owning campaign and validates that a template is owned by that
 * campaign before acting, and relies on the repository to persist so templates and
 * their instantiated enemies reload across sessions.</p>
 */
@Service
public class CreatureTemplateService {

    private final CampaignRepository campaigns;
    private final CreatureTemplateRepository templates;
    private final NpcRepository npcs;

    public CreatureTemplateService(CampaignRepository campaigns,
                                   CreatureTemplateRepository templates,
                                   NpcRepository npcs) {
        this.campaigns = campaigns;
        this.templates = templates;
        this.npcs = npcs;
    }

    // ------------------------------------------------------------------
    // Creation and listing
    // ------------------------------------------------------------------

    /**
     * Creates a new, empty-named-but-named template in the given campaign carrying the
     * given combat profile. All combat values may be {@code null}, so a template can
     * carry a partial or full profile.
     *
     * @param campaignId       the owning campaign
     * @param name             the template's name (required)
     * @param description      optional description
     * @param health           optional hit points
     * @param defense          optional armor class / defence
     * @param attack           optional attack bonus
     * @param damage           optional damage output
     * @param initiativeModifier optional initiative modifier
     * @param behaviorNotes    optional tactical or roleplay guidance
     * @return the persisted template
     */
    public CreatureTemplate createTemplate(Long campaignId,
                                           String name,
                                           String description,
                                           Integer health,
                                           Integer defense,
                                           Integer attack,
                                           Integer damage,
                                           Integer initiativeModifier,
                                           String behaviorNotes) {
        Campaign campaign = requireCampaign(campaignId);
        CreatureTemplate template = new CreatureTemplate(campaign, name);
        template.setDescription(description);
        template.setHealth(health);
        template.setDefense(defense);
        template.setAttack(attack);
        template.setDamage(damage);
        template.setInitiativeModifier(initiativeModifier);
        template.setBehaviorNotes(behaviorNotes);
        return templates.save(template);
    }

    /**
     * Lists every template owned by the given campaign, ordered by name.
     */
    public List<CreatureTemplate> listTemplates(Long campaignId) {
        return templates.findByCampaignOrderByName(requireCampaign(campaignId));
    }

    /**
     * Updates the mutable fields of an existing template in the given campaign.
     * All combat values may be {@code null}, so a template may be updated to a
     * partial or full profile. The template's name is left untouched; callers
     * that want to rename a template instantiate an enemy from it instead.
     *
     * @param campaignId       the owning campaign
     * @param templateId       the template to update
     * @param description      optional description
     * @param health           optional hit points
     * @param defense          optional armor class / defence
     * @param attack           optional attack bonus
     * @param damage           optional damage output
     * @param initiativeModifier optional initiative modifier
     * @param behaviorNotes    optional tactical or roleplay guidance
     * @return the persisted template
     */
    public CreatureTemplate updateTemplate(Long campaignId,
                                           Long templateId,
                                           String description,
                                           Integer health,
                                           Integer defense,
                                           Integer attack,
                                           Integer damage,
                                           Integer initiativeModifier,
                                           String behaviorNotes) {
        CreatureTemplate template = requireOwnedTemplate(campaignId, templateId);
        template.setDescription(description);
        template.setHealth(health);
        template.setDefense(defense);
        template.setAttack(attack);
        template.setDamage(damage);
        template.setInitiativeModifier(initiativeModifier);
        template.setBehaviorNotes(behaviorNotes);
        return templates.save(template);
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    public CreatureTemplate getTemplate(Long campaignId, Long templateId) {
        return requireOwnedTemplate(campaignId, templateId);
    }

    // ------------------------------------------------------------------
    // Instantiation
    // ------------------------------------------------------------------

    /**
     * Instantiates a concrete enemy {@link Npc} from a template in the same campaign.
     * The new NPC inherits the template's name and description and, when the template
     * carries a combat profile, its health (as hit points and maximum hit points),
     * defence (as armor class), attack, damage, and initiative modifier.
     *
     * <p>The instantiated enemy starts {@code active} with a {@link Disposition#HOSTILE}
     * disposition and a {@link NpcRelationship#KNOWN} relationship to the party.</p>
     *
     * @param campaignId   the owning campaign (shared with the template and the enemy)
     * @param templateId   the template to instantiate
     * @param enemyName     optional override for the enemy's name; falls back to the
     *                      template's name when {@code null} or empty
     * @return the persisted enemy NPC
     */
    public Npc instantiateEnemy(Long campaignId, Long templateId, String enemyName) {
        CreatureTemplate template = requireOwnedTemplate(campaignId, templateId);
        String name = (enemyName == null || enemyName.isBlank()) ? template.getName() : enemyName;
        Npc enemy = new Npc(template.getCampaign(), name, Disposition.HOSTILE, NpcRelationship.KNOWN);
        enemy.setDescription(template.getDescription());
        enemy.setHitPoints(template.getHealth());
        enemy.setMaxHitPoints(template.getHealth());
        enemy.setArmorClass(template.getDefense());
        enemy.setAttack(template.getAttack());
        enemy.setDamage(template.getDamage());
        enemy.setInitiativeBonus(template.getInitiativeModifier());
        return npcs.save(enemy);
    }

    // ------------------------------------------------------------------
    // Removal
    // ------------------------------------------------------------------

    public void removeTemplate(Long campaignId, Long templateId) {
        CreatureTemplate template = requireOwnedTemplate(campaignId, templateId);
        templates.delete(template);
    }

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private CreatureTemplate requireOwnedTemplate(Long campaignId, Long templateId) {
        return templates.findById(templateId)
                .filter(t -> t.getCampaign() != null && t.getCampaign().getId() != null
                        && t.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No creature template with id " + templateId));
    }
}
