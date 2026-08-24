package com.example.dto;

/**
 * Small, purpose-built value objects for the dashboard read model.
 *
 * <p>The dashboard summarises the state of one campaign at a glance. Rather than re-using the
 * full {@link CampaignDto}, {@code PlayerCharacter}, {@code Quest}, {@code Encounter}, and
 * {@code CampaignEvent} types, the dashboard exposes compact projections that carry only the
 * fields a glance view needs. Each projection below is a thin, immutable snapshot serialised to
 * JSON for the front-end dashboard.</p>
 */
public final class Summary {

    private Summary() {
        /* Value barrel: no instances. */
    }

    /**
     * A compact projection of a {@link com.example.domain.PlayerCharacter}.
     */
    public static final class Character {
        private Long id;
        private String name;
        private String characterClass;
        private int level;
        private int hitPoints;
        private int maxHitPoints;

        public Character() {
            /* Required by Jackson. */
        }

        public Character(Long id, String name, String characterClass, int level,
                         int hitPoints, int maxHitPoints) {
            this.id = id;
            this.name = name;
            this.characterClass = characterClass;
            this.level = level;
            this.hitPoints = hitPoints;
            this.maxHitPoints = maxHitPoints;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCharacterClass() {
            return characterClass;
        }

        public int getLevel() {
            return level;
        }

        public int getHitPoints() {
            return hitPoints;
        }

        public int getMaxHitPoints() {
            return maxHitPoints;
        }
    }

    /**
     * A compact projection of a {@link com.example.domain.Quest}.
     */
    public static final class Quest {
        private Long id;
        private String title;
        private String status;

        public Quest() {
            /* Required by Jackson. */
        }

        public Quest(Long id, String title, String status) {
            this.id = id;
            this.title = title;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getStatus() {
            return status;
        }
    }

    /**
     * A compact projection of a {@link com.example.domain.Encounter}.
     */
    public static final class Encounter {
        private Long id;
        private String name;
        private String status;

        public Encounter() {
            /* Required by Jackson. */
        }

        public Encounter(Long id, String name, String status) {
            this.id = id;
            this.name = name;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }
    }

    /**
     * A compact projection of a {@link com.example.domain.CampaignEvent}.
     */
    public static final class Event {
        private Long id;
        private String eventType;
        private String description;
        private java.time.LocalDateTime timestamp;

        public Event() {
            /* Required by Jackson. */
        }

        public Event(Long id, String eventType, String description,
                     java.time.LocalDateTime timestamp) {
            this.id = id;
            this.eventType = eventType;
            this.description = description;
            this.timestamp = timestamp;
        }

        public Long getId() {
            return id;
        }

        public String getEventType() {
            return eventType;
        }

        public String getDescription() {
            return description;
        }

        public java.time.LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
