# BestHouse 🏠

家庭房屋評估系統。記錄看過的房屋、評分、比較，並支援 AI 截圖自動匯入資料。

## 功能

- **房屋列表**：新增、編輯、刪除房屋，即時計算單價、折扣後總價與每坪、折扣後不含車位每坪、貸款月付與租買比較；顯示開價與折扣後 vs 實價登錄差距；欄位可排序、可自由顯示/隱藏（設定存於 localStorage）、可拖拉調整欄位寬度
- **評分系統**：多維度家庭評分、加權排名
- **篩選條件**：自訂規則自動淘汰不符條件的房屋
- **看房評估**：記錄現場狀況（壁癌、海砂、兇宅等），列表直接顯示是否有任何問題項目
- **AI 截圖匯入**：對任意房屋網站截圖，自動解析資料（Google Gemini）
- **Chrome Extension**：常駐側邊欄，截圖後直接匯入
- **看房心法**：整合看房注意事項靜態頁面
- **實價登錄比對**：批次下載新竹縣市近 3 年成交資料，依地址、面積、屋齡、樓層自動找出最相近成交，並顯示不含車位每坪單價

---

## 技術架構

| 層 | 技術 |
|---|---|
| 前端 | Angular 15（Standalone Component） |
| 後端 | Spring Boot 3.2 + Java 17 |
| 資料庫 | MariaDB 11（Docker） |
| DB Migration | Flyway |
| AI | Google Gemini API |
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

```bash
# besthouse-backend/src/main/resources/application-local.yml
gemini:
  api-key: 你的-gemini-api-key
```

### 3. 啟動後端

```bash
cd besthouse-backend
mvn spring-boot:run
```

後端運行於 `http://localhost:8080`

### 4. 啟動前端

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

## 實價登錄比對

### 同步資料

在房屋列表右上角點「**同步實價登錄**」，系統會自動下載近 3 年新竹縣市所有季度成交資料（約數千筆），儲存至本機資料庫。

> 資料來源：內政部不動產交易實價查詢服務網，僅支援新竹縣（J）、新竹市（O）。

### 查詢相近成交

進入房屋編輯頁 → 「實價登錄估計總價」欄位旁點「**查實價登錄**」，系統會依以下條件過濾並排序：

**硬篩選（不符即排除）**

| 條件 | 門檻 |
|---|---|
| 地區相符 | 地址需包含成交記錄的行政區 |
| 同路名 | 含縣市前綴的跨格式比對 |
| 門牌號差 | ≤ 30 號 |
| 屋齡差 | ≤ 10 年 |

**相似度評分（0–100）**

| 維度 | 權重 | 歸零閾值 |
|---|---|---|
| 面積差 | 75% | 差 35% |
| 屋齡差 | 15% | 差 10 年 |
| 樓層差 | 7% | 差 6 層 |
| 房間數差 | 3% | 差 2 間 |

點「**採用此價**」可直接將成交總價填入估計欄位。

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
