'use strict';

const $ = (id) => document.getElementById(id);
let state = null;

function render() {
  const h = ReelsBrain.health(state.count, state.limit);
  $('brain').innerHTML = ReelsBrain.svg(h, { ring: true });
  $('health').textContent = `Beyin sağlığı %${h}`;
  $('health').style.color = ReelsBrain.statusColor(h);
  $('mood').textContent = ReelsBrain.mood(h);
  $('count').textContent = `${state.count} / ${state.limit}`;
  $('remaining').textContent = state.count >= state.limit
    ? 'Limit doldu. Reels, Shorts ve TikTok yarına kadar kapalı.'
    : `${state.limit - state.count} reels hakkın kaldı`;
  $('breakdown').textContent =
    `Instagram ${state.sites.instagram} · YouTube ${state.sites.youtube} · TikTok ${state.sites.tiktok}\nDün: ${state.yesterday} reels`;
  $('limit').textContent = state.limit;
}

async function changeLimit(delta) {
  const result = await ReelsStore.setLimit(state.limit + delta);
  state = result.state;
  $('message').textContent = result.ok ? '' : 'Limit doldu, bugün artıramazsın. Yarın görüşürüz 🙂';
  render();
}

$('minus').addEventListener('click', () => changeLimit(-ReelsStore.LIMIT_STEP));
$('plus').addEventListener('click', () => changeLimit(ReelsStore.LIMIT_STEP));

ReelsStore.onChange((next) => {
  state = next;
  render();
});

ReelsStore.load().then((s) => {
  state = s;
  render();
});
