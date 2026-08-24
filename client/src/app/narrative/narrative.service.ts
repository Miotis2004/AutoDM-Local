import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { NarrativeCategory, NarrativeEntry } from './narrative';

/**
 * The front-end client for the back-end narrative template system.
 *
 * <p>The narrative templates own every rule for turning structured game state into a line for the
 * game log; this service only asks the back-end for those lines and passes them to the log. It maps
 * the two endpoints exposed by {@code server/.../NarrativeController} onto small typed methods:
 * listing the categories that have a template, rendering a single category from a data map, rolling
 * dice and rendering the result, and appending an entry the caller already holds (for example one
 * the engine produced locally).</p>
 *
 * <p>Every method returns the structured {@link NarrativeEntry} the game log consumes, so the
 * front-end never re-derives narrative rules - it only renders what the back-end produced.</p>
 */
@Injectable({ providedIn: 'root' })
export class NarrativeService {
  private readonly http = inject(HttpClient);

  /**
   * @return the categories that currently have a back-end template
   */
  categories(): Observable<NarrativeCategory[]> {
    return this.http.get<NarrativeCategory[]>('/api/narrative/categories');
  }

  /**
   * Renders a single category from a free-form structured data map.
   *
   * @param category the category to render
   * @param data the structured game state describing the moment
   * @return the rendered game-log entry
   */
  render(category: NarrativeCategory, data: Record<string, unknown>): Observable<NarrativeEntry> {
    return this.http.post<NarrativeEntry>('/api/narrative/render', { category, data });
  }

  /**
   * Rolls dice on the back-end and renders the roll as a dice-result entry.
   *
   * @param sides the number of faces for each die to roll (repeatable)
   * @param modifier the value added to the sum of the dice (defaults to 0)
   * @return the rendered dice-result entry
   */
  dice(sides: number[], modifier = 0): Observable<NarrativeEntry> {
    return this.http.post<NarrativeEntry>('/api/narrative/dice', { sides, modifier });
  }

  /**
   * Appends an entry the caller already holds, without a back-end round trip.
   *
   * @param entry the entry to record in the log
   */
  append(entry: NarrativeEntry): void {
    this.entries.set(entry.timestamp ?? new Date().toISOString(), entry);
    this.refresh();
  }

  /**
   * @return the entries recorded so far, most recent last
   */
  getEntries(): NarrativeEntry[] {
    return [...this.entries.values()];
  }

  /**
   * Clears the log.
   */
  clear(): void {
    this.entries.clear();
    this.refresh();
  }

  private readonly entries = new Map<string, NarrativeEntry>();
  private readonly subject = new Subject<void>();

  private refresh(): void {
    this.subject.next();
  }

  /**
   * @return a stream that emits whenever the log changes
   */
  changes(): Observable<void> {
    return this.subject.asObservable();
  }
}
