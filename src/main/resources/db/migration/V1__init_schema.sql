-- 프로필 테이블
CREATE TABLE profiles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    summary TEXT,
    years_of_experience INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 경력 정보 테이블
CREATE TABLE experiences (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    company VARCHAR(200) NOT NULL,
    position VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    is_current BOOLEAN DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_experience_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
);

-- 프로젝트 테이블
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    experience_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    tech_stack TEXT,
    role VARCHAR(200),
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_experience FOREIGN KEY (experience_id) REFERENCES experiences(id) ON DELETE CASCADE
);

-- 기술 스킬 테이블
CREATE TABLE technical_skills (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    category VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    level VARCHAR(50),
    years_of_experience INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_skill_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
);

-- 면접 질문 세트 테이블
CREATE TABLE interview_question_sets (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    target_position VARCHAR(200),
    target_company VARCHAR(200),
    generation_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_question_set_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
);

-- 면접 질문 테이블
CREATE TABLE interview_questions (
    id BIGSERIAL PRIMARY KEY,
    question_set_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    category VARCHAR(100),
    difficulty VARCHAR(50),
    expected_answer_points TEXT,
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_question_set FOREIGN KEY (question_set_id) REFERENCES interview_question_sets(id) ON DELETE CASCADE
);

-- 학습 로드맵 테이블
CREATE TABLE learning_roadmaps (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    target_position VARCHAR(200),
    total_duration_weeks INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_roadmap_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
);

-- 학습 로드맵 아이템 테이블
CREATE TABLE learning_roadmap_items (
    id BIGSERIAL PRIMARY KEY,
    roadmap_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    duration_weeks INTEGER,
    resources TEXT,
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_roadmap_item FOREIGN KEY (roadmap_id) REFERENCES learning_roadmaps(id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_profiles_email ON profiles(email);
CREATE INDEX idx_experiences_profile_id ON experiences(profile_id);
CREATE INDEX idx_projects_experience_id ON projects(experience_id);
CREATE INDEX idx_skills_profile_id ON technical_skills(profile_id);
CREATE INDEX idx_question_sets_profile_id ON interview_question_sets(profile_id);
CREATE INDEX idx_questions_set_id ON interview_questions(question_set_id);
CREATE INDEX idx_roadmaps_profile_id ON learning_roadmaps(profile_id);
CREATE INDEX idx_roadmap_items_roadmap_id ON learning_roadmap_items(roadmap_id);