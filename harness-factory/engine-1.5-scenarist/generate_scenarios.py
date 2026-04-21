from __future__ import annotations

import argparse
import json
import os
import re
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parent.parent
_SCANNER = _ROOT / "engine-0-scanner"
if str(_SCANNER) not in sys.path:
    sys.path.insert(0, str(_SCANNER))

from db_env import DEFAULT_DOTENV_PATH, load_harness_env  # noqa: E402
from ingest_to_postgres import _connect  # noqa: E402
from exporters.to_hicksfield import to_hicksfield_payload  # noqa: E402
from exporters.to_markdown import render_markdown  # noqa: E402
from validators.semantic import evaluate_quality  # noqa: E402
from validators.structural import validate_scenario  # noqa: E402

DEFAULT_MODEL = "gemini-flash-latest"
DEFAULT_CTA_VARIANT = "play_puzzle"


@dataclass
class TrendContext:
    trend_id: int
    platform: str
    title: str
    channel: str
    url: str
    view_count: int | None
    category: str
    summary_features: str
    hooking_score: int | None
    theme: str
    mapping_confidence: float | None
    duration_seconds: float


def _parse_json(text: str) -> dict[str, Any]:
    text = (text or "").strip()
    if not text:
        raise ValueError("Gemini 응답이 비어 있습니다.")
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?\s*", "", text)
        text = re.sub(r"\s*```$", "", text)
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        m = re.search(r"\{[\s\S]*\}", text)
        if not m:
            raise
        return json.loads(m.group(0))


def _safe_int(v: Any, default: int | None = None) -> int | None:
    try:
        return int(v)
    except (TypeError, ValueError):
        return default


def _safe_float(v: Any, default: float) -> float:
    try:
        return float(v)
    except (TypeError, ValueError):
        return default


def _scenario_id(trend_id: int) -> str:
    return f"scn_{datetime.now().strftime('%Y%m%d_%H%M%S')}_{trend_id}"


def _mask_word(word: str) -> str:
    w = (word or "").strip()
    return "●" * len(w) if w else "●●●"


def _theme_url(theme: str) -> str:
    return f"https://k-crossword.com/K-CrossWord/{theme}"


def _load_system_prompt(path: Path) -> str:
    with open(path, encoding="utf-8") as f:
        return f.read().strip()


def _select_trends(
    conn,
    trend_id: int | None,
    since: str | None,
    limit: int,
    regenerate: bool,
    scenario_id: str | None,
) -> list[TrendContext]:
    cur = conn.cursor()

    if regenerate and scenario_id:
        cur.execute(
            """
            SELECT t.id, t.platform, COALESCE(t.title, ''), COALESCE(t.channel, ''), COALESCE(t.url, ''),
                   t.view_count, COALESCE(t.category, ''), COALESCE(t.summary_features, ''), t.hooking_score,
                   m.theme_code, m.confidence, 22.0
            FROM engine15_scenarios e15
            JOIN engine0_trends t ON t.id = e15.source_trend_id
            JOIN engine1_trend_theme_map m ON m.engine0_trend_id = t.id
            WHERE e15.scenario_id = %s
            LIMIT 1
            """,
            (scenario_id,),
        )
        rows = cur.fetchall()
    else:
        q = [
            """
            SELECT t.id, t.platform, COALESCE(t.title, ''), COALESCE(t.channel, ''), COALESCE(t.url, ''),
                   t.view_count, COALESCE(t.category, ''), COALESCE(t.summary_features, ''), t.hooking_score,
                   m.theme_code, m.confidence, 22.0
            FROM engine0_trends t
            JOIN engine1_trend_theme_map m ON m.engine0_trend_id = t.id
            """
        ]
        params: list[Any] = []
        where: list[str] = []
        if not regenerate:
            where.append(
                "NOT EXISTS (SELECT 1 FROM engine15_scenarios s WHERE s.source_trend_id = t.id AND s.status <> 'failed')"
            )
        if trend_id is not None:
            where.append("t.id = %s")
            params.append(trend_id)
        if since:
            where.append("t.collected_at >= %s::timestamp")
            params.append(since)
        if where:
            q.append("WHERE " + " AND ".join(where))
        q.append("ORDER BY t.id DESC LIMIT %s")
        params.append(limit)
        cur.execute("\n".join(q), tuple(params))
        rows = cur.fetchall()

    cur.close()
    return [
        TrendContext(
            trend_id=r[0],
            platform=r[1] or "youtube",
            title=r[2] or "",
            channel=r[3] or "",
            url=r[4] or "",
            view_count=_safe_int(r[5]),
            category=r[6] or "",
            summary_features=r[7] or "",
            hooking_score=_safe_int(r[8]),
            theme=r[9] or "Korean",
            mapping_confidence=_safe_float(r[10], 0.5),
            duration_seconds=_safe_float(r[11], 22.0),
        )
        for r in rows
    ]


def _puzzle_samples(conn, theme: str, sample_count: int = 5) -> tuple[list[dict[str, Any]], str]:
    cur = conn.cursor()
    cat2_map = {
        "K-POP": ["가수", "앨범", "곡명"],
        "K-DRAMA": ["드라마", "배우", "OST"],
        "K-MOVIE": ["영화", "감독", "배우"],
        "K-CULTURE": ["문화", "음식", "전통"],
        "Korean": ["한국어", "상식"],
    }
    cat2_candidates = cat2_map.get(theme, [theme])
    try:
        cur.execute(
            """
            SELECT w.id, w.word, w.length, w.difficulty, COALESCE(w.cat2, ''), COALESCE(h.hint_text, '')
            FROM pz_words w
            LEFT JOIN LATERAL (
                SELECT hint_text
                FROM pz_hints
                WHERE word_id = w.id
                ORDER BY is_primary DESC NULLS LAST, id ASC
                LIMIT 1
            ) h ON true
            WHERE w.is_active = true
              AND COALESCE(w.conf_yn, 'N') = 'Y'
              AND COALESCE(w.is_approved, false) = true
              AND (w.cat2 = ANY(%s) OR w.category ILIKE %s)
            ORDER BY random()
            LIMIT %s
            """,
            (cat2_candidates, f"%{theme}%", sample_count),
        )
        rows = cur.fetchall()
    except Exception:
        conn.rollback()
        rows = []
    finally:
        cur.close()

    samples = [
        {
            "word_id": r[0],
            "word": r[1],
            "length": _safe_int(r[2], 0),
            "difficulty": _safe_int(r[3], 2),
            "cat2": r[4] or "",
            "primary_hint": r[5] or "테마 연관 힌트",
        }
        for r in rows
    ]
    return samples, _theme_url(theme)


def _build_prompt(
    system_prompt: str,
    trend: TrendContext,
    sample_words: list[dict[str, Any]],
    puzzle_url: str,
    target_min: int,
    target_max: int,
    cta_variant: str,
    voice_persona: str,
    banned_phrases: list[str],
) -> str:
    trend_dna = {
        "trend_id": trend.trend_id,
        "platform": trend.platform,
        "title": trend.title,
        "view_count": trend.view_count,
        "hooking_score": trend.hooking_score,
        "summary_features": trend.summary_features,
        "duration_seconds": trend.duration_seconds,
    }
    theme_binding = {
        "theme": trend.theme,
        "mapping_confidence": trend.mapping_confidence,
    }
    puzzle_sample = {
        "sample_words": [
            {
                "word": _mask_word(s["word"]),
                "length": s["length"],
                "difficulty": s["difficulty"],
                "cat2": s["cat2"],
                "primary_hint": s["primary_hint"],
            }
            for s in sample_words
        ],
        "daily_puzzle_url": puzzle_url,
    }
    policy = {
        "target_duration_range": [target_min, target_max],
        "cta_variant": cta_variant,
        "voice_persona": voice_persona,
        "banned_phrases": banned_phrases,
    }

    return (
        system_prompt.format(target_min=target_min, target_max=target_max)
        + "\n\n[trend_dna]\n"
        + json.dumps(trend_dna, ensure_ascii=False)
        + "\n\n[theme_binding]\n"
        + json.dumps(theme_binding, ensure_ascii=False)
        + "\n\n[puzzle_sample]\n"
        + json.dumps(puzzle_sample, ensure_ascii=False)
        + "\n\n[policy]\n"
        + json.dumps(policy, ensure_ascii=False)
    )


def _call_gemini(client, model_name: str, prompt: str) -> dict[str, Any]:
    from google.genai import types

    try:
        resp = client.models.generate_content(
            model=model_name,
            contents=prompt,
            config=types.GenerateContentConfig(
                temperature=0.55,
                response_mime_type="application/json",
            ),
        )
    except Exception:
        resp = client.models.generate_content(
            model=model_name,
            contents=prompt,
            config=types.GenerateContentConfig(temperature=0.55),
        )
    return _parse_json(resp.text or "")


def _fallback_scenario(trend: TrendContext, sample_words: list[dict[str, Any]], cta_variant: str) -> dict[str, Any]:
    sample = sample_words[0] if sample_words else {"word": "정답", "primary_hint": "힌트", "word_id": 0}
    masked = _mask_word(sample.get("word", "정답"))
    theme_url = _theme_url(trend.theme)
    return {
        "scenario_id": _scenario_id(trend.trend_id),
        "source_trend_id": trend.trend_id,
        "theme": trend.theme,
        "total_duration_sec": 20.0,
        "scene_count": 4,
        "narrative_arc": "호기심 유발 → 퍼즐 힌트 제시 → 퍼즐로 유도",
        "target_audience": "K-CrossWord 사용자",
        "cta_variant": cta_variant,
        "scenes": [
            {
                "scene_id": "s01",
                "role": "hook",
                "start_sec": 0.0,
                "end_sec": 1.5,
                "duration_sec": 1.5,
                "visual": {"hicksfield_prompt": "9:16, energetic close-up", "camera": "close_up_zoom_in", "negative_prompt": "brand logo"},
                "audio": {"tts_text": "이거 아는 사람?", "tts_emotion": "excited", "tts_voice_id": "ko-KR-female-20s"},
                "caption": {"text": "이거 아는 사람?", "position": "center_top", "font_scale": "large"},
                "puzzle_binding": None,
                "hicksfield_params": {"aspect_ratio": "9:16", "fps": 30, "seed": 42},
            },
            {
                "scene_id": "s02",
                "role": "body",
                "start_sec": 1.5,
                "end_sec": 12.0,
                "duration_sec": 10.5,
                "visual": {"hicksfield_prompt": "short-form trendy montage"},
                "audio": {"tts_text": "요즘 이 테마가 진짜 뜨고 있어요.", "tts_voice_id": "ko-KR-female-20s"},
                "caption": {"text": "요즘 제일 핫한 테마"},
                "puzzle_binding": None,
                "hicksfield_params": {"aspect_ratio": "9:16", "fps": 30, "seed": 42},
            },
            {
                "scene_id": "s03",
                "role": "puzzle_tease",
                "start_sec": 12.0,
                "end_sec": 17.0,
                "duration_sec": 5.0,
                "visual": {"hicksfield_prompt": "crossword tease scene"},
                "audio": {"tts_text": "힌트만 보고 맞혀보세요.", "tts_voice_id": "ko-KR-female-20s"},
                "caption": {"text": f"{masked} 누구게?"},
                "puzzle_binding": {
                    "theme": trend.theme,
                    "displayed_word": masked,
                    "displayed_hint": sample.get("primary_hint", "테마 관련 힌트"),
                    "grid_preview_pattern": [[2, 1, 1], [1, 1, 2], [1, 2, 1]],
                    "answer_masked": True,
                    "answer_word_id": sample.get("word_id", 0),
                    "link_cta_text": "정답 맞히러 가기 →",
                },
                "hicksfield_params": {"aspect_ratio": "9:16", "fps": 30, "seed": 42},
            },
            {
                "scene_id": "s04",
                "role": "cta",
                "start_sec": 17.0,
                "end_sec": 20.0,
                "duration_sec": 3.0,
                "visual": {"hicksfield_prompt": "clean CTA screen"},
                "audio": {"tts_text": "정답은 퍼즐에서 확인해요.", "tts_voice_id": "ko-KR-female-20s"},
                "caption": {"text": "정답 맞히러 가기 →"},
                "puzzle_binding": None,
                "hicksfield_params": {"aspect_ratio": "9:16", "fps": 30, "seed": 42},
            },
        ],
        "quality_flags": {},
        "generation_meta": {},
        "target_puzzle_url": theme_url,
    }


def _normalize_scenario(
    scenario: dict[str, Any],
    trend: TrendContext,
    model_name: str,
    prompt_version: str,
    cta_variant: str,
) -> dict[str, Any]:
    out = dict(scenario)
    out["scenario_id"] = str(out.get("scenario_id") or _scenario_id(trend.trend_id))
    out["source_trend_id"] = trend.trend_id
    out["theme"] = trend.theme
    out["cta_variant"] = str(out.get("cta_variant") or cta_variant)

    scenes = out.get("scenes") if isinstance(out.get("scenes"), list) else []
    out["scenes"] = scenes
    out["scene_count"] = len(scenes)

    if scenes:
        total = max(_safe_float(s.get("end_sec"), 0.0) for s in scenes)
    else:
        total = _safe_float(out.get("total_duration_sec"), 0.0)
    out["total_duration_sec"] = round(total, 2)

    gen_meta = out.get("generation_meta")
    if not isinstance(gen_meta, dict):
        gen_meta = {}
    gen_meta["gemini_model"] = model_name
    gen_meta["prompt_version"] = prompt_version
    gen_meta["generated_at"] = datetime.now().isoformat()
    out["generation_meta"] = gen_meta
    return out


def _save_outputs(
    base_dir: Path,
    scenario: dict[str, Any],
    markdown_text: str,
    hicks_payload: dict[str, Any],
) -> tuple[Path, Path, Path]:
    scenario_id = scenario["scenario_id"]
    json_path = base_dir / "scenarios" / "json" / f"{scenario_id}.json"
    md_path = base_dir / "scenarios" / "md" / f"{scenario_id}.md"
    hicks_path = base_dir / "scenarios" / "json" / f"{scenario_id}.hicksfield.json"
    json_path.parent.mkdir(parents=True, exist_ok=True)
    md_path.parent.mkdir(parents=True, exist_ok=True)
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(scenario, f, ensure_ascii=False, indent=2)
    with open(md_path, "w", encoding="utf-8") as f:
        f.write(markdown_text)
    with open(hicks_path, "w", encoding="utf-8") as f:
        json.dump(hicks_payload, f, ensure_ascii=False, indent=2)
    return json_path, md_path, hicks_path


def _upsert_scenario(conn, scenario: dict[str, Any], md_path: Path, status: str) -> None:
    qf = scenario.get("quality_flags") or {}
    gm = scenario.get("generation_meta") or {}
    cur = conn.cursor()
    cur.execute(
        """
        INSERT INTO engine15_scenarios (
            scenario_id, source_trend_id, theme, total_duration_sec, scene_count,
            scenario_json, markdown_path, hook_score, puzzle_integration_score, copyright_risk,
            status, gemini_model, prompt_version, token_input, token_output
        ) VALUES (%s, %s, %s, %s, %s, %s::jsonb, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT (scenario_id) DO UPDATE SET
            source_trend_id = EXCLUDED.source_trend_id,
            theme = EXCLUDED.theme,
            total_duration_sec = EXCLUDED.total_duration_sec,
            scene_count = EXCLUDED.scene_count,
            scenario_json = EXCLUDED.scenario_json,
            markdown_path = EXCLUDED.markdown_path,
            hook_score = EXCLUDED.hook_score,
            puzzle_integration_score = EXCLUDED.puzzle_integration_score,
            copyright_risk = EXCLUDED.copyright_risk,
            status = EXCLUDED.status,
            gemini_model = EXCLUDED.gemini_model,
            prompt_version = EXCLUDED.prompt_version,
            token_input = EXCLUDED.token_input,
            token_output = EXCLUDED.token_output,
            updated_at = CURRENT_TIMESTAMP
        """,
        (
            scenario["scenario_id"],
            scenario["source_trend_id"],
            scenario["theme"],
            scenario.get("total_duration_sec"),
            scenario.get("scene_count"),
            json.dumps(scenario, ensure_ascii=False),
            str(md_path),
            qf.get("hook_score"),
            qf.get("puzzle_integration_naturalness"),
            qf.get("copyright_risk"),
            status,
            gm.get("gemini_model"),
            gm.get("prompt_version"),
            (gm.get("token_usage") or {}).get("input"),
            (gm.get("token_usage") or {}).get("output"),
        ),
    )
    cur.close()


def main() -> None:
    parser = argparse.ArgumentParser(description="Engine-1.5: trend -> scenario 생성")
    parser.add_argument("--trend-id", type=int, default=None, help="단일 trend_id 지정")
    parser.add_argument("--limit", type=int, default=50, help="생성 최대 건수")
    parser.add_argument("--since", default=None, help="수집 시각 필터 (YYYY-MM-DD)")
    parser.add_argument("--regenerate", action="store_true", help="기존 시나리오도 재생성")
    parser.add_argument("--scenario-id", default=None, help="재생성 대상 scenario_id")
    parser.add_argument("--dry-run", action="store_true", help="DB 미저장")
    parser.add_argument("--mock", action="store_true", help="Gemini 호출 대신 템플릿 시나리오 생성")
    parser.add_argument("--model", default=None, help=f"Gemini 모델 (기본 {DEFAULT_MODEL})")
    parser.add_argument("--prompt-version", default="v1.0", help="프롬프트 버전")
    parser.add_argument("--target-min", type=int, default=15, help="최소 길이(초)")
    parser.add_argument("--target-max", type=int, default=45, help="최대 길이(초)")
    parser.add_argument("--cta-variant", default=DEFAULT_CTA_VARIANT, help="CTA 종류")
    parser.add_argument("--voice-persona", default="친근한 20대 여성", help="TTS 페르소나")
    parser.add_argument("--banned-phrases", default="", help="콤마 구분 금칙어")
    parser.add_argument("--retry", type=int, default=1, help="검증 실패 시 재시도 횟수")
    args = parser.parse_args()

    load_harness_env()
    model_name = args.model or os.environ.get("GEMINI_MODEL", DEFAULT_MODEL)
    banned_phrases = [x.strip() for x in args.banned_phrases.split(",") if x.strip()]
    prompt_path = Path(__file__).resolve().parent / "prompts" / "system_prompt.txt"
    system_prompt = _load_system_prompt(prompt_path)

    if not args.mock:
        api_key = os.environ.get("GEMINI_API_KEY")
        if not api_key:
            print(
                f"[!] GEMINI_API_KEY 필요 (.env: {DEFAULT_DOTENV_PATH}). mock 테스트는 --mock 사용.",
                file=sys.stderr,
            )
            raise SystemExit(1)
        from google import genai

        client = genai.Client(api_key=api_key)
    else:
        client = None

    conn = None
    try:
        conn = _connect()
        conn.autocommit = False
    except Exception as e:
        if args.dry_run and args.mock:
            print(f"[!] DB 연결 생략(dry-run/mock): {e}")
        else:
            raise
    base_dir = _ROOT

    if conn is not None:
        trends = _select_trends(
            conn=conn,
            trend_id=args.trend_id,
            since=args.since,
            limit=args.limit,
            regenerate=args.regenerate,
            scenario_id=args.scenario_id,
        )
    else:
        trends = [
            TrendContext(
                trend_id=args.trend_id or 999999,
                platform="youtube",
                title="샘플 트렌드",
                channel="sample_channel",
                url="https://www.youtube.com/shorts/sample",
                view_count=100000,
                category="sample",
                summary_features="샘플 요약",
                hooking_score=8,
                theme="K-POP",
                mapping_confidence=0.8,
                duration_seconds=20.0,
            )
        ]
    if not trends:
        print("[*] 생성 대상 트렌드가 없습니다.")
        if conn is not None:
            conn.close()
        return

    success = 0
    failed = 0
    for trend in trends:
        if conn is not None:
            sample_words, puzzle_url = _puzzle_samples(conn, trend.theme)
        else:
            sample_words, puzzle_url = (
                [
                    {
                        "word_id": 0,
                        "word": "샘플",
                        "length": 2,
                        "difficulty": 2,
                        "cat2": "가수",
                        "primary_hint": "샘플 힌트",
                    }
                ],
                _theme_url(trend.theme),
            )
        trend_dict = {
            "id": trend.trend_id,
            "platform": trend.platform,
            "view_count": trend.view_count,
            "hooking_score": trend.hooking_score,
        }

        last_errors: list[str] = []
        final_scenario: dict[str, Any] | None = None
        for attempt in range(args.retry + 1):
            try:
                if args.mock:
                    raw = _fallback_scenario(trend, sample_words, args.cta_variant)
                else:
                    prompt = _build_prompt(
                        system_prompt=system_prompt,
                        trend=trend,
                        sample_words=sample_words,
                        puzzle_url=puzzle_url,
                        target_min=args.target_min,
                        target_max=args.target_max,
                        cta_variant=args.cta_variant,
                        voice_persona=args.voice_persona,
                        banned_phrases=banned_phrases,
                    )
                    if last_errors:
                        prompt += "\n\n[validation_feedback]\n" + "\n".join(last_errors)
                    raw = _call_gemini(client, model_name, prompt)

                scenario = _normalize_scenario(
                    scenario=raw,
                    trend=trend,
                    model_name=model_name,
                    prompt_version=args.prompt_version,
                    cta_variant=args.cta_variant,
                )

                quality, warns = evaluate_quality(scenario, trend_dict, banned_phrases=banned_phrases)
                scenario["quality_flags"] = quality
                if warns:
                    scenario["generation_meta"]["warnings"] = warns

                errors = validate_scenario(
                    scenario,
                    target_min=float(args.target_min),
                    target_max=float(args.target_max),
                    banned_phrases=banned_phrases,
                )
                if errors:
                    last_errors = errors
                    if attempt < args.retry:
                        continue
                    raise RuntimeError("; ".join(errors))

                markdown_text = render_markdown(scenario, trend_dict, puzzle_url)
                hicks_payload = to_hicksfield_payload(scenario)
                _, md_path, _ = _save_outputs(base_dir, scenario, markdown_text, hicks_payload)

                if not args.dry_run and conn is not None:
                    _upsert_scenario(conn, scenario, md_path, status="draft")
                    conn.commit()
                final_scenario = scenario
                print(
                    f"[+] trend_id={trend.trend_id} scenario={scenario['scenario_id']} scenes={scenario['scene_count']}"
                )
                success += 1
                break
            except Exception as e:
                if attempt < args.retry:
                    continue
                failed += 1
                err = str(e)
                print(f"[!] trend_id={trend.trend_id} 생성 실패: {err}", file=sys.stderr)
                fail_scenario = {
                    "scenario_id": _scenario_id(trend.trend_id),
                    "source_trend_id": trend.trend_id,
                    "theme": trend.theme,
                    "total_duration_sec": 0,
                    "scene_count": 0,
                    "scenes": [],
                    "quality_flags": {},
                    "generation_meta": {
                        "gemini_model": model_name,
                        "prompt_version": args.prompt_version,
                        "generated_at": datetime.now().isoformat(),
                        "error": err,
                    },
                }
                if not args.dry_run and conn is not None:
                    try:
                        _, md_path, _ = _save_outputs(
                            base_dir,
                            fail_scenario,
                            f"# 시나리오 실패\n\n- trend_id: {trend.trend_id}\n- error: {err}\n",
                            {"scenario_id": fail_scenario["scenario_id"], "scenes": []},
                        )
                        _upsert_scenario(conn, fail_scenario, md_path, status="failed")
                        conn.commit()
                    except Exception:
                        conn.rollback()
                final_scenario = None
        if final_scenario is None and args.dry_run and conn is not None:
            conn.rollback()

    if conn is not None:
        conn.close()
    print(f"[*] 완료: 성공 {success}건, 실패 {failed}건")


if __name__ == "__main__":
    main()
