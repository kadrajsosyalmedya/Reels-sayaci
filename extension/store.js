// Günlük sayaç ve limit (chrome.storage.local). Gün değişince sayaç kendiliğinden sıfırlanır.
(function (global) {
  'use strict';

  const KEY = 'reelsSayaci';
  const DEFAULT_LIMIT = 50;
  const MIN_LIMIT = 5;
  const MAX_LIMIT = 300;
  const LIMIT_STEP = 5;
  const SITES = ['instagram', 'youtube', 'tiktok'];

  function dayString(offsetDays = 0) {
    const d = new Date();
    d.setDate(d.getDate() + offsetDays);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  function emptySites() {
    return Object.fromEntries(SITES.map((s) => [s, 0]));
  }

  function normalize(raw) {
    const s = raw || {};
    const limit = Number.isFinite(s.limit) ? s.limit : DEFAULT_LIMIT;
    const today = dayString();
    if (s.day === today) {
      return { day: today, count: s.count || 0, sites: { ...emptySites(), ...s.sites }, limit, yesterday: s.yesterday || 0 };
    }
    return { day: today, count: 0, sites: emptySites(), limit, yesterday: s.day === dayString(-1) ? s.count || 0 : 0 };
  }

  async function save(state) {
    await chrome.storage.local.set({ [KEY]: state });
    return state;
  }

  async function load() {
    const raw = (await chrome.storage.local.get(KEY))[KEY];
    const state = normalize(raw);
    if (!raw || raw.day !== state.day) await save(state);
    return state;
  }

  async function increment(site) {
    const state = await load();
    state.count += 1;
    state.sites[site] = (state.sites[site] || 0) + 1;
    return save(state);
  }

  /** Limit dolduysa o gün limit artırılamaz; yoksa limit tek tıkla kalkardı. */
  async function setLimit(limit) {
    const state = await load();
    const next = Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, limit));
    if (next > state.limit && state.count >= state.limit) return { ok: false, state };
    state.limit = next;
    return { ok: true, state: await save(state) };
  }

  function onChange(callback) {
    chrome.storage.onChanged.addListener((changes, area) => {
      if (area === 'local' && changes[KEY]) callback(normalize(changes[KEY].newValue));
    });
  }

  global.ReelsStore = { load, increment, setLimit, onChange, dayString, LIMIT_STEP };
})(globalThis);
