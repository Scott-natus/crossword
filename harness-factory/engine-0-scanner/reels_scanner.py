"""
Instagram Reels URL 수집 (Engine-0 확장)

- **탐색(돋보기) 피드** (`--explore`): 앱에서 돋보기로 들어갈 때와 같이 /explore/ 큐레이션·인기도에 가까운 순서로 노출되는 경우가 많음 (URL로 정렬을 고정하진 않음).
- **해시태그** (/explore/tags/{tag}/) 또는 **직접 URL** (--url)에서 /reel/{shortcode}/ 링크를 스크롤 수집.
- 인스타는 비로그인·데이터센터 IP에서 차단되거나 로그인 유도가 잦음 → instagram_cookies.json 권장.
"""

from __future__ import annotations

import argparse
import asyncio
import json
import os
import random
import re
from urllib.parse import quote, urljoin

from playwright.async_api import async_playwright
from playwright_stealth import Stealth

DEFAULT_TAG = "reels"
DEFAULT_MAX = 100
# 웹: 검색(돋보기) 탭과 동일 계열 — 추천·인기 큐레이션 피드 (해시태그 최신순과 다름)
EXPLORE_URL = "https://www.instagram.com/explore/"


def _env_bool(name: str, default: bool) -> bool:
    v = os.environ.get(name)
    if v is None:
        return default
    return v.strip().lower() in ("1", "true", "yes", "y", "on")


def build_tag_explore_url(tag: str) -> str:
    t = tag.strip().lstrip("#")
    return f"https://www.instagram.com/explore/tags/{quote(t)}/"


def normalize_reel_url(href: str) -> str | None:
    if not href or "/reel/" not in href:
        return None
    if href.startswith("/"):
        href = urljoin("https://www.instagram.com/", href)
    m = re.search(
        r"(https?://(?:www\.)?instagram\.com/reel/([A-Za-z0-9_-]+)/?)",
        href,
        re.I,
    )
    if not m:
        return None
    return m.group(1).rstrip("/") + "/"


async def dismiss_instagram_overlays(page) -> None:
    for name in (
        "Allow all cookies",
        "Accept All",
        "Only allow essential cookies",
        "Not Now",
        "Dismiss",
    ):
        try:
            btn = page.get_by_role("button", name=name).first
            if await btn.is_visible(timeout=600):
                await btn.click()
                await page.wait_for_timeout(500)
        except Exception:
            continue


async def extract_reels_from_dom(page) -> list[dict]:
    return await page.evaluate(
        """() => {
  const out = [];
  const seen = new Set();
  document.querySelectorAll('a[href*="/reel/"]').forEach(a => {
    let href = a.href || a.getAttribute('href') || '';
    if (!href) return;
    const m = href.match(/\\/reel\\/([A-Za-z0-9_-]+)/);
    if (!m) return;
    const code = m[1];
    if (seen.has(code)) return;
    seen.add(code);
    let caption = '';
    const art = a.closest('article') || a.closest('div');
    if (art) {
      const cap = art.querySelector('span[class*="_ap3a"], span[class*="x1lliihq"], [data-testid="post-caption"]');
      if (cap) caption = (cap.textContent || '').trim().slice(0, 500);
    }
    let user = 'Unknown';
    const art0 = a.closest('article');
    if (art0) {
      const h = art0.querySelector('header a[href^="/"]');
      if (h) {
        const p = (h.getAttribute('href') || '').split('/').filter(Boolean);
        if (p.length && !['explore', 'reel', 'reels', 'p'].includes(p[0])) user = p[0];
      }
    }
    out.push({ href: a.href, caption, user });
  });
  return out;
}"""
    )


async def scrape_instagram_reels(
    start_url: str,
    max_target: int,
    cookies_path: str | None,
    entry_kind: str = "custom",
) -> None:
    async with async_playwright() as p:
        print("\n" + "=" * 50)
        print("[*] Engine-0: Instagram Reels URL 수집")
        print("=" * 50)
        print(f"    시작 URL: {start_url}")
        print(f"    진입 방식: {entry_kind}")
        print(f"    쿠키 파일: {cookies_path or '(없음)'}")

        browser = await p.chromium.launch(
            headless=True,
            args=["--disable-blink-features=AutomationControlled"],
        )
        context = await browser.new_context(
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            ),
            viewport={"width": 1280, "height": 900},
            locale="en-US",
        )

        if cookies_path and os.path.isfile(cookies_path):
            try:
                with open(cookies_path, "r", encoding="utf-8") as f:
                    raw = json.load(f)
                fixed = []
                for c in raw:
                    if "sameSite" in c and c["sameSite"] not in ("Strict", "Lax", "None"):
                        c["sameSite"] = "Lax"
                    fixed.append(c)
                await context.add_cookies(fixed)
                print("[+] 쿠키 로드 완료.")
            except Exception as e:
                print(f"[!] 쿠키 로드 실패: {e}")
        else:
            print("[!] 쿠키 없음 — 로그인 페이지·차단 가능성 높음. 브라우저에서 내보낸 instagram_cookies.json 권장.")

        page = await context.new_page()
        try:
            await Stealth().apply_stealth_async(page)
            print("[+] Stealth 적용.")
        except Exception as e:
            print(f"[!] Stealth 생략: {e}")

        await page.goto(start_url, wait_until="domcontentloaded", timeout=90000)
        await dismiss_instagram_overlays(page)
        await asyncio.sleep(2)

        body = await page.evaluate("() => document.body.innerText.slice(0, 400)")
        if "Log in" in body and "Sign up" in body and await page.locator('input[name="username"]').count() > 0:
            print("[!] 로그인 화면으로 리다이렉트됨. instagram_cookies.json(세션 쿠키)을 넣고 재실행하세요.")

        results: list[dict] = []
        seen: set[str] = set()
        stall = 0
        max_stall = 10

        while len(results) < max_target:
            batch = await extract_reels_from_dom(page)
            for row in batch:
                nu = normalize_reel_url(row.get("href", ""))
                if not nu or nu in seen:
                    continue
                seen.add(nu)
                results.append(
                    {
                        "no": len(results) + 1,
                        "platform": "instagram",
                        "title": (row.get("caption") or "")[:500],
                        "channel": row.get("user") or "Unknown",
                        "url": nu,
                    }
                )
                cap = (row.get("caption") or "")[:35]
                print(f"[{len(results)}/{max_target}] {cap or nu}")

                if len(results) >= max_target:
                    break

            if len(results) >= max_target:
                break

            prev = len(seen)
            await page.evaluate(
                "window.scrollBy(0, Math.max(600, innerHeight * 0.9))"
            )
            await asyncio.sleep(random.uniform(1.4, 2.8))
            if len(seen) == prev:
                stall += 1
                if stall >= max_stall:
                    print("[!] 스크롤해도 신규 릴스 링크가 없어 중단.")
                    break
            else:
                stall = 0

        print("\n" + "=" * 50)
        print(f"[*] 종료: 총 {len(results)}건 (고유 Reel URL)")
        print("=" * 50)

        out_path = os.environ.get("HARNESS_REELS_OUTPUT_JSON", "engine0_reels.json")
        try:
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(
                    {
                        "mode": "instagram_reels",
                        "entry_kind": entry_kind,
                        "entry_url": start_url,
                        "count": len(results),
                        "items": results,
                    },
                    f,
                    ensure_ascii=False,
                    indent=2,
                )
            print(f"[+] 저장: {out_path}")
        except Exception as e:
            print(f"[!] JSON 저장 실패: {e}")

        await browser.close()


def main() -> None:
    parser = argparse.ArgumentParser(description="Instagram Reels URL 수집 (Engine-0)")
    g = parser.add_mutually_exclusive_group()
    g.add_argument(
        "--tag",
        default=os.environ.get("INSTAGRAM_TAG", DEFAULT_TAG),
        help="해시태그( # 없이 ). explore/tags/{tag}/ 로 이동 (--explore·--url 과 배타)",
    )
    g.add_argument(
        "--explore",
        action="store_true",
        help=f"탐색(돋보기) 피드 {EXPLORE_URL} 로 이동 (인기·추천 큐레이션에 가까운 노출)",
    )
    g.add_argument(
        "--url",
        default=None,
        help="시작 URL 직접 지정 (최우선)",
    )
    parser.add_argument(
        "--max",
        type=int,
        default=int(os.environ.get("HARNESS_MAX_REELS", str(DEFAULT_MAX))),
        help="수집 목표 개수",
    )
    parser.add_argument(
        "--cookies",
        default=os.environ.get("INSTAGRAM_COOKIES_PATH", "instagram_cookies.json"),
        help="Playwright 포맷 쿠키 JSON 경로 (기본: instagram_cookies.json)",
    )
    parser.add_argument(
        "--no-cookies",
        action="store_true",
        help="쿠키 파일 사용 안 함",
    )
    args = parser.parse_args()

    use_explore = bool(
        args.explore or _env_bool("INSTAGRAM_USE_EXPLORE", False)
    )
    if args.url:
        start_url = args.url
        entry_kind = "custom"
    elif use_explore:
        start_url = EXPLORE_URL
        entry_kind = "explore"
    else:
        start_url = build_tag_explore_url(args.tag)
        entry_kind = "tag"

    cookies_path = None if args.no_cookies else (args.cookies or None)

    asyncio.run(
        scrape_instagram_reels(
            start_url=start_url,
            max_target=args.max,
            cookies_path=cookies_path,
            entry_kind=entry_kind,
        )
    )


if __name__ == "__main__":
    main()
