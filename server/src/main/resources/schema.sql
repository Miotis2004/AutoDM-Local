-- AutoDM SQLite schema
-- Maintained as source: edit this file to version the database schema and run
-- `mvn clean` (or delete autodm.db) to re-apply. Loaded by Hibernate at bootstrap
-- via spring.jpa.hibernate.hbm2ddl.import_files.

CREATE TABLE IF NOT EXISTS messages (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    sender      VARCHAR(255) NOT NULL,
    recipient   VARCHAR(255) NOT NULL,
    body        TEXT NOT NULL,
    created_at  DATETIME NOT NULL
);

-- Campaigns. Each campaign owns an isolated game state stored in the `state`
-- column so that games never share state across campaigns.
CREATE TABLE IF NOT EXISTS campaigns (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    title          VARCHAR(255) NOT NULL,
    description    VARCHAR(255),
    status         VARCHAR(32) NOT NULL,
    created_at     DATE NOT NULL,
    last_played_at DATE,
    notes          TEXT,
    state          TEXT
);

-- Player characters. Each player character is owned by exactly one campaign
-- (the many side of a campaign's one-to-many party). Identity and combat
-- statistics live in this row; ability scores are inlined here while skills and
-- saving throws are stored in their own per-character tables.
CREATE TABLE IF NOT EXISTS player_characters (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id       INTEGER NOT NULL,
    name              VARCHAR(255) NOT NULL,
    ancestry          VARCHAR(100) NOT NULL,
    character_class   VARCHAR(100) NOT NULL,
    level             INTEGER NOT NULL,
    background        VARCHAR(100) NOT NULL,
    alignment         VARCHAR(100) NOT NULL,
    hit_points        INTEGER NOT NULL,
    max_hit_points    INTEGER NOT NULL,
    armor_class       INTEGER NOT NULL,
    movement          INTEGER NOT NULL,
    proficiency_bonus INTEGER NOT NULL,
    ability_strength  INTEGER NOT NULL,
    ability_dexterity INTEGER NOT NULL,
    ability_constitution INTEGER NOT NULL,
    ability_intelligence INTEGER NOT NULL,
    ability_wisdom    INTEGER NOT NULL,
    ability_charisma  INTEGER NOT NULL,
    CONSTRAINT fk_player_character_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Per-character saving throws, one row per ability the character can throw on.
CREATE TABLE IF NOT EXISTS player_character_saving_throws (
    player_character_id   INTEGER NOT NULL,
    saving_ability        VARCHAR(10) NOT NULL,
    saving_bonus          INTEGER NOT NULL,
    saving_proficient     INTEGER NOT NULL,
    CONSTRAINT fk_pc_saving_throw_player
        FOREIGN KEY (player_character_id) REFERENCES player_characters(id)
);

-- Per-character skills, one row per skill, each carrying its bonus and status.
CREATE TABLE IF NOT EXISTS player_character_skills (
    player_character_id   INTEGER NOT NULL,
    skill_name            VARCHAR(100) NOT NULL,
    skill_bonus           INTEGER NOT NULL,
    skill_proficient      INTEGER NOT NULL,
    CONSTRAINT fk_pc_skill_player
        FOREIGN KEY (player_character_id) REFERENCES player_characters(id)
);

-- Character vitals: persistent health, temporary health, and life-state flags for a
-- campaign. Health and the death/unconscious flags survive across sessions because
-- they are stored here rather than only in transient memory.
CREATE TABLE IF NOT EXISTS character_vitals (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id        INTEGER NOT NULL,
    hit_points         INTEGER NOT NULL,
    max_hit_points     INTEGER NOT NULL,
    temporary_health   INTEGER NOT NULL,
    unconscious        INTEGER NOT NULL,
    dead               INTEGER NOT NULL,
    CONSTRAINT fk_vitals_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Limited-use abilities: a named power with a number of remaining uses out of its
-- maximum, plus which rests reset its uses.
CREATE TABLE IF NOT EXISTS limited_abilities (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id           INTEGER NOT NULL,
    name                  VARCHAR(255) NOT NULL,
    max_uses              INTEGER NOT NULL,
    uses_remaining        INTEGER NOT NULL,
    recovers_on_long_rest INTEGER NOT NULL,
    recovers_on_short_rest INTEGER NOT NULL,
    CONSTRAINT fk_ability_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Spell / power resources: remaining spell slots or power points, optionally tied to
-- a slot level, and whether the resource is maintained by concentration.
CREATE TABLE IF NOT EXISTS spell_power_resources (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id        INTEGER NOT NULL,
    name               VARCHAR(255) NOT NULL,
    max_points         INTEGER NOT NULL,
    points_remaining   INTEGER NOT NULL,
    slot_level         INTEGER,
    concentration      INTEGER NOT NULL,
    CONSTRAINT fk_spell_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Ammunition: a stack of a single ammunition type held by the campaign.
CREATE TABLE IF NOT EXISTS ammunition (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id   INTEGER NOT NULL,
    ammo_type     VARCHAR(100) NOT NULL,
    count         INTEGER NOT NULL,
    CONSTRAINT fk_ammunition_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Consumables: a stack of a usable item, with an optional category.
CREATE TABLE IF NOT EXISTS consumables (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id INTEGER NOT NULL,
    name        VARCHAR(255) NOT NULL,
    category    VARCHAR(100),
    count       INTEGER NOT NULL,
    CONSTRAINT fk_consumable_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Currency: an amount of a single coin denomination held by the campaign.
CREATE TABLE IF NOT EXISTS currency (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id     INTEGER NOT NULL,
    currency_unit   VARCHAR(10) NOT NULL,
    amount          INTEGER NOT NULL,
    CONSTRAINT fk_currency_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Conditions: a temporary effect applied to the campaign, with source,
-- concentration, stackability, and optional remaining duration in rounds.
CREATE TABLE IF NOT EXISTS conditions (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id        INTEGER NOT NULL,
    name               VARCHAR(255) NOT NULL,
    condition_kind     VARCHAR(32) NOT NULL,
    source             TEXT,
    concentration      INTEGER NOT NULL,
    stackable          INTEGER NOT NULL,
    remaining_rounds   INTEGER,
    CONSTRAINT fk_condition_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- World model. A campaign owns its own world so games never share place data.
--
-- Regions are the top level of the world's spatial hierarchy. Locations nest in
-- regions; settlements and points of interest each reference one Location.
-- The Location is the canonical discoverable "place" node: it stores the
-- description and discovered/undiscovered state, and it is what travel routes
-- connect and what the party's current location points at.
CREATE TABLE IF NOT EXISTS world_regions (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id   INTEGER NOT NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    CONSTRAINT fk_world_region_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

CREATE TABLE IF NOT EXISTS world_locations (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id   INTEGER NOT NULL,
    region_id     INTEGER,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    discovered    INTEGER NOT NULL,
    latitude      DOUBLE,
    longitude     DOUBLE,
    CONSTRAINT fk_world_location_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_world_location_region
        FOREIGN KEY (region_id) REFERENCES world_regions(id)
);

-- Settlements: a location where people live or gather, with a size type and
-- reported population. The backing Location carries its description and discovery.
CREATE TABLE IF NOT EXISTS world_settlements (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id    INTEGER NOT NULL,
    location_id    INTEGER NOT NULL,
    type           VARCHAR(32) NOT NULL,
    population     INTEGER NOT NULL,
    CONSTRAINT fk_world_settlement_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_world_settlement_location
        FOREIGN KEY (location_id) REFERENCES world_locations(id)
);

-- Points of interest: a notable building or spot within a settlement. The
-- backing Location carries its description and discovery.
CREATE TABLE IF NOT EXISTS world_points_of_interest (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id   INTEGER NOT NULL,
    location_id   INTEGER NOT NULL,
    settlement_id INTEGER,
    category      VARCHAR(32) NOT NULL,
    CONSTRAINT fk_poi_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_poi_location
        FOREIGN KEY (location_id) REFERENCES world_locations(id),
    CONSTRAINT fk_poi_settlement
        FOREIGN KEY (settlement_id) REFERENCES world_settlements(id)
);

-- Travel routes: a directed connection between two locations, with distance and
-- estimated travel time. Unique per (campaign, from, to) so a route is not
-- duplicated.
CREATE TABLE IF NOT EXISTS world_travel_routes (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id      INTEGER NOT NULL,
    from_location_id INTEGER NOT NULL,
    to_location_id   INTEGER NOT NULL,
    distance_km      DOUBLE,
    travel_minutes   INTEGER,
    CONSTRAINT fk_route_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_route_from
        FOREIGN KEY (from_location_id) REFERENCES world_locations(id),
    CONSTRAINT fk_route_to
        FOREIGN KEY (to_location_id) REFERENCES world_locations(id),
    CONSTRAINT uq_world_route
        UNIQUE (campaign_id, from_location_id, to_location_id)
);

-- The party's current location for a campaign. Exactly one per campaign
-- (campaign_id is the primary key), pointing at any Location in the world. This
-- is what lets the game know where the party is at any given moment.
CREATE TABLE IF NOT EXISTS world_party_locations (
    campaign_id INTEGER PRIMARY KEY,
    location_id INTEGER NOT NULL,
    CONSTRAINT fk_party_location_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_party_location_location
        FOREIGN KEY (location_id) REFERENCES world_locations(id)
);

-- Non-player characters. Each NPC is owned by exactly one campaign so a character
-- exists only inside the game that created it. Identity and story fields (name,
-- description, role, disposition, faction, location, active state, relationship,
-- notes) live in this row. Combat statistics are entirely optional and are stored
-- inline here as nullable columns, so a purely social NPC keeps no combat data.
CREATE TABLE IF NOT EXISTS npcs (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id            INTEGER NOT NULL,
    name                   VARCHAR(255) NOT NULL,
    description            TEXT,
    role                   VARCHAR(255),
    disposition            VARCHAR(32) NOT NULL,
    faction                VARCHAR(255),
    location_id            INTEGER,
    active                 INTEGER NOT NULL,
    relationship           VARCHAR(32) NOT NULL,
    notes                  TEXT,
    hit_points             INTEGER,
    max_hit_points         INTEGER,
    armor_class            INTEGER,
    movement               INTEGER,
    proficiency_bonus      INTEGER,
    ability_strength       INTEGER,
    ability_dexterity      INTEGER,
    ability_constitution   INTEGER,
    ability_intelligence   INTEGER,
    ability_wisdom         INTEGER,
    ability_charisma       INTEGER,
    attack                 INTEGER,
    damage                 INTEGER,
    initiative_bonus       INTEGER,
    CONSTRAINT fk_npc_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_npc_location
        FOREIGN KEY (location_id) REFERENCES world_locations(id)
);

-- Optional NPC saving throws, one row per ability an NPC can be thrown on. Present
-- only when the NPC has combat statistics that include saving throws.
CREATE TABLE IF NOT EXISTS npc_saving_throws (
    npc_id           INTEGER NOT NULL,
    saving_ability   VARCHAR(10) NOT NULL,
    saving_bonus     INTEGER NOT NULL,
    saving_proficient INTEGER NOT NULL,
    CONSTRAINT fk_npc_saving_throw_npc
        FOREIGN KEY (npc_id) REFERENCES npcs(id)
);

-- Factions: an organization, group, or power that shares a common purpose within a
-- campaign. Each faction is owned by exactly one campaign so it exists only inside
-- the game that created it. Identity and nature (name, description, disposition,
-- reputation, notes) live in this row.
CREATE TABLE IF NOT EXISTS factions (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id    INTEGER NOT NULL,
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    disposition    VARCHAR(32) NOT NULL,
    reputation     VARCHAR(32) NOT NULL,
    notes          TEXT,
    CONSTRAINT fk_faction_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Faction-to-faction relationships. Each row records how one faction regards another
-- faction in the same campaign (the acting faction), naming the related faction and
-- the relationship that describes the bond. Unique per (acting faction, related
-- faction, relationship) so a link is not duplicated.
CREATE TABLE IF NOT EXISTS faction_relationships (
    faction_id         INTEGER NOT NULL,
    related_faction_id INTEGER NOT NULL,
    relationship       VARCHAR(32) NOT NULL,
    CONSTRAINT fk_faction_relationship_faction
        FOREIGN KEY (faction_id) REFERENCES factions(id),
    CONSTRAINT fk_faction_relationship_related
        FOREIGN KEY (related_faction_id) REFERENCES factions(id),
    CONSTRAINT uq_faction_relationship
        UNIQUE (faction_id, related_faction_id, relationship)
);

-- Quests: a tracked strand of campaign story. Each quest is owned by exactly one
-- campaign so a quest exists only inside the game that created it. Its status
-- (ACTIVE, COMPLETED, or FAILED), giver, rewards, related locations, and notes live
-- in this row.
CREATE TABLE IF NOT EXISTS quests (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id INTEGER NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(16) NOT NULL,
    giver       VARCHAR(255),
    rewards     TEXT,
    notes       TEXT,
    CONSTRAINT fk_quest_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Quest objectives. One row per objective of a quest, each carrying its own
-- independent completion tracking (target vs. current progress and status). Owned by
-- exactly one quest, and therefore by exactly one campaign. Unique per (quest,
-- description) so an objective is not duplicated within a quest.
CREATE TABLE IF NOT EXISTS quest_objectives (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id    INTEGER NOT NULL,
    quest_id       INTEGER NOT NULL,
    description    VARCHAR(255) NOT NULL,
    target_count   INTEGER NOT NULL,
    current_count  INTEGER NOT NULL DEFAULT 0,
    status         VARCHAR(16) NOT NULL,
    CONSTRAINT fk_objective_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_objective_quest
        FOREIGN KEY (quest_id) REFERENCES quests(id),
    CONSTRAINT uq_quest_objective
        UNIQUE (quest_id, description)
);

-- Quest-to-location relationships. Each row records a related location for a quest in
-- the same campaign, referenced by location id.
CREATE TABLE IF NOT EXISTS quest_related_locations (
    quest_id      INTEGER NOT NULL,
    location_id   INTEGER NOT NULL,
    CONSTRAINT fk_quest_location_ref_quest
        FOREIGN KEY (quest_id) REFERENCES quests(id),
    CONSTRAINT fk_quest_location_ref_location
        FOREIGN KEY (location_id) REFERENCES world_locations(id)
);

-- Inventory items. Each row is a holding of a single item owned by one owner: the
-- owning campaign (a shared, campaign-wide stash) or a single player character (a
-- hero's goods). Category (weapon, armor, consumable, quest item, miscellaneous),
-- quantity, value, equipped state, and description live in this row, so a transferred
-- stack carries its own attributes with it. owner_kind names which of the two owner
-- columns is in use; owner_id holds that owner's id.
CREATE TABLE IF NOT EXISTS inventory_items (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id   INTEGER NOT NULL,
    name          VARCHAR(255) NOT NULL,
    category      VARCHAR(32) NOT NULL,
    quantity      INTEGER NOT NULL,
    value         INTEGER NOT NULL,
    equipped      INTEGER NOT NULL,
    description   TEXT,
    owner_kind    VARCHAR(32) NOT NULL,
    owner_id      INTEGER NOT NULL,
    CONSTRAINT fk_inventory_item_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Inventory transfers. Each row is an immutable record of one hand-off of a held item
-- (quantity) from one owner to another within the same campaign. The record never
-- mutates: applying a transfer changes the two holdings' quantities, while this row
-- only records that the move happened and when.
CREATE TABLE IF NOT EXISTS inventory_transfers (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id     INTEGER NOT NULL,
    item_id         INTEGER NOT NULL,
    from_owner_kind VARCHAR(32) NOT NULL,
    from_owner_id   INTEGER NOT NULL,
    to_owner_kind   VARCHAR(32) NOT NULL,
    to_owner_id     INTEGER NOT NULL,
    quantity        INTEGER NOT NULL,
    transferred_at  DATETIME NOT NULL,
    CONSTRAINT fk_inventory_transfer_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_inventory_transfer_item
        FOREIGN KEY (item_id) REFERENCES inventory_items(id)
);

-- Play sessions. Each session is owned by exactly one campaign so a session exists
-- only inside the game that created it. It records when the game was started
-- (start_time) and, optionally, when it was ended (end_time); its status
-- (ACTIVE or ENDED) tracks whether the session is still open; and its covered events
-- live in the session_events table. Session history therefore survives across
-- application restarts within a campaign.
CREATE TABLE IF NOT EXISTS sessions (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id  INTEGER NOT NULL,
    start_time   DATETIME NOT NULL,
    end_time     DATETIME,
    status       VARCHAR(16) NOT NULL,
    CONSTRAINT fk_session_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Session-to-event references. Each row records one in-game event referenced by a
-- session in the same campaign, named by event id and a human-readable label.
CREATE TABLE IF NOT EXISTS session_events (
    session_id   INTEGER NOT NULL,
    event_id     INTEGER NOT NULL,
    event_name   VARCHAR(255),
    CONSTRAINT fk_session_event_session
        FOREIGN KEY (session_id) REFERENCES sessions(id)
);

-- Campaign events. Each row is an immutable record of one significant event that
-- happened within a campaign in the same campaign (a session start, a location
-- entry, a discovery, a combat, damage, an item acquisition, a quest or
-- relationship change, a session end, and so on). It records the event type and
-- when it happened, plus a human-readable description and optional structured
-- detail. Event history lives in this row so it survives across sessions and
-- application restarts within a campaign.
CREATE TABLE IF NOT EXISTS campaign_events (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id   INTEGER NOT NULL,
    event_type    VARCHAR(32) NOT NULL,
    timestamp     DATETIME NOT NULL,
    description   TEXT,
    details       TEXT,
    CONSTRAINT fk_campaign_event_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
);

-- Scenes. A single, contiguous slice of in-game time within a campaign. Encounters
-- and combat participants are anchored to a scene so a fight reloads exactly as left.
-- A scene stores its title and narrative, the location it takes place in, the active
-- encounter it references (nullable), and its status (READY, ACTIVE, or COMPLETED).
CREATE TABLE IF NOT EXISTS scenes (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id   INTEGER NOT NULL,
    title         VARCHAR(255) NOT NULL,
    narrative     TEXT,
    location_id   INTEGER,
    encounter_id  INTEGER,
    status        VARCHAR(16) NOT NULL DEFAULT 'READY',
    created_at    DATETIME,
    CONSTRAINT fk_scene_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_scene_location
        FOREIGN KEY (location_id) REFERENCES world_locations(id),
    CONSTRAINT fk_scene_encounter
        FOREIGN KEY (encounter_id) REFERENCES encounters(id)
);

-- Scene-to-character involvement. Each row names one player character or NPC as involved
-- in a scene, tagged by involved_kind and the character's id, so a scene's involved
-- characters reload across sessions within a campaign. Unique per (scene, kind, id).
CREATE TABLE IF NOT EXISTS scene_involved_characters (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    scene_id       INTEGER NOT NULL,
    involved_kind  VARCHAR(32) NOT NULL,
    involved_id    INTEGER NOT NULL,
    CONSTRAINT fk_scene_involved_character_scene
        FOREIGN KEY (scene_id) REFERENCES scenes(id),
    CONSTRAINT uq_scene_involved
        UNIQUE (scene_id, involved_kind, involved_id)
);

-- Encounters. A discrete beat of play anchored to a scene and a location. It stores
-- its status (SCHEDULED, ACTIVE, or FINISHED) and the current turn position (the
-- 1-based turn-order slot of the participant whose turn it is), so turn order and
-- whose-turn-it-is reload across sessions within a campaign.
CREATE TABLE IF NOT EXISTS encounters (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id   INTEGER NOT NULL,
    scene_id      INTEGER,
    location_id   INTEGER,
    name          VARCHAR(255),
    status        VARCHAR(32) NOT NULL,
    current_turn  INTEGER,
    created_at    DATETIME,
    CONSTRAINT fk_encounter_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_encounter_scene
        FOREIGN KEY (scene_id) REFERENCES scenes(id),
    CONSTRAINT fk_encounter_location
        FOREIGN KEY (location_id) REFERENCES world_locations(id)
);

-- Combatants. A single participant in an encounter: a hero or an enemy. It stores
-- identity, which side (PLAYER or ENEMY), hit points, maximum hit points, the
-- defeated flag, and the initiative used to establish turn order. Every combatant
-- belongs to exactly one campaign and optionally points at its encounter and scene.
CREATE TABLE IF NOT EXISTS combatants (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id      INTEGER NOT NULL,
    encounter_id     INTEGER,
    scene_id         INTEGER,
    name             VARCHAR(255) NOT NULL,
    kind             VARCHAR(16) NOT NULL,
    hit_points       INTEGER NOT NULL,
    max_hit_points   INTEGER NOT NULL,
    initiative       INTEGER,
    order            INTEGER,
    defeated         INTEGER NOT NULL,
    CONSTRAINT fk_combatant_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_combatant_encounter
        FOREIGN KEY (encounter_id) REFERENCES encounters(id),
    CONSTRAINT fk_combatant_scene
        FOREIGN KEY (scene_id) REFERENCES scenes(id)
);

-- Combat conditions: status effects applied to a combatant. Stores the effect's
-- name, description, duration (in rounds), source, and whether it is active. Owned
-- by exactly one campaign and pointing at the combatant it applies to.
CREATE TABLE IF NOT EXISTS combat_conditions (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id   INTEGER NOT NULL,
    combatant_id  INTEGER,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    duration      INTEGER,
    source        VARCHAR(255),
    active        INTEGER NOT NULL,
    created_at    DATETIME,
    CONSTRAINT fk_combat_condition_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_combat_condition_combatant
        FOREIGN KEY (combatant_id) REFERENCES combatants(id)
);
