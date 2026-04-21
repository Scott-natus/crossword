from __future__ import annotations

from typing import Any

ALLOWED_ROLES = {"hook", "body", "puzzle_tease", "cta"}


def validate_scenario(
    scenario: dict[str, Any],
    target_min: float,
    target_max: float,
    banned_phrases: list[str] | None = None,
) -> list[str]:
    errors: list[str] = []
    banned_phrases = banned_phrases or []

    scenes = scenario.get("scenes")
    if not isinstance(scenes, list) or not scenes:
        errors.append("scenes는 비어 있지 않은 배열이어야 합니다.")
        return errors

    role_counts = {"puzzle_tease": 0, "cta": 0}
    last_end = 0.0
    total_duration = 0.0

    for idx, scene in enumerate(scenes):
        scene_id = scene.get("scene_id", f"idx:{idx}")
        role = scene.get("role")
        if role not in ALLOWED_ROLES:
            errors.append(f"{scene_id}: role={role!r} 는 허용되지 않습니다.")
        if role == "puzzle_tease":
            role_counts["puzzle_tease"] += 1
        if role == "cta":
            role_counts["cta"] += 1

        start_sec = _to_float(scene.get("start_sec"))
        end_sec = _to_float(scene.get("end_sec"))
        duration_sec = _to_float(scene.get("duration_sec"))

        if start_sec is None or end_sec is None or duration_sec is None:
            errors.append(f"{scene_id}: start_sec/end_sec/duration_sec 숫자값이 필요합니다.")
            continue
        if end_sec <= start_sec:
            errors.append(f"{scene_id}: end_sec는 start_sec보다 커야 합니다.")
        if abs((end_sec - start_sec) - duration_sec) > 0.2:
            errors.append(f"{scene_id}: duration_sec가 시간 범위와 일치하지 않습니다.")
        if idx > 0 and start_sec < last_end:
            errors.append(f"{scene_id}: 이전 scene과 시간이 겹칩니다.")
        last_end = max(last_end, end_sec)
        total_duration = max(total_duration, end_sec)

        if role == "puzzle_tease":
            pb = scene.get("puzzle_binding")
            if not isinstance(pb, dict):
                errors.append(f"{scene_id}: puzzle_tease는 puzzle_binding 객체가 필요합니다.")
            elif not pb.get("answer_masked", False):
                errors.append(f"{scene_id}: answer_masked=true 여야 합니다.")

        caption = scene.get("caption") or {}
        c_text = str(caption.get("text") or "")
        if any(bp and bp in c_text for bp in banned_phrases):
            errors.append(f"{scene_id}: caption에 금칙어가 포함되어 있습니다.")

    if role_counts["puzzle_tease"] != 1:
        errors.append("puzzle_tease scene은 정확히 1개여야 합니다.")
    if role_counts["cta"] < 1:
        errors.append("cta scene이 최소 1개 필요합니다.")
    elif scenes[-1].get("role") != "cta":
        errors.append("마지막 scene은 cta여야 합니다.")

    declared_total = _to_float(scenario.get("total_duration_sec"))
    if declared_total is not None and abs(declared_total - total_duration) > 0.2:
        errors.append("total_duration_sec가 scene 타임라인과 일치하지 않습니다.")

    effective_total = declared_total if declared_total is not None else total_duration
    if effective_total < target_min or effective_total > target_max:
        errors.append(
            f"총 길이 {effective_total:.2f}초가 target 범위({target_min}~{target_max})를 벗어났습니다."
        )

    return errors


def _to_float(v: Any) -> float | None:
    try:
        return float(v)
    except (TypeError, ValueError):
        return None
