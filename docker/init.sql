-- Initial database setup
CREATE SCHEMA IF NOT EXISTS careercoach;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA careercoach TO careercoach;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA careercoach TO careercoach;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA careercoach TO careercoach;

-- Set default schema
ALTER USER careercoach SET search_path TO careercoach, public;