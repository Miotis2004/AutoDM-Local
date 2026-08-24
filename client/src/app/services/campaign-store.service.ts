import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, forkJoin, Observable, ReplaySubject } from 'rxjs';

import { Campaign } from '../models/campaign';
import { Character } from '../models/character';
import { Npc } from '../models/npc';
import { Quest } from '../models/quest';
import { InventoryItem } from '../models/item';
import { Session } from '../models/session';
import { Location, Region, PartyLocation } from '../models/world';
import { Faction } from '../models/faction';
import { CreatureTemplate } from '../models/creature-template';
import { CampaignEvent } from '../models/campaign-event';

import { CampaignsService } from './campaigns.service';
import { CharactersService } from './characters.service';
import { NpcsService } from './npcs.service';
import { QuestsService } from './quests.service';
import { ItemsService } from './items.service';
import { SessionsService } from './sessions.service';
import { WorldService } from './world.service';
import { FactionsService } from './faction.service';
import { CreatureTemplatesService } from './creature-template.service';
import { CampaignEventsService } from './campaign-event.service';

/**
 * The single authoritative home of campaign state.
 *
 * <p>Every feature area - campaigns, characters, NPCs, quests, items, sessions, and world data -
 * otherwise knows nothing about the active campaign. Without a shared owner, each component would
 * fetch and cache its own copy, and the app would drift into inconsistent, duplicated state.
 * {@code CampaignStore} owns that state instead.</p>
 *
 * <p>The store keeps, per campaign, the collections the services populate: the active
 * {@link Campaign}, its roster of {@link Character}s, its {@link Npc}s, its {@link Quest}s, its
 * {@link InventoryItem} holdings, its {@link Session}s, and its world contents. Switching the active
 * campaign swaps every collection at once, so components always observe a consistent snapshot.</p>
 *
 * <p>Each collection is a {@link BehaviorSubject} of {@code null} until the campaign is loaded, so
 * components can render a loading state and never read stale data from a different campaign.</p>
 */
@Injectable({ providedIn: 'root' })
export class CampaignStore {
  private readonly campaigns = inject(CampaignsService);
  private readonly characters = inject(CharactersService);
  private readonly npcs = inject(NpcsService);
  private readonly quests = inject(QuestsService);
  private readonly items = inject(ItemsService);
  private readonly sessions = inject(SessionsService);
  private readonly world = inject(WorldService);
  private readonly factions = inject(FactionsService);
  private readonly creatureTemplates = inject(CreatureTemplatesService);
  private readonly events = inject(CampaignEventsService);

  private readonly activeSubject = new BehaviorSubject<Campaign | null>(null);
  private readonly charactersByCampaign = new Map<number, Character[]>();
  private readonly npcsByCampaign = new Map<number, Npc[]>();
  private readonly questsByCampaign = new Map<number, Quest[]>();
  private readonly itemsByCampaign = new Map<number, InventoryItem[]>();
  private readonly sessionsByCampaign = new Map<number, Session[]>();
  private readonly regionsByCampaign = new Map<number, Region[]>();
  private readonly locationsByCampaign = new Map<number, Location[]>();
  private readonly factionsByCampaign = new Map<number, Faction[]>();
  private readonly templatesByCampaign = new Map<number, CreatureTemplate[]>();
  private readonly eventsByCampaign = new Map<number, CampaignEvent[]>();
  private readonly partyLocationByCampaign = new Map<number, PartyLocation>();

  /**
   * @return the active campaign, or {@code null} when none is selected
   */
  get activeCampaign(): Campaign | null {
    return this.activeSubject.value;
  }

  /**
   * @return an observable of the active campaign that emits on every change
   */
  get activeCampaign$(): Observable<Campaign | null> {
    return this.activeSubject.asObservable();
  }

  /**
   * Selects the active campaign and resumes it from its persisted state.
   *
   * <p>This is the single entry point for "open a campaign": components call {@code
   * store.select(campaignId)} and every observable below updates together. It performs two steps
   * so the resume survives application restarts:
   *
   * <ol>
   *   <li>makes the campaign active on the back-end, which persists the selection to a sidecar
   *       properties file (see {@code application.properties}) so the selection survives a restart;
   *   </li>
   *   <li>loads every persisted collection for the campaign - characters, NPCs, quests, items,
   *       sessions, and world data - replacing any data left over from a previous campaign.</li>
   * </ol>
   *
   * <p>Because the back-end marks the campaign active, a user who closes and reopens the
   * application can re-select this campaign and continue from the state that was persisted when it
   * was last active.</p>
   *
   * @param campaignId the campaign to make active
   * @return an observable that emits once the campaign has been made active
   */
  select(campaignId: number): Observable<Campaign> {
    const replay = new ReplaySubject<Campaign>(1);
    // Mark the campaign active on the back-end. This POST (not the read-only {@code get}) is what
    // persists the selection to the sidecar file, so the campaign stays selected across restarts.
    this.campaigns.select(campaignId).subscribe({
      next: (campaign: Campaign) => {
        this.activeSubject.next(campaign);
        replay.next(campaign);
        // Load the persisted collections so the app resumes with saved data instead of an empty
        // slate. The play page reads these collections (characters$, npcs$, etc.) to continue.
        this.clearAll();
        this.loadCollections(campaignId);
      },
      // A failure here (for example an unavailable back-end) must surface to the caller, not be
      // swallowed, so the campaigns screen can report it to the user.
      error: (err) => {
        replay.error(err);
        this.activeSubject.error(err);
      },
    });
    return replay;
  }

  /**
   * Loads every collection for the campaign and returns an observable that emits once they have all
   * been stored. Components can subscribe to {@link allLoaded} instead of composing the individual
   * streams. Equivalent to {@link select}; both make the campaign active and load its persisted
   * data, so a restart followed by a selection resumes from the saved state.
   *
   * @param campaignId the campaign to load
   * @return an observable of the active campaign, emitted after its collections are stored
   */
  loadAll(campaignId: number): Observable<Campaign> {
    return this.select(campaignId);
  }

  /**
   * Loads every persisted collection for the campaign into the store. Used by {@link select} so
   * that opening a campaign resumes from the state persisted on the back-end rather than an empty
   * slate.
   *
   * @param campaignId the campaign whose collections should be loaded
   */
  private loadCollections(campaignId: number): void {
    const loaded = forkJoin<
      [
        Character[],
        Npc[],
        Quest[],
        InventoryItem[],
        Session[],
        Region[],
        Location[],
        Faction[],
        CreatureTemplate[],
        CampaignEvent[],
      ]
    >([
      this.characters.list(campaignId),
      this.npcs.list(campaignId),
      this.quests.list(campaignId),
      this.items.list(campaignId),
      this.sessions.list(campaignId),
      this.world.listRegions(campaignId),
      this.world.listLocations(campaignId),
      this.factions.list(campaignId),
      this.creatureTemplates.list(campaignId),
      this.events.list(campaignId),
    ]);
    loaded.subscribe({
      next: ([
        characters,
        npcs,
        quests,
        items,
        sessions,
        regions,
        locations,
        factions,
        templates,
        events,
      ]) => {
        this.charactersByCampaign.set(campaignId, characters ?? []);
        this.npcsByCampaign.set(campaignId, npcs ?? []);
        this.questsByCampaign.set(campaignId, quests ?? []);
        this.itemsByCampaign.set(campaignId, items ?? []);
        this.sessionsByCampaign.set(campaignId, sessions ?? []);
        this.regionsByCampaign.set(campaignId, regions ?? []);
        this.locationsByCampaign.set(campaignId, locations ?? []);
        this.factionsByCampaign.set(campaignId, factions ?? []);
        this.templatesByCampaign.set(campaignId, templates ?? []);
        this.eventsByCampaign.set(campaignId, events ?? []);
      },
      // If the back-end cannot be reached (or any collection fails to load) this must not be
      // silently ignored: forward the error so the caller can report it to the user.
      error: (err: unknown) => {
        this.activeSubject.error(err);
      },
    });
  }

  /**
   * @return the characters for the active campaign, or an empty stream while it is loading
   */
  get characters$(): Observable<Character[]> {
    const id = this.activeSubject.value?.id;
    return id === undefined
      ? new Observable((subscriber) => subscriber.next([]))
      : new Observable((subscriber) => subscriber.next(this.charactersByCampaign.get(id) ?? []));
  }

  /**
   * @return the NPCs for the active campaign
   */
  get npcs$(): Observable<Npc[]> {
    const id = this.activeSubject.value?.id;
    return id === undefined
      ? new Observable((subscriber) => subscriber.next([]))
      : new Observable((subscriber) => subscriber.next(this.npcsByCampaign.get(id) ?? []));
  }

  /**
   * @return the quests for the active campaign
   */
  get quests$(): Observable<Quest[]> {
    const id = this.activeSubject.value?.id;
    return id === undefined
      ? new Observable((subscriber) => subscriber.next([]))
      : new Observable((subscriber) => subscriber.next(this.questsByCampaign.get(id) ?? []));
  }

  /**
   * @return the inventory holdings for the active campaign
   */
  get items$(): Observable<InventoryItem[]> {
    const id = this.activeSubject.value?.id;
    return id === undefined
      ? new Observable((subscriber) => subscriber.next([]))
      : new Observable((subscriber) => subscriber.next(this.itemsByCampaign.get(id) ?? []));
  }

  /**
   * @return the sessions for the active campaign
   */
  get sessions$(): Observable<Session[]> {
    const id = this.activeSubject.value?.id;
    return id === undefined
      ? new Observable((subscriber) => subscriber.next([]))
      : new Observable((subscriber) => subscriber.next(this.sessionsByCampaign.get(id) ?? []));
  }

  /**
   * @return the regions for the active campaign
   */
  get regions$(): Observable<Region[]> {
    const id = this.activeSubject.value?.id;
    return id === undefined
      ? new Observable((subscriber) => subscriber.next([]))
      : new Observable((subscriber) => subscriber.next(this.regionsByCampaign.get(id) ?? []));
  }

  /**
   * @return the factions for the active campaign
   */
  get factions$(): Observable<Faction[]> {
    const id = this.activeSubject.value?.id;
    return id === undefined
      ? new Observable((subscriber) => subscriber.next([]))
      : new Observable((subscriber) => subscriber.next(this.factionsByCampaign.get(id) ?? []));
  }

  /**
   * @return the creature templates for the active campaign
   */
  get creatureTemplates$(): Observable<CreatureTemplate[]> {
    const id = this.activeSubject.value?.id;
    return id === undefined
      ? new Observable((subscriber) => subscriber.next([]))
      : new Observable((subscriber) => subscriber.next(this.templatesByCampaign.get(id) ?? []));
  }

  /**
   * @return the campaign events for the active campaign, most recent first
   */
  get events$(): Observable<CampaignEvent[]> {
    const id = this.activeSubject.value?.id;
    return id === undefined
      ? new Observable((subscriber) => subscriber.next([]))
      : new Observable((subscriber) => subscriber.next(this.eventsByCampaign.get(id) ?? []));
  }

  /**
   * @return the locations for the active campaign
   */
  get locations$(): Observable<Location[]> {
    const id = this.activeSubject.value?.id;
    return id === undefined
      ? new Observable((subscriber) => subscriber.next([]))
      : new Observable((subscriber) => subscriber.next(this.locationsByCampaign.get(id) ?? []));
  }

  /**
   * @return the party's current location for the active campaign
   */
  get partyLocation$(): Observable<PartyLocation | null> {
    const id = this.activeSubject.value?.id;
    return id === undefined
      ? new Observable((subscriber) => subscriber.next(null))
      : new Observable((subscriber) => subscriber.next(this.partyLocationByCampaign.get(id) ?? null));
  }

  /**
   * Replaces the characters stored for a campaign.
   *
   * @param campaignId the campaign the characters belong to
   * @param characters the new roster
   */
  setCharacters(campaignId: number, characters: Character[]): void {
    this.charactersByCampaign.set(campaignId, characters);
  }

  /**
   * Replaces the NPCs stored for a campaign.
   *
   * @param campaignId the campaign the NPCs belong to
   * @param npcs the new list
   */
  setNpcs(campaignId: number, npcs: Npc[]): void {
    this.npcsByCampaign.set(campaignId, npcs);
  }

  /**
   * Replaces the quests stored for a campaign.
   *
   * @param campaignId the campaign the quests belong to
   * @param quests the new list
   */
  setQuests(campaignId: number, quests: Quest[]): void {
    this.questsByCampaign.set(campaignId, quests);
  }

  /**
   * Replaces the inventory stored for a campaign.
   *
   * @param campaignId the campaign the holdings belong to
   * @param items the new holdings
   */
  setItems(campaignId: number, items: InventoryItem[]): void {
    this.itemsByCampaign.set(campaignId, items);
  }

  /**
   * Replaces the sessions stored for a campaign.
   *
   * @param campaignId the campaign the sessions belong to
   * @param sessions the new list
   */
  setSessions(campaignId: number, sessions: Session[]): void {
    this.sessionsByCampaign.set(campaignId, sessions);
  }

  /**
   * Replaces the factions stored for a campaign.
   *
   * @param campaignId the campaign the factions belong to
   * @param factions the new list
   */
  setFactions(campaignId: number, factions: Faction[]): void {
    this.factionsByCampaign.set(campaignId, factions);
  }

  /**
   * Replaces the creature templates stored for a campaign.
   *
   * @param campaignId the campaign the templates belong to
   * @param templates the new list
   */
  setCreatureTemplates(campaignId: number, templates: CreatureTemplate[]): void {
    this.templatesByCampaign.set(campaignId, templates);
  }

  /**
   * Replaces the campaign events stored for a campaign.
   *
   * @param campaignId the campaign the events belong to
   * @param events the new list
   */
  setEvents(campaignId: number, events: CampaignEvent[]): void {
    this.eventsByCampaign.set(campaignId, events);
  }

  /**
   * Sets the party's current location for a campaign.
   *
   * @param campaignId the campaign the location belongs to
   * @param location the party's location
   */
  setPartyLocation(campaignId: number, location: PartyLocation): void {
    this.partyLocationByCampaign.set(campaignId, location);
  }

  private clearAll(): void {
    this.charactersByCampaign.clear();
    this.npcsByCampaign.clear();
    this.questsByCampaign.clear();
    this.itemsByCampaign.clear();
    this.sessionsByCampaign.clear();
    this.regionsByCampaign.clear();
    this.locationsByCampaign.clear();
    this.factionsByCampaign.clear();
    this.templatesByCampaign.clear();
    this.eventsByCampaign.clear();
    this.partyLocationByCampaign.clear();
  }
}
