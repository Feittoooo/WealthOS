/**
 * The subset of java.text.MessageFormat used in messages*.properties, so the backend and the
 * frontend read the same files:
 *   {0}                         plain argument
 *   {0,choice,0#none|1#one|1<{0} many}   plural-ish choices
 *   ''                          a literal apostrophe (only when the message has arguments)
 * Patterns without arguments are returned untouched, which is what Spring does too.
 */
export function formatMessage(pattern: string, args: readonly unknown[] = [], locale = 'en'): string {
  if (args.length === 0) return pattern;
  return render(pattern.replace(/''/g, '\u0000'), args, locale).replace(/\u0000/g, "'");
}

function render(pattern: string, args: readonly unknown[], locale: string): string {
  let out = '';
  let i = 0;
  while (i < pattern.length) {
    const ch = pattern[i];
    if (ch !== '{') {
      out += ch;
      i++;
      continue;
    }
    const end = matchingBrace(pattern, i);
    if (end < 0) {
      out += pattern.slice(i);
      break;
    }
    out += placeholder(pattern.slice(i + 1, end), args, locale);
    i = end + 1;
  }
  return out;
}

function matchingBrace(s: string, open: number): number {
  let depth = 0;
  for (let i = open; i < s.length; i++) {
    if (s[i] === '{') depth++;
    else if (s[i] === '}' && --depth === 0) return i;
  }
  return -1;
}

function placeholder(body: string, args: readonly unknown[], locale: string): string {
  const firstComma = body.indexOf(',');
  const index = Number(firstComma < 0 ? body : body.slice(0, firstComma));
  const value = args[index];
  if (firstComma < 0) return display(value, locale);

  const rest = body.slice(firstComma + 1);
  const secondComma = rest.indexOf(',');
  const type = (secondComma < 0 ? rest : rest.slice(0, secondComma)).trim();
  if (type === 'choice' && secondComma >= 0) {
    return render(choose(rest.slice(secondComma + 1), Number(value)), args, locale);
  }
  return display(value, locale);
}

// "0#none|1#one|1<{0} many": pick the last option whose limit is met
function choose(spec: string, n: number): string {
  let chosen = '';
  let first = true;
  for (const option of splitTopLevel(spec)) {
    const m = /^\s*(-?[\d.]+)\s*([#<])([\s\S]*)$/.exec(option);
    if (!m) continue;
    const limit = Number(m[1]);
    const ok = m[2] === '#' ? n >= limit : n > limit;
    if (ok || first) chosen = m[3];
    first = false;
    if (!ok) break;
  }
  return chosen;
}

function splitTopLevel(spec: string): string[] {
  const parts: string[] = [];
  let depth = 0;
  let current = '';
  for (const ch of spec) {
    if (ch === '{') depth++;
    if (ch === '}') depth--;
    if (ch === '|' && depth === 0) {
      parts.push(current);
      current = '';
    } else {
      current += ch;
    }
  }
  parts.push(current);
  return parts;
}

function display(value: unknown, locale: string): string {
  if (value === null || value === undefined) return '';
  if (typeof value === 'number') return new Intl.NumberFormat(locale).format(value);
  return String(value);
}
