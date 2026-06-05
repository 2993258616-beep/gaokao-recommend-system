CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS major_name_stat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    major_name VARCHAR(100) NOT NULL UNIQUE,
    count_num INT DEFAULT 0,
    subject_types VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS prediction_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    predict_year INT NOT NULL,
    province VARCHAR(20) NOT NULL,
    school_code VARCHAR(20),
    school_name VARCHAR(120) NOT NULL,
    subject_type VARCHAR(20) NOT NULL,
    major_group VARCHAR(50),
    major_group_full VARCHAR(120),
    school_type VARCHAR(80),
    school_level VARCHAR(50),
    school_nature VARCHAR(50),
    plan_count INT,
    filing_score INT,
    filing_rank INT,
    range_float INT,
    predict_score INT NOT NULL,
    predict_low INT NOT NULL,
    predict_high INT NOT NULL,
    predict_range VARCHAR(30) NOT NULL,
    major_direction VARCHAR(255),
    major_category VARCHAR(100),
    school_province VARCHAR(30) NOT NULL,
    confidence VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_prediction_filter ON prediction_line (province, subject_type, school_province, predict_score, confidence);
CREATE INDEX IF NOT EXISTS idx_prediction_major ON prediction_line (major_category);
CREATE INDEX IF NOT EXISTS idx_prediction_school ON prediction_line (school_name);

ALTER TABLE prediction_line ADD COLUMN IF NOT EXISTS filing_rank INT;
ALTER TABLE prediction_line ALTER COLUMN major_direction VARCHAR(2000);
ALTER TABLE prediction_line ALTER COLUMN major_category VARCHAR(200);

CREATE TABLE IF NOT EXISTS admission_major_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_subject VARCHAR(20) NOT NULL,
    source_row_no INT,
    school_code VARCHAR(20),
    school_name VARCHAR(120) NOT NULL,
    subject_type VARCHAR(20) NOT NULL,
    batch_name VARCHAR(30) NOT NULL,
    major_group VARCHAR(50),
    elective_subject VARCHAR(80),
    group_min_score INT,
    min_rank INT,
    majors CLOB,
    major_count INT,
    pdf_page VARCHAR(80),
    book_page VARCHAR(80),
    review_note VARCHAR(255),
    raw_group_line CLOB,
    source_file VARCHAR(255),
    import_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admission_group_lookup ON admission_major_group (source_subject, school_code, school_name, batch_name, major_group);
ALTER TABLE admission_major_group ADD COLUMN IF NOT EXISTS source_row_no INT;

CREATE TABLE IF NOT EXISTS access_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    path VARCHAR(120) NOT NULL,
    remote_ip VARCHAR(60),
    user_agent VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_access_log_time ON access_log (create_time);
CREATE INDEX IF NOT EXISTS idx_access_log_path ON access_log (path);
