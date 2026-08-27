CREATE TABLE report_jobs (
    id UUID PRIMARY KEY,
    requested_by BIGINT NOT NULL,
    seller_id BIGINT,
    type VARCHAR(30) NOT NULL,
    format VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    object_key VARCHAR(500),
    failure_reason VARCHAR(500),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_report_jobs_requested_by ON report_jobs (requested_by, requested_at);
