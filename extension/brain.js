// Beyin sağlığı hesabı ve beyin çizimi (SVG). Android uygulamasındaki BrainView ile aynı tasarım.
(function (global) {
  'use strict';

  const LOBES = [
    [30, 38, 17], [21, 54, 14], [32, 66, 15], [44, 28, 15],
    [70, 38, 17], [79, 54, 14], [68, 66, 15], [56, 28, 15],
    [50, 52, 24],
  ];
  const FOLDS = [
    'M38,22 q6,4 4,10', 'M62,22 q-6,4 -4,10',
    'M20,46 q6,-4 10,1', 'M80,46 q-6,-4 -10,1',
    'M24,64 q4,-5 9,-2', 'M76,64 q-4,-5 -9,-2',
    'M50,16 q-3,6 0,12',
  ];
  const EYES_X = [41, 59];
  const EYES_Y = 50;
  const FACE = '#3A1F2B';
  // Sağlıklı pembeden hasta yeşilimsi griye / yeşil -> sarı -> kırmızı.
  const BRAIN_STOPS = ['#93A86A', '#D4C060', '#FFB870', '#FFA3A3', '#FF8FB8'];
  const STATUS_STOPS = ['#EF4444', '#F97316', '#FACC15', '#A3E635', '#4ADE80'];

  function health(count, limit) {
    if (limit <= 0) return 0;
    const h = Math.round((100 * (limit - count)) / limit);
    return Math.max(0, Math.min(100, h));
  }

  function mood(h) {
    if (h >= 80) return 'Beynin bomba gibi! 💪';
    if (h >= 60) return 'Beynin iyi, ama dikkat 🙂';
    if (h >= 40) return 'Beynin yorulmaya başladı 😐';
    if (h >= 20) return 'Beynin eriyor... 🫠';
    if (h > 0) return 'Beynin alarm veriyor! 🚨';
    return 'Beynin tükendi, bugünlük bitti 💤';
  }

  const toRgb = (hex) => [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16));
  const toHex = (rgb) => '#' + rgb.map((v) => Math.round(v).toString(16).padStart(2, '0')).join('').toUpperCase();

  function gradient(stops, h) {
    const t = (Math.max(0, Math.min(100, h)) / 100) * (stops.length - 1);
    const i = Math.min(Math.floor(t), stops.length - 2);
    const a = toRgb(stops[i]);
    const b = toRgb(stops[i + 1]);
    return toHex(a.map((v, k) => v + (b[k] - v) * (t - i)));
  }

  const brainColor = (h) => gradient(BRAIN_STOPS, h);
  const statusColor = (h) => gradient(STATUS_STOPS, h);
  const darken = (hex, amount) => toHex(toRgb(hex).map((v) => v * (1 - amount)));

  const circle = ([x, y, r]) => `<circle cx="${x}" cy="${y}" r="${r}"/>`;
  const sparkle = (x, y, s) =>
    `<path d="M${x},${y - s} Q${x},${y} ${x + s},${y} Q${x},${y} ${x},${y + s} Q${x},${y} ${x - s},${y} Q${x},${y} ${x},${y - s}Z"/>`;

  function face(h, outline) {
    let out = '';
    if (h >= 60) out += `<g fill="#FF5C8A" fill-opacity=".45"><circle cx="34" cy="57" r="3.8"/><circle cx="66" cy="57" r="3.8"/></g>`;
    for (const x of EYES_X) {
      if (h < 10) {
        out += `<g stroke="${FACE}" stroke-width="2.2" stroke-linecap="round">` +
          `<line x1="${x - 3}" y1="${EYES_Y - 3}" x2="${x + 3}" y2="${EYES_Y + 3}"/>` +
          `<line x1="${x + 3}" y1="${EYES_Y - 3}" x2="${x - 3}" y2="${EYES_Y + 3}"/></g>`;
      } else if (h < 40) {
        out += `<path fill="${FACE}" d="M${x - 3.6},${EYES_Y} A3.6,3.6 0 0,0 ${x + 3.6},${EYES_Y}Z"/>` +
          `<line x1="${x - 4.2}" y1="${EYES_Y}" x2="${x + 4.2}" y2="${EYES_Y}" stroke="${outline}" stroke-width="1.8" stroke-linecap="round"/>`;
      } else {
        out += `<circle cx="${x}" cy="${EYES_Y}" r="3.6" fill="${FACE}"/><circle cx="${x + 1.2}" cy="${EYES_Y - 1.2}" r="1.2" fill="#fff"/>`;
      }
    }
    const mouth = h < 10
      ? 'M42,61 q2,-3 4,0 q2,3 4,0 q2,-3 4,0 q2,3 4,0'
      : `M42,60 Q50,${(60 + ((h - 50) / 50) * 8).toFixed(2)} 58,60`;
    out += `<path d="${mouth}" fill="none" stroke="${FACE}" stroke-width="2.6" stroke-linecap="round"/>`;
    return out;
  }

  function brain(h) {
    const body = brainColor(h);
    const outline = darken(body, 0.32);
    let out = '';
    if (h < 25) {
      out += '<g fill="none" stroke="#9CA3AF" stroke-opacity=".7" stroke-width="2" stroke-linecap="round">' +
        [36, 50, 64].map((x) => `<path d="M${x},12 q-3,-3 0,-6 q3,-3 0,-6"/>`).join('') + '</g>';
    }
    // Önce kalın kontur, sonra dolgu: dairelerden tek parça bir siluet çıkar.
    out += `<g fill="none" stroke="${outline}" stroke-width="6">${LOBES.map(circle).join('')}</g>`;
    out += `<g fill="${body}">${LOBES.map(circle).join('')}</g>`;
    out += '<circle cx="33" cy="30" r="5" fill="#fff" fill-opacity=".25"/>';
    out += `<g fill="none" stroke="${outline}" stroke-width="2.4" stroke-linecap="round">${FOLDS.map((d) => `<path d="${d}"/>`).join('')}</g>`;
    out += face(h, outline);
    if (h >= 80) out += `<g fill="#FFE66D">${sparkle(12, 18, 5)}${sparkle(89, 14, 4)}</g>`;
    if (h >= 10 && h < 50) out += '<g fill="#7DD3FC"><path d="M80,18 L83.6,26 L76.4,26Z"/><circle cx="80" cy="27.5" r="4"/></g>';
    return out;
  }

  /** @param {number} h beyin sağlığı 0-100  @param {{ring?: boolean}} opts */
  function svg(h, opts = {}) {
    let inner = brain(h);
    if (opts.ring) {
      const c = 2 * Math.PI * 46;
      const arc = h > 0
        ? `<circle cx="50" cy="50" r="46" fill="none" stroke="${statusColor(h)}" stroke-width="4.5" stroke-linecap="round" ` +
          `stroke-dasharray="${((c * h) / 100).toFixed(2)} ${c.toFixed(2)}" transform="rotate(-90 50 50)"/>`
        : '';
      inner = `<circle cx="50" cy="50" r="46" fill="none" stroke="rgba(255,255,255,.15)" stroke-width="4.5"/>${arc}` +
        `<g transform="translate(50 50) scale(.72) translate(-50 -50)">${inner}</g>`;
    }
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100%" height="100%" overflow="visible" aria-hidden="true">${inner}</svg>`;
  }

  global.ReelsBrain = { health, mood, brainColor, statusColor, svg };
})(globalThis);
