-- Projects 테이블 생성
CREATE TABLE IF NOT EXISTS projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE,
    technologies TEXT,
    role TEXT,
    achievements TEXT,
    url VARCHAR(500),
    profile_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
);

-- Projects 인덱스 생성
CREATE INDEX idx_projects_profile_id ON projects(profile_id);
CREATE INDEX idx_projects_start_date ON projects(start_date);

-- Profiles 테이블에 current_position 컬럼 추가
ALTER TABLE profiles
ADD COLUMN IF NOT EXISTS current_position VARCHAR(100);