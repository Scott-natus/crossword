-- Engine-1.5 v1: 실행 가능한 scene 시나리오 저장 테이블
-- 적용: psql "$DATABASE_URL" -f sql/001_engine15_scenarios.sql

CREATE TABLE IF NOT EXISTS engine15_scenarios (
    id SERIAL PRIMARY KEY,
    scenario_id VARCHAR(50) UNIQUE NOT NULL,
    source_trend_id INTEGER NOT NULL REFERENCES engine0_trends (id) ON DELETE CASCADE,
    theme VARCHAR(20) NOT NULL,
    total_duration_sec NUMERIC(5, 2),
    scene_count INTEGER,
    scenario_json JSONB NOT NULL,
    markdown_path TEXT,
    hook_score INTEGER,
    puzzle_integration_score INTEGER,
    copyright_risk VARCHAR(10),
    status VARCHAR(20) DEFAULT 'draft', -- draft | approved | rendered | published | failed
    gemini_model VARCHAR(50),
    prompt_version VARCHAR(10),
    token_input INTEGER,
    token_output INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_e15_trend ON engine15_scenarios (source_trend_id);
CREATE INDEX IF NOT EXISTS idx_e15_status ON engine15_scenarios (status);
CREATE INDEX IF NOT EXISTS idx_e15_theme ON engine15_scenarios (theme);

COMMENT ON TABLE engine15_scenarios IS 'Engine-1.5가 생성한 scene 기반 시나리오 원본(JSON) 저장';
