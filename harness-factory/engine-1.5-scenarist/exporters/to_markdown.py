from __future__ import annotations

from typing import Any


def render_markdown(scenario: dict[str, Any], trend: dict[str, Any], puzzle_url: str) -> str:
    scenario_id = scenario.get("scenario_id", "unknown")
    theme = scenario.get("theme", "Korean")
    total = scenario.get("total_duration_sec", 0)
    scene_count = len(scenario.get("scenes") or [])
    trend_id = trend.get("id")
    platform = trend.get("platform", "-")
    view_count = trend.get("view_count", "-")
    hook = trend.get("hooking_score", "-")

    lines: list[str] = []
    lines.append(f"# 시나리오 {scenario_id}")
    lines.append("")
    lines.append(f"- **테마**: {theme} · **길이**: {total}초 · **Scene**: {scene_count}개")
    lines.append(
        f"- **출처 트렌드**: #{trend_id} ({platform}, 조회 {view_count}, 후킹 {hook}/10)"
    )
    lines.append(f"- **유도 목표**: {theme} 일일 퍼즐 ({puzzle_url})")
    lines.append("")
    lines.append("## 내러티브 아크")
    lines.append(f"> {scenario.get('narrative_arc', '호기심 유발 → 퍼즐로 유도')}")
    lines.append("")
    lines.append("---")
    lines.append("")

    for scene in scenario.get("scenes") or []:
        scene_id = str(scene.get("scene_id", "s??")).upper()
        role = str(scene.get("role", "body")).upper()
        start_sec = scene.get("start_sec", 0.0)
        end_sec = scene.get("end_sec", 0.0)
        caption = scene.get("caption") or {}
        audio = scene.get("audio") or {}
        visual = scene.get("visual") or {}
        lines.append(f"### {scene_id} · {role} · {start_sec}–{end_sec}초")
        lines.append(f'**자막**: "{caption.get("text", "")}"')
        lines.append(f'**TTS**: "{audio.get("tts_text", "")}"')
        lines.append(f'**비주얼**: {visual.get("hicksfield_prompt", "")}')
        if role == "PUZZLE_TEASE":
            pb = scene.get("puzzle_binding") or {}
            lines.append(f'**힌트**: "{pb.get("displayed_hint", "")}"')
            lines.append(f'**마스킹 단어**: "{pb.get("displayed_word", "")}"')
        lines.append("")

    q = scenario.get("quality_flags") or {}
    lines.append("---")
    lines.append("")
    lines.append("## 품질 체크")
    lines.append(f"- [x] 후킹 점수 {q.get('hook_score', '-')}/10")
    lines.append(f"- [x] 퍼즐 연결 자연스러움 {q.get('puzzle_integration_naturalness', '-')}/10")
    lines.append(f"- [x] 저작권 리스크: {q.get('copyright_risk', '-')}")
    lines.append(f"- [x] 금칙어 미포함: {not bool(q.get('banned_phrase_hit', False))}")
    lines.append("")
    return "\n".join(lines)
