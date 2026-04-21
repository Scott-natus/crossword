from __future__ import annotations

from typing import Any


def evaluate_quality(
    scenario: dict[str, Any],
    trend: dict[str, Any],
    banned_phrases: list[str] | None = None,
) -> tuple[dict[str, Any], list[str]]:
    warnings: list[str] = []
    banned_phrases = banned_phrases or []

    scenes = scenario.get("scenes") or []
    hook_score = 5
    puzzle_score = 5
    copyright_risk = "low"

    if scenes:
        first_caption = str((scenes[0].get("caption") or {}).get("text") or "")
        if first_caption.strip():
            hook_score += 2
        if len(first_caption) > 14:
            warnings.append("hook caption 길이가 다소 길어 초반 전달력이 약할 수 있습니다.")
            hook_score -= 1

    has_puzzle = any(s.get("role") == "puzzle_tease" for s in scenes)
    if has_puzzle:
        puzzle_score += 2
    else:
        warnings.append("puzzle_tease scene 없음")
        puzzle_score -= 3

    all_text = _collect_text(scenario)
    if any(bp and bp in all_text for bp in banned_phrases):
        warnings.append("금칙어가 시나리오 본문에 포함됨")
        copyright_risk = "medium"

    if "실존 연예인" in all_text or "브랜드 로고" in all_text:
        warnings.append("민감 키워드가 포함되어 저작권 리스크가 높을 수 있음")
        copyright_risk = "high"

    trend_hook = trend.get("hooking_score")
    if isinstance(trend_hook, int):
        hook_score = int(round((hook_score + min(10, max(1, trend_hook // 10))) / 2))

    quality = {
        "hook_score": max(1, min(10, hook_score)),
        "puzzle_integration_naturalness": max(1, min(10, puzzle_score)),
        "copyright_risk": copyright_risk,
        "banned_phrase_hit": any(bp and bp in all_text for bp in banned_phrases),
    }
    return quality, warnings


def _collect_text(scenario: dict[str, Any]) -> str:
    texts: list[str] = []
    for scene in scenario.get("scenes") or []:
        for key in ("audio", "caption", "visual"):
            block = scene.get(key) or {}
            if isinstance(block, dict):
                texts.extend(str(v) for v in block.values() if isinstance(v, (str, int, float)))
        pb = scene.get("puzzle_binding") or {}
        if isinstance(pb, dict):
            texts.extend(str(v) for v in pb.values() if isinstance(v, (str, int, float)))
    return " ".join(texts)
