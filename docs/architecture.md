# Architecture

AutoDM is a classic two-part full-stack application:

- **Front-end (`client/`)**: An Angular single-page application rendered in the
  browser. It communicates with the back-end over REST/HTTP.
- **Back-end (`server/`)**: A Spring Boot application that exposes REST APIs and owns
  the business logic and data persistence.
- **Shared documentation (`docs/`)**: Markdown documents describing the system.

## Communication

The Angular front-end makes HTTP calls to the Spring Boot back-end. During development
the front-end dev server proxies these requests (see `client/proxy.conf.json`); in
production the back-end serves the compiled front-end assets.

## Directory Layout

```
.
├── client/      Angular application
├── server/      Spring Boot application
├── docs/        Shared documentation
└── README.md
```

## Domain Model: Non-Player Characters

Non-player characters follow the same layered design as every other entity: an
`@Entity` domain model, a Spring Data repository, a service that owns the business
logic, and a thin REST controller.

| File | Layer | Responsibility |
|------|-------|----------------|
| `server/.../domain/Npc.java` | Entity | Stores NPC data and maps to the `npcs` table. |
| `server/.../db/NpcRepository.java` | Repository | Data access for NPCs. |
| `server/.../service/NpcService.java` | Service | Creation, updates, and queries. |
| `server/.../NpcController.java` | Controller | REST surface. |

The {@link Npc} entity stores the identity and story of a character — `name`,
`description`, `role`, `disposition`, `faction`, its current `location`, and its
`active` (alive/offstage) state — together with `relationship` toward the party and
free-form `notes`. Every NPC belongs to exactly one `Campaign` (a foreign key on the
`campaigns` table), so a character exists only inside the game that created it and
never leaks into another campaign.

NPC state is persisted in the SQLite database (`server/src/main/resources/schema.sql`,
loaded by Hibernate at bootstrap), so NPCs reload across sessions within a campaign.

### Optional Combat Statistics

Combat statistics are entirely optional. Every combat column (`hit_points`,
`max_hit_points`, `armor_class`, `movement`, `proficiency_bonus`, the six `ability_*`
columns) is nullable, so a purely social NPC stores no combat data. An NPC that fights
fills in those values and, optionally, its saving throws in the `npc_saving_throws`
table. `NpcService.npcHasCombatStats` reports whether a full set was provided.

## Domain Model: Creature and Enemy Templates

Creature templates follow the same layered design as every other entity: an `@Entity`
domain model, a Spring Data repository, a service that owns the business logic, and a
thin REST controller.

| File | Layer | Responsibility |
|------|-------|----------------|
| `server/.../domain/CreatureTemplate.java` | Entity | Stores a reusable creature/enemy definition and maps to the `creature_templates` table. |
| `server/.../db/CreatureTemplateRepository.java` | Repository | Data access for templates. |
| `server/.../service/CreatureTemplateService.java` | Service | Creation, listing, and enemy instantiation. |
| `server/.../CreatureTemplateController.java` | Controller | REST surface. |

A {@link CreatureTemplate} is a reusable blueprint for an adversary rather than a
living enemy in a game. It stores the creature's combat profile — `health` (hit points),
`defense` (armor class), `attack` (attack bonus), `damage` (damage output), and the
`initiative_modifier` applied when the creature rolls for initiative — plus free-form
`behavior_notes` describing how it fights or acts. Every template belongs to exactly one
`Campaign` (a foreign key on the `creature_templates` table), so a template exists only
inside the game that created it and never leaks into another campaign's catalogue of
foes. Combat values are all nullable, so a template may carry a partial or a full
profile.

Templates are reusable: a single template can be instantiated into many distinct enemies.
`CreatureTemplateService.instantiateEnemy` creates a concrete {@link Npc} enemy in the
same campaign from a template, inheriting the template's name and description and, when
the template carries a combat profile, its health (stored as the enemy's hit points and
maximum hit points), defence (as armor class), attack, damage, and initiative modifier.
The instantiated enemy is hostile to the party and starts active. The template-sourced
combat profile is preserved on the enemy through the optional `attack`, `damage`, and
`initiative_bonus` columns on the `npcs` table. All mutations are persisted in the SQLite
database (`server/src/main/resources/schema.sql`, loaded by Hibernate at bootstrap) so
templates and their instantiated enemies reload across sessions within a campaign.

## Domain Model: Factions

Factions follow the same layered design as every other entity: an `@Entity` domain
model, a Spring Data repository, a service that owns the business logic, and a thin
REST controller.

| File | Layer | Responsibility |
|------|-------|----------------|
| `server/.../domain/Faction.java` | Entity | Stores faction data and maps to the `factions` table. |
| `server/.../db/FactionRepository.java` | Repository | Data access for factions. |
| `server/.../service/FactionService.java` | Service | Creation, updates, relationship tracking, and queries. |
| `server/.../FactionController.java` | Controller | REST surface. |

The `@Faction` entity stores the identity and nature of a group - `name`,
`description`, `disposition`, `reputation` (the durable standing the wider world holds
toward it), and free-form `notes`. Every faction belongs to exactly one `Campaign` (a
foreign key on the `factions` table), so a faction exists only inside the game that
created it and never leaks into another campaign.

Factions can be linked to one another with directed `relationships`. Each relationship
names a related faction and the `@NpcRelationship` that describes the bond (allied,
neutral, foe, and so on); the links live in the `faction_relationships` table, scoped to
the owning campaign. `FactionService` records, reads, and removes these links, and all
mutations are persisted in the SQLite database
(`server/src/main/resources/schema.sql`, loaded by Hibernate at bootstrap) so factions
reload across sessions within a campaign.

## Domain Model: Quests

Quests follow the same layered design as every other entity: an @Entity domain
model, a Spring Data repository, a service that owns the business logic, and a thin
REST controller.

| File | Layer | Responsibility |
|------|-------|----------------|
| server/.../domain/Quest.java | Entity | Stores quest data and maps to the quests table. |
| server/.../domain/Objective.java | Entity | Stores one tracked objective with per-objective completion. |
| server/.../db/QuestRepository.java | Repository | Data access for quests. |
| server/.../db/ObjectiveRepository.java | Repository | Data access for objectives. |
| server/.../service/QuestService.java | Service | Creation, updates, objective tracking, and queries. |
| server/.../QuestController.java | Controller | REST surface. |

The Quest entity stores a strand of campaign story: its 	itle, description,
durable status (ACTIVE, COMPLETED, or FAILED), the quest giver, the

ewards on completion, related 
elatedLocations, and free-form 
otes. Every
quest belongs to exactly one Campaign (a foreign key on the quests table), so a
quest exists only inside the game that created it and never leaks into another
campaign.

A quest carries one or more Objectives. Each objective is its own entity stored in
the quest_objectives table (scoped to the owning quest and campaign) so that
per-objective completion survives across sessions. Every objective tracks its own
	argetCount and currentCount and derives an ObjectiveStatus
(INCOMPLETE or COMPLETE) from them independently of the quest and its siblings.
QuestService attaches objectives, advances an objective's progress with
setObjectiveProgress (clamped to the target and re-deriving completion), transitions
a quest to COMPLETED or FAILED, and queries quests by campaign and by status.
Related locations are stored as id references in the quest_related_locations table,
validated against the owning campaign. All mutations are persisted in the SQLite
database (server/src/main/resources/schema.sql, loaded by Hibernate at bootstrap) so
quests reload across sessions within a campaign.

## Domain Model: Inventory Items and Ownership

Inventory follows the same layered design as every other entity: `@Entity` domain
models, Spring Data repositories, a service that owns the business logic, and a thin
REST controller.

| File | Layer | Responsibility |
|------|-------|----------------|
| server/.../domain/ItemCategory.java | Enum | Item categories (weapon, armor, consumable, quest item, miscellaneous). |
| server/.../domain/InventoryOwnerKind.java | Enum | The two kinds of owner (campaign, player character). |
| server/.../domain/InventoryItem.java | Entity | Stores one holding: category, quantity, value, equipped state, description, and its owner. |
| server/.../domain/InventoryTransfer.java | Entity | Records one immutable hand-off of a holding between two owners. |
| server/.../db/InventoryItemRepository.java | Repository | Data access for holdings. |
| server/.../db/InventoryTransferRepository.java | Repository | Data access for transfers. |
| server/.../service/InventoryService.java | Service | Holdings, quantity/equipped updates, ownership, and transfers. |
| server/.../InventoryController.java | Controller | REST surface. |

An {@link InventoryItem} holding is where an item's catalogue attributes meet an
owner. It stores the item's {@link #name}, its {@link #category}
(weapon, armor, consumable, quest item, or miscellaneous), its {@link #quantity},
it value ({@link #value}), its {@link #equipped} state, and free-form
{@link #description}. The holding is owned by exactly one owner, named by an owner
kind plus an owner id: either the owning {@link Campaign} (a shared, campaign-wide
stash) or a single player character (a hero's goods).

An item's quantity can be adjusted, and its catalogue attributes and equipped state
can be updated in place. `InventoryService.setEquipped` only flips the flag when the
holding is owned by a player character; a campaign-owned stack is never equipped.

Ownership transfers between owners are represented by {@link InventoryTransfer}.
`InventoryService.transfer` moves a quantity of a holding from one owner to another
within the same campaign: the source holding is reduced (removed when it is emptied),
the destination holding is created if necessary and increased, and an immutable
transfer row is recorded. Transfers are only allowed between two different owners, and
a source holding cannot give to itself. Every holding and transfer is persisted in the
SQLite database (server/src/main/resources/schema.sql, loaded by Hibernate at bootstrap)
so inventory and its transfer history reload across sessions within a campaign.

## Domain Model: Encounters, Combatants, and Conditions

Encounters, combatants, and combat conditions follow the same layered design as every
other entity: {@code @Entity} domain models, Spring Data repositories, services that
own the business logic, and thin REST controllers.

| File | Layer | Responsibility |
|------|-------|----------------|
| server/.../domain/Scene.java | Entity | Stores a slice of in-game time: its title, narrative, location, referenced encounter, and status. |
| server/.../domain/SceneStatus.java | Enum | Lifecycle state of a scene (READY, ACTIVE, COMPLETED). |
| server/.../domain/SceneInvolvedCharacter.java | Entity | Join row naming a character or NPC as involved in a scene. |
| server/.../domain/Encounter.java | Entity | Stores an encounter anchored to a scene, a location, and its status. |
| server/.../domain/Combatant.java | Entity | Stores a single participant (hero or enemy) with hit points and a defeated flag. |
| server/.../domain/CombatCondition.java | Entity | Stores a status effect: name, description, duration, source, and active state. |
| server/.../db/SceneRepository.java | Repository | Data access for scenes. |
| server/.../db/SceneInvolvedCharacterRepository.java | Repository | Data access for scene-involved-character join rows. |
| server/.../db/EncounterRepository.java | Repository | Data access for encounters. |
| server/.../db/CombatantRepository.java | Repository | Data access for combatants. |
| server/.../db/CombatConditionRepository.java | Repository | Data access for combat conditions. |
| server/.../service/SceneService.java | Service | Scene creation, location/encounter wiring, involved-character management, and scene advancing. |
| server/.../service/EncounterService.java | Service | Encounter creation, start/finish, and lookup. |
| server/.../service/CombatantService.java | Service | Combatant creation, damage/healing, turn-ordering, and encounter-completion detection. |
| server/.../service/CombatConditionService.java | Service | Combat condition creation, toggling, and round advancement. |
| server/.../SceneController.java | Controller | REST surface for scenes, involved characters, and scene advancing. |
| server/.../EncounterController.java | Controller | REST surface for encounters. |
| server/.../CombatantController.java | Controller | REST surface for combatants and turn order. |
| server/.../CombatConditionController.java | Controller | REST surface for combat conditions. |

An {@link Encounter} is a discrete beat of play anchored to a {@link Scene} and a
{@link Location}. It belongs to exactly one campaign, references one scene and one
location, and stores its {@link EncounterStatus} (SCHEDULED, ACTIVE, or FINISHED) plus
the {@code current_turn} turn-order slot in the {@code encounters} table, so turn
order and whose turn it is reload across sessions. A {@link Scene} is a single,
contiguous slice of in-game time. It carries its {@link Scene#getTitle() title},
its {@link Scene#getNarrative() narrative}, the {@link Scene#getLocation() location}
it takes place in, the {@link Scene#getEncounter() encounter} it references when one
exists, and its {@link Scene#getStatus() status} (READY, ACTIVE, or COMPLETED), so at
most one scene is in focus per campaign. The DM engine advances between scenes via
{@link SceneService#advanceScene}, which moves the active focus to the next scene and
marks the scene left behind COMPLETED. The characters and NPCs involved in a scene are
recorded through {@link SceneInvolvedCharacter} join rows, which reload across sessions
within a campaign.

A {@link Combatant} is one participant in an encounter: a hero or an enemy
({@link CombatantKind}). It stores its identity, side, current and maximum hit points,
the {@link Combatant#getDefeated() defeated} flag, and its {@link Combatant#getInitiative()
initiative} used to establish turn order. Every combatant belongs to exactly one
campaign and optionally points at its encounter and scene.

{@link CombatantService} owns the turn-ordering rule: {@code buildTurnOrder} assigns
each active participant a 1-based turn position ordered by descending initiative (ties
broken by id). Defeated combatants are excluded from the turn order entirely - they are
not assigned a position and never appear in the returned list - so {@code nextTurn}
can simply advance the current turn around the round without ever landing on a defeated
combatant. {@code currentCombatant} resolves the combatant whose turn it is. Damage
({@code takeDamage}) clamps at zero and marks a combatant defeated when it reaches zero.

The engine also detects when an encounter is over. {@code isEncounterComplete} reports
true once every combatant on at least one side ({@link CombatantKind}) has been
defeated, and {@code winningSide} names the winning side (the side still standing).
Both treat a round in which both sides have been fully defeated as unresolved. The REST
surface exposes these at {@code GET /api/campaigns/{campaignId}/encounters/{encounterId}/complete}
and {@code GET /api/campaigns/{campaignId}/encounters/{encounterId}/winner}.

A {@link CombatCondition} is a status effect applied to a combatant. It stores the
effect's name, description, duration (in rounds), source, and whether it is active.
Combat conditions are scoped to one campaign and point at the combatant they apply to,
so status effects reload across sessions. {@code CombatConditionService.addCondition} is
idempotent for an identical active condition, and {@code advanceRounds} steps every
active condition down by a round, deactivating any whose duration has run out.

All of these entities are persisted in the SQLite database
(server/src/main/resources/schema.sql, loaded by Hibernate at bootstrap), so scenes,
encounters, combatants, and conditions reload across application restarts within a
campaign. The back-end API is served on {@code http://localhost:5150}, configured in
server/src/main/resources/application.properties.

## Domain Model: Automated Encounter Generation

Encounter generation follows the same layered design as every other area of the game: game logic
lives in a service, and a thin REST controller exposes the result. The generated encounter itself
is persisted through the existing {@link com.example.service.EncounterService} and its enemies are
instantiated through {@link com.example.service.CombatantService}, so a generated encounter and its
combatants reload across sessions within a campaign.

| File | Layer | Responsibility |
|------|-------|----------------|
| server/.../domain/EncounterDifficulty.java | Enum | Difficulty tiers and their budget multiplier |
| server/.../service/EncounterGenerator.java | Service | Sizes and fills encounters from the party and templates |
| server/.../service/EncounterGenerationResult.java | Value | The outcome of a generation (encounter, enemies, budget, party) |
| server/.../EncounterController.java | Controller | REST surface for encounters and generation |

The {@link com.example.service.EncounterGenerator} is the single place automated encounters are
built. It considers four inputs: the **party strength** (the number of player characters and their
average level, both overridable), the **difficulty** ({@link com.example.domain.EncounterDifficulty}),
the **available enemy definitions** (the campaign's {@link com.example.domain.CreatureTemplate}s,
optionally narrowed to a set), and the **campaign state** (the owning campaign is always resolved
first). Generation defaults to {@link com.example.domain.EncounterDifficulty#MEDIUM} when no
difficulty is supplied.

The party's combined level and size produce a base power budget, which the difficulty's budget
multiplier scales into a threat budget. Each template contributes a threat score derived from its
combat profile (health, defence, attack, and damage). Generation fills the budget with templates
- one of each first, then repeating the cheapest template while budget remains - so a harder
difficulty fields a proportionally stronger encounter. Each selected template is instantiated into
one or more {@link com.example.domain.Combatant} enemies of {@link com.example.domain.CombatantKind#ENEMY}
joined to the encounter, which is what puts the generated encounter into the combat engine.

The REST surface is {@code /api/campaigns}. The generation endpoint is
`POST /api/campaigns/{campaignId}/encounters/generate`, which takes a required `locationId` plus
optional `difficulty`, `partySize`, `averageLevel`, and `templateIds` parameters, for example
`POST /api/campaigns/1/encounters/generate?locationId=3&difficulty=HARD&partySize=4&averageLevel=5`.

## Domain Model: Dice Rolling

Dice rolling follows the same layered design as every other area of the game: game logic
lives in a service, randomness is generated on the back-end, and a thin REST controller
exposes the result. There is no persistence for rolls — a roll is a pure function of its
inputs plus freshly generated randomness.

| File | Layer | Responsibility |
|------|-------|----------------|
| server/.../domain/DieResult.java | Value | One die: its face count and the value rolled |
| server/.../domain/DiceRollResult.java | Value | A full roll: the dice, the modifier, the total, and the percentile flag |
| server/.../service/DiceService.java | Service | Validation, back-end randomness, total calculation |
| server/.../DiceController.java | Controller | REST surface |

The {@link com.example.service.DiceService} is the single owner of every dice rule. It
supports d4, d6, d8, d10, d12, d20, and percentile (d100), any number of dice in a single
roll, and an additive (or subtractive) modifier. All randomness comes from a
{@link java.security.SecureRandom} held in the service, so rolls are generated on the
back-end and can never be influenced by the browser. A roll returns a
{@link com.example.domain.DiceRollResult} that breaks down each individual die (its face
count and value), the modifier, and the final total, so callers can render the full
history of the roll.

The REST surface is {@code /api/dice}. The primary endpoint is
`POST /api/dice/roll`, which takes a repeatable `sides` parameter (one entry per die to
roll) and an optional `modifier`, for example `POST /api/dice/roll?sides=20&sides=20&modifier=+3`
to roll `2d20+3`. Percentile rolls use `sides=100`. A single-die shortcut is exposed at
`POST /api/dice/roll/single?sides=20`, and `GET /api/dice/catalog` lists the dice the
service supports.

## Domain Model: Ability and Skill Check Resolution

Ability and skill check resolution follows the same layered design as every other area of
the game: game logic lives in a service, randomness is generated on the back-end (via the
`{@link com.example.service.DiceService}`), and a thin REST controller exposes the result.
There is no persistence for a check — resolving a check is a pure function of its inputs
plus freshly generated randomness.

| File | Layer | Responsibility |
|------|-------|----------------|
| server/.../domain/AbilityCheckResult.java | Value | A resolved check: the statistic, modifier, roll, total, difficulty, and outcome |
| server/.../domain/AbilityCheckOutcome.java | Enum | The binary result (SUCCESS or FAILURE) |
| server/.../service/AbilityCheckService.java | Service | Total calculation and success/failure comparison |
| server/.../AbilityCheckController.java | Controller | REST surface |

The {@link com.example.service.AbilityCheckService} is the single owner of the
ability-check rule. It combines four inputs — the character's raw ability
`statistic`, the `modifier` applied to that ability, the generated `roll`, and the
`difficulty` class — into a single {@link com.example.domain.AbilityCheckResult}. The
total is the sum of the statistic, the modifier, and the roll, and the check succeeds when
that total meets or exceeds the difficulty and fails otherwise. Rolls are generated on the
back-end through the `{@link com.example.service.DiceService}`, so a check's die can never
be influenced by the browser.

The REST surface is {@code /api/ability-checks}. The primary endpoint is
`POST /api/ability-checks/resolve`, which takes the `statistic`, `modifier`, `roll`, and
`difficulty` for a check that has already been rolled, for example
`POST /api/ability-checks/resolve?statistic=16&modifier=+3&roll=12&difficulty=15`.
To roll the die on the back-end instead, use `POST /api/ability-checks/roll` with an
optional `sides` parameter (defaults to a d20), for example
`POST /api/ability-checks/roll?statistic=8&modifier=+3&sides=20&difficulty=15`.

## Domain Model: Enemy Behavior

Enemy behavior follows the same layered design as every other area of the game: game logic
lives in a service, randomness is generated on the back-end (via the
`{@link com.example.service.DiceService}`), and a thin REST controller exposes the result.
There is no persistence for enemy behavior itself — choosing a target and resolving an attack
is a pure function of its inputs plus freshly generated randomness; the damage it causes is
persisted through the existing {@link com.example.service.CombatantService}.

| File | Layer | Responsibility |
|------|-------|----------------|
| server/.../domain/EnemyActionOutcome.java | Value | A resolved enemy action: what happened and how much damage landed |
| server/.../service/EnemyBehaviorEngine.java | Strategy | Selects living targets and resolves attacks (the pluggable AI) |
| server/.../service/DefaultEnemyBehaviorEngine.java | Strategy | The default, focus-fire enemy AI |
| server/.../service/EnemyBehaviorService.java | Service | Orchestrates an enemy turn end to end |
| server/.../EnemyBehaviorController.java | Controller | REST surface |

The {@link com.example.service.EnemyBehaviorService} is the single place an enemy's turn is
driven. It gathers the valid living targets an enemy may act on, delegates the choice of
target and the resolution of the attack to a pluggable
{@link com.example.service.EnemyBehaviorEngine}, and applies any resulting damage through the
`{@link com.example.service.CombatantService}`, which persists it and marks the combatant
defeated at zero hit points. The engine resolves the attack without touching hit points of
its own: {@link com.example.service.EnemyBehaviorService} is the single source of truth for
hit-point changes and persistence, so damage is applied exactly once.

Every attack carries a {@link com.example.domain.DamageType} describing the kind of harm it
deals (physical, fire, cold, lightning, and so on), defaulting to physical. When an attack
lands and deals damage, {@code EnemyBehaviorService} records it as a {@link
com.example.domain.CampaignEventType#DAMAGE} campaign event through the campaign's event
system, so the attack — its attacker, target, damage, damage type, roll total, and difficulty
— is available to the encounter engine and to anyone consulting the campaign's event
history.

The engine is modular by design. It is a single interface,
`{@link com.example.service.EnemyBehaviorEngine}`, with two questions an enemy asks of itself:
which valid living target to act on, and how its attack resolves. The
{@link com.example.service.DefaultEnemyBehaviorEngine} implements the interface with a simple,
predictable AI: it focuses fire on the weakest living target (fewest hit points, ties broken
by id) and resolves attacks by rolling a d20 plus the attack bonus and comparing against the
target's defence. Every implementation skips defeated or otherwise invalid candidates —
`selectLivingTarget` only ever returns a fighting combatant, and `resolveAttack` reports
`EnemyActionOutcome#actionTaken()` as `false` when no valid living target exists.

Because behaviour lives behind the interface, a richer AI can replace the default strategy
without changing how the service or the REST surface works. A different
`EnemyBehaviorEngine` can be provided as a Spring bean, or installed at runtime through
`EnemyBehaviorService#setEnemyBehaviorEngine`, so a campaign — or a verification — can wire in
a bespoke strategy.

The REST surface is {@code /api/campaigns}. The enemy-attack endpoint is
`POST /api/campaigns/{campaignId}/combatants/{combatantId}/attack`, which takes the enemy's
`attackBonus`, `damage`, and the `difficulty` its roll must meet (defaulting to 10), for
example `POST /api/campaigns/1/combatants/3/attack?attackBonus=+4&damage=5`.

All of these entities are persisted in the SQLite database
(server/src/main/resources/schema.sql, loaded by Hibernate at bootstrap), so scenes,
encounters, combatants, and conditions reload across application restarts within a
campaign. The back-end API is served on {@code http://localhost:5150}, configured in
server/src/main/resources/application.properties.

## Domain Model: The Dungeon Master Engine

The Dungeon Master (DM) engine follows the same layered, pluggable design as every other
area of the game: a thin interface abstracts the cognitive core, a deterministic
implementation drives the initial application, and a service orchestrates the full
per-action flow (scene presentation, validation, mechanic resolution, state changes, and
event recording). This makes it possible to add a future local or remote LLM provider for
the engine without rewriting the service, the persistence, or the REST surface.

| File | Layer | Responsibility |
|------|-------|----------------|
| server/.../service/DungeonMasterEngine.java | Strategy | Abstracts action handling and response generation (the pluggable DM) |
| server/.../service/DefaultDungeonMasterEngine.java | Strategy | The default, deterministic DM |
| server/.../service/DungeonMasterService.java | Service | Orchestrates a player action end to end |
| server/.../service/ActionEffectsService.java | Service | Triggers the world effects a recognised action may cause (discoveries, objective completion, relationship updates, encounters) |
| server/.../service/ActionParser.java | Service | Reads structured meaning out of free-form player text |
| server/.../service/ActionValidator.java | Service | Rejects impossible actions against the current scene |
| server/.../domain/SceneBrief.java | Value | The presented scene snapshot |
| server/.../domain/PlayerActionInput.java | Value | A raw player action input |
| server/.../domain/PlayerAction.java | Value | A structured player action with an optional free-text description |
| server/.../domain/PlayerActionType.java | Enum | The structured categories a player action can express |
| server/.../domain/ActionValidationResult.java | Value | The outcome of validating an action against game state |
| server/.../domain/PlayerActionResolution.java | Value | The engine's mechanical verdict |
| server/.../domain/StateChange.java | Value | A pending hit-point change the service applies |
| server/.../domain/EngineResponse.java | Value | The complete engine response (scene, verdict, check, narrative, pending state change, and triggered effects) |
| server/.../DungeonMasterController.java | Controller | REST surface |

The {@link com.example.service.DungeonMasterService} is the single place a player's action
is driven end to end. For each action it:

- <strong>presents scene info</strong> by delegating to {@link
  com.example.service.DungeonMasterEngine#presentScene}, which returns a compact {@link
  com.example.domain.SceneBrief} (the active scene plus the combatants present);
- <strong>validates actions</strong> and <strong>resolves mechanics</strong> by delegating
  to {@link
  com.example.service.DungeonMasterEngine#resolvePlayerAction}, which understands the
  free-form action, checks it against a small verb vocabulary, rolls a d20 ability check
  through the shared {@link com.example.service.DiceService}, and records any intended
  hit-point change as a {@link com.example.domain.StateChange} intent;
- <strong>applies state changes</strong> itself, as the single source of truth for
  hit points, by applying the resolved intent through {@link
  com.example.service.CombatantService} (damage via {@code applyDamage}, healing via
  {@code heal}); and
- <strong>triggers world effects</strong> through {@link
  com.example.service.ActionEffectsService}, which realises the action's broader
  consequences - discovering a location, completing a quest objective, updating an NPC
  relationship, or beginning a combat encounter - records each as a campaign event, and
  returns a short description of every effect that fired; and
- <strong>records events</strong> by writing the resolved action as a {@link
  com.example.domain.CampaignEventType#GAME_ACTION} campaign event (and a {@link
  com.example.domain.CampaignEventType#DAMAGE} event when damage is dealt) through the
  existing {@link com.example.domain.CampaignEvent} system.

A player's action flows through two stages before it is resolved. The
{@link com.example.service.ActionParser} first interprets the free-form text into a structured
{@link com.example.domain.PlayerAction} carrying a {@link com.example.domain.PlayerActionType}
(investigate, talk, travel, attack, use item, rest, search, interact, or a named skill action) and
an optional free-form description. The {@link com.example.service.ActionValidator} then checks that
structured action against the current scene - rejecting, with a clear error, an attack with no
living target, a rest while an enemy still stands, or a skill action that never names its skill.
Only an action that parses and validates reaches the mechanical resolution below. Both the parser
and the type list are deliberately open, so a richer natural-language interpreter can be added
later without changing what an action is or how it is validated.

The {@link com.example.service.DungeonMasterEngine} is abstract by design. It exposes
exactly the two seams an LLM provider would specialise: <em>action handling</em>
({@code resolvePlayerAction}) and <em>response generation</em>
({@code generateResponse}). The {@link
com.example.service.DefaultDungeonMasterEngine} implements both with a small, predictable,
deterministic DM that understands a handful of plain-language verbs (attacking, socialising,
healing, escaping) and narrates short, fixed responses. Because everything lives behind the
interface, a richer or LLM-backed strategy can replace the default without changing how the
service or the REST surface works. A different engine can be provided as a Spring bean, or
installed at runtime through {@link com.example.service.DungeonMasterService#setEngine}, so
a campaign (or a verification) can wire in a bespoke strategy while the application is
running. As with the {@link com.example.service.EnemyBehaviorEngine}, the engine resolves
mechanics without mutating state: it never applies damage or healing itself.

The REST surface is {@code /api/campaigns}. The action endpoint is
`POST /api/campaigns/{campaignId}/scenes/{sceneId}/action`, which takes the required
`action` plus optional `statistic`, `modifier`, and `difficulty` parameters, for example
`POST /api/campaigns/1/scenes/2/action?action=I%20attack%20the%20guard&statistic=Athletics&modifier=+3`}.

All of these values are persisted in the SQLite database
(server/src/main/resources/schema.sql, loaded by Hibernate at bootstrap) only through the
existing campaign-event system, so a campaign's resolved-action history reloads across
application restarts. The back-end API is served on {@code http://localhost:5150},
configured in server/src/main/resources/application.properties.

## Domain Model: Campaign Events

Campaign events follow the same layered design as every other area of the game: an
{@code @Entity} domain model, a Spring Data repository, a service that owns the recording
and querying logic, and a thin REST controller.

| File | Layer | Responsibility |
|------|-------|----------------|
| server/.../domain/CampaignEvent.java | Entity | Stores one significant event: type, timestamp, campaign, description, details |
| server/.../domain/CampaignEventType.java | Enum | The kinds of moment recorded (session start, location entry, discovery, combat, damage, item acquisition, quest change, relationship change, session end, rest, game action) |
| server/.../db/CampaignEventRepository.java | Repository | Data access for events, organized by owning campaign |
| server/.../service/CampaignEventService.java | Service | The single recording and querying entry point for events |
| server/.../CampaignEventController.java | Controller | REST surface |

A {@link com.example.domain.CampaignEvent} records one significant moment within a
{@link com.example.domain.Campaign}: what happened ({@link #eventType}), when
({@link #timestamp}), which campaign it belongs to, a human-readable {@link #description},
and optional structured (JSON) {@link #details}. Every event is owned by exactly one
campaign, so event history never leaks across games, and it persists in the
`campaign_events` table (loaded by Hibernate at bootstrap) so it reloads across
restarts.

{@link com.example.service.CampaignEventService} is the single place events are recorded
and consulted. It centralizes event construction through a small set of helpers, so every
event in a campaign's history has a consistent shape:

- {@link com.example.service.CampaignEventService#recordSessionStart(Long) recordSessionStart}
  and
  {@link com.example.service.CampaignEventService#recordSessionEnd(Long) recordSessionEnd}
  bracket play; a session starts with a
  {@link com.example.domain.CampaignEventType#SESSION_START} event when the first active
  session is created for a campaign, and ends with a
  {@link com.example.domain.CampaignEventType#SESSION_END} event when that session is
  ended.
- {@link com.example.service.CampaignEventService#recordLocationEntry(Long, Long, String)
  recordLocationEntry} records
  a {@link com.example.domain.CampaignEventType#LOCATION_ENTRY} when the party's location
  changes, and
  {@link com.example.service.CampaignEventService#recordDiscovery(Long, String, Long, String)
  recordDiscovery} records a
  {@link com.example.domain.CampaignEventType#DISCOVERY} the first time a location is
  discovered.
- {@link com.example.service.CampaignEventService#recordCombat(Long, Long, String)
  recordCombat} records a
  {@link com.example.domain.CampaignEventType#COMBAT} when an encounter begins.
- {@link com.example.service.CampaignEventService#recordItemAcquisition(Long, String, String,
  Long) recordItemAcquisition} records a
  {@link com.example.domain.CampaignEventType#ITEM_ACQUISITION} the first time a named item
  is added to an owner.
- {@link com.example.service.CampaignEventService#recordQuestChange(Long, Long, String,
  String) recordQuestChange} records a
  {@link com.example.domain.CampaignEventType#QUEST_CHANGE} whenever a quest's tracked
  status changes.
- {@link com.example.service.CampaignEventService#recordRelationshipChange(Long, Long, Long,
  String) recordRelationshipChange} records a
  {@link com.example.domain.CampaignEventType#RELATIONSHIP_CHANGE} whenever a faction's
  relationship to another faction changes.
- The shared {@link com.example.service.CampaignEventService#recordEvent(Long,
  com.example.domain.CampaignEventType, String, String) recordEvent(campaignId, type,
  description, details)} helper backs every helper above, and the simpler
  {@link com.example.service.CampaignEventService#recordEvent(Long, com.example.domain.CampaignEventType)
  recordEvent} overloads are available for ad-hoc recording.

The engine actions that now record events through this service are:

- {@link com.example.service.SessionService} records SESSION_START (first active session)
  and SESSION_END (session end).
- {@link com.example.service.WorldService} records LOCATION_ENTRY (party location changes)
  and DISCOVERY (first discovery of a location).
- {@link com.example.service.EncounterService} records COMBAT (an encounter begins).
- {@link com.example.service.InventoryService} records ITEM_ACQUISITION (a new item holding
  is created).
- {@link com.example.service.QuestService} records QUEST_CHANGE (a quest's status changes).
- {@link com.example.service.FactionService} records RELATIONSHIP_CHANGE (a faction
  relationship changes).
- {@link com.example.service.DungeonMasterService} and
  {@link com.example.service.EnemyBehaviorService} already record DAMAGE events, and
  {@link com.example.service.RestService} records REST events.

Every one of these events is inspectable later through the REST surface
(`GET /api/campaigns/{campaignId}/events`,
`GET /api/campaigns/{campaignId}/events/by-type`, and
`GET /api/campaigns/{campaignId}/events/{eventId}`) on
`http://localhost:5150`.

## Domain Model: Narrative Templates

Narrative templates follow the same layered design as every other area of the game: game
logic lives in a service, and a thin REST controller exposes the result. There is no
persistence for narrative output — rendering a line is a pure function of the structured
game state it is given.

| File | Layer | Responsibility |
|------|-------|----------------|
| server/.../domain/NarrativeCategory.java | Enum | The five log categories (DM narration, player action, dice result, combat event, system event) |
| server/.../domain/NarrativeEntry.java | Value | One structured game-log line: category, title, message, timestamp, data |
| server/.../domain/NarrativeContext.java | Value | The structured game state a template renders from |
| server/.../domain/NarrativeRenderRequest.java | Value | The REST payload for a render request |
| server/.../service/NarrativeTemplate.java | Strategy | Turns one piece of structured state into one {@link NarrativeEntry} |
| server/.../service/NarrativeTemplates.java | Service | Registry and renderer of the canonical templates |
| server/.../NarrativeController.java | Controller | REST surface |

The {@link com.example.service.NarrativeTemplates} service owns the five canonical
templates, one per {@link com.example.domain.NarrativeCategory}: {@code DM_NARRATION}
(from a {@link com.example.domain.SceneBrief}), {@code PLAYER_ACTION} (from a
{@link com.example.domain.EngineResponse}), {@code DICE_RESULT} (from a
{@link com.example.domain.DiceRollResult}), {@code COMBAT_EVENT} (from a
{@link com.example.domain.EnemyActionOutcome}), and {@code SYSTEM_EVENT} (from a
{@link com.example.domain.CampaignEvent}). Each template reads only the field it cares
about and returns a {@link com.example.domain.NarrativeEntry}, the unit the frontend game
log consumes.

Templates are <em>data-driven</em>: a template renders from the fully-typed domain object
when the caller supplies it (the way the rest of the back-end does), and from free-form
data when the caller supplies only a payload (the way the REST surface does). A caller
that holds a structured payload rather than a domain object rebuilds the typed object from
the data map, so a single template serves both paths.

The registry is open and extensible. {@link com.example.service.NarrativeTemplates#register}
and {@link com.example.service.NarrativeTemplates#unregister} let a campaign or a
verification replace or add a template for any category. The {@link
com.example.domain.NarrativeCategory} enum is deliberately open, so a new category can be
introduced by adding a constant and registering a template — the REST surface already passes
categories through by name, so no endpoint changes to pick up the new category.

Rendering never mutates game state: every template is a pure function of its context,
exactly like the {@link com.example.service.DiceService} and
{@link com.example.service.AbilityCheckService} the templates draw from.

The REST surface is {@code /api/narrative}. The categories endpoint is
GET /api/narrative/categories, which lists the categories that currently have a
template. The {@code /api/narrative/dice} endpoint rolls the requested dice on the
back-end and renders the roll as a {@code DICE_RESULT} entry, for example
POST /api/narrative/dice?sides=20&sides=20&modifier=+3. The general endpoint is
POST /api/narrative/render, which takes a category plus a free-form structured data
map and returns the rendered {@link com.example.domain.NarrativeEntry}, so the frontend
game log can render any category from any structured payload.

The back-end API is served on {@code http://localhost:5150}, configured in
server/src/main/resources/application.properties.

## Front-End Services and State

The Angular application keeps all back-end communication and campaign state in one place:

```
client/src/app/services/
  campaigns.service.ts          /api/campaign-management
  characters.service.ts         /api/campaigns/{campaignId}/characters
  npcs.service.ts               /api/campaigns/{campaignId}/npcs
  world.service.ts              /api/campaigns/{campaignId}/...
  quests.service.ts             /api/campaigns/{campaignId}/quests
  items.service.ts              /api/campaigns/{campaignId}/inventory
  sessions.service.ts           /api/campaigns/{campaignId}/sessions
  dungeon-master.service.ts     /api/campaigns/{campaignId}/scenes
  campaign-store.service.ts     authoritative current-campaign state
  dashboard.service.ts          /api/campaigns/{campaignId}/dashboard
client/src/app/models/          front-end view models mirroring the back-end DTOs
```

Every service wraps a single area of the back-end API and exposes an observable API, so
components drive the UI directly from the HTTP stream. The {@code CampaignStore} is the single
authoritative home of the active campaign and every collection loaded for it (characters, NPCs,
quests, items, sessions, and world data); components never duplicate campaign state, and the
store swaps every collection together when a different campaign is selected.

The dashboard aggregates that state into one snapshot and, in addition to the usual summary,
returns a {@code setupProgress} object. That object reports how many of the five setup stages of
the end-to-end workflow (party characters, world, NPCs, quests, inventory) already have persisted
data, and lists the next steps to fill any remaining gaps. The dashboard links those next steps
straight to the relevant page so a freshly created campaign can be walked through to completion.

The services talk to the back-end over {@code /api}, which the development server proxies to
{@code http://localhost:5150} (see {@code client/proxy.conf.json}).
