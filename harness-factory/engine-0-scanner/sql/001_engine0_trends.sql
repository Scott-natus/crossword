-- Engine-0 수집 트렌드 (문서 900 스키마)
-- 적용: psql "$DATABASE_URL" -f sql/001_engine0_trends.sql

CREATE TABLE IF NOT EXISTS engine0_trends (
    id SERIAL PRIMARY KEY,
    platform VARCHAR(20) NOT NULL,
    title TEXT,
    channel VARCHAR(100),
    view_count BIGINT,
    url TEXT NOT NULL,
    category VARCHAR(50),
    summary_features TEXT,
    hooking_score INT,
    collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT engine0_trends_url_key UNIQUE (url)
);

CREATE INDEX IF NOT EXISTS idx_engine0_trends_platform ON engine0_trends (platform);
CREATE INDEX IF NOT EXISTS idx_engine0_trends_collected_at ON engine0_trends (collected_at DESC);
