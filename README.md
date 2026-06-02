# BestHouse 🏠

家庭房屋評估系統。記錄看過的房屋、評分、比較，並支援 AI 截圖自動匯入資料。

## 功能

- **房屋列表**：新增、編輯、刪除房屋，即時計算單價、折扣後總價與每坪、折扣後不含車位每坪、公設比、貸款月付與租買比較；欄位（含代號、地址）可排序、可自由顯示/隱藏（設定存於 localStorage）、可拖拉調整欄位寬度；**預設依「不含車位單價」升序**；表格採 sticky thead + 凍結代號欄（捲動時欄位名永遠在頂端、代號永遠在最左）
  - 「折扣後不含車位每坪」公式：`(折扣後總價 − 有效車位價) ÷ (建坪 − 車位坪)`；車位價未填時以「車位坪 × 30 萬」估算（與後端一致）
  - 「距高點跌幅」欄位：折扣後不含車位每坪較「實登上限不含車位每坪」（視為歷史高點）下降的百分比，公式 `(實登上限 − 折扣後每坪) ÷ 實登上限 × 100`；正值代表現價低於高點，未填實登上限或屋主折讓時顯示 `-`
- **屋主折扣**：表單改為直接填寫「**折扣後總價（萬）**」，系統自動反算折扣 %（後端仍存 discountPercent，schema 不變）
- **實登每坪參考**：表單改為直接填寫「**實登成交總價（萬）**」上下限與最新一筆，系統自動套用「不含車位每坪」公式 `(實登總價 − 有效車位價) ÷ (建坪 − 車位坪)` 換算成每坪單價（後端仍存每坪值，schema 不變）；輸入框下方即時顯示「→ 不含車位每坪：XX.XX 萬」；列表顯示「實登每坪範圍」（例：`60~70`）與「最新實登每坪」；表單在「下限」label 旁附 🔍 樂居實登連結（自動帶入新竹市東區、過去一年、總坪 ±3 篩選）
- **房屋地圖**：將所有房屋地址地理編碼並在 Google Maps 標記，點擊 marker 可查看名稱、地址、開價與折扣資訊；可切換顯示/隱藏已淘汰物件
- **評分系統**：多維度家庭評分、加權排名；維度依權重由高到低顯示。權重：地點與交通 40%、生活機能 15%、價格性價比 15%、格局與空間感 9%、採光與通風 7%、社區環境與管理 7%、屋況與裝潢 5%、整體主觀感受 2%
- **篩選條件**：自訂規則自動淘汰不符條件的房屋，支援總價/單價/屋齡/室內坪/最低樓層/**最高樓層**/車位坪/戶梯比上限、車位類型排除、4 條步行距離上限。看房問題分兩級處理：
  - **致命缺陷** → 直接淘汰：凶宅、海砂屋、輻射屋、違建、嫌惡設施、淹水高風險
  - **可修缮缺陷** → 標記警告但不淘汰：發霉/漏水、地板不平、門窗異常、水壓異常、車位最低層、管委會 NG
- **看房評估**：記錄現場狀況（壁癌、海砂、兇宅、車位在地下最下層、管委會疑慮、嫌惡設施等）；嫌惡設施與管委會疑慮支援多行 textarea；列表「看房問題」欄位直接顯示是否有任何問題項目；觸發可修缮缺陷的房屋會在代號旁顯示 ⚠️ icon，hover 即可看到警告原因
- **機車位**：每間房屋可記錄「有/無機車位」狀態，列表獨立欄位顯示
- **大樓配置（戶/梯比）**：記錄每層戶數與電梯數，自動算出戶/梯比並支援列表顯示與篩選（建議 ≤3 為佳）
- **交通與學區**：每間房屋可記錄「步行至竹北高鐵 / 新竹火車站 / 最近國小 / 最近國中」公尺數與站名/校名；編輯頁附 Google Maps 大眾運輸路線連結（下一個工作日早上 9 點出發），列表 click 展開站名/校名 tooltip
- **AI 截圖匯入**：對任意房屋網站截圖，自動解析資料（Google Gemini）
- **Chrome Extension**：常駐側邊欄，截圖後直接匯入
- **看房心法**：整合看房注意事項靜態頁面

---

## 技術架構

| 層 | 技術 |
|---|---|
| 前端 | Angular 15（Standalone Component） |
| 後端 | Spring Boot 3.2 + Java 17 |
| 資料庫 | MariaDB 11（Docker） |
| DB Migration | Flyway |
| AI | Google Gemini API |
| 地圖 | Google Maps JavaScript API |
| 本機連線 | Tailscale VPN |

---

## 本機啟動

### 前置需求

- Java 17+
- Node.js 18+
- Docker Desktop

### 1. 啟動資料庫

```bash
docker compose up -d
```

### 2. 設定 Gemini API Key

到 [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey) 取得免費 API Key，
建立 `application-local.yml`（此檔不會上傳 git）：

```yaml
# besthouse-backend/src/main/resources/application-local.yml
gemini:
  api-key: 你的-gemini-api-key
```

### 3. 設定 Google Maps API Key

到 [Google Cloud Console](https://console.cloud.google.com/) 建立 API Key，並啟用以下兩個 API：
- **Maps JavaScript API**
- **Geocoding API**

將 Key 填入前端 environment 檔（此檔已設為 `skip-worktree`，本地變更不會被 git 追蹤）：

```typescript
// besthouse-frontend/src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: '',
  googleMapsApiKey: '你的-google-maps-api-key',
};
```

> **注意**：`environment.ts` 已透過 `git update-index --skip-worktree` 設定，填入 Key 後不會意外上傳至 GitHub。

### 4. 啟動後端

```bash
cd besthouse-backend
mvn spring-boot:run
```

後端運行於 `http://localhost:8080`

### 5. 啟動前端

```bash
cd besthouse-frontend
npm install
npm start
```

前端運行於 `http://localhost:4200`

---

## Chrome Extension 安裝

1. 開啟 Chrome `chrome://extensions/`
2. 右上角開啟「**開發人員模式**」
3. 點「**載入未封裝項目**」→ 選擇 `besthouse-extension` 資料夾
4. 點 Extension 圖示，右側開啟常駐側邊欄

### 使用方式

1. 開啟任意房屋網站的物件詳情頁
2. 在側邊欄調整截圖張數（預設 3 張）
3. 點「📸 AI 截圖匯入」
4. 等待 AI 解析（約 3–5 秒）
5. 確認欄位 → 儲存 → 跳轉到編輯頁補填細節

---

## 手機存取（Tailscale）

使用 [Tailscale](https://tailscale.com) VPN，讓手機安全連線到本機：

1. 電腦與手機都安裝 Tailscale 並登入同一帳號
2. 手機開啟 Tailscale VPN
3. 查詢電腦的 Tailscale IP：
   ```bash
   tailscale ip -4
   ```
4. 手機瀏覽器輸入 `http://<Tailscale-IP>:4200`

---

## 資料庫 Migration

Flyway 自動執行，版本如下：

| 版本 | 內容 |
|---|---|
| V1 | 初始 Schema（HOUSE、MEMBER、RATING 等） |
| V2 | 新增 FilterRule 表 |
| V3 | 新增 HouseStatus、淘汰欄位 |
| V4 | 新增 discountPercent、estimatedRegistryPrice |
| V5 | 新增 hasVisited |
| V6 | 新增 15 個看房評估欄位 |
| V7 | 新增 listingUrl |
| V8 | listingUrl 加唯一索引 |
| V9 | 新增 monthlyRent（每月租金） |
| V10 | 新增 REAL_PRICE_RECORD 表（實價登錄） |
| V11 | REAL_PRICE_RECORD 新增 parkingAreaPing（車位坪） |
| V12 | REAL_PRICE_RECORD 新增 LAT / LNG（地理編碼座標，已廢棄） |
| V13 | 移除 LAT / LNG 欄位 |
| V14 | 新增 IS_PARKING_LOWEST_FLOOR（車位在地下最下層） |
| V15 | 新增 MANAGEMENT_NOTE（管委會疑慮說明） |
| V16 | 新增 UNITS_PER_FLOOR、ELEVATOR_COUNT（每層戶數、電梯數） |
| V17 | 新增 WALK_METERS_TO_HSR_ZHUBEI、WALK_METERS_TO_FENGYUAN（步行至竹北高鐵、新竹火車站公尺數） |
| V18 | 新增 NEAREST_STATION_TO_HSR_ZHUBEI、NEAREST_STATION_TO_FENGYUAN、WALK_METERS_TO_ELEMENTARY、NEAREST_ELEMENTARY_SCHOOL、WALK_METERS_TO_JUNIOR_HIGH、NEAREST_JUNIOR_HIGH_SCHOOL（站名/校名 + 國小國中步行距離） |
| V19 | 重新平衡 RATING_DIMENSION 權重（地點 40%、生活機能/價格 15%，其他等比縮減） |
| V20 | 重置所有 HOUSE.PARKING_PRICE 為 0（預設估算公式從車位坪 × 20 萬改為 × 30 萬） |
| V21 | 移除 ESTIMATED_REGISTRY_PRICE 欄位與 REAL_PRICE_RECORD 表，新增 REGISTRY_PRICE_PER_PING_MIN/MAX（過去一年實登不含車位每坪上下限，使用者手填） |
| V22 | 新增 LATEST_REGISTRY_PRICE_PER_PING（最新一筆實登不含車位每坪，使用者手填） |
| V23 | 新增 HAS_MOTORCYCLE_PARKING（有/無機車位） |
| V24 | 新增 WARNING_REASON 欄位；把舊 `EXCLUDE_VISIT_ISSUES` 規則升級為 `EXCLUDE_FATAL_VISIT_ISSUES`（致命缺陷淘汰）；新增 `WARN_REPAIRABLE_VISIT_ISSUES` 規則（可修缮缺陷只警告） |
