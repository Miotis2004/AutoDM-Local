package com.example.dto;

import com.example.domain.CampaignStatus;

/**
 * Request body for creating a new {@link com.example.domain.Campaign}.
 *
 * <p>The title is required; every other field is optional and falls back to a sane
 * default when omitted (an {@link CampaignStatus#DRAFT} status and today's creation
 * date are applied by the service).</p>
 */
public class CreateCampaignRequest {

    private String title;
    private String description;
    private CampaignStatus status;
    private String notes;

    public CreateCampaignRequest() {
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
