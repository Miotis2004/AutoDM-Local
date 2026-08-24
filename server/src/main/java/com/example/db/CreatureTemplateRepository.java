package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.CreatureTemplate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link CreatureTemplate} entities.
 *
 * <p>Every template is owned by exactly one {@link Campaign}, so this repository is
 * organised around the campaign that owns a catalogue of templates. It exposes the
 * data access operations needed to persist and retrieve creature templates; it
 * contains no business logic and only depends on Spring Data JPA.</p>
 */
@Repository
public interface CreatureTemplateRepository extends JpaRepository<CreatureTemplate, Long> {

    /**
     * Finds every creature template owned by the given campaign, ordered by name.
     *
     * @param campaign the owning campaign
     * @return all templates owned by the campaign, ordered by name (never {@code null})
     */
    List<CreatureTemplate> findByCampaignOrderByName(Campaign campaign);

    /**
     * Finds a single template owned by the given campaign with the given name, if any.
     *
     * @param campaign the owning campaign
     * @param name     the template name to look up
     * @return the matching template, if present
     */
    Optional<CreatureTemplate> findByCampaignAndName(Campaign campaign, String name);
}
