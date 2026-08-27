CREATE TABLE notification_outbox_events (
    id UUID PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    sub_order_id UUID NOT NULL REFERENCES sub_orders (id),
    seller_id BIGINT NOT NULL,
    order_status VARCHAR(30) NOT NULL,
    publish_attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notification_outbox_due ON notification_outbox_events (published_at, next_attempt_at);

CREATE TABLE notification_messages (
    id UUID PRIMARY KEY,
    deduplication_key VARCHAR(150) NOT NULL UNIQUE,
    recipient_id BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notification_messages_due ON notification_messages (status, next_attempt_at);
