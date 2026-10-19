import { describe, expect, it } from 'vitest';
import { TableSort } from './table-sort';

interface Row { name: string; value: number | null; date: string | null }

const rows: Row[] = [
  { name: 'beta', value: 10, date: '2026-03-01' },
  { name: 'Alpha', value: null, date: '2026-01-15' },
  { name: 'gamma', value: 30, date: null }
];

describe('TableSort', () => {
  it('sorts numbers descending first and leaves empty values at the end', () => {
    const s = new TableSort<Row>('value');
    expect(s.apply(rows).map(r => r.name)).toEqual(['gamma', 'beta', 'Alpha']);
  });

  it('flips direction when the same header is clicked again', () => {
    const s = new TableSort<Row>('value');
    s.toggle('value');
    expect(s.apply(rows).map(r => r.name)).toEqual(['beta', 'gamma', 'Alpha']);
    expect(s.mark('value')).toBe('↑');
  });

  it('sorts ISO dates newest first and text case-insensitively', () => {
    const s = new TableSort<Row>('value');
    s.toggle('date');
    expect(s.apply(rows).map(r => r.date)).toEqual(['2026-03-01', '2026-01-15', null]);
    s.toggle('name');
    s.toggle('name');
    expect(s.apply(rows).map(r => r.name)).toEqual(['Alpha', 'beta', 'gamma']);
  });

  it('does not touch the original array', () => {
    const s = new TableSort<Row>('name');
    s.apply(rows);
    expect(rows[0].name).toBe('beta');
  });
});
