import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import {
  NarrativeCategory,
  NarrativeEntry,
  CATEGORY_LABELS,
} from './narrative';
import { NarrativeService } from './narrative.service';

/**
 * The game log: the front-end consumer of the narrative templates.
 *
 * <p>This component renders the structured {@link NarrativeEntry} values the back-end narrative
 * templates produce. It shows each entry's category (with a readable label), its optional title, and
 * its readable message, newest last, and offers a small control panel to render the five canonical
 * categories from sample data so a DM can see every line type flow into the log. It owns no
 * narrative rules - it only renders {@link NarrativeService} output.</p>
 */
@Component({
  selector: 'app-game-log',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './game-log.component.html',
  styleUrl: './game-log.component.css',
})
export class GameLogComponent implements OnInit, OnDestroy {
  readonly categories = Object.values(NarrativeCategory);
  readonly categoryLabels = CATEGORY_LABELS;

  readonly entries = signal<NarrativeEntry[]>([]);
  readonly selected = signal<NarrativeCategory>(NarrativeCategory.DM_NARRATION);

  readonly rollSides = signal('20');
  readonly rollModifier = signal('0');

  private readonly service = inject(NarrativeService);
  private readonly subscription = new Subscription();

  ngOnInit(): void {
    this.subscription.add(
      this.service.changes().subscribe(() => {
        this.entries.set(this.service.getEntries());
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /**
   * Renders the currently selected category from a small sample payload and appends it to the log.
   */
  renderSelected(): void {
    const data = this.sampleData(this.selected());
    this.service.render(this.selected(), data).subscribe((entry) => {
      this.service.append(entry);
    });
  }

  /**
   * Rolls a d{N} on the back-end and appends the rendered dice result to the log.
   */
  rollDice(): void {
    const sides = parseInt(this.rollSides(), 10) || 20;
    const modifier = parseInt(this.rollModifier(), 10) || 0;
    this.service.dice([sides], modifier).subscribe((entry) => {
      this.service.append(entry);
    });
  }

  /**
   * Clears the log.
   */
  clearLog(): void {
    this.service.clear();
  }

  /**
   * @param category the category to build a sample payload for
   * @return a minimal structured data map describing a sample moment for that category
   */
  private sampleData(category: NarrativeCategory): Record<string, unknown> {
    switch (category) {
      case NarrativeCategory.DM_NARRATION:
        return {
          title: 'The Gated Hollow',
          narrative: 'Cold seeps through the shattered archway; the torches gutter against a draft from below.',
          combatantNames: ['Vesryn', 'the hound of ash'],
        };
      case NarrativeCategory.PLAYER_ACTION:
        return {
          recognized: true,
          response: 'You shoulder the collapsed beam aside and press on into the dark.',
        };
      case NarrativeCategory.COMBAT_EVENT:
        return {
          actionTaken: true,
          attacker: 'the hound of ash',
          hit: true,
          damageApplied: 7,
          damageType: 'PHYSICAL',
          target: 'Vesryn',
        };
      case NarrativeCategory.SYSTEM_EVENT:
        return {
          eventType: 'SESSION_START',
          description: 'The session begins at dusk in the Gated Hollow.',
        };
      default:
        return {};
    }
  }
}
