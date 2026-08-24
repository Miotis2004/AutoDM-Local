package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A join row that names one character or NPC as involved in a {@link Scene}.
 *
 * <p>A scene can involve several player characters and several NPCs. Rather than a
 * many-to-many collection on a single target type, each involvement is its own row that
 * records both the kind of the involved character ({@link #involvedKind}) and that
 * character's id ({@link #involvedId}). A player character and an NPC may share an id
 * space without confusion because the {@code involved_kind} column disambiguates them.</p>
 *
 * <p>Every row belongs to exactly one scene and is therefore owned by exactly one campaign,
 * so the set of involved characters reloads across sessions within a campaign.</p>
 */
@Entity
@Table(
        name = "scene_involved_characters",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_scene_involved",
                columnNames = {"scene_id", "involved_kind", "involved_id"}))
public class SceneInvolvedCharacter {

    /** Which kind of character a join row points at. */
    public enum InvolvedKind {
        /** A player character of the owning campaign. */
        PLAYER_CHARACTER,
        /** A non-player character of the owning campaign. */
        NPC
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "scene_id", nullable = false)
    private Scene scene;

    /** Whether the involved character is a player character or an NPC. */
    @Enumerated(EnumType.STRING)
    @Column(name = "involved_kind", nullable = false)
    private InvolvedKind involvedKind;

    /** The id of the involved player character or NPC. */
    @Column(name = "involved_id", nullable = false)
    private Long involvedId;

    public SceneInvolvedCharacter() {
        /* Required by JPA. */
    }

    public SceneInvolvedCharacter(Scene scene, InvolvedKind involvedKind, Long involvedId) {
        this.scene = scene;
        this.involvedKind = involvedKind;
        this.involvedId = involvedId;
    }

    public Long getId() {
        return id;
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public InvolvedKind getInvolvedKind() {
        return involvedKind;
    }

    public void setInvolvedKind(InvolvedKind involvedKind) {
        this.involvedKind = involvedKind;
    }

    public Long getInvolvedId() {
        return involvedId;
    }

    public void setInvolvedId(Long involvedId) {
        this.involvedId = involvedId;
    }
}
