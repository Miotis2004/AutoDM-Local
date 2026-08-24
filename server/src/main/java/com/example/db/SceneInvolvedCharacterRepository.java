package com.example.db;

import com.example.domain.Scene;
import com.example.domain.SceneInvolvedCharacter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access abstraction for the {@link SceneInvolvedCharacter} join rows that name the
 * characters and NPCs involved in each {@link Scene}.
 *
 * <p>Rows are campaign-scoped through their owning scene, so lookups are organised by scene
 * and by involved kind. This repository contains no business logic and only depends on
 * Spring Data JPA.</p>
 */
@Repository
public interface SceneInvolvedCharacterRepository
        extends JpaRepository<SceneInvolvedCharacter, Long> {

    /**
     * @param scene the scene whose involved characters are wanted
     * @return the involvement rows for the scene, in insertion order
     */
    List<SceneInvolvedCharacter> findBySceneOrderByIdAsc(Scene scene);

    /**
     * @param scene     the scene to search within
     * @param involvedKind the kind of involved character to find
     * @return the involvement rows of the given kind for the scene
     */
    List<SceneInvolvedCharacter> findBySceneAndInvolvedKindOrderByIdAsc(
            Scene scene, SceneInvolvedCharacter.InvolvedKind involvedKind);

    /**
     * @param sceneId the owning scene id
     * @return the involvement rows for the scene with the given id
     */
    List<SceneInvolvedCharacter> findBySceneId(Long sceneId);
}
