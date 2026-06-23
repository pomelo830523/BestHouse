# besthouse-kb — 程式碼搜尋 MCP

讓 Claude Code 用自然語言查 BestHouse 的**業務邏輯與公式**，答案直接來自當前原始碼，
不維護任何靜態文件，因此**不會與程式碼 drift**。

## 提供的工具

| 工具 | 說明 |
|------|------|
| `search_code(query, top_k=3)` | 關鍵字搜尋原始碼，回傳最相關的 method/function（含 `檔名:行號` 與原文） |
| `read_source(relative_path)` | 讀取指定原始碼檔案的完整內容（路徑以專案根為基準） |

> 中文語意搜尋改由 [codegraph](https://github.com/colbymchenry/codegraph) 的 MCP 處理（見下方「搭配 codegraph」），
> 本 server 專注於關鍵字 + 結構化的原始碼查詢。

掃描範圍：`besthouse-backend/src`、`besthouse-frontend/src`（`.java` / `.ts`），
自動跳過 `node_modules`、`target`、`dist`、`.venv` 等。

## 實作原理

### 核心觀念：原始碼就是唯一真實來源

傳統知識庫（KB）把「公式說明」抄一份到 `.md` 檔，於是有**兩份會各自演化**的東西：
程式碼改了、文件忘了改 → drift。本 MCP **不存任何說明**，每次查詢都**即時讀當前原始碼**，
所以根本沒有第二份東西需要同步——這就是「零維護」的來源。

### 一次查詢的流程

```
使用者問「不含車位單價怎麼算」
  → MCP 即時掃描原始碼檔案（不讀任何快取/索引）
  → 把每個檔案切成 method/function 區塊
  → 對每個區塊評分，挑出最相關的幾個
  → 回傳「檔名:行號 + 原文」給 Claude
  → Claude 讀真碼，用自然語言解釋公式
```

關鍵：**沒有預先建好的索引、也不快取**。每次都重讀檔案，因此回答永遠等於「此刻磁碟上的程式碼」。
你改完 `HouseService` 存檔，下一次查詢就是新邏輯，不需要動這個 MCP 一行。

### 兩個核心技巧

1. **大括號配對切區塊**（`extract_blocks`）
   逐字掃描原始碼、追蹤 `{` `}` 深度，把每個「signature 含 `(` 且非控制流程」的區塊
   （method / function / constructor）連同起訖行號切出來。
   這讓回傳單位是「一個完整方法」，而不是整個檔案或零碎幾行。

2. **中英雙語評分**（`score`）
   查詢可能是中文，程式碼則同時有英文 identifier 與中文註解，所以雙管齊下：
   - **英文**：查詢中的英文詞（如 `parking`、`pricePerPing`）出現在程式碼即加分，命中 signature 加重。
   - **中文**：把查詢切成 bigram（如 `車位`、`單價`、`建坪`），比對程式碼中的中文註解。
   因此「不含車位單價的計算公式」能命中那個註解寫著「車位價未填時…」的計算方法。
   > 這也是為什麼**保持中文註解清楚**會直接提升命中率——註解是中文查詢的主要錨點。

### 分工：MCP 只負責「找」，Claude 負責「懂」

本 MCP 的搜尋刻意做得簡單（關鍵字/bigram 比對即可），因為**真正理解自然語言、
讀懂 Java 並翻成白話的是 Claude**。MCP 只要把「對的那段程式碼」撈出來交給它就夠了。
這個分工也讓 server 維持零外部依賴、可在封閉環境離線運行。

## 搭配 codegraph

本專案另裝了 [codegraph](https://github.com/colbymchenry/codegraph)（結構圖 + call graph + 中文搜尋）。
兩者分工：

- **本 server（`search_code` / `read_source`）**：關鍵字 + 中文註解比對找到 method/規則，回原始碼原文。
- **codegraph 的 MCP**：中文語意搜尋、跨檔結構查詢（`callers` / `callees` / `impact` / `explore`）。

中文語意檢索交給 codegraph 處理，本 server 不再自建 embedding 層。
三種 code KB 方案的完整比較見 `code-kb-comparison.md`。

## 環境需求

- Python 3.10+
- Claude Code

## 安裝 / 重建環境

venv 為可重建產物，已 gitignore，不進版控。clone 後重建：

```powershell
cd C:\Java_workspace\BestHouse\besthouse-kb
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

## 註冊到 Claude Code

MCP 設定已存於專案根的 `BestHouse/.mcp.json`（project scope，可隨專案共用）。
從 `BestHouse` 目錄啟動 Claude Code 即會自動載入。

若需手動重新註冊：

```powershell
cd C:\Java_workspace\BestHouse
claude mcp remove besthouse-kb -s project
claude mcp add besthouse-kb --scope project --env PYTHONUTF8=1 -- `
  C:\Java_workspace\BestHouse\besthouse-kb\.venv\Scripts\python.exe `
  C:\Java_workspace\BestHouse\besthouse-kb\kb_server.py
claude mcp list   # 確認狀態為 Connected
```

> 改了 `.mcp.json` 或 `kb_server.py` 後，需在 Claude 內 `/mcp` → Reconnect（或重開）才會生效。

## 使用方式

在 Claude Code 內直接問，例如：

> 不含車位單價怎麼算？用程式碼回答

Claude 會呼叫 `search_code` 撈出對應 method 並解釋。

## 注意事項

- **唯讀**：本 MCP 只讀檔案、不寫不執行，是最安全的 MCP 類型。
- **路徑為絕對路徑**：搬移 `besthouse-kb` 資料夾後，需重建 `.venv` 並更新 `.mcp.json` 內的路徑。
- 中文查詢靠比對程式碼中的中文註解，因此**保持註解清楚**可提升搜尋命中率。
