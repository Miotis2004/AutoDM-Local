# Argus vs. Jules: AutoDM Development Comparison

**Date reviewed:** August 24, 2026  
**Argus repository:** [Miotis2004/AutoDM-Local](https://github.com/Miotis2004/AutoDM-Local)  
**Jules repository:** [Miotis2004/AutoDM](https://github.com/Miotis2004/AutoDM)

## Executive Summary

This experiment compared two AI development systems using the same JSON planning file for a long-running, complicated, full-stack application. Argus developed `AutoDM-Local` with comparatively little human involvement. Jules, Google's Gemini coding agent, developed `AutoDM`, but required the operator to enter instructions for each task and approve every merge.

After controlling for differences in testing instructions, Git usage, and unfinished late-stage Jules tasks, **Argus produced the stronger overall result**. Its repository is more coherent across the frontend, REST API, domain model, persistence layer, and gameplay systems. Jules produced useful automated tests and some cleaner conventional organization, but it also introduced cross-layer inconsistencies that repeated human checkpoints did not prevent.

The most defensible conclusion is:

> Given the same complex planning artifact, the Argus agent system produced a more coherent, integrated, and autonomous full-stack implementation than the Jules agent system, while requiring substantially less human orchestration.

This is a comparison of complete agent systems, not only their underlying language models. The outcome reflects planning persistence, context management, tool use, instruction following, assumption-making, verification behavior, and repository implementation.

## Experimental Context

The test had two purposes:

1. Push Argus toward its boundaries using a large, dependency-heavy, 60-task full-stack application.
2. Compare Argus behind the scenes against a full-featured frontier coding agent using the same JSON planning file.

The development conditions were not identical in every respect, so the following controls are important.

### Testing policy

Testing requirements were deliberately removed from the Argus plan. Tests are intended to be added during a later stabilization phase. Jules did not receive the same explicit test-deferral instruction.

Consequently:

- Argus's lack of a conventional automated test suite is not considered an implementation failure.
- Jules's test suite is useful, but it is not evidence that Jules followed the intended Argus development strategy more effectively.
- Test counts are reported as repository characteristics, not used as a decisive competitive score.

### Git workflow

Argus did not use Git as its working storage mechanism. The completed repository was uploaded to GitHub afterward to make this comparison possible.

Jules used a GitHub-centered workflow in which every task required manual instruction entry and a human-approved merge.

Consequently:

- Argus's sparse Git history is not evidence of poor iteration or weak traceability during development.
- Jules's task-oriented commits and pull requests primarily reflect its required operating workflow.
- Git activity is not used as a code-quality or autonomy advantage for either system.

### Human intervention

Argus was mostly hands-off. It requested manual review several times, apparently because reports were poorly formatted. These interventions were primarily orchestration or protocol failures rather than requests for substantive product decisions.

Jules required continuous human orchestration:

- The operator entered the instructions for each task.
- Jules sometimes asked questions despite explicit instructions not to do so.
- The answer was consistently: "Make reasonable assumptions and proceed."
- The operator approved each repository merge.

This distinction is central to the autonomy evaluation.

### Incomplete Jules tasks

The absence of Jules's remaining late-stage tasks is not treated as a code-quality failure. Feature-completion comparisons are interpreted carefully, particularly where end-to-end integration was intentionally scheduled for later tasks.

However, defects inside work already represented as completed, such as incorrect API contracts or a placeholder encounter screen associated with an earlier task, remain relevant.

## Repository Snapshots

The review used the repositories' current `main` branches on August 24, 2026:

- Argus `AutoDM-Local`: commit `73d5064348cb3380abe3c46cd18bf05df719192b`
- Jules `AutoDM`: commit `b2f19948bf6e85ef204a513a22efe104ae4165aa`

At the time of review, Jules's Task 55 changes had been merged into `main`.

Neither repository had GitHub CI status checks configured. Statements in task reports or documentation that the application builds successfully were therefore not independently enforced by GitHub.

## Shared Architecture

Both implementations use the same broad technical approach:

- Angular 22 standalone frontend
- TypeScript 6
- Java 21
- Spring Boot backend
- Spring Data JPA
- Local SQLite persistence
- REST communication between frontend and backend
- Local-only deployment
- Campaign, character, NPC, world, quest, faction, item, session, event, scene, encounter, and combat concepts
- Controller, service, repository, and domain/model layering
- A deterministic Dungeon Master abstraction that can later be replaced by a richer engine or local LLM implementation
- Frontend routing and centralized application state
- HTTP error-handling and loading-state infrastructure

The repositories are recognizably independent implementations of the same plan.

## Quantitative Snapshot

| Characteristic | Argus: AutoDM-Local | Jules: AutoDM |
|---|---:|---:|
| Repository files | 299 | 210 |
| Java files | 168 | 109, including tests |
| TypeScript files | 59 | 51 |
| Java and TypeScript source lines | Approximately 26,500 | Approximately 10,675 |
| Production Java and TypeScript lines | Approximately 26,500 | Approximately 9,000 |
| Conventional test lines | Intentionally deferred | Approximately 1,679 |
| Conventional Java test classes | Intentionally deferred | 16 |
| Angular component specs | Intentionally deferred | 1 |

These numbers describe implementation scope, not quality by themselves. Argus's larger codebase contains a substantially more detailed domain model and more fully implemented frontend features, but it also contains oversized components and considerable generated documentation.

## Comparative Results

| Evaluation area | Stronger result | Basis |
|---|---|---|
| Following the intended plan | Argus | Progressed through the full planning artifact with little intervention. |
| Autonomous task progression | Argus | Did not require manual submission and merge approval for every task. |
| Reasonable assumption-making | Argus | Proceeded without repeated clarification and maintained broad system coherence. |
| Cross-layer consistency | Argus | Frontend services, backend controllers, persistence, and UI behavior agree more consistently. |
| Domain-model depth | Argus | Models more gameplay resources, transfers, locations, routes, conditions, and state transitions explicitly. |
| Frontend/backend integration | Argus | Major pages call real services and have corresponding backend endpoints. |
| Persistence design | Argus | Uses an explicit, version-controlled SQLite schema and configurable database path. |
| Encounter and gameplay implementation | Argus | Provides substantive encounter, combat, narrative, and session behavior. |
| Conventional test suite | Jules | Contains a useful JUnit suite, but the testing instructions differed. |
| Transaction boundaries | Jules | Uses `@Transactional` throughout multi-operation backend services. |
| Standard validation infrastructure | Jules | Uses Jakarta Bean Validation and structured validation errors. |
| Package and directory conventions | Jules | Uses `com.autodm.server` and clear `core`, `features`, `shared`, and `layout` frontend divisions. |
| Overall application coherence | Argus | The assembled system is more internally consistent and closer to working end to end. |

## Argus Strengths

### 1. Autonomous long-horizon execution

Argus's most significant achievement is not raw code volume. It maintained enough context across a large sequence of dependent tasks to produce an application whose major layers generally agree.

The system includes meaningful implementations for campaigns, characters, world data, factions, NPCs, quests, items, sessions, history, scenes, encounters, combatants, conditions, deterministic Dungeon Master behavior, and narrative output.

This is precisely where long-running coding systems often degrade. They may complete individual tasks convincingly while gradually losing architectural consistency. Argus largely avoided that collapse.

### 2. Substantive frontend integration

Argus's major Angular pages inject real services and perform backend requests. The [character screen](https://github.com/Miotis2004/AutoDM-Local/blob/main/client/src/app/pages/characters/characters.component.ts), for example, loads, creates, edits, and deletes characters through `CharactersService`.

The same pattern appears across inventory, NPCs, quests, factions, locations, sessions, history, gameplay, and encounters.

### 3. Rich domain model

Argus models many gameplay concepts independently, including:

- Inventory holdings and transfers
- Ammunition and consumables
- Limited-use abilities and spell resources
- Currency
- Regions, locations, settlements, and points of interest
- Travel routes and party location
- Campaign events and session-event references
- Scenes and involved characters
- Encounters, combatants, initiative, and combat conditions
- NPC relationships and faction relationships
- Quest objectives and objective progress

The [architecture documentation](https://github.com/Miotis2004/AutoDM-Local/blob/main/docs/architecture.md) describes many of these relationships.

### 4. Deliberate local persistence

Argus provides a large, explicit [SQLite schema](https://github.com/Miotis2004/AutoDM-Local/blob/main/server/src/main/resources/schema.sql), a configurable database path, and idempotent startup initialization.

This is more deliberate than relying entirely on Hibernate's `ddl-auto=update`, especially for a persistence-heavy application intended to preserve long-running campaigns.

### 5. Coherent local development configuration

Argus's Angular application uses a [development proxy](https://github.com/Miotis2004/AutoDM-Local/blob/main/client/proxy.conf.json) that sends relative `/api` requests to the Spring Boot backend on port 5150. Its documented ports, npm start command, proxy, and backend configuration agree.

### 6. Real encounter implementation

The [Argus encounter component](https://github.com/Miotis2004/AutoDM-Local/blob/main/client/src/app/pages/encounters/encounters.component.ts) implements encounter selection, combatants, initiative, turn order, current turn, health, damage, healing, enemy actions, completion, winners, and narrative events.

This is not merely a routed placeholder or static mockup.

## Jules Strengths

### 1. Useful automated tests

Although the testing-policy difference prevents this from serving as a fair measure of agent superiority, Jules did produce useful tests for:

- Dice resolution
- Ability and skill checks
- Combat resolution
- Conditions
- Enemy behavior
- Encounter generation and progression
- Rest mechanics
- Deterministic DM behavior
- Narrative generation
- Exception handling
- Several controllers

The [deterministic DM engine tests](https://github.com/Miotis2004/AutoDM/blob/main/server/src/test/java/com/autodm/server/service/dm/DeterministicDungeonMasterEngineTest.java) cover successful actions, invalid actions, encounter restrictions, scene creation, campaign events, and missing campaigns.

These tests may provide useful patterns when testing is deliberately reintroduced into Argus.

### 2. Transactional discipline

Jules uses `@Transactional` throughout the service layer. This helps ensure that multi-write operations succeed or fail as a unit.

Argus currently lacks equivalent transaction boundaries around operations such as inventory transfers, session transitions, combat updates, and event-producing mutations.

### 3. Conventional organization

Jules uses a conventional backend package, `com.autodm.server`, and divides the Angular application into `core`, `features`, `shared`, and `layout` areas.

Argus remains under the placeholder package `com.example`, and several of its Angular components have grown unusually large.

### 4. Standard validation foundation

Jules includes Spring Boot Validation, uses `@Valid` at controller boundaries, and provides structured validation responses through its [global exception handler](https://github.com/Miotis2004/AutoDM/blob/main/server/src/main/java/com/autodm/server/exception/GlobalExceptionHandler.java).

Argus instead uses a custom DTO validator and does not obtain the same field-level validation behavior automatically.

## Jules Quality Concerns

The following findings are independent of the missing late-stage tasks.

### 1. Frontend and backend routes disagree

The [Jules character service](https://github.com/Miotis2004/AutoDM/blob/main/client/src/app/core/services/character.service.ts) requests:

```text
GET  /api/campaigns/{campaignId}/characters
POST /api/campaigns/{campaignId}/characters
```

The [backend character controller](https://github.com/Miotis2004/AutoDM/blob/main/server/src/main/java/com/autodm/server/controller/PlayerCharacterController.java) exposes:

```text
GET  /api/characters/campaign/{campaignId}
POST /api/characters
```

The same class of mismatch exists in the location, faction, and NPC services. The backend-communication services were already represented as completed work, so they should describe the actual backend contracts even before final UI integration.

### 2. Relative API URLs lack an Angular proxy

Jules's frontend calls relative `/api` URLs and runs plain `ng serve`. Its Angular configuration contains no proxy to the backend on port 8080.

The backend allows CORS from port 4200, but CORS does not redirect a relative request. A browser request to `/api/...` still goes to the Angular development server unless the frontend uses an absolute backend URL or configures a proxy.

This is a system-level integration defect.

### 3. Encounter task state does not match the implementation

The current [Jules encounter component](https://github.com/Miotis2004/AutoDM/blob/main/client/src/app/features/encounters/encounters.component.ts) contains only:

```typescript
@Component({
  selector: 'app-encounters',
  template: `<p>encounters works!</p>`,
})
export class EncountersComponent {}
```

This conflicts with the earlier task and commit history describing encounter UI implementation. It is not explained by the unfinished final integration tasks.

### 4. Repeated clarification did not prevent contract defects

Jules repeatedly asked questions despite instructions to make reasonable assumptions and proceed. Those interruptions might have been defensible if they produced greater cross-layer correctness. The route mismatches, proxy omission, and encounter discrepancy show that the additional human interaction did not reliably produce that benefit.

## Argus Quality Concerns and Hardening Priorities

Argus produced the stronger application, but several areas should be addressed during stabilization.

### 1. Add the deliberately deferred tests

The future test phase should emphasize:

- API contract tests between Angular services and Spring controllers
- Transactional persistence tests
- Campaign isolation
- Save, restart, and resume behavior
- Encounter progression and completion
- Inventory transfers and resource consumption
- Invalid-action behavior
- End-to-end campaign creation and session play

Jules's backend tests can serve as a source of test-case ideas, although they should not be copied without verifying that they match the Argus architecture.

### 2. Add transaction boundaries

Multi-write services should use `@Transactional`. The [Argus InventoryService](https://github.com/Miotis2004/AutoDM-Local/blob/main/server/src/main/java/com/example/service/InventoryService.java) is a clear example because a transfer can update a source holding, update or create a destination holding, record a transfer, and record an event.

Without one encompassing transaction, a failure can leave partially updated state.

### 3. Decompose oversized Angular components

Notable examples include:

- Encounter component: approximately 852 lines
- Play component: approximately 493 lines
- Campaign store: approximately 419 lines
- Session component: approximately 348 lines
- History component: approximately 326 lines

These should be divided into focused presentation components, state facades, and reusable form or combat-log components.

### 4. Correct interceptor registration

Argus registers a class-based interceptor through `HTTP_INTERCEPTORS` while configuring standalone Angular HTTP with `provideHttpClient()`.

The configuration should be reviewed and likely changed to include `withInterceptorsFromDi()`, or the interceptor should be converted to a functional interceptor. Otherwise, the global interceptor may not run.

### 5. Replace placeholder package naming

The backend should move from `com.example` to a project-specific package such as `com.autodm.server` before external release or long-term maintenance.

### 6. Improve documentation accuracy

The root README incorrectly describes AutoDM as an "automated direct messaging" application. It should identify the application as a local automated Dungeon Master.

Some architecture prose also contains formatting artifacts that should be cleaned during documentation review.

### 7. Use structured logging

`System.err.println` should be replaced with an SLF4J logger so errors include consistent levels, context, and stack traces where appropriate.

### 8. Reconsider silent schema errors

`spring.sql.init.continue-on-error=true` may allow startup with a partially incomplete schema. For authoritative campaign persistence, schema failures should normally stop startup with a clear diagnostic.

### 9. Connect storage settings to backend behavior

Argus's settings screen stores a database path preference in browser `localStorage`, but that preference does not change the backend SQLite location.

The user interface should either connect this setting to an actual backend configuration mechanism or relabel it so it does not imply behavior that is not implemented.

## Autonomy Assessment

Argus behaved more like an autonomous development system. Jules behaved more like a capable coding assistant requiring a human scheduler and merge coordinator.

Argus's manual-review interruptions should be separated into three categories:

1. **Report-formatting failures:** Output did not satisfy the review protocol.
2. **Implementation failures:** Code was incorrect, incomplete, or could not pass a required gate.
3. **Human decision requests:** Product ambiguity required an operator decision.

The observed Argus reviews appear to have been primarily the first category. This is still an agent-system weakness, but it is much narrower and more mechanically correctable than repeated product or architectural uncertainty.

A strict report-schema validator should be added before Argus requests manual review. Malformed reports should be repaired automatically or returned to the worker instead of escalating immediately to the human operator.

## Final Judgment

After controlling for the different testing policies, Git workflows, and unfinished late-stage tasks, **AutoDM-Local is the stronger repository and Argus is the stronger performer in this experiment**.

Argus won on the dimensions the experiment was primarily designed to test:

1. Long-horizon execution
2. Autonomous task progression
3. Instruction following
4. Context retention
5. Cross-task architectural consistency
6. Reasonable assumption-making
7. End-to-end implementation coherence
8. Reduction of human workload

Jules demonstrated useful strengths in conventional tests, transaction handling, validation infrastructure, and localized code organization. These are worth incorporating into the Argus hardening phase. They do not offset the central result: Argus produced the more coherent application with substantially less human orchestration.

The experiment does not prove that Argus's underlying language model is universally superior to Gemini. It does provide strong evidence that, for this planning artifact and development workflow, the complete Argus agent system managed long-running autonomous software development more effectively than Jules.
