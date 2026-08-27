CREATE TABLE return_requests (
    id UUID PRIMARY KEY,
    sub_order_id UUID NOT NULL UNIQUE REFERENCES sub_orders (id),
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    resolved_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_return_requests_status ON return_requests (status);
