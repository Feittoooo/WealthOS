import { signal } from '@angular/core';

/**
 * Sort state for a table: click a header once to sort by it, again to flip the direction.
 * Numbers sort as numbers, ISO dates and text alphabetically, empty values always last.
 */
export class TableSort<T> {
  readonly key;
  readonly desc;

  constructor(initialKey: keyof T & string, initialDesc = true) {
    this.key = signal<keyof T & string>(initialKey);
    this.desc = signal(initialDesc);
  }

  toggle(key: keyof T & string): void {
    if (this.key() === key) {
      this.desc.update(d => !d);
    } else {
      this.key.set(key);
      // a new column starts biggest / newest / Z first; a second click flips it
      this.desc.set(true);
    }
  }

  mark(key: keyof T & string): string {
    return this.key() === key ? (this.desc() ? '↓' : '↑') : '';
  }

  apply(rows: readonly T[]): T[] {
    const key = this.key();
    const dir = this.desc() ? -1 : 1;
    return rows.slice().sort((a, b) => compare(a[key], b[key], dir));
  }
}

export function compare(x: unknown, y: unknown, dir: number): number {
  const xEmpty = x === null || x === undefined || x === '';
  const yEmpty = y === null || y === undefined || y === '';
  if (xEmpty || yEmpty) return xEmpty === yEmpty ? 0 : xEmpty ? 1 : -1;
  if (typeof x === 'number' && typeof y === 'number') return (x - y) * dir;
  if (typeof x === 'boolean' && typeof y === 'boolean') return (Number(x) - Number(y)) * dir;
  return String(x).localeCompare(String(y), undefined, { numeric: true, sensitivity: 'base' }) * dir;
}
