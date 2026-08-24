import { Component, OnDestroy, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';

import { Title } from '@angular/platform-browser';

import { CampaignStore } from '../../services/campaign-store.service';
import { EncounterService } from '../../services/encounter.service';
import { CombatantService } from '../../services/combatant.service';
import { SceneService } from '../../services/scene.service';
import { NarrativeService } from '../../narrative/narrative.service';
import { Scene } from '../../models/scene';
import {
  Combatant,
  CombatantKind,
  Encounter,
  EncounterStatus,
  EnemyActionOutcome,
} from '../../models/encounter';
import { NarrativeCategory, NarrativeEntry } from '../../narrative/narrative';
import { GameLogComponent } from '../../narrative/game-log.component';

/**
 * The Encounters screen: the combat surface of AutoDM.
 *
 * <p>This screen drives a single turn-based encounter from start to finish. It lists a campaign's
 * encounters, lets the DM start one (creating it from the active scene and a location, or using an
 * already-generated encounter), and adds combatants for the heroes and enemies. While an encounter
 * is active it shows the turn order, the current turn, and every combatant's health, and it offers
 * the controls that progress the fight: advancing to the next turn, applying damage or healing to a
 * combatant, and driving an enemy's attack. When the encounter ends it reports the winner and lets
 * the DM mark it finished.</p>
 *
 * <p>Combat moments - enemy attacks, damage, and dice rolls - are rendered into the shared game log
 * ({@link GameLogComponent}) as {@link NarrativeCategory.COMBAT_EVENT} and
 * {@link NarrativeCategory.DICE_RESULT} lines, so both the combat events and the dice results are
 * visible on the screen.</p>
 */
@Component({
  selector: 'app-encounters',
  standalone: true,
  templateUrl: './encounters.component.html',
  styleUrl: './encounters.component.css',
  imports: [CommonModule, ReactiveFormsModule, GameLogComponent],
})
export class EncountersComponent implements OnDestroy {
  /**
   * The combatant-side enum, exposed so the template can pass it to {@link addCombatant}.
   */
  readonly CombatantKindRef = CombatantKind;

  private readonly formBuilder = inject(FormBuilder);
  private readonly store = inject(CampaignStore);
  private readonly encounters = inject(EncounterService);
  private readonly combatants = inject(CombatantService);
  private readonly scenes = inject(SceneService);
  private readonly narrative = inject(NarrativeService);
  private readonly title = inject(Title);

  private readonly subscription = new Subscription();

  /** The active campaign id, or {@code null} while no campaign is selected. */
  campaignId: number | null = null;

  /** Every encounter the campaign holds, oldest first. */
  encounterList: Encounter[] = [];

  /** The encounter currently being played on the surface. */
  selected: Encounter | null = null;

  /** Every combatant taking part in the selected encounter, kept in turn order when known. */
  combatantsList: Combatant[] = [];

  /** The turn order for the selected encounter, or {@code null} before it has been built. */
  turnOrder: Combatant[] | null = null;

  /** The combatant whose turn it is now, when a turn has been reached. */
  currentCombatant: Combatant | null = null;

  /** Whether the selected encounter has been reported complete. */
  complete = false;

  /** The side still standing when the encounter is over, when determinable. */
  winner: CombatantKind | null = null;

  /** True while the campaign and its encounters are being loaded. */
  loading = true;

  /** True while a request is in flight, disabling interactive controls. */
  submitting = false;

  /** Error surfaced by the most recent action, if any. */
  error: string | null = null;

  /** Whether any combatants are known for the selected encounter. */
  get hasCombatants(): boolean {
    return this.combatantsList.length > 0;
  }

  /** Whether the selected encounter is currently active. */
  get isActive(): boolean {
    return this.selected?.status === EncounterStatus.ACTIVE;
  }

  /** Whether the selected encounter has been finished. */
  get isFinished(): boolean {
    return this.selected?.status === EncounterStatus.FINISHED;
  }

  /**
   * The heroes in the selected encounter.
   */
  get heroes(): Combatant[] {
    return this.combatantsList.filter((c) => c.kind === CombatantKind.PLAYER);
  }

  /**
   * The enemies in the selected encounter.
   */
  get enemies(): Combatant[] {
    return this.combatantsList.filter((c) => c.kind === CombatantKind.ENEMY);
  }

  /**
   * Whether a turn order has been built for the selected encounter.
   */
  get hasTurnOrder(): boolean {
    return this.turnOrder !== null;
  }

  /**
   * The name of the scene the selected encounter is anchored to, when resolvable.
   */
  sceneTitle: string | null = null;

  /** The active scene id the encounter is anchored to, when known. */
  activeSceneId: number | null = null;

  /** The first location available to anchor a new encounter, when known. */
  firstLocationId: number | null = null;

  /**
   * The "start encounter" form. The encounter name is optional; the scene and location are derived
   * from campaign state.
   */
  readonly startForm = this.formBuilder.group({
    name: ['', [Validators.maxLength(200)]],
  });

  /**
   * The damage form: pick a combatant, then the amount to subtract from its hit points.
   */
  readonly damageForm = this.formBuilder.group({
    combatantId: ['', [Validators.required]],
    amount: ['', [Validators.required, Validators.min(1)]],
  });

  /**
   * The healing form: pick a combatant, then the amount to restore.
   */
  readonly healForm = this.formBuilder.group({
    combatantId: ['', [Validators.required]],
    amount: ['', [Validators.required, Validators.min(1)]],
  });

  /**
   * The enemy-attack form: an enemy attacks a living target.
   */
  readonly attackForm = this.formBuilder.group({
    combatantId: ['', [Validators.required]],
    attackBonus: ['0'],
    damage: ['1', [Validators.required, Validators.min(0)]],
    difficulty: ['10'],
  });

  /**
   * The combatant chosen in the damage form.
   */
  get damageCombatant(): Combatant | null {
    const id = this.damageForm.get('combatantId')?.value;
    return id ? this.combatantsList.find((c) => c.id === Number(id)) ?? null : null;
  }

  /**
   * The combatant chosen in the heal form.
   */
  get healCombatant(): Combatant | null {
    const id = this.healForm.get('combatantId')?.value;
    return id ? this.combatantsList.find((c) => c.id === Number(id)) ?? null : null;
  }

  /**
   * The enemy chosen in the attack form.
   */
  get attackEnemy(): Combatant | null {
    const id = this.attackForm.get('combatantId')?.value;
    return id ? this.combatantsList.find((c) => c.id === Number(id)) ?? null : null;
  }

  /**
   * Living enemies available to be targeted by an attack.
   */
  get livingEnemies(): Combatant[] {
    return this.enemies.filter((c) => !c.defeated);
  }

  /**
   * Living heroes available to be targeted by an attack.
   */
  get livingHeroes(): Combatant[] {
    return this.heroes.filter((c) => !c.defeated);
  }

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Encounters');
    this.subscription.add(
      this.store.activeCampaign$.subscribe((campaign) => {
        if (campaign) {
          this.campaignId = campaign.id;
          this.selected = null;
          this.combatantsList = [];
          this.turnOrder = null;
          this.currentCombatant = null;
          this.complete = false;
          this.winner = null;
          this.error = null;
          this.startForm.reset();
          this.damageForm.reset();
          this.healForm.reset();
          this.attackForm.reset();
          this.load();
        }
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /**
   * Loads the campaign's encounters and the scene/location context the surface needs.
   */
  private load(): void {
    if (this.campaignId === null) {
      return;
    }
    this.loading = true;
    this.error = null;
    this.subscription.add(
      this.encounters.list(this.campaignId).subscribe({
        next: (list) => {
          this.encounterList = list;
          this.loading = false;
        },
        error: () => {
          this.encounterList = [];
          this.loading = false;
        },
      }),
    );
    this.scenes.list(this.campaignId).subscribe({
      next: (scenesList: Scene[]) => {
        const active = scenesList.find((scene) => scene.status === 'ACTIVE');
        this.activeSceneId = (active ?? scenesList[0])?.id ?? null;
      },
      // A failure here (for example an unavailable back-end) must be surfaced, not silently
      // ignored, so the errors region reports it to the user.
      error: (err: unknown) => {
        this.error = (err as { message?: string })?.message ??
          'The scenes could not be loaded.';
      },
    });
    this.store.locations$.subscribe((locations) => {
      this.firstLocationId = locations[0]?.id ?? null;
    });
  }

  /**
   * Selects an existing encounter to play on the surface.
   *
   * @param encounter the encounter to open
   */
  selectEncounter(encounter: Encounter): void {
    this.selected = encounter;
    this.complete = false;
    this.winner = null;
    this.error = null;
    this.turnOrder = null;
    this.currentCombatant = null;
    this.subscription.add(
      this.combatants
        .listOfEncounter(this.campaignId as number, encounter.id)
        .subscribe({
          next: (list) => {
            this.combatantsList = list;
            if (list.length > 0 && encounter.status === EncounterStatus.ACTIVE) {
              this.refreshTurnState();
            }
          },
        }),
    );
  }

  /**
   * Starts a new encounter. Creates it from the active scene and a location when the campaign has no
   * scheduled encounter to open, then marks it active and builds its turn order.
   */
  startEncounter(): void {
    if (this.campaignId === null) {
      return;
    }
    if (this.activeSceneId === null || this.firstLocationId === null) {
      this.error = 'No active scene or location to anchor an encounter yet.';
      return;
    }
    this.setSubmitting(true);
    this.error = null;
    const value = this.startForm.getRawValue();
    const payload = {
      sceneId: this.activeSceneId,
      locationId: this.firstLocationId,
      name: value.name?.trim() || undefined,
    };
    this.encounters
      .create(
        this.campaignId,
        payload.sceneId,
        payload.locationId,
      )
      .subscribe({
        next: (created) => {
          this.afterStart(created);
        },
        error: (err) => {
          this.setSubmitting(false);
          this.error =
            err?.message ?? 'The encounter could not be started.';
        },
      });
  }

  /**
   * Refreshes the campaign's encounter list after an encounter changes.
   */
  private refreshList(): void {
    this.subscription.add(
      this.encounters.list(this.campaignId as number).subscribe((list) => {
        this.encounterList = list;
      }),
    );
  }

  /**
   * Marks a freshly created encounter active, tracks it, and builds its turn order.
   *
   * @param created the newly created encounter
   */
  private afterStart(created: Encounter): void {
    this.subscription.add(
      this.encounters.begin(this.campaignId as number, created.id).subscribe({
        next: (started) => {
          this.setSubmitting(false);
          this.selected = started;
          this.refreshList();
          this.refreshTurnState();
          this.narrative.append(this.encounterStartEntry(started));
        },
        error: (err) => {
          this.setSubmitting(false);
          this.error = err?.message ?? 'The encounter could not be started.';
        },
      }),
    );
  }

  /**
   * Builds the turn order, resolves the current turn, and reloads the combatants and completion
   * state for the selected encounter.
   */
  private refreshTurnState(): void {
    if (this.campaignId === null || this.selected === null) {
      return;
    }
    const encounterId = this.selected.id;
    const campaignId = this.campaignId as number;
    this.subscription.add(
      this.combatants.buildTurnOrder(campaignId, encounterId).subscribe({
        next: (order) => {
          this.turnOrder = order;
          if (order.length > 0) {
            this.combatantsList = order;
          }
          this.subscription.add(
            this.combatants.currentCombatant(campaignId, encounterId).subscribe({
              next: (current) => {
                this.currentCombatant = current;
              },
            }),
          );
          this.subscription.add(
            this.combatants.isComplete(campaignId, encounterId).subscribe({
              next: (complete) => {
                this.complete = complete;
                if (complete) {
                  this.resolveWinner();
                }
              },
            }),
          );
        },
      }),
    );
  }

  /**
   * Resolves and records the winner of a complete encounter.
   */
  private resolveWinner(): void {
    if (this.campaignId === null || this.selected === null) {
      return;
    }
    const campaignId = this.campaignId as number;
    const encounterId = this.selected.id;
    this.subscription.add(
      this.combatants.winner(campaignId, encounterId).subscribe({
        next: (side) => {
          this.winner = side;
        },
      }),
    );
  }

  /**
   * Advances the encounter to the next turn.
   */
  nextTurn(): void {
    if (this.campaignId === null || this.selected === null) {
      return;
    }
    this.setSubmitting(true);
    this.error = null;
    this.subscription.add(
      this.combatants.nextTurn(this.campaignId, this.selected.id).subscribe({
        next: () => {
          this.setSubmitting(false);
          this.refreshTurnState();
        },
        error: (err) => {
          this.setSubmitting(false);
          this.error = err?.message ?? 'The turn could not be advanced.';
        },
      }),
    );
  }

  /**
   * Adds a hero or an enemy to the selected encounter.
   */
  addCombatant(kind: CombatantKind): void {
    if (this.campaignId === null || this.selected === null) {
      return;
    }
    this.setSubmitting(true);
    this.error = null;
    const campaignId = this.campaignId as number;
    const encounterId = this.selected!.id;
    const name = `New ${kind === CombatantKind.PLAYER ? 'hero' : 'enemy'}`;
    this.combatants
      .create(campaignId, name, kind, 10, 10)
      .subscribe({
        next: (created) => {
          this.subscription.add(
            this.combatants
              .join(campaignId, created.id, encounterId)
              .subscribe({
                next: () => {
                  this.setSubmitting(false);
                  this.reloadCombatants();
                },
              }),
          );
        },
        error: (err) => {
          this.setSubmitting(false);
          this.error = err?.message ?? 'The combatant could not be added.';
        },
      });
  }

  /**
   * Applies damage to the combatant chosen in the damage form.
   */
  applyDamageToCombatant(): void {
    if (this.damageForm.invalid || this.damageCombatant === null) {
      this.damageForm.markAllAsTouched();
      return;
    }
    if (this.campaignId === null) {
      return;
    }
    this.setSubmitting(true);
    this.error = null;
    const value = this.damageForm.getRawValue();
    this.combatants
      .applyDamage(
        this.campaignId,
        this.damageCombatant!.id,
        Number(value.amount),
      )
      .subscribe({
        next: (updated) => {
          this.setSubmitting(false);
          this.updateCombatant(updated);
          this.damageForm.reset({ combatantId: '', amount: '' });
          this.recordDamageEvent(updated);
        },
        error: (err) => {
          this.setSubmitting(false);
          this.error = err?.message ?? 'Damage could not be applied.';
        },
      });
  }

  /**
   * Heals the combatant chosen in the heal form.
   */
  applyHeal(): void {
    if (this.healForm.invalid || this.healCombatant === null) {
      this.healForm.markAllAsTouched();
      return;
    }
    if (this.campaignId === null) {
      return;
    }
    this.setSubmitting(true);
    this.error = null;
    const value = this.healForm.getRawValue();
    this.combatants
      .heal(this.campaignId, this.healCombatant!.id, Number(value.amount))
      .subscribe({
        next: (updated) => {
          this.setSubmitting(false);
          this.updateCombatant(updated);
          this.healForm.reset({ combatantId: '', amount: '' });
          this.narrative.append(this.healEntry(updated));
        },
        error: (err) => {
          this.setSubmitting(false);
          this.error = err?.message ?? 'Healing could not be applied.';
        },
      });
  }

  /**
   * Drives the enemy chosen in the attack form against the living targets of its opposition.
   */
  runEnemyAttack(): void {
    if (this.attackForm.invalid || this.attackEnemy === null) {
      this.attackForm.markAllAsTouched();
      return;
    }
    if (this.campaignId === null) {
      return;
    }
    this.setSubmitting(true);
    this.error = null;
    const value = this.attackForm.getRawValue();
    this.combatants
      .enemyAttack(
        this.campaignId,
        this.attackEnemy!.id,
        parseInt(value.attackBonus ?? '0', 10) || 0,
        parseInt(value.damage ?? '1', 10) || 0,
        parseInt(value.difficulty ?? '10', 10) || 10,
      )
      .subscribe({
        next: (outcome) => {
          this.setSubmitting(false);
          this.updateCombatantsFromOutcome(outcome);
          this.recordAttack(outcome);
        },
        error: (err) => {
          this.setSubmitting(false);
          this.error = err?.message ?? 'The enemy action could not be resolved.';
        },
      });
  }

  /**
   * Marks the selected encounter finished.
   */
  finishEncounter(): void {
    if (this.campaignId === null || this.selected === null) {
      return;
    }
    this.setSubmitting(true);
    this.error = null;
    this.encounters
      .finish(this.campaignId, this.selected.id)
      .subscribe({
        next: (finished) => {
          this.setSubmitting(false);
          this.selected = finished;
          this.complete = true;
          this.refreshList();
        },
        error: (err) => {
          this.setSubmitting(false);
          this.error = err?.message ?? 'The encounter could not be finished.';
        },
      });
  }

  /**
   * Reloads the combatants for the selected encounter after a mutation.
   */
  private reloadCombatants(): void {
    if (this.campaignId === null || this.selected === null) {
      return;
    }
    this.subscription.add(
      this.combatants
        .listOfEncounter(this.campaignId, this.selected!.id)
        .subscribe({
          next: (list) => {
            this.combatantsList = list;
            this.turnOrder = null;
            this.currentCombatant = null;
          },
        }),
    );
  }

  /**
   * Replaces a single combatant in the local list with its refreshed copy.
   *
   * @param updated the refreshed combatant
   */
  private updateCombatant(updated: Combatant): void {
    const index = this.combatantsList.findIndex((c) => c.id === updated.id);
    if (index >= 0) {
      this.combatantsList = [...this.combatantsList.slice(0, index), updated, ...this.combatantsList.slice(index + 1)];
    }
    if (this.turnOrder) {
      const tIndex = this.turnOrder.findIndex((c) => c.id === updated.id);
      if (tIndex >= 0) {
        this.turnOrder = [...this.turnOrder.slice(0, tIndex), updated, ...this.turnOrder.slice(tIndex + 1)];
      }
    }
  }

  /**
   * Refreshes the local combatant list from an enemy-attack outcome.
   *
   * @param outcome the outcome of the enemy's action
   */
  private updateCombatantsFromOutcome(outcome: EnemyActionOutcome): void {
    const refreshed = [...this.combatantsList];
    if (outcome.attacker) {
      this.replace(refreshed, outcome.attacker);
    }
    if (outcome.target) {
      this.replace(refreshed, outcome.target);
    }
    this.combatantsList = refreshed;
  }

  /**
   * Replaces a combatant in a list with its refreshed copy.
   *
   * @param list the list to update in place
   * @param updated the refreshed combatant
   */
  private replace(list: Combatant[], updated: Combatant): void {
    const index = list.findIndex((c) => c.id === updated.id);
    if (index >= 0) {
      list[index] = updated;
    }
  }

  /**
   * Records a damage application as a combat-event line in the game log.
   *
   * @param combatant the combatant that took damage
   */
  private recordDamageEvent(combatant: Combatant): void {
    this.narrative.append(this.combatEventEntry({
      actionTaken: true,
      attacker: combatant,
      target: combatant,
      hit: true,
      damageApplied: 0,
      damageType: 'PHYSICAL',
      targetDefeated: combatant.defeated,
      attackRollTotal: 0,
      difficulty: 0,
    }));
  }

  /**
   * Records an enemy attack as a combat-event line in the game log.
   *
   * @param outcome the outcome of the enemy's action
   */
  private recordAttack(outcome: EnemyActionOutcome): void {
    this.narrative.append(this.combatEventEntry(outcome));
    if (outcome.actionTaken) {
      this.narrative.append(this.rollEntry(outcome));
    }
  }

  /**
   * Builds the encounter-start line for the game log.
   *
   * @param encounter the started encounter
   * @return the narrative entry
   */
  private encounterStartEntry(encounter: Encounter): NarrativeEntry {
    return {
      category: NarrativeCategory.SYSTEM_EVENT,
      title: 'Encounter begins',
      message: `The ${encounter.name ?? 'encounter'} begins.`,
      timestamp: new Date().toISOString(),
      data: { encounterId: encounter.id, status: encounter.status },
    };
  }

  /**
   * Builds the healing line for the game log.
   *
   * @param combatant the healed combatant
   * @return the narrative entry
   */
  private healEntry(combatant: Combatant): NarrativeEntry {
    return {
      category: NarrativeCategory.COMBAT_EVENT,
      title: 'Healing',
      message: `${combatant.name} recovers ${combatant.hitPoints} hit points.`,
      timestamp: new Date().toISOString(),
      data: { combatantId: combatant.id, healed: combatant.hitPoints },
    };
  }

  /**
   * Builds a combat-event line for the game log from an outcome.
   *
   * @param outcome the outcome of the enemy's action
   * @return the narrative entry
   */
  private combatEventEntry(outcome: EnemyActionOutcome): NarrativeEntry {
    if (!outcome.actionTaken) {
      return {
        category: NarrativeCategory.COMBAT_EVENT,
        title: 'Combat',
        message: `${outcome.attacker.name} has no living target.`,
        timestamp: new Date().toISOString(),
        data: { actionTaken: false },
      };
    }
    return {
      category: NarrativeCategory.COMBAT_EVENT,
      title: outcome.hit ? 'Attack lands' : 'Attack misses',
      message: `${outcome.attacker.name} attacks ${outcome.target?.name} - ${outcome.hit ? 'hits' : 'misses'} for ${outcome.damageApplied} ${outcome.damageType} damage${outcome.targetDefeated ? '; the target is defeated' : ''}.`,
      timestamp: new Date().toISOString(),
      data: {
        attacker: outcome.attacker.name,
        target: outcome.target?.name,
        hit: outcome.hit,
        damageApplied: outcome.damageApplied,
        damageType: outcome.damageType,
        targetDefeated: outcome.targetDefeated,
      },
    };
  }

  /**
   * Builds the dice-result line for an enemy attack roll.
   *
   * @param outcome the outcome of the enemy's action
   * @return the narrative entry
   */
  private rollEntry(outcome: EnemyActionOutcome): NarrativeEntry {
    return {
      category: NarrativeCategory.DICE_RESULT,
      title: 'Attack roll',
      message: `d20 + ${outcome.attackRollTotal - 20} = ${outcome.attackRollTotal} vs ${outcome.difficulty}.`,
      timestamp: new Date().toISOString(),
      data: {
        attackRollTotal: outcome.attackRollTotal,
        difficulty: outcome.difficulty,
        hit: outcome.hit,
      },
    };
  }

  /**
   * @param value whether a request is in flight
   */
  private setSubmitting(value: boolean): void {
    this.submitting = value;
  }

  /**
   * @param combatant the combatant to describe
   * @return the combatant's hit points as a percentage of the maximum, clamped to 0-100
   */
  hpPercent(combatant: Combatant): number {
    if (combatant.maxHitPoints <= 0) {
      return 100;
    }
    return Math.min(100, Math.max(0, (combatant.hitPoints / combatant.maxHitPoints) * 100));
  }

  /**
   * @param combatant the combatant to describe
   * @return the health badge label, naming a defeated combatant
   */
  healthLabel(combatant: Combatant): string {
    if (combatant.defeated) {
      return 'Defeated';
    }
    return `${combatant.hitPoints} / ${combatant.maxHitPoints} HP`;
  }

  /**
   * @param kind the combatant side
   * @return a short badge label for the side
   */
  sideLabel(kind: CombatantKind): string {
    return kind === CombatantKind.PLAYER ? 'Hero' : 'Enemy';
  }

  /**
   * @param status the encounter status
   * @return the human-readable status label
   */
  statusLabel(status: EncounterStatus): string {
    switch (status) {
      case EncounterStatus.SCHEDULED:
        return 'Scheduled';
      case EncounterStatus.ACTIVE:
        return 'Active';
      case EncounterStatus.FINISHED:
        return 'Finished';
      default:
        return status;
    }
  }
}
