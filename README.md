# BestHouse 🏠

家庭房屋評估系統。讓 6 位家庭成員（本人、女友、雙方父母）能對多間房屋進行加權評分與比較，協助做出最佳購屋決策。

## 功能總覽

| 模組 | 功能 |
|------|------|
| 房屋列表 | 新增 / 編輯 / 刪除房屋，欄位排序，一鍵套用篩選條件 |
| 房屋評分 | 6 位成員各自對 8 個維度評分，系統自動計算加權總分 |
| 排名總覽 | 依加權分數排名，比較所有有效房屋 |
| 篩選條件 | 設定淘汰規則（最低坪數、最高總價、不接受車位類型等），自動標記不符合的房屋 |
| 看房心法 | 整理看房注意事項（結構安全、水電、法律產權、台灣特有風險等） |
| 看房評估 | 看完房後記錄現場狀況（壁癌、水壓、兇宅、淹水風險等 15 個評估欄位） |

### 價格計算
- 含車位每坪 = 總價 ÷ 總坪
- 不含車位每坪 = (總價 − 車位價格) ÷ (總坪 − 車位坪數)
- 折扣後總價 / 每坪（依屋主折扣自動計算）
- 實價登錄比較：開價 / 折扣後 vs 實價登錄估計的差距 %

## 技術架構

```
besthouse-frontend/   Angular 15 (standalone components)
besthouse-backend/    Spring Boot 3.2 + Java 17
docker-compose.yml    PostgreSQL 16
```

### 後端
- **Spring Boot 3.2** + Java 17
- **Spring Data JPA** + Hibernate 6
- **Flyway** 資料庫版本管理（V1–V6）
- **Lombok** 減少樣板程式碼
- **Jakarta Validation** 輸入驗證
- 分層架構：Controller → Service → Repository
- 統一 Exception Handling（`@RestControllerAdvice`）

### 前端
- **Angular 15** standalone components（無 NgModule）
- **Reactive Forms** + 即時價格計算
- **RxJS** HTTP 呼叫統一封裝在 Service 層
- Lazy-loaded routes
- 純 SCSS，無第三方 UI library

### 資料庫
- **PostgreSQL 16**（via Docker）

## 快速啟動

### 前置需求
- Docker Desktop
- Java 17+
- Maven 3.8+
- Node.js 18+ / npm

### 1. 啟動資料庫

```bash
docker compose up -d
```

### 2. 啟動後端

```bash
cd besthouse-backend
mvn spring-boot:run
```

後端啟動後 Flyway 會自動建立 schema 並執行初始資料（V1–V6）。
API 位址：`http://localhost:8080`

### 3. 啟動前端

```bash
cd besthouse-frontend
npm install
npm start
```

前端位址：`http://localhost:4200`

---

## 手機存取（Tailscale）

使用 [Tailscale](https://tailscale.com) 可以在看房現場用手機操作，安全且免費。

### 原理

Tailscale 在電腦和手機之間建立加密的私人網路（基於 WireGuard VPN）。
電腦 port 對外部網際網路仍然關閉，只有同帳號的裝置能連線。

### 設定步驟（一次性）

**1. 安裝 Tailscale**
- 電腦：前往 [tailscale.com/download](https://tailscale.com/download) 下載 Windows 版
- 手機：App Store / Google Play 搜尋「Tailscale」
- 兩台裝置用**同一個 Google 帳號**登入

**2. 開放防火牆 Port 4200**

以系統管理員身份執行 PowerShell：
```powershell
New-NetFirewallRule -DisplayName "BestHouse" -Direction Inbound -Protocol TCP -LocalPort 4200 -Action Allow -Profile Any
```

**3. 關閉電腦休眠（看房期間）**

控制台 → 電源選項 → 插電時「永不」休眠

### 每次使用

**電腦端（看房前啟動）：**
```bash
# 啟動資料庫
docker compose up -d

# 啟動後端
cd besthouse-backend
mvn spring-boot:run

# 啟動前端（新開終端機）
cd besthouse-frontend
npm start
```

**手機端：**
1. 打開 Tailscale app，確認電腦旁邊顯示綠點
2. 查看電腦的 Tailscale IP（在 Tailscale app 裡可以看到，格式：`100.x.x.x`）
3. 手機瀏覽器開啟：`http://100.x.x.x:4200`

---

## 資料庫 Schema

### 主要資料表

| 資料表 | 說明 |
|--------|------|
| `HOUSE` | 房屋資訊（基本資料、價格、看房評估） |
| `MEMBER` | 家庭成員與評分權重 |
| `RATING_DIMENSION` | 評分維度定義（共 8 個） |
| `HOUSE_RATING` | 成員對各房屋各維度的評分 |
| `FILTER_RULE` | 篩選淘汰規則 |

### 評分維度與預設權重

| 成員 | 權重 |
|------|------|
| 本人 | 30% |
| 女友 | 30% |
| 本方父親 | 10% |
| 本方母親 | 10% |
| 對方父親 | 10% |
| 對方母親 | 10% |

| 維度 | 說明 |
|------|------|
| 價格性價比 | 單價與市場行情比較 |
| 交通便利性 | 捷運、公車、開車動線 |
| 生活機能 | 超市、學校、醫院等 |
| 格局採光 | 室內格局、採光通風 |
| 屋況品質 | 結構、裝潢、屋齡 |
| 社區環境 | 管委會、公設、鄰居素質 |
| 未來發展 | 都更潛力、重劃區、增值空間 |
| 家人感受 | 整體直覺喜好 |

### 篩選條件類型（FilterRuleType）

| 類型 | 說明 |
|------|------|
| `MAX_TOTAL_PRICE` | 總價上限 |
| `MIN_BUILD_AREA_PING` | 總坪下限 |
| `MIN_INDOOR_PING` | 室內坪下限 |
| `MAX_HOUSE_AGE_YEAR` | 屋齡上限 |
| `MIN_FLOOR` | 樓層下限 |
| `EXCLUDE_PARKING_TYPE` | 排除特定車位類型 |
| `MIN_PARKING_PING` | 車位坪數下限 |

## 專案結構

```
besthouse-backend/
├── controller/        API endpoints
├── service/           業務邏輯
├── repository/        Spring Data JPA
├── entity/            JPA entities + enums
├── dto/               Request / Response DTOs
├── exception/         統一例外處理
└── resources/
    └── db/migration/  Flyway SQL scripts (V1–V6)

besthouse-frontend/
└── src/app/
    ├── core/
    │   ├── models/    TypeScript 型別定義
    │   └── services/  HTTP service 層
    └── features/
        ├── houses/    房屋列表、表單、評分
        ├── ranking/   排名總覽
        ├── filter-rules/  篩選條件管理
        └── house-tips/    看房心法
```

## API 端點

| Method | Path | 說明 |
|--------|------|------|
| GET | `/api/houses` | 取得所有房屋 |
| POST | `/api/houses` | 新增房屋 |
| GET | `/api/houses/{id}` | 取得單一房屋 |
| PUT | `/api/houses/{id}` | 更新房屋 |
| DELETE | `/api/houses/{id}` | 刪除房屋 |
| POST | `/api/houses/{id}/restore` | 恢復已淘汰房屋 |
| GET | `/api/members` | 取得所有成員 |
| GET | `/api/rating-dimensions` | 取得評分維度 |
| GET | `/api/ratings/house/{id}` | 取得房屋評分 |
| PUT | `/api/ratings/house/{id}` | 更新房屋評分 |
| GET | `/api/ranking` | 取得加權排名 |
| GET | `/api/filter-rules` | 取得篩選規則 |
| POST | `/api/filter-rules` | 新增篩選規則 |
| DELETE | `/api/filter-rules/{id}` | 刪除篩選規則 |
| POST | `/api/filter-rules/apply` | 套用篩選條件 |
