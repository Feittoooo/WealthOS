import { describe, expect, it } from 'vitest';
import { formatMessage } from './message-format';

describe('formatMessage', () => {
  it('returns patterns without arguments as they are, apostrophes included', () => {
    expect(formatMessage("You can't undo this.")).toBe("You can't undo this.");
    expect(formatMessage("EEEE, d 'de' MMMM")).toBe("EEEE, d 'de' MMMM");
  });

  it('replaces numbered arguments', () => {
    expect(formatMessage('Sold {0} {1}', [10, 'ACME'])).toBe('Sold 10 ACME');
    expect(formatMessage('{1} then {0}', ['a', 'b'])).toBe('b then a');
  });

  it('turns doubled apostrophes into one when there are arguments', () => {
    expect(formatMessage("You can''t sell more than {0}", ['5'])).toBe("You can't sell more than 5");
  });

  it('picks the right choice', () => {
    const p = 'In {0,choice,0#no accounts|1#1 account|1<{0} accounts}';
    expect(formatMessage(p, [0])).toBe('In no accounts');
    expect(formatMessage(p, [1])).toBe('In 1 account');
    expect(formatMessage(p, [6])).toBe('In 6 accounts');
  });

  it('formats large numbers with the locale', () => {
    expect(formatMessage('{0} items', [12000], 'en')).toBe('12,000 items');
  });

  it('leaves missing arguments empty instead of breaking', () => {
    expect(formatMessage('a {0} b {1}', ['x'])).toBe('a x b ');
  });
});
