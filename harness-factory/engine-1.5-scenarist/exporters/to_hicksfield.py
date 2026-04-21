from __future__ import annotations

from typing import Any


def to_hicksfield_payload(scenario: dict[str, Any]) -> dict[str, Any]:
    return {
        "scenario_id": scenario.get("scenario_id"),
        "theme": scenario.get("theme"),
        "scene_count": len(scenario.get("scenes") or []),
        "scenes": [
            {
                "scene_id": s.get("scene_id"),
                "role": s.get("role"),
                "duration_sec": s.get("duration_sec"),
                "hicksfield_prompt": (s.get("visual") or {}).get("hicksfield_prompt"),
                "negative_prompt": (s.get("visual") or {}).get("negative_prompt"),
                "camera": (s.get("visual") or {}).get("camera"),
                "tts_text": (s.get("audio") or {}).get("tts_text"),
                "tts_voice_id": (s.get("audio") or {}).get("tts_voice_id"),
                "caption_text": (s.get("caption") or {}).get("text"),
                "hicksfield_params": s.get("hicksfield_params") or {},
            }
            for s in (scenario.get("scenes") or [])
        ],
    }
