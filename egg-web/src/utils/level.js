// Count-based Lv1-10 level system.
// Level source = totalBets (distinct matches participated; one bet per match).
// Thresholds are inclusive lower bounds: Lv1: 0-4, Lv2: 5-14, Lv3: 15-29,
// Lv4: 30-49, Lv5: 50-79, Lv6: 80-109, Lv7: 110-139, Lv8: 140-169,
// Lv9: 170-199, Lv10: 200+.

export const LEVEL_THRESHOLDS = [0, 5, 15, 30, 50, 80, 110, 140, 170, 200]

const LEVEL_PALETTE = ['#9e9e9e', '#7cb342', '#00bcd4', '#ff9800', '#ff1a1a']

function normalizeCount(count) {
  var n = Number(count)
  if (!Number.isFinite(n) || n < 0) return 0
  return Math.floor(n)
}

// totalBets (camelCase VO) or total_bets (snake_case raw SQL) both accepted by callers;
// this function just takes the resolved numeric count.
export function getLevel(count) {
  var n = normalizeCount(count)
  var lv = 1
  for (var i = 0; i < LEVEL_THRESHOLDS.length; i++) {
    if (n >= LEVEL_THRESHOLDS[i]) lv = i + 1
  }
  return Math.min(10, Math.max(1, lv))
}

export function getLevelColor(lv) {
  var level = Number(lv)
  if (!Number.isFinite(level)) level = 1
  level = Math.min(10, Math.max(1, Math.floor(level)))
  return LEVEL_PALETTE[Math.floor(level / 2)]
}

export function getLevelProgress(count) {
  var n = normalizeCount(count)
  var lv = getLevel(n)
  if (lv >= 10) return 100
  var cur = LEVEL_THRESHOLDS[lv - 1]
  var next = LEVEL_THRESHOLDS[lv]
  if (next <= cur) return 100
  return Math.min(100, Math.max(0, ((n - cur) / (next - cur)) * 100))
}

// Resolve totalBets from a row object supporting both camelCase and snake_case.
export function getBetCount(row) {
  if (!row) return 0
  var v = row.totalBets
  if (v === undefined || v === null) v = row.total_bets
  return normalizeCount(v)
}
