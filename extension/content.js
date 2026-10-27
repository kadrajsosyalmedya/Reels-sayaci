// Instagram Reels, YouTube Shorts ve TikTok'ta izlenen videoları sayar,
// köşede beyin + sayaç gösterir ve günlük limit dolunca bu sayfaları kapatır.
(() => {
  'use strict';

  const host = location.hostname;
  const SITE = /(^|\.)instagram\.com$/.test(host) ? 'instagram'
    : /(^|\.)youtube\.com$/.test(host) ? 'youtube'
    : /(^|\.)tiktok\.com$/.test(host) ? 'tiktok'
    : null;
  if (!SITE) return;

  const TITLES = { instagram: 'Instagram Reels', youtube: 'YouTube Shorts', tiktok: 'TikTok' };
  const HOMES = { instagram: 'https://www.instagram.com/', youtube: 'https://www.youtube.com/' };
  const TICK_MS = 500;

  let state = null;
  let lastKey = null;
  let blocked = false;
  const seen = new Set();
  const elementIds = new WeakMap();
  let nextElementId = 1;

  // ---- Hangi sayfadayız, hangi video oynuyor? ----

  function isReelsPage() {
    const p = location.pathname;
    if (SITE === 'instagram') return /^\/reels?\//.test(p);
    if (SITE === 'youtube') return p.startsWith('/shorts');
    return true; // TikTok'un tamamı kısa video akışı
  }

  function urlKey() {
    const re = SITE === 'instagram' ? /^\/reels?\/([^/?#]+)/ : SITE === 'youtube' ? /^\/shorts\/([^/?#]+)/ : null;
    const m = re && location.pathname.match(re);
    return m ? 'u:' + m[1] : null;
  }

  function elementId(el) {
    let id = elementIds.get(el);
    if (!id) {
      id = nextElementId++;
      elementIds.set(el, id);
    }
    return id;
  }

  /** Ekranda en çok görünen, oynamaya hazır videonun anahtarı. */
  function visibleVideoKey() {
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    let best = null;
    let bestArea = 0;
    for (const v of document.querySelectorAll('video')) {
      const src = v.currentSrc || v.src;
      if (!src) continue;
      const r = v.getBoundingClientRect();
      if (r.width < 120 || r.height < 160) continue;
      const w = Math.min(r.right, vw) - Math.max(r.left, 0);
      const h = Math.min(r.bottom, vh) - Math.max(r.top, 0);
      if (w <= 0 || h <= 0 || (w * h) / (r.width * r.height) < 0.6) continue;
      if (w * h > bestArea) {
        bestArea = w * h;
        best = v;
      }
    }
    if (!best) return null;
    const item = best.closest('[data-e2e="recommend-list-item-container"], article') || best;
    return 'v:' + elementId(item) + '|' + (best.currentSrc || best.src);
  }

  function currentKey() {
    if (SITE === 'tiktok') return visibleVideoKey();
    return urlKey() || visibleVideoKey();
  }

  // ---- Ekrandaki sayaç ----

  const hostEl = document.createElement('reels-sayaci');
  const shadow = hostEl.attachShadow({ mode: 'closed' });
  shadow.innerHTML = `
    <style>
      :host { all: initial; }
      .pill {
        position: fixed; left: 20px; bottom: 24px; z-index: 2147483646;
        display: flex; align-items: center; gap: 8px;
        padding: 6px 14px 6px 6px; border-radius: 999px;
        background: rgba(0, 0, 0, .8); color: #fff; pointer-events: none;
        font: 600 15px/1.2 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
        box-shadow: 0 4px 18px rgba(0, 0, 0, .35);
      }
      .pill[hidden] { display: none; }
      .brain { width: 44px; height: 44px; transition: transform .35s ease-out; }
      .brain.pulse { transform: scale(1.3); transition: none; }
      .count { font-size: 17px; font-weight: 700; }
      .health { font-size: 12px; font-weight: 700; }
      .block {
        position: fixed; inset: 0; z-index: 2147483647;
        display: flex; align-items: center; justify-content: center;
        background: rgba(18, 14, 26, .97); color: #F5F3FA; padding: 24px;
        font: 16px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; text-align: center;
      }
      .block[hidden] { display: none; }
      .box { max-width: 420px; }
      .box .brain { width: 180px; height: 180px; margin: 0 auto; }
      h1 { font-size: 28px; margin: 12px 0 8px; }
      p { color: #A39DB5; margin: 0 0 20px; }
      ul { list-style: none; padding: 16px 20px; margin: 0 0 20px; background: #1E1830; border-radius: 18px; text-align: left; }
      li { padding: 4px 0; }
      li.title { color: #FF8FB8; font-weight: 700; }
      button {
        font: 700 16px system-ui, sans-serif; border: 0; border-radius: 14px; padding: 14px 20px;
        background: #FF8FB8; color: #1B1526; cursor: pointer; width: 100%;
      }
      small { display: block; margin-top: 12px; color: #A39DB5; }
    </style>
    <div class="pill" hidden>
      <div class="brain"></div>
      <div><div class="count"></div><div class="health"></div></div>
    </div>
    <div class="block" hidden>
      <div class="box">
        <div class="brain"></div>
        <h1>Bugünlük bu kadar! 🛑</h1>
        <p class="info"></p>
        <ul class="tips"></ul>
        <button type="button" class="home"></button>
        <small>Sayaç gece 00:00'da sıfırlanır.</small>
      </div>
    </div>`;
  const pill = shadow.querySelector('.pill');
  const pillBrain = pill.querySelector('.brain');
  const blockEl = shadow.querySelector('.block');
  const homeBtn = shadow.querySelector('.home');

  function mount() {
    if (!hostEl.isConnected) document.documentElement.appendChild(hostEl);
  }

  function renderPill(pulse) {
    if (!state) return;
    const h = ReelsBrain.health(state.count, state.limit);
    pillBrain.innerHTML = ReelsBrain.svg(h);
    pill.querySelector('.count').textContent = `${state.count} / ${state.limit}`;
    const healthEl = pill.querySelector('.health');
    healthEl.textContent = state.count >= state.limit ? 'Son reels!' : `Beyin %${h}`;
    healthEl.style.color = ReelsBrain.statusColor(h);
    if (pulse) {
      pillBrain.classList.add('pulse');
      requestAnimationFrame(() => requestAnimationFrame(() => pillBrain.classList.remove('pulse')));
    }
  }

  function showPill() {
    mount();
    pill.hidden = false;
  }

  function hidePill() {
    pill.hidden = true;
  }

  // ---- Engelleme ----

  const TIPS = [
    '💧 Bir bardak su iç', '🚶 10 dakika yürüyüşe çık', '📖 Birkaç sayfa kitap oku', '🧘 5 derin nefes al',
    '📞 Bir arkadaşını ara', '🎧 Sevdiğin bir şarkıyı dinle', '✍️ Yarın için bir hedef yaz', '🛏️ Ekranı kapat, gözlerini dinlendir',
  ];

  function stopMedia() {
    for (const m of document.querySelectorAll('video, audio')) {
      try {
        m.muted = true;
        m.pause();
      } catch (_) { /* bazı oynatıcılar dokunulmaz */ }
    }
  }

  function swallow(e) {
    if (!blocked || e.composedPath().includes(homeBtn)) return;
    e.preventDefault();
    e.stopImmediatePropagation();
  }
  for (const type of ['wheel', 'keydown', 'touchmove']) {
    window.addEventListener(type, swallow, { capture: true, passive: false });
  }

  function block() {
    blocked = true;
    hidePill();
    mount();
    blockEl.querySelector('.brain').innerHTML = ReelsBrain.svg(0);
    blockEl.querySelector('.info').textContent =
      `Bugün ${state.count} reels izledin. ${TITLES[SITE]} yarına kadar kapalı. Beynin dinlenmeyi hak ediyor.`;
    const tips = [...TIPS].sort(() => Math.random() - 0.5).slice(0, 3);
    blockEl.querySelector('.tips').innerHTML =
      '<li class="title">Bunun yerine şunu dene:</li>' + tips.map((t) => `<li>${t}</li>`).join('');
    homeBtn.textContent = HOMES[SITE] ? 'Ana sayfaya dön' : 'Tamam, sekmeyi kapatıyorum';
    blockEl.hidden = false;
    document.documentElement.style.setProperty('overflow', 'hidden', 'important');
    stopMedia();
  }

  function unblock() {
    blocked = false;
    blockEl.hidden = true;
    document.documentElement.style.removeProperty('overflow');
  }

  homeBtn.addEventListener('click', () => {
    if (HOMES[SITE]) location.assign(HOMES[SITE]);
    else location.replace('about:blank');
  });

  // ---- Ana döngü ----

  async function tick() {
    if (!state) return;
    if (state.day !== ReelsStore.dayString()) state = await ReelsStore.load();

    if (!isReelsPage()) {
      if (blocked) unblock();
      hidePill();
      lastKey = null;
      return;
    }
    if (blocked) {
      if (state.count < state.limit) unblock(); // yeni gün başladı
      else return stopMedia();
    }
    showPill();

    const key = currentKey();
    if (!key || key === lastKey) return;
    lastKey = key;
    if (seen.has(key)) return; // geri kaydırıp eski videoya dönmek sayılmaz
    seen.add(key);

    // Limitteki son reels'i izleyebilirsin; bir sonrakine geçince kapanır.
    if (state.count >= state.limit) return block();
    state = await ReelsStore.increment(SITE);
    renderPill(true);
  }

  ReelsStore.onChange((next) => {
    state = next;
    renderPill(false);
  });

  ReelsStore.load().then((s) => {
    state = s;
    renderPill(false);
    tick();
    setInterval(tick, TICK_MS);
  });
})();
