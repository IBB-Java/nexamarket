CREATE TABLE wallet_accounts (
    customer_id BIGINT PRIMARY KEY,
    balance NUMERIC(19, 2) NOT NULL CHECK (balance >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (id),
    customer_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    wallet_amount NUMERIC(19, 2) NOT NULL CHECK (wallet_amount >= 0),
    card_amount NUMERIC(19, 2) NOT NULL CHECK (card_amount >= 0),
    provider_payment_id UUID UNIQUE,
    polling_attempts INTEGER NOT NULL DEFAULT 0,
    next_poll_at TIMESTAMP WITH TIME ZONE,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_payment_transactions_order_created ON payment_transactions (order_id, created_at DESC);
CREATE INDEX idx_payment_transactions_poll ON payment_transactions (status, next_poll_at);

CREATE TABLE processed_payment_webhooks (
    id UUID PRIMARY KEY,
    provider_event_id VARCHAR(100) NOT NULL UNIQUE,
    provider_payment_id UUID NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE mock_provider_payments (
    id UUID PRIMARY KEY,
    merchant_payment_id UUID NOT NULL UNIQUE,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE mock_provider_callbacks (
    id UUID PRIMARY KEY,
    provider_payment_id UUID NOT NULL REFERENCES mock_provider_payments (id),
    provider_event_id VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    delivery_count INTEGER NOT NULL CHECK (delivery_count > 0),
    deliver_at TIMESTAMP WITH TIME ZONE NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_mock_provider_callbacks_delivery ON mock_provider_callbacks (delivered_at, deliver_at);
