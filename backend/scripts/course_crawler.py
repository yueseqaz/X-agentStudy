#!/usr/bin/env python3
import argparse
import html
import json
import re
import subprocess
import sys
from urllib.parse import parse_qs, quote, urlparse
from urllib.request import Request, urlopen


def fetch_json(url):
    req = Request(url, headers={
        "User-Agent": "Mozilla/5.0",
        "Referer": "https://search.bilibili.com/",
    })
    try:
        with urlopen(req, timeout=12) as response:
            return json.loads(response.read().decode("utf-8", errors="ignore"))
    except Exception:
        output = subprocess.check_output([
            "curl",
            "-sS",
            "-L",
            url,
            "-H",
            "User-Agent: Mozilla/5.0",
            "-H",
            "Referer: https://search.bilibili.com/",
        ], timeout=12)
        return json.loads(output.decode("utf-8", errors="ignore"))


def search_bilibili(query, limit):
    if not query:
        return []
    api = (
        "https://api.bilibili.com/x/web-interface/search/type"
        "?search_type=video&order=totalrank&page=1&keyword="
        + quote(query)
    )
    data = fetch_json(api)
    if data.get("code") != 0:
        raise RuntimeError(data.get("message") or "Bilibili 搜索失败")
    results = []
    for item in data.get("data", {}).get("result", []):
        title = clean_title(item.get("title", ""))
        if not title:
            continue
        bvid = item.get("bvid") or ""
        url = f"https://www.bilibili.com/video/{bvid}" if bvid else item.get("arcurl", "")
        if "/cheese/" in url:
            continue
        author = clean(item.get("author", ""))
        play = item.get("play", 0)
        duration = item.get("duration", "")
        description = " / ".join(part for part in [
            f"UP 主：{author}" if author else "",
            f"播放：{play}" if play else "",
            f"时长：{duration}" if duration else "",
        ] if part)
        results.append(candidate(
            title=title,
            description=description,
            url=url,
            source="哔哩哔哩",
            cover=normalize_cover(item.get("pic", "")),
            query=query,
            author=author,
        ))
        if len(results) >= limit:
            break
    return dedupe(results)


def candidate(title, description, url, source, cover, query, author=""):
    subject = guess_subject(query, title)
    return {
        "title": clean(title)[:180],
        "description": clean(description)[:600],
        "url": url,
        "source": source,
        "coverUrl": cover,
        "subjectName": subject,
        "subjectScope": guess_scope(title + " " + query),
        "tags": tags(subject, title + " " + author, query),
        "difficulty": guess_difficulty(title + " " + description),
    }


def guess_subject(query, title):
    text = clean(query or title)
    for token in re.split(r"[\s,，/|：:]+", text):
        if token and len(token) <= 32:
            return token
    return "公开课程"


def guess_scope(text):
    lowered = text.lower()
    if any(word in lowered for word in ["vue", "react", "前端", "css", "javascript", "typescript"]):
        return "前端开发"
    if any(word in lowered for word in ["java", "redis", "mysql", "spring", "后端", "数据库"]):
        return "后端开发"
    if any(word in lowered for word in ["数学", "线性代数", "概率", "calculus"]):
        return "数学基础"
    if any(word in lowered for word in ["ai", "机器学习", "深度学习", "llm"]):
        return "AI 技术"
    return "公开课程"


def guess_difficulty(text):
    lowered = text.lower()
    if any(word in lowered for word in ["入门", "基础", "beginner", "basic"]):
        return "入门"
    if any(word in lowered for word in ["高级", "进阶", "advanced"]):
        return "进阶"
    return "通用"


def tags(subject, title, query):
    values = [subject]
    for token in re.split(r"[\s,，/|：:]+", title + " " + query):
        safe = clean(token)
        if 1 < len(safe) <= 24 and safe not in values:
            values.append(safe)
        if len(values) >= 6:
            break
    return values


def source_name(url):
    host = urlparse(url).netloc.replace("www.", "")
    return host or "公开课程"


def clean(value):
    return html.unescape(value or "").replace("\n", " ").replace("\t", " ").strip()


def clean_title(value):
    return clean(re.sub(r"<[^>]+>", "", value or ""))


def normalize_cover(value):
    safe = clean(value)
    if safe.startswith("//"):
        return "https:" + safe
    return safe


def dedupe(items):
    seen = set()
    result = []
    for item in items:
        key = item["url"]
        if key in seen:
            continue
        seen.add(key)
        result.append(item)
    return result


def query_from_bilibili_url(url):
    if not url:
        return ""
    parsed = urlparse(url)
    if "bilibili.com" not in parsed.netloc:
        return ""
    params = parse_qs(parsed.query)
    return (params.get("keyword") or params.get("q") or [""])[0]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--query", default="")
    parser.add_argument("--url", default="")
    parser.add_argument("--limit", type=int, default=8)
    args = parser.parse_args()
    try:
        query = args.query or query_from_bilibili_url(args.url)
        data = search_bilibili(query, args.limit)
        print(json.dumps(data, ensure_ascii=False))
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
