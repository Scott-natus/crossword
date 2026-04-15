
import argparse
import asyncio
import json
import os
import random
import re
from urllib.parse import quote_plus, urlparse, urlunparse

from playwright.async_api import async_playwright
from playwright_stealth import Stealth

# --- 기본값 ---
DEFAULT_SEARCH_QUERY = "best shorts"
# URL sp는 검색과 조합 시 레이아웃이 바뀌어 쇼츠 링크가 사라지는 경우가 있음(예: CAMSAhAB).
# 인기도·쇼츠·기간은 기본적으로 UI 필터로 적용하고, 필요 시 브라우저에서 복사한 sp만 YOUTUBE_SEARCH_SP 로 지정.
DEFAULT_SEARCH_SP = ""


def _env_bool(name: str, default: bool) -> bool:
    v = os.environ.get(name)
    if v is None:
        return default
    return v.strip().lower() in ("1", "true", "yes", "y", "on")


def build_search_url(
    query: str,
    sp: str | None = None,
    hl: str = "en",
    gl: str = "US",
) -> str:
    """검색 결과 URL (hl=en·gl=US: 필터 UI/셀렉터를 영문으로 통일)."""
    q = quote_plus(query)
    parts = list(urlparse(f"https://www.youtube.com/results?search_query={q}"))
    # query string 재구성
    qs = f"search_query={q}&hl={hl}&gl={gl}"
    if sp:
        qs += f"&sp={sp}"
    parts[4] = qs
    return urlunparse(parts)


def normalize_shorts_url(href: str) -> str | None:
    if not href or "/shorts/" not in href:
        return None
    if href.startswith("/"):
        href = "https://www.youtube.com" + href
    m = re.search(r"(https?://[^#\s]+/shorts/([a-zA-Z0-9_-]+))", href)
    if not m:
        return None
    vid = m.group(2)
    # 네비게이션용 /shorts/ 홈 등(비디오 ID 없음·짧음) 제외
    if len(vid) < 8:
        return None
    return m.group(1)


async def dismiss_consent_if_present(page) -> None:
    """YouTube/EU 동의 배너가 있으면 닫기 (headless에서 결과 로딩 차단 방지)."""
    for name in (
        "Accept all",
        "Accept All",
        "모두 동의",
        "동의",
        "I agree",
    ):
        try:
            btn = page.get_by_role("button", name=name).first
            if await btn.is_visible(timeout=800):
                await btn.click()
                await page.wait_for_timeout(800)
                return
        except Exception:
            continue


async def apply_search_filters_ui(page) -> bool:
    """
    검색 필터 패널에서 Type=Shorts, Upload date=This week, Sort/Prioritize=Popularity(인기도) 적용 시도.
    영문 UI(hl=en) 기준. 실패 시 False — 이후 URL의 sp로 폴백.
    """
    try:
        # 필터 버튼 (영문/한글)
        opened = False
        for name_pat in (
            re.compile(r"search filters", re.I),
            re.compile(r"^필터$"),
            re.compile(r"filter", re.I),
        ):
            try:
                btn = page.get_by_role("button", name=name_pat).first
                await btn.click(timeout=4000)
                opened = True
                break
            except Exception:
                continue
        if not opened:
            # 대체: 상단 필터 칩 옆 버튼
            alt = page.locator("#filter-button, ytd-button-renderer button").first
            await alt.click(timeout=4000)

        await page.wait_for_timeout(1200)

        overlay = page.locator(
            "ytd-search-filter-overlay-renderer, [role='dialog'], tp-yt-paper-dialog"
        ).first
        await overlay.wait_for(state="visible", timeout=8000)

        # Type → Shorts
        try:
            await overlay.get_by_text("Shorts", exact=True).first.click(timeout=5000)
        except Exception:
            await page.get_by_text("Shorts", exact=True).first.click(timeout=5000)
        await page.wait_for_timeout(400)

        # Upload date → This week
        try:
            await overlay.get_by_text("This week", exact=True).first.click(timeout=5000)
        except Exception:
            await page.get_by_text("This week", exact=True).first.click(timeout=5000)
        await page.wait_for_timeout(400)

        # Sort by / Prioritize → Popularity (조회수·인기도)
        for label in ("Popularity", "View count"):
            try:
                await overlay.get_by_text(label, exact=True).first.click(timeout=3000)
                break
            except Exception:
                continue
        else:
            try:
                await page.get_by_text("Popularity", exact=True).first.click(timeout=5000)
            except Exception:
                pass

        await page.wait_for_timeout(2500)
        return True
    except Exception as e:
        print(f"[!] UI 필터 적용 실패 (sp URL로 대체 가능): {e}")
        return False


async def extract_shorts_from_dom(page) -> list[dict]:
    """검색 결과에서 Shorts URL·제목·채널 추출 (href 다양한 형식 대응)."""
    rows = await page.evaluate(
        """() => {
  const out = [];
  const seen = new Set();
  const nodes = document.querySelectorAll('a[href]');
  nodes.forEach(a => {
    let href = a.href || a.getAttribute('href') || '';
    if (!href || href.includes('googleads')) return;
    if (!href.includes('/shorts/')) return;
    const m = href.match(/\\/shorts\\/([a-zA-Z0-9_-]+)/);
    if (!m || m[1].length < 8) return;
    const id = m[1];
    if (seen.has(id)) return;
    seen.add(id);
    let title = (a.textContent || '').trim();
    const vr = a.closest(
      'ytd-video-renderer, ytd-reel-item-renderer, ytd-rich-item-renderer, ytd-compact-video-renderer'
    );
    if (vr) {
      const t = vr.querySelector('#video-title, a#video-title, h3 a, yt-formatted-string#video-title');
      if (t) title = (t.textContent || '').trim() || title;
      let channel = 'Unknown';
      const ch = vr.querySelector('#channel-name a, ytd-channel-name a, .ytd-channel-name a');
      if (ch) channel = (ch.textContent || '').trim();
      out.push({ href: a.href, title, channel });
    } else {
      out.push({ href: a.href, title: title || '', channel: 'Unknown' });
    }
  });
  return out;
}"""
    )
    return rows


async def scrape_search_shorts(
    search_query: str,
    search_sp: str,
    max_target: int,
    use_cookies: bool,
    apply_ui_filters: bool,
) -> None:
    async with async_playwright() as p:
        print("\n" + "=" * 50)
        print("[*] Engine-0: 검색·필터 기반 쇼츠 URL 수집")
        print("=" * 50)
        print(f"    검색어: {search_query!r}")
        print(f"    sp(폴백): {search_sp!r} | 쿠키 주입: {use_cookies} | UI필터 시도: {apply_ui_filters}")

        browser = await p.chromium.launch(
            headless=True,
            args=["--disable-blink-features=AutomationControlled"],
        )

        context = await browser.new_context(
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            ),
            viewport={"width": 1920, "height": 1080},
            locale="en-US",
        )

        if use_cookies:
            try:
                with open("cookies.json", "r", encoding="utf-8") as f:
                    raw_cookies = json.load(f)
                formatted_cookies = []
                for cookie in raw_cookies:
                    if "sameSite" in cookie:
                        if cookie["sameSite"] not in ("Strict", "Lax", "None"):
                            cookie["sameSite"] = "Lax"
                    formatted_cookies.append(cookie)
                await context.add_cookies(formatted_cookies)
                print("[+] 쿠키 로드 완료 (개인화 피드에 영향 가능).")
            except FileNotFoundError:
                print("[!] cookies.json 없음 — 비로그인.")
            except Exception as e:
                print(f"[!] 쿠키 오류: {e}")
        else:
            print("[+] 쿠키 미사용 — 추천/개인화 영향 최소화 (검색·필터 위주).")

        page = await context.new_page()

        try:
            await Stealth().apply_stealth_async(page)
            print("[+] Stealth 적용.")
        except Exception as e:
            print(f"[!] Stealth 생략: {e}")

        sp_value = (search_sp or "").strip() or None
        ui_ok = False

        async def _wait_results() -> None:
            await dismiss_consent_if_present(page)
            try:
                await page.wait_for_selector(
                    "ytd-video-renderer, ytd-reel-item-renderer, ytd-item-section-renderer",
                    timeout=25000,
                )
            except Exception:
                print("[!] 검색 결과 블록 대기 타임아웃 — 계속 시도")
            await asyncio.sleep(2)

        if apply_ui_filters:
            first_url = build_search_url(search_query, sp=None)
            print(f"[*] 이동 (UI 필터: Shorts / This week / Popularity): {first_url}")
            await page.goto(first_url, wait_until="domcontentloaded", timeout=60000)
            await _wait_results()
            ui_ok = await apply_search_filters_ui(page)
            if not ui_ok and sp_value:
                fallback = build_search_url(search_query, sp=sp_value)
                print(f"[*] UI 실패 — sp URL로 재시도: {fallback}")
                await page.goto(fallback, wait_until="domcontentloaded", timeout=60000)
                await _wait_results()
            elif not ui_ok:
                print("[!] UI 필터 실패, sp 미지정 — 현재 검색 결과 페이지로 수집 계속")
        else:
            target = build_search_url(search_query, sp=sp_value)
            print(f"[*] 이동: {target}")
            await page.goto(target, wait_until="domcontentloaded", timeout=60000)
            await _wait_results()

        results: list[dict] = []
        seen_urls: set[str] = set()
        stall = 0
        max_stall = 8

        while len(results) < max_target:
            batch = await extract_shorts_from_dom(page)
            for row in batch:
                nu = normalize_shorts_url(row.get("href", ""))
                if not nu or nu in seen_urls:
                    continue
                seen_urls.add(nu)
                results.append(
                    {
                        "no": len(results) + 1,
                        "title": row.get("title") or "",
                        "channel": row.get("channel") or "Unknown",
                        "url": nu,
                    }
                )
                t = (row.get("title") or "")[:40]
                print(f"[{len(results)}/{max_target}] {t}")
                if len(results) >= max_target:
                    break

            if len(results) >= max_target:
                break

            prev = len(seen_urls)
            await page.evaluate("window.scrollBy(0, Math.max(800, innerHeight * 1.2))")
            await asyncio.sleep(random.uniform(1.2, 2.4))
            if len(seen_urls) == prev:
                stall += 1
                if stall >= max_stall:
                    print("[!] 스크롤해도 신규 쇼츠가 없어 중단.")
                    break
            else:
                stall = 0

        print("\n" + "=" * 50)
        print(f"[*] 스캔 종료: 총 {len(results)}건 (고유 Shorts URL)")
        print("=" * 50)

        out_path = os.environ.get("HARNESS_OUTPUT_JSON", "engine0_shorts.json")
        try:
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(
                    {
                        "mode": "search",
                        "query": search_query,
                        "sp_used_in_url": sp_value,
                        "ui_filters_applied": ui_ok if apply_ui_filters else False,
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
    parser = argparse.ArgumentParser(description="YouTube Shorts 검색·필터 수집 (Engine-0)")
    parser.add_argument(
        "--query",
        default=os.environ.get("YOUTUBE_SEARCH_QUERY", DEFAULT_SEARCH_QUERY),
        help="검색어 (기본: best shorts)",
    )
    parser.add_argument(
        "--sp",
        default=os.environ.get("YOUTUBE_SEARCH_SP", DEFAULT_SEARCH_SP),
        help="URL에 붙일 sp (선택). 빈 값 권장 — UI 필터 실패 시에만 사용",
    )
    parser.add_argument(
        "--use-cookies",
        action="store_true",
        help="cookies.json 주입 명시",
    )
    parser.add_argument(
        "--no-use-cookies",
        action="store_true",
        help="쿠키 미사용 (개인화 최소화, 플래그 우선)",
    )
    parser.add_argument(
        "--no-ui-filters",
        action="store_true",
        default=False,
        help="UI 필터 클릭 생략, --sp URL만 사용",
    )
    parser.add_argument(
        "--max",
        type=int,
        default=int(os.environ.get("HARNESS_MAX_SHORTS", "100")),
        help="수집 목표 개수",
    )
    args = parser.parse_args()
    if args.no_use_cookies:
        use_cookies = False
    elif args.use_cookies:
        use_cookies = True
    else:
        use_cookies = _env_bool("HARNESS_USE_COOKIES", False)
    apply_ui = not args.no_ui_filters

    asyncio.run(
        scrape_search_shorts(
            search_query=args.query,
            search_sp=args.sp,
            max_target=args.max,
            use_cookies=use_cookies,
            apply_ui_filters=apply_ui,
        )
    )


if __name__ == "__main__":
    main()
