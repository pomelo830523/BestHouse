/**
 * BestHouse Content Script
 * 從各房屋平台擷取資料，回應 popup 的訊息
 */

const PARSERS = {
  '591.com.tw': parse591,
  'rakuya.com.tw': parseRakuya,
  'sinyi.com.tw': parseSinyi,
};

/**
 * 擷取 591 售屋網 house-detail 頁面資料
 * URL 範例: https://sale.591.com.tw/home/house/detail/2/XXXXXXXX.html
 *
 * 591 HTML 結構重點：
 *  - 標題在 document.title（格式：「標題 - 591售屋網」）
 *  - 價格：span.info-price-num-2（含 web component，試讀 textContent）
 *  - 坪數/格局 等 info1 資料：div.info-floor-left-2 裡，
 *      label = div.info-floor-value，value = div.info-floor-key-2（v-html）
 *  - 樓層/社區 等 info2 資料：div.info-addr-content 裡，
 *      label = span.info-addr-key，value = (div|span).info-addr-value
 *  - 房屋資料（server-rendered）：div.detail-house-item 裡，
 *      label = div.detail-house-key，value = div.detail-house-value
 *    （key/value 含反爬蟲空標籤，textContent 可正確去除）
 */
function parse591() {
  // 標題：從 document.title 擷取，去除網站名稱後綴
  const nickname = document.title
    .replace(/\s*[-–]\s*591.*/i, '')
    .trim() || null;

  // ── server-rendered detail-house-item ──────────────────────────────────────
  // 這些欄位是伺服器渲染的，包含：型態、管理費、車位、公設比等
  const detailMap = {};
  document.querySelectorAll('div.detail-house-item').forEach(item => {
    const keyEl  = item.querySelector('.detail-house-key');
    const valEl  = item.querySelector('.detail-house-value');
    if (!keyEl || !valEl) return;
    const key = keyEl.textContent.trim();
    const val = valEl.textContent.trim();
    if (key && val) detailMap[key] = val;
  });

  // ── Vue-rendered info1（坪數、格局）────────────────────────────────────────
  // 結構：label 在 .info-floor-value，value 在 .info-floor-key-2
  const info1Map = {};
  document.querySelectorAll('div.info-floor-left-2').forEach(item => {
    const labelEl = item.querySelector('.info-floor-value');
    const valEl   = item.querySelector('.info-floor-key-2');
    if (!labelEl) return;
    const label = labelEl.textContent.trim();
    const val   = valEl ? valEl.textContent.trim() : '';
    if (label) info1Map[label] = val;
  });

  // ── Vue-rendered info2（樓層、社區）────────────────────────────────────────
  // 結構：label 在 span.info-addr-key，value 在 .info-addr-value
  const info2Map = {};
  document.querySelectorAll('div.info-addr-content').forEach(item => {
    const labelEl = item.querySelector('.info-addr-key');
    const valEl   = item.querySelector('.info-addr-value');
    if (!labelEl) return;
    const label = labelEl.textContent.trim();
    const val   = valEl ? valEl.textContent.trim() : '';
    if (label) info2Map[label] = val;
  });

  // ── 總價（萬）─────────────────────────────────────────────────────────────
  // web component 可能渲染為圖片（防爬蟲），能取到就取，取不到讓使用者手填
  let totalPrice = null;
  const priceSpan = document.querySelector('span.info-price-num-2');
  if (priceSpan) {
    const raw = priceSpan.textContent.replace(/[^\d.]/g, '');
    if (raw) totalPrice = parseFloat(raw) || null;
  }

  // ── 坪數──────────────────────────────────────────────────────────────────
  let buildAreaPing = null;
  const AREA_KEYS = ['建坪', '面積', '建物坪數', '總坪'];
  for (const k of AREA_KEYS) {
    const src = info1Map[k] || detailMap[k];
    if (src) {
      const m = src.match(/[\d.]+/);
      if (m) { buildAreaPing = parseFloat(m[0]); break; }
    }
  }

  // ── 格局（房/廳/衛）──────────────────────────────────────────────────────
  let bedroomCount = null, livingRoomCount = null, bathroomCount = null;
  const LAYOUT_KEYS = ['格局', '坪數格局', '房型'];
  for (const k of LAYOUT_KEYS) {
    const src = info1Map[k] || detailMap[k];
    if (src) {
      const r = src.match(/(\d+)\s*房/); const l = src.match(/(\d+)\s*廳/); const b = src.match(/(\d+)\s*衛/);
      if (r) bedroomCount    = parseInt(r[1]);
      if (l) livingRoomCount = parseInt(l[1]);
      if (b) bathroomCount   = parseInt(b[1]);
      break;
    }
  }

  // ── 樓層 ─────────────────────────────────────────────────────────────────
  let floor = null, totalFloor = null;
  const FLOOR_KEYS = ['樓層', '所在樓層'];
  for (const k of FLOOR_KEYS) {
    const src = info2Map[k] || info1Map[k] || detailMap[k];
    if (src) {
      const m = src.match(/(\d+)[^0-9]+(\d+)/);
      if (m) { floor = parseInt(m[1]); totalFloor = parseInt(m[2]); break; }
    }
  }

  // ── 社區名稱 ──────────────────────────────────────────────────────────────
  const communityName =
    document.querySelector('span.community-link')?.textContent?.trim() ||
    info2Map['社區'] || info2Map['社區名稱'] || null;

  // ── 管理費 ────────────────────────────────────────────────────────────────
  let monthlyFee = null;
  const feeText = detailMap['管理費'] || info2Map['管理費'];
  if (feeText) {
    const m = feeText.match(/[\d]+/);
    if (m) monthlyFee = parseInt(m[0]);
  }

  return {
    nickname,
    address: null,   // 591 地址以圖片防爬蟲，無法擷取文字
    communityName,
    floor,
    totalFloor,
    buildAreaPing,
    bedroomCount,
    livingRoomCount,
    bathroomCount,
    totalPrice,
    monthlyFee,
    listingUrl: window.location.href,
  };
}

/**
 * 擷取樂屋網 house-detail 頁面資料
 * URL 範例: https://www.rakuya.com.tw/sell/show/id/...
 */
function parseRakuya() {
  const getText = (selector) => {
    const el = document.querySelector(selector);
    return el ? el.textContent.trim() : null;
  };

  const title = getText('h1.title') || getText('.house-title') || getText('h1');
  const address = getText('.address-text') || getText('.house-address');

  // 總價
  let totalPrice = null;
  const priceEl = document.querySelector('.price-num') || document.querySelector('.price strong');
  if (priceEl) {
    const raw = priceEl.textContent.replace(/[^\d.]/g, '');
    totalPrice = raw ? parseFloat(raw) : null;
  }

  // 坪數與格局 - 從 info table 擷取
  let buildAreaPing = null, bedroomCount = null, livingRoomCount = null, bathroomCount = null;
  let floor = null, totalFloor = null;

  document.querySelectorAll('.info-table tr, .house-info tr, .info-list li').forEach(row => {
    const label = row.querySelector('th, .label, dt');
    const value = row.querySelector('td, .value, dd');
    if (!label || !value) return;
    const labelText = label.textContent.trim();
    const valueText = value.textContent.trim();

    if (labelText.includes('建坪') || labelText.includes('總坪')) {
      buildAreaPing = parseFloat(valueText.replace(/[^\d.]/g, '')) || null;
    } else if (labelText.includes('格局')) {
      const rMatch = valueText.match(/(\d+)\s*房/);
      const lMatch = valueText.match(/(\d+)\s*廳/);
      const bMatch = valueText.match(/(\d+)\s*衛/);
      if (rMatch) bedroomCount = parseInt(rMatch[1]);
      if (lMatch) livingRoomCount = parseInt(lMatch[1]);
      if (bMatch) bathroomCount = parseInt(bMatch[1]);
    } else if (labelText.includes('樓層')) {
      const match = valueText.match(/(\d+)\s*[\/\/]\s*(\d+)/);
      if (match) { floor = parseInt(match[1]); totalFloor = parseInt(match[2]); }
    }
  });

  return {
    nickname: title,
    address,
    totalPrice,
    buildAreaPing,
    bedroomCount,
    livingRoomCount,
    bathroomCount,
    floor,
    totalFloor,
    listingUrl: window.location.href,
  };
}

/**
 * 擷取信義房屋 house-detail 頁面資料
 * URL 範例: https://www.sinyi.com.tw/buy/house/...
 */
function parseSinyi() {
  const getText = (selector) => {
    const el = document.querySelector(selector);
    return el ? el.textContent.trim() : null;
  };

  const title = getText('h1.case-title') || getText('.case-name h1') || getText('h1');
  const address = getText('.address') || getText('.case-address');

  // 總價（萬）
  let totalPrice = null;
  const priceEl = document.querySelector('.price-num') || document.querySelector('.case-price .num');
  if (priceEl) {
    const raw = priceEl.textContent.replace(/[^\d.]/g, '');
    totalPrice = raw ? parseFloat(raw) : null;
  }

  // 從規格表擷取
  let buildAreaPing = null, bedroomCount = null, livingRoomCount = null, bathroomCount = null;
  let floor = null, totalFloor = null;

  document.querySelectorAll('.spec-item, .info-item, .case-spec li').forEach(item => {
    const label = item.querySelector('.spec-label, .label, dt');
    const value = item.querySelector('.spec-value, .value, dd');
    if (!label || !value) return;
    const labelText = label.textContent.trim();
    const valueText = value.textContent.trim();

    if (labelText.includes('建坪') || labelText.includes('總坪') || labelText.includes('坪數')) {
      buildAreaPing = parseFloat(valueText.replace(/[^\d.]/g, '')) || null;
    } else if (labelText.includes('格局') || labelText.includes('房型')) {
      const rMatch = valueText.match(/(\d+)\s*房/);
      const lMatch = valueText.match(/(\d+)\s*廳/);
      const bMatch = valueText.match(/(\d+)\s*衛/);
      if (rMatch) bedroomCount = parseInt(rMatch[1]);
      if (lMatch) livingRoomCount = parseInt(lMatch[1]);
      if (bMatch) bathroomCount = parseInt(bMatch[1]);
    } else if (labelText.includes('樓層')) {
      const match = valueText.match(/(\d+)\s*[\/\/]\s*(\d+)/);
      if (match) { floor = parseInt(match[1]); totalFloor = parseInt(match[2]); }
    }
  });

  return {
    nickname: title,
    address,
    totalPrice,
    buildAreaPing,
    bedroomCount,
    livingRoomCount,
    bathroomCount,
    floor,
    totalFloor,
    listingUrl: window.location.href,
  };
}

// 監聽 popup 發來的訊息
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action !== 'extractHouseData') {
    sendResponse({ error: '未知動作' });
    return;
  }

  const hostname = window.location.hostname;
  const parserKey = Object.keys(PARSERS).find(k => hostname.includes(k));

  if (!parserKey) {
    sendResponse({ error: '此頁面不支援' });
    return;
  }

  try {
    const data = PARSERS[parserKey]();
    sendResponse({ data });
  } catch (e) {
    sendResponse({ error: '擷取失敗: ' + e.message });
  }
});
