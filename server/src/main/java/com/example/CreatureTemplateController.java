package com.example;

import com.example.domain.CreatureTemplate;
import com.example.domain.Npc;
import com.example.service.CreatureTemplateService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST surface for reusable creature and enemy templates and for the enemies
 * instantiated from them.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link CreatureTemplateService} call. Template creation, listing, and instantiation
 * logic lives in the service, and persistence is what lets templates and their
 * instantiated enemies reload across sessions.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class CreatureTemplateController {

    private final CreatureTemplateService templates;

    public CreatureTemplateController(CreatureTemplateService templates) {
        this.templates = templates;
    }

    // ------------------------------------------------------------------
    // Template creation and listing
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/creature-templates")
    public CreatureTemplate createTemplate(@PathVariable Long campaignId,
                                           @RequestParam String name,
                                           @RequestParam(required = false) String description,
                                           @RequestParam(required = false) Integer health,
                                           @RequestParam(required = false) Integer defense,
                                           @RequestParam(required = false) Integer attack,
                                           @RequestParam(required = false) Integer damage,
                                           @RequestParam(required = false) Integer initiativeModifier,
                                           @RequestParam(required = false) String behaviorNotes) {
        return templates.createTemplate(campaignId, name, description, health, defense, attack,
                damage, initiativeModifier, behaviorNotes);
    }

    @GetMapping("/{campaignId}/creature-templates")
    public List<CreatureTemplate> listTemplates(@PathVariable Long campaignId) {
        return templates.listTemplates(campaignId);
    }

    // ------------------------------------------------------------------
    // Template reading
    // ------------------------------------------------------------------

    @GetMapping("/{campaignId}/creature-templates/{templateId}")
    public CreatureTemplate getTemplate(@PathVariable Long campaignId, @PathVariable Long templateId) {
        return templates.getTemplate(campaignId, templateId);
    }

    // ------------------------------------------------------------------
    // Instantiating an enemy from a template
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/creature-templates/{templateId}/instantiate")
    public Npc instantiateEnemy(@PathVariable Long campaignId, @PathVariable Long templateId,
                                @RequestParam(required = false) String enemyName) {
        return templates.instantiateEnemy(campaignId, templateId, enemyName);
    }

    // ------------------------------------------------------------------
    // Template update
    // ------------------------------------------------------------------

    @PutMapping("{campaignId}/creature-templates/{templateId}")
    public CreatureTemplate updateTemplate(@PathVariable Long campaignId,
                                           @PathVariable Long templateId,
                                           @RequestParam(required = false) String description,
                                           @RequestParam(required = false) Integer health,
                                           @RequestParam(required = false) Integer defense,
                                           @RequestParam(required = false) Integer attack,
                                           @RequestParam(required = false) Integer damage,
                                           @RequestParam(required = false) Integer initiativeModifier,
                                           @RequestParam(required = false) String behaviorNotes) {
        return templates.updateTemplate(campaignId, templateId, description, health, defense,
                attack, damage, initiativeModifier, behaviorNotes);
    }

    // ------------------------------------------------------------------
    // Template removal
    // ------------------------------------------------------------------

    @DeleteMapping("/{campaignId}/creature-templates/{templateId}")
    public void removeTemplate(@PathVariable Long campaignId, @PathVariable Long templateId) {
        templates.removeTemplate(campaignId, templateId);
    }
}
