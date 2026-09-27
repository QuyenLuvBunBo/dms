/**
 * Reads a number input. An empty field becomes NaN, which JSON.stringify sends as null, so the
 * API answers with a field error instead of silently receiving 0.
 */
export function parseNumber(value: string): number {
  return value.trim() === '' ? Number.NaN : Number(value)
}
