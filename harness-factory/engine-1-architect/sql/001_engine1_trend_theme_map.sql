-- Engine-1 v1: 트렌드(Engine-0) → 일일 테마 코드(문서 100 §8.1) 매핑
-- 적용: psql "$DATABASE_URL" -f sql/001_engine1_trend_theme_map.sql

CREATE TABLE IF NOT EXISTS engine1_trend_theme_map (
    id SERIAL PRIMARY KEY,
    engine0_trend_id INTEGER NOT NULL REFERENCES engine0_trends (id) ON DELETE CASCADE,
    theme_code VARCHAR(32) NOT NULL,
    confidence REAL NOT NULL DEFAULT 0.5,
    match_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT engine1_trend_theme_map_trend_unique UNIQUE (engine0_trend_id)
);

CREATE INDEX IF NOT EXISTS idx_engine1_theme_code ON engine1_trend_theme_map (theme_code);
CREATE INDEX IF NOT EXISTS idx_engine1_created ON engine1_trend_theme_map (created_at DESC);

COMMENT ON TABLE engine1_trend_theme_map IS 'Engine-0 수집 트렌드와 K-CrossWord 테마(K-POP 등) 정합 결과';
