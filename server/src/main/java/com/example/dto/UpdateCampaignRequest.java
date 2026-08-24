package com.example.dto;

import com.example.domain.CampaignStatus;

import java.time.LocalDate;

/**
 * Request body for editing an existing {@link com.example.domain.Campaign}.
 *
 * <p>Every field is optional; a field that is omitted (or explicitly {@code null})
 * is left untouched. Only the supplied attributes are applied to the campaign by
 * the service.</p>
 */
public class UpdateCampaignRequest {

    private String title;
    private String description;
    private CampaignStatus status;
    private LocalDate lastPlayedAt;
    private String notes;

    public UpdateCampaignRequest() {
        /* Required by Jackson. */
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

    public LocalDate getLastPlayedAt() {
        return lastPlayedAt;
    }

    public void setLastPlayedAt(LocalDate lastPlayedAt) {
        this.lastPlayedAt = lastPlayedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
