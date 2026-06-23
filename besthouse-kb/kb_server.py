"""BestHouse 程式碼搜尋 MCP server（方案 A）。

直接讀最新原始碼，不維護任何靜態知識庫，因此永遠不會與程式碼 drift。
以大括號配對把 .java / .ts 切成 method 層級區塊，依查詢相關度回傳 檔名:行號 + 原文。
純標準函式庫、無外部依賴、唯讀。
啟動：python kb_server.py（由 Claude Code 以 stdio 方式叫起）
"""

from mcp.server.fastmcp import FastMCP
from pathlib import Path
import re

REPO_ROOT = Path(__file__).parent.parent  # BestHouse 專案根
SOURCE_DIRS = [
    REPO_ROOT / "besthouse-backend" / "src",
    REPO_ROOT / "besthouse-frontend" / "src",
]
SOURCE_GLOBS = ("*.java", "*.ts")
CONTROL_KEYWORDS = (
    "if", "for", "while", "switch", "catch", "else",
    "try", "do", "return", "new", "synchronized",
)
MAX_BODY_CHARS = 6000   # 過大的區塊（整個 class）不回傳
MAX_BODY_LINES = 90     # 單一區塊回傳行數上限

mcp = FastMCP("besthouse-code")


def iter_source_files():
    """走訪所有原始碼檔案，跳過 node_modules / target / dist 等。"""
    skip = {"node_modules", "target", "dist", ".git", ".venv", ".angular"}
    for base in SOURCE_DIRS:
        if not base.exists():
            continue
        for pattern in SOURCE_GLOBS:
            for path in base.rglob(pattern):
                if any(part in skip for part in path.parts):
                    continue
                yield path


def extract_blocks(text: str):
    """用大括號配對切出 method/function 層級區塊。

    回傳 [(start_line, end_line, signature, body), ...]。
    只保留「signature 含 ( 且非控制流程」的區塊（method/function/constructor）。
    """
    blocks = []
    stack = []          # (start_line, signature, open_index)
    sig_start = 0       # 目前 signature 文字的起點（上一個 ; { } 之後）
    line = 1
    for idx, ch in enumerate(text):
        if ch == "\n":
            line += 1
        elif ch == "{":
            raw = text[sig_start:idx].strip().splitlines()
            signature = raw[-1].strip() if raw else ""
            stack.append((line, signature, idx))
            sig_start = idx + 1
        elif ch == "}":
            if stack:
                start_line, signature, open_idx = stack.pop()
                body = text[open_idx:idx + 1]
                head = signature.split("(")[0].strip()
                name = head.split()[-1] if head.split() else ""
                is_control = any(signature.startswith(k) for k in CONTROL_KEYWORDS)
                is_method = "(" in signature and bool(name) and not is_control
                # 也納入型別宣告（enum / class / interface / record），
                # 這樣「有哪幾種 X」這種答案在 enum 常數裡的問題也查得到。
                is_type_decl = re.search(
                    r"\b(class|enum|interface|record)\s+\w+", signature
                ) is not None
                if (is_method or is_type_decl) and len(body) <= MAX_BODY_CHARS:
                    blocks.append((start_line, line, signature, body))
            sig_start = idx + 1
        elif ch == ";":
            sig_start = idx + 1
    return blocks


def score(query: str, signature: str, body: str) -> int:
    """中文 bigram 比對註解 + 英文 identifier 比對。signature 命中加重。"""
    q = query.lower()
    sig_l = signature.lower()
    body_l = body.lower()
    total = 0

    # 英文 / identifier：查詢中的 ascii 詞出現在程式碼即加分，命中 signature 更重
    for term in set(re.findall(r"[a-z]{3,}", q)):
        if term in sig_l:
            total += 5
        elif term in body_l:
            total += 2

    # 中文：query 取 bigram，比對程式碼（含中文註解）
    haystack = sig_l + "\n" + body_l
    bigrams = {q[i:i + 2] for i in range(len(q) - 1)}
    chinese_bigrams = {bg for bg in bigrams if any("一" <= c <= "鿿" for c in bg)}
    for bg in chinese_bigrams:
        if bg in sig_l:
            total += 3
        elif bg in haystack:
            total += 1
    return total


def _truncate(body: str) -> str:
    lines = body.splitlines()
    if len(lines) <= MAX_BODY_LINES:
        return body
    kept = lines[:MAX_BODY_LINES]
    kept.append(f"    // ...（區塊過長，已截斷，共 {len(lines)} 行；可用 read_source 取完整檔案）")
    return "\n".join(kept)


@mcp.tool()
def search_code(query: str, top_k: int = 3) -> str:
    """用自然語言搜尋 BestHouse 最新原始碼，回傳最相關的 method/function 原文（含檔名與行號）。

    適合詢問「某公式怎麼算」「某規則的實作在哪」，答案直接來自當前程式碼，不會過期。
    """
    candidates = []
    for path in iter_source_files():
        try:
            text = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        rel = path.relative_to(REPO_ROOT).as_posix()
        for start, end, signature, body in extract_blocks(text):
            s = score(query, signature, body)
            if s > 0:
                candidates.append((s, rel, start, end, body))

    if not candidates:
        return "找不到相關程式碼。"

    candidates.sort(key=lambda c: c[0], reverse=True)
    parts = []
    for _, rel, start, end, body in candidates[:top_k]:
        parts.append(f"### {rel}:{start}-{end}\n```\n{_truncate(body)}\n```")
    return "\n\n".join(parts)


@mcp.tool()
def read_source(relative_path: str) -> str:
    """讀取指定原始碼檔案的完整內容。relative_path 以專案根為基準，例如
    besthouse-backend/src/main/java/com/besthouse/service/HouseService.java"""
    target = (REPO_ROOT / relative_path).resolve()
    # 防目錄穿越：限制在 REPO_ROOT 內
    if REPO_ROOT.resolve() not in target.parents and target != REPO_ROOT.resolve():
        return "路徑超出專案範圍，拒絕讀取。"
    if not target.is_file():
        return f"找不到檔案：{relative_path}"
    return target.read_text(encoding="utf-8")


if __name__ == "__main__":
    mcp.run()
