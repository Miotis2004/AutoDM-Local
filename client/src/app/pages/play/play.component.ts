import { Component, OnDestroy, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';

import { Title } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';

import { CampaignStore } from '../../services/campaign-store.service';
import { DungeonMasterService } from '../../services/dungeon-master.service';
import { SceneService } from '../../services/scene.service';
import { SceneBrief, EngineResponse, AbilityCheckResult } from '../../models/dm';
import { Character } from '../../models/character';
import { Npc, Disposition } from '../../models/npc';
import { Location } from '../../models/world';
import { CampaignEvent, CampaignEventType } from '../../models/campaign-event';
import { NarrativeService } from '../../narrative/narrative.service';
import { NarrativeCategory, NarrativeEntry } from '../../narrative/narrative';
import { GameLogComponent } from '../../narrative/game-log.component';

/**
 * The Play screen: the primary play surface of AutoDM.
 *
 * <p>This screen drives a single scene. It shows the DM narrative for the scene currently in
 * focus, the party summary, the current scene and location, the relevant NPCs, the encounter
 * status, and the most recent dice rolls, and it offers the action input area that sends a player
 * action to the Dungeon Master engine. The narrative log ({@link GameLogComponent}) records every
 * moment - DM narration, a player action, a dice result, a combat event, and a system event - with
 * each line tagged by its category.</p>
 *
 * <p>The active scene id is resolved from the back-end scene list (the one scene whose status is
 * {@code ACTIVE}); the engine needs it to resolve an action. The engine response refreshes the
 * presented scene brief, so the narrative stays current as play progresses.</p>
 */
@Component({
  selector: 'app-play',
  standalone: true,
  templateUrl: './play.component.html',
  styleUrl: './play.component.css',
  imports: [CommonModule, ReactiveFormsModule, GameLogComponent],
})
export class PlayComponent implements OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly store = inject(CampaignStore);
  private readonly dungeonMaster = inject(DungeonMasterService);
  private readonly scenes = inject(SceneService);
  private readonly narrative = inject(NarrativeService);
  private readonly title = inject(Title);

  private readonly subscription = new Subscription();

  /** The active campaign id, or {@code null} while no campaign is selected. */
  campaignId: number | null = null;

  /** The active scene id the current action resolves against, when known. */
  sceneId: number | null = null;

  /** The scene brief most recently presented by the engine, or {@code null}. */
  sceneBrief: SceneBrief | null = null;

  /** True while the campaign and its scenes are being loaded. */
  loading = true;

  /** Whether a campaign has been selected. */
  get hasCampaign(): boolean {
    return this.campaignId !== null;
  }

  /** True while a request is in flight, disabling interactive controls. */
  submitting = false;

  /** The latest engine response, when an action has been resolved. */
  response: EngineResponse | null = null;

  /** Error surfaced by the most recent request, if any. */
  error: string | null = null;

  /** The party roster. */
  characters: Character[] = [];

  /** The relevant NPCs (the party's own, active non-player characters). */
  npcs: Npc[] = [];

  /** Every location in the campaign, used to name the party's current location. */
  locations: Location[] = [];

  /** The current scene's location, when it can be resolved. */
  currentLocation: string | null = null;

  /** The campaign's event history, used to derive the encounter status. */
  events: CampaignEvent[] = [];

  /** The most recent dice rolls, newest last, drawn from the narrative log. */
  recentRolls: NarrativeEntry[] = [];

  /** The label shown for the scene before the engine has presented one. */
  readonly currentSceneTitle = 'The active scene';

  /**
   * The action form. The action text is required; the statistic, modifier, and difficulty are
   * optional overrides the Dungeon Master engine falls back to sensible defaults for.
   */
  readonly actionForm = this.formBuilder.group({
    action: ['', [Validators.required, Validators.maxLength(500)]],
    statistic: [''],
    modifier: [''],
    difficulty: [''],
  });

  /** Every character in the party. */
  get hasParty(): boolean {
    return this.characters.length > 0;
  }

  /** Whether an active encounter is currently in progress. */
  get hasEncounter(): boolean {
    const combatants = this.sceneBrief?.combatants;
    if (combatants && combatants.length > 0) {
      return true;
    }
    const last = this.events.at(-1);
    return last != null &&
      (last.eventType === CampaignEventType.COMBAT ||
        last.eventType === CampaignEventType.DAMAGE);
  }

  /**
   * The label describing the current encounter status.
   */
  encounterStatus(): string {
    if (this.hasEncounter) {
      const combatants = this.sceneBrief?.combatants;
      if (combatants && combatants.length > 0) {
        return `Combat in progress (${combatants.length} combatant${combatants.length === 1 ? '' : 's'})`;
      }
      return 'Combat';
    }
    return 'No active encounter';
  }

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Play');
    this.subscription.add(
      this.store.activeCampaign$.subscribe((campaign) => {
        if (campaign) {
          this.campaignId = campaign.id;
          this.sceneId = null;
          this.sceneBrief = null;
          this.response = null;
          this.error = null;
          this.actionForm.reset();
          this.load();
        }
      }),
    );
    this.subscription.add(
      this.narrative.changes().subscribe(() => {
        this.recentRolls = this.narrative
          .getEntries()
          .filter((entry) => entry.category === NarrativeCategory.DICE_RESULT)
          .slice(-5);
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /**
   * Loads the campaign collections and resolves the active scene id.
   */
  private load(): void {
    if (this.campaignId === null) {
      return;
    }
    this.loading = true;
    this.error = null;
    this.subscription.add(
      this.store.characters$.subscribe((characters) => {
        this.characters = characters;
      }),
    );
    this.subscription.add(
      this.store.npcs$.subscribe((npcs) => {
        this.npcs = npcs.filter((npc) => npc.active);
      }),
    );
    this.subscription.add(
      this.store.locations$.subscribe((locations) => {
        this.locations = locations;
      }),
    );
    this.subscription.add(
      this.store.events$.subscribe((events) => {
        this.events = events;
      }),
    );
    this.store.partyLocation$.subscribe((location) => {
      this.currentLocation = this.resolveLocationName(location);
    });
    this.scenes.list(this.campaignId).subscribe({
      next: (scenesList) => {
        this.sceneId = this.scenes.activeScene(scenesList)?.id ?? null;
        this.loading = false;
      },
      error: () => {
        this.sceneId = null;
        this.loading = false;
      },
    });
  }

  /**
   * @param location the party's current location
   * @return the name of the location, or {@code null} when it cannot be resolved
   */
  private resolveLocationName(
    location: { locationId?: number } | null,
  ): string | null {
    const id = location?.locationId;
    if (id == null) {
      return null;
    }
    return (
      this.locations.find((entry) => entry.id === id)?.name ??
      `Location ${id}`
    );
  }

  /**
   * Resolves the scene id to send the action against, refreshing it from the engine response.
   *
   * @param response the engine response just resolved
   */
  private refreshSceneId(response: EngineResponse): void {
    if (response.scene?.sceneId) {
      this.sceneId = response.scene.sceneId;
    }
    this.sceneBrief = response.scene;
  }

  /**
   * Sends the action in the form through the Dungeon Master engine.
   */
  submitAction(): void {
    if (this.actionForm.invalid) {
      this.actionForm.markAllAsTouched();
      return;
    }
    if (this.campaignId === null || this.sceneId === null) {
      this.error = 'No active scene to act against yet.';
      return;
    }
    this.setSubmitting(true);
    this.error = null;
    const value = this.actionForm.getRawValue();
    const request = {
      action: value.action ?? '',
      statistic: this.parseInt(value.statistic),
      modifier: this.parseInt(value.modifier),
      difficulty: this.parseInt(value.difficulty),
    };
    this.dungeonMaster.resolveAction(this.campaignId, this.sceneId, request).subscribe({
      next: (response) => {
        this.setSubmitting(false);
        this.response = response;
        this.refreshSceneId(response);
        this.actionForm.reset();
        this.recordResponse(response);
      },
      error: (err) => {
        this.setSubmitting(false);
        const detail = err?.message;
        this.error = detail ?? 'The action could not be resolved.';
        // An unrecognised or impossible action is a 400 carrying the validation problem. Surface it
        // as a player-action line with the real message rather than a silent failure.
        this.recordUnrecognized(this.error ?? 'Nothing happens.');
      },
    });
  }

  /**
   * Advances the active scene to the next one.
   */
  advanceScene(): void {
    if (this.campaignId === null) {
      return;
    }
    this.setSubmitting(true);
    this.error = null;
    this.dungeonMaster.advanceScene({ campaignId: this.campaignId }).subscribe({
      next: (response) => {
        this.setSubmitting(false);
        this.response = response;
        this.refreshSceneId(response);
        this.recordResponse(response);
        // Refresh the scene id from the list now that focus has moved.
        const campaignId = this.campaignId;
        if (campaignId === null) {
          return;
        }
        this.scenes.list(campaignId).subscribe((scenesList) => {
          this.sceneId = this.scenes.activeScene(scenesList)?.id ?? null;
        });
      },
      error: (err) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'The scene could not be advanced.';
      },
    });
  }

  /**
   * Records the narrative lines for a resolved engine response: the DM narration for the scene,
   * the player-action line, and - when the action carried an ability check - the dice result.
   *
   * @param response the resolved engine response
   */
  private recordResponse(response: EngineResponse): void {
    this.narrative.append(this.narrationEntry(response.scene));
    this.narrative.append(this.actionEntry(response));
    const check = response.check;
    if (check) {
      this.narrative.append(this.rollEntry(check));
    }
  }

  /**
   * Records an unrecognized action as a player-action line carrying the validation message.
   *
   * @param message the validation problem reported by the engine
   */
  private recordUnrecognized(message: string): void {
    this.narrative.append({
      category: NarrativeCategory.PLAYER_ACTION,
      title: 'Unrecognized',
      message,
      timestamp: new Date().toISOString(),
      data: { recognized: false },
    });
  }

  /**
   * Builds the DM-narration line for the presented scene.
   *
   * @param scene the scene brief, when present
   * @return the narrative entry
   */
  private narrationEntry(scene: SceneBrief | undefined): NarrativeEntry {
    return {
      category: NarrativeCategory.DM_NARRATION,
      title: scene?.sceneTitle ?? 'The scene',
      message:
        scene?.sceneNarrative && scene.sceneNarrative.trim().length > 0
          ? scene.sceneNarrative
          : `You are in ${scene?.sceneTitle ?? 'the scene'}.`,
      timestamp: new Date().toISOString(),
      data: {
        sceneId: scene?.sceneId,
        title: scene?.sceneTitle,
        narrative: scene?.sceneNarrative,
        combatantNames: scene?.combatants,
      },
    };
  }

  /**
   * Builds the player-action line for a resolved response.
   *
   * @param response the resolved engine response
   * @return the narrative entry
   */
  private actionEntry(response: EngineResponse): NarrativeEntry {
    const recognized = response.recognized;
    return {
      category: NarrativeCategory.PLAYER_ACTION,
      title: recognized ? 'Player action' : 'Unrecognized',
      message: recognized
        ? response.response
        : (response.validationErrors?.[0] ?? 'Nothing happens.'),
      timestamp: new Date().toISOString(),
      data: { recognized, response: response.response },
    };
  }

  /**
   * Builds the dice-result line for a resolved ability check, reconstructing the single die that
   * was rolled so the roll summary can render.
   *
   * @param check the resolved ability check
   * @return the narrative entry
   */
  private rollEntry(check: AbilityCheckResult): NarrativeEntry {
    return {
      category: NarrativeCategory.DICE_RESULT,
      title: `Dice roll (${check.statistic})`,
      message: `d${check.roll} = ${check.roll}; ${check.statistic} ${check.modifier >= 0 ? '+' : ''}${check.modifier} = ${check.total} - ${check.success ? 'success' : 'failure'}.`,
      timestamp: new Date().toISOString(),
      data: {
        statistic: check.statistic,
        modifier: check.modifier,
        roll: check.roll,
        total: check.total,
        difficulty: check.difficulty,
        outcome: check.success ? 'SUCCESS' : 'FAILURE',
        dice: [{ sides: 20, value: check.roll }],
      },
    };
  }

  /**
   * Parses an optional numeric form value, returning {@code undefined} when it is empty so the
   * engine applies its own default.
   *
   * @param value the raw form value
   * @return the parsed number, or {@code undefined}
   */
  private parseInt(value: string | null | undefined): number | undefined {
    if (value == null) {
      return undefined;
    }
    const parsed = Number(value);
    return value.trim() === '' || Number.isNaN(parsed)
      ? undefined
      : parsed;
  }

  /**
   * @param value whether a request is in flight
   */
  private setSubmitting(value: boolean): void {
    this.submitting = value;
  }

  /**
   * @param character the character to describe
   * @return the non-empty identity parts (ancestry and class) joined by a bullet
   */
  characterIdentity(character: Character): string {
    return [character.ancestry, character.characterClass]
      .filter((value): value is string => Boolean(value))
      .join(' / ');
  }

  /**
   * @param character the character to describe
   * @return the character's current hit points as a percentage of the maximum, clamped to 0-100
   */
  hpPercent(character: Character): number {
    if (character.maxHitPoints <= 0) {
      return 100;
    }
    return Math.min(100, Math.max(0, (character.hitPoints / character.maxHitPoints) * 100));
  }

  /**
   * @param npc the NPC to describe
   * @return a short disposition badge, or an unlabeled badge when the disposition is unknown
   */
  dispositionBadge(npc: Npc): string {
    switch (npcDisposition(npc)) {
      case Disposition.FRIENDLY:
        return 'Friendly';
      case Disposition.HOSTILE:
        return 'Hostile';
      case Disposition.NEUTRAL:
        return 'Neutral';
      default:
        return '';
    }
  }

  /**
   * @param npc the NPC to describe
   * @return the non-empty identity parts (role, relationship) joined, for a short line
   */
  npcLine(npc: Npc): string {
    return [npc.role, npc.relationship]
      .filter((value): value is string => Boolean(value))
      .join(' · ');
  }
}

/**
 * Resolves an NPC's disposition, tolerating the optional field.
 *
 * @param npc the NPC
 * @return the disposition, or {@code null} when unset
 */
function npcDisposition(npc: Npc): Disposition | null {
  return npc.disposition ?? null;
}
