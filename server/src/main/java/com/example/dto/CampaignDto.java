package com.example.dto;

import com.example.domain.Campaign;
import com.example.domain.CampaignStatus;

import java.time.LocalDate;

/**
 * Read model for a {@link Campaign}. Exposes the campaign metadata (title,
 * description, status, dates and notes) to REST clients without leaking the
 * internal game {@code state} or persistence concerns.
 */
public class CampaignDto {

    private Long id;
    private String title;
    private String description;
    private CampaignStatus status;
    private LocalDate createdAt;
    private LocalDate lastPlayedAt;
    private String notes;

    public CampaignDto() {
        /* Required by Jackson. */
    }

    public CampaignDto(Long id, String title, String description, CampaignStatus status,
                       LocalDate createdAt, LocalDate lastPlayedAt, String notes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.lastPlayedAt = lastPlayedAt;
        this.notes = notes;
    }

    /**
     * Builds a {@link CampaignDto} view of the given campaign.
     *
     * @param campaign the campaign to project into a DTO
     * @return a DTO carrying the campaign's public metadata
     */
    public static CampaignDto from(Campaign campaign) {
        return new CampaignDto(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getDescription(),
                campaign.getStatus(),
                campaign.getCreatedAt(),
                campaign.getLastPlayedAt(),
                campaign.getNotes());
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public LocalDate getLastPlayedAt() {
        return lastPlayedAt;
    }

    public String getNotes() {
        return notes;
    }
}
