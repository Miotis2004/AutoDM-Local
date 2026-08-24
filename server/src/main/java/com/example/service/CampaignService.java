package com.example.service;

import com.example.domain.Campaign;
import com.example.domain.CampaignStatus;
import com.example.db.CampaignRepository;
import com.example.dto.CampaignDto;
import com.example.dto.CreateCampaignRequest;
import com.example.dto.UpdateCampaignRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Optional;

/**
 * Business logic for the campaign lifecycle: creating, editing, archiving,
 * deleting, and selecting the active campaign.
 *
 * <p>This service is the single place where campaigns are managed. The controller
 * stays thin and only maps HTTP requests onto these methods. Persistence lives in
 * the {@link CampaignRepository}; the {@code state} of each campaign is intentionally
 * out of scope for campaign management.</p>
 *
 * <p>The currently selected active campaign is tracked as an in-memory value that is
 * loaded from, and persisted to, a small sidecar properties file. Keeping this out of
 * the database schema avoids a schema migration while still surviving restarts.</p>
 */
@Service
public class CampaignService {

    private final CampaignRepository campaigns;
    private final DtoValidator validator;

    /** In-memory view of the active campaign id, loaded from disk at startup. */
    private volatile Long activeCampaignId;

    private final Path activeCampaignPath;

    public CampaignService(
            CampaignRepository campaigns,
            @Value("${autodm.active.campaign.path}") Path activeCampaignPath,
            DtoValidator validator) {
        this.campaigns = campaigns;
        this.activeCampaignPath = activeCampaignPath;
        this.activeCampaignId = loadActiveId();
        this.validator = validator;
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    /**
     * Creates a new campaign with a {@link CampaignStatus#DRAFT} status stamped with
     * today's creation date.
     *
     * @param request the campaign metadata
     * @return the created campaign as a DTO
     */
    public CampaignDto create(CreateCampaignRequest request) {
        if (request == null) {
            throw new ValidationException("A campaign request is required.");
        }
        validator.requireNonBlank("Campaign title", request.getTitle());
        Campaign campaign = new Campaign();
        campaign.setTitle(request.getTitle().trim());
        campaign.setDescription(request.getDescription());
        campaign.setStatus(request.getStatus() != null ? request.getStatus() : CampaignStatus.DRAFT);
        campaign.setNotes(request.getNotes());
        campaign.setCreatedAt(LocalDate.now());
        return CampaignDto.from(campaigns.save(campaign));
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    /**
     * Returns every campaign, most recently created first.
     *
     * @return the list of campaigns (never {@code null})
     */
    public List<CampaignDto> list() {
        List<Campaign> all = campaigns.findAll();
        all.sort((a, b) -> {
            LocalDate da = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDate.MIN;
            LocalDate db = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDate.MIN;
            return db.compareTo(da);
        });
        return all.stream().map(CampaignDto::from).toList();
    }

    /**
     * Returns a single campaign.
     *
     * @param id the campaign to look up
     * @return the campaign as a DTO
     */
    public CampaignDto get(Long id) {
        return CampaignDto.from(requireCampaign(id));
    }

    // ------------------------------------------------------------------
    // Edit
    // ------------------------------------------------------------------

    /**
     * Applies the supplied, non-null attributes to an existing campaign. Any attribute
     * left {@code null} in the request is preserved unchanged.
     *
     * @param id    the campaign to edit
     * @param request the changes to apply
     * @return the updated campaign as a DTO
     */
    public CampaignDto update(Long id, UpdateCampaignRequest request) {
        Campaign campaign = requireCampaign(id);
        if (request == null) {
            return CampaignDto.from(campaign);
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            campaign.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            campaign.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            campaign.setStatus(request.getStatus());
        }
        if (request.getLastPlayedAt() != null) {
            campaign.setLastPlayedAt(request.getLastPlayedAt());
        }
        if (request.getNotes() != null) {
            campaign.setNotes(request.getNotes());
        }
        return CampaignDto.from(campaigns.save(campaign));
    }

    // ------------------------------------------------------------------
    // Archive
    // ------------------------------------------------------------------

    /**
     * Archives a campaign by moving it to the {@link CampaignStatus#ARCHIVED} status.
     *
     * @param id the campaign to archive
     * @return the archived campaign as a DTO
     */
    public CampaignDto archive(Long id) {
        Campaign campaign = requireCampaign(id);
        campaign.setStatus(CampaignStatus.ARCHIVED);
        return CampaignDto.from(campaigns.save(campaign));
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------

    /**
     * Deletes a campaign. If it was the active campaign, the active selection is
     * cleared.
     *
     * @param id the campaign to delete
     */
    public void delete(Long id) {
        Campaign campaign = requireCampaign(id);
        campaigns.delete(campaign);
        if (activeCampaignId != null && activeCampaignId.equals(id)) {
            setActiveCampaignId(null);
        }
    }

    // ------------------------------------------------------------------
    // Active selection
    // ------------------------------------------------------------------

    /**
     * Selects the given campaign as the active campaign and returns it.
     *
     * @param id the campaign to make active
     * @return the now-active campaign as a DTO
     */
    public CampaignDto setActive(Long id) {
        requireCampaign(id);
        setActiveCampaignId(id);
        return CampaignDto.from(requireCampaign(id));
    }

    /**
     * Returns the currently active campaign, if one is selected.
     *
     * @return the active campaign, or empty when no campaign is selected
     */
    public Optional<CampaignDto> getActive() {
        if (activeCampaignId == null) {
            return Optional.empty();
        }
        return findByIdDto(activeCampaignId);
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private Optional<Campaign> findByIdDtoOrNull(Long id) {
        return campaigns.findById(id);
    }

    private Optional<CampaignDto> findByIdDto(Long id) {
        return campaigns.findById(id).map(CampaignDto::from);
    }

    private Campaign requireCampaign(Long id) {
        return campaigns.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + id));
    }

    private void setActiveCampaignId(Long id) {
        this.activeCampaignId = id;
        persistActiveId(id);
    }

    private Long loadActiveId() {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(activeCampaignPath)) {
            properties.load(input);
        } catch (IOException | IllegalArgumentException | NullPointerException ignored) {
            // No sidecar file yet, or it could not be read: start with no active campaign.
            return null;
        }
        String raw = properties.getProperty("active.campaign-id");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException malformed) {
            return null;
        }
    }

    private void persistActiveId(Long id) {
        Properties properties = new Properties();
        if (id == null) {
            properties.remove("active.campaign-id");
        } else {
            properties.setProperty("active.campaign-id", String.valueOf(id));
        }
        try {
            Path parent = activeCampaignPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(activeCampaignPath)) {
                properties.store(output, "AutoDM active campaign selection");
            }
        } catch (IOException cannotPersist) {
            // In-memory selection still works for the running server; persistence is best-effort.
        }
    }

}
