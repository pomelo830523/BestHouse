/**
 * BestHouse Extension - Popup Script
 */

const STORAGE_KEY_API_URL = 'besthouse_api_url';
const DEFAULT_API_URL     = 'http://localhost:8080';

// DOM
const inputApiUrl        = document.getElementById('input-api-url');
const btnSaveUrl         = document.getElementById('btn-save-url');
const msgSaved           = document.getElementById('msg-saved');
const msgStatus          = document.getElementById('msg-status');
const sectionActions     = document.getElementById('section-actions');
const btnAiScreenshot    = document.getElementById('btn-ai-screenshot');
const inputShotCount     = document.getElementById('input-shot-count');
const shotCountDisplay   = document.getElementById('shot-count-display');
const previewScreenshot  = document.getElementById('preview-screenshot');
const badgeDuplicate     = document.getElementById('badge-duplicate');
const sectionPreview     = document.getElementById('section-preview');
const previewBody        = document.getElementById('preview-body');
const inputNickname      = document.getElementById('input-nickname');
const inputTotalPrice    = document.getElementById('input-total-price');
const sectionSaveActions = document.getElementById('section-save-actions');
const btnSave            = document.getElementById('btn-save');
const btnGoEdit          = document.getElementById('btn-go-edit');

let extractedData    = null;
let existingHouseId  = null;

// ─── 初始化 ───────────────────────────────────────────────────────────────────

chrome.storage.local.get([STORAGE_KEY_API_URL, 'besthouse_shot_count'], (result) => {
  inputApiUrl.value    = result[STORAGE_KEY_API_URL]      || DEFAULT_API_URL;
  inputShotCount.value = result['besthouse_shot_count']   || 3;
  shotCountDisplay.textContent = inputShotCount.value;
});

inputShotCount.addEventListener('input', () => {
  shotCountDisplay.textContent = inputShotCount.value;
  chrome.storage.local.set({ besthouse_shot_count: parseInt(inputShotCount.value) });
});

btnSaveUrl.addEventListener('click', () => {
  const url = inputApiUrl.value.trim().replace(/\/$/, '');
  chrome.storage.local.set({ [STORAGE_KEY_API_URL]: url }, () => {
    showEl(msgSaved);
    setTimeout(() => { msgSaved.hidden = true; }, 1500);
  });
});

// ─── AI 截圖匯入 ──────────────────────────────────────────────────────────────

btnAiScreenshot.addEventListener('click', async () => {
  resetPreview();
  setStatus('截圖中...', 'info');
  btnAiScreenshot.disabled = true;

  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });

    // 自動捲動截圖並拼接成長圖
    const shotCount = parseInt(inputShotCount.value) || 3;
    const stitched  = await captureScrollingScreenshot(tab, shotCount);

    // 顯示縮圖
    previewScreenshot.src    = stitched;
    previewScreenshot.hidden = false;

    setStatus('AI 解析中...', 'info');

    const formData = new FormData();
    formData.append('image', dataUrlToBlob(stitched), 'screenshot.jpg');

    const res = await fetch(`${getApiUrl()}/api/ai/extract-house`, {
      method: 'POST',
      body: formData,
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    const data      = await res.json();
    extractedData   = { ...data, listingUrl: data.listingUrl || tab.url };

    clearStatus();
    await checkDuplicate(extractedData.listingUrl);
    renderPreview(extractedData);
    showEl(sectionPreview);
    showEl(sectionSaveActions);

    if (!existingHouseId) {
      btnSave.hidden = false;
    }

  } catch (e) {
    setStatus('失敗：' + e.message, 'error');
  } finally {
    btnAiScreenshot.disabled = false;
  }
});


// ─── 儲存 ─────────────────────────────────────────────────────────────────────

btnSave.addEventListener('click', async () => {
  const nickname   = inputNickname.value.trim();
  const totalPrice = parseFloat(inputTotalPrice.value);

  if (!nickname) { inputNickname.focus(); setStatus('請填寫代號 / 暱稱', 'error'); return; }
  if (!totalPrice || totalPrice <= 0) { inputTotalPrice.focus(); setStatus('請填寫總價', 'error'); return; }

  btnSave.disabled = true;
  setStatus('儲存中...', 'info');

  try {
    const res = await fetch(`${getApiUrl()}/api/houses`, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(buildPayload(nickname, totalPrice)),
    });

    if (!res.ok) {
      const body = await res.text();
      throw new Error(`HTTP ${res.status}: ${body}`);
    }

    const saved         = await res.json();
    existingHouseId     = saved.houseId;
    badgeDuplicate.textContent = '✅ 已儲存到資料庫';
    badgeDuplicate.hidden      = false;
    btnSave.hidden             = true;
    btnGoEdit.hidden           = false;
    clearStatus();

  } catch (e) {
    setStatus('儲存失敗：' + e.message, 'error');
  } finally {
    btnSave.disabled = false;
  }
});

// ─── 前往編輯 ─────────────────────────────────────────────────────────────────

btnGoEdit.addEventListener('click', () => {
  if (!existingHouseId) return;
  const frontendUrl = getApiUrl().replace(':8080', ':4200');
  chrome.tabs.create({ url: `${frontendUrl}/houses/${existingHouseId}/edit` });
});

// ─── 工具函式 ─────────────────────────────────────────────────────────────────

function getApiUrl() {
  return inputApiUrl.value.trim().replace(/\/$/, '') || DEFAULT_API_URL;
}

const sleep = (ms) => new Promise(r => setTimeout(r, ms));

/**
 * 自動捲動截圖並拼接成長圖
 * @param {chrome.tabs.Tab} tab
 * @param {number} maxShots  最多截幾張（預設 4）
 */
async function captureScrollingScreenshot(tab, maxShots = 4) {
  // 取得頁面原始捲動位置與尺寸
  const [{ result: pageInfo }] = await chrome.scripting.executeScript({
    target: { tabId: tab.id },
    func: () => ({
      scrollY:        window.scrollY,
      viewportHeight: window.innerHeight,
      scrollHeight:   document.documentElement.scrollHeight,
    }),
  });

  const { viewportHeight, scrollHeight } = pageInfo;
  const dataUrls = [];

  // 捲回頂部
  await chrome.scripting.executeScript({
    target: { tabId: tab.id },
    func:   () => window.scrollTo({ top: 0, behavior: 'instant' }),
  });
  await sleep(300);

  for (let i = 0; i < maxShots; i++) {
    setStatus(`截圖中 ${i + 1} / ${Math.min(maxShots, Math.ceil(scrollHeight / viewportHeight))}...`, 'info');
    dataUrls.push(await chrome.tabs.captureVisibleTab(null, { format: 'jpeg', quality: 88 }));

    const nextY = (i + 1) * viewportHeight;
    if (nextY >= scrollHeight) break;

    await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func:   (y) => window.scrollTo({ top: y, behavior: 'instant' }),
      args:   [nextY],
    });
    await sleep(400);
  }

  // 恢復原始捲動位置
  await chrome.scripting.executeScript({
    target: { tabId: tab.id },
    func:   (y) => window.scrollTo({ top: y, behavior: 'instant' }),
    args:   [pageInfo.scrollY],
  });

  if (dataUrls.length === 1) return dataUrls[0];
  return stitchImages(dataUrls);
}

/**
 * 將多張 dataUrl 垂直拼接成一張
 */
function stitchImages(dataUrls) {
  return new Promise((resolve) => {
    const imgs = [];
    let loaded = 0;

    dataUrls.forEach((url, i) => {
      const img = new Image();
      img.onload = () => {
        imgs[i] = img;
        if (++loaded === dataUrls.length) {
          const w      = imgs[0].width;
          const h      = imgs[0].height;
          const canvas = document.createElement('canvas');
          canvas.width  = w;
          canvas.height = h * imgs.length;
          const ctx = canvas.getContext('2d');
          imgs.forEach((im, idx) => ctx.drawImage(im, 0, idx * h));
          resolve(canvas.toDataURL('image/jpeg', 0.88));
        }
      };
      img.src = url;
    });
  });
}

/**
 * dataUrl → Blob
 */
function dataUrlToBlob(dataUrl) {
  const [header, data] = dataUrl.split(',');
  const mimeType = header.match(/:(.*?);/)[1];
  const binary   = atob(data);
  const arr      = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) arr[i] = binary.charCodeAt(i);
  return new Blob([arr], { type: mimeType });
}

/**
 * 檢查 listingUrl 是否已存在
 */
async function checkDuplicate(url) {
  if (!url) return;
  try {
    const res = await fetch(`${getApiUrl()}/api/houses/by-url?url=${encodeURIComponent(url)}`);
    if (res.ok) {
      const existing      = await res.json();
      existingHouseId     = existing.houseId;
      badgeDuplicate.textContent = '⚠️ 此房屋已在資料庫中';
      badgeDuplicate.hidden      = false;
      btnGoEdit.hidden           = false;
    }
  } catch (_) { /* 忽略網路錯誤 */ }
}

/**
 * 渲染擷取結果預覽表格
 */
function renderPreview(data) {
  const LABELS = {
    communityName:  '社區',
    address:        '地址',
    totalPrice:     '總價（萬）',
    buildAreaPing:  '坪數',
    indoorPing:     '室內坪',
    bedroomCount:   '房',
    livingRoomCount:'廳',
    bathroomCount:  '衛',
    floor:          '樓層',
    totalFloor:     '總樓層',
    houseAgeYear:   '屋齡',
    monthlyFee:     '管理費（元/月）',
    listingUrl:     '網址',
  };

  previewBody.innerHTML    = '';
  inputNickname.value      = data.nickname || '';
  inputTotalPrice.value    = data.totalPrice != null ? data.totalPrice : '';

  Object.entries(LABELS).forEach(([key, label]) => {
    const val = data[key];
    if (val === null || val === undefined) return;
    const tr  = document.createElement('tr');
    const tdL = document.createElement('td');
    const tdV = document.createElement('td');
    tdL.textContent = label;
    if (key === 'listingUrl') {
      const a = document.createElement('a');
      a.href = val; a.target = '_blank'; a.textContent = '開啟';
      tdV.appendChild(a);
    } else {
      tdV.textContent = val;
    }
    tr.appendChild(tdL); tr.appendChild(tdV);
    previewBody.appendChild(tr);
  });
}

/**
 * 組合送出的 payload
 */
function buildPayload(nickname, totalPrice) {
  const d = extractedData;
  return {
    nickname,
    address:         d.address         || null,
    communityName:   d.communityName   || null,
    builder:         d.builder         || null,
    houseAgeYear:    d.houseAgeYear    || null,
    floor:           d.floor           || null,
    totalFloor:      d.totalFloor      || null,
    buildAreaPing:   d.buildAreaPing   || null,
    indoorPing:      d.indoorPing      || null,
    bedroomCount:    d.bedroomCount    || null,
    livingRoomCount: d.livingRoomCount || null,
    bathroomCount:   d.bathroomCount   || null,
    totalPrice,
    parkingType:     d.parkingType     || 'NONE',
    parkingPrice:    d.parkingPrice    ?? 0,
    monthlyFee:      d.monthlyFee      || null,
    listingUrl:      d.listingUrl      || null,
    hasVisited:      false,
  };
}

function resetPreview() {
  extractedData            = null;
  existingHouseId          = null;
  previewScreenshot.hidden = true;
  badgeDuplicate.hidden    = true;
  sectionPreview.hidden    = true;
  sectionSaveActions.hidden = true;
  btnSave.hidden           = true;
  btnGoEdit.hidden         = true;
  previewBody.innerHTML    = '';
}

function setStatus(text, type) {
  msgStatus.textContent = text;
  msgStatus.className   = `msg msg--${type}`;
  msgStatus.hidden      = false;
}

function clearStatus() {
  msgStatus.hidden = true;
}

function showEl(el) {
  el.hidden = false;
}

