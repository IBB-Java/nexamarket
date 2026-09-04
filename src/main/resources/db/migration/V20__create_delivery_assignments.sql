CREATE TABLE delivery_assignments (
    id UUID PRIMARY KEY,
    sub_order_id UUID NOT NULL REFERENCES sub_orders (id),
    courier_id BIGINT NOT NULL REFERENCES user_accounts (id),
    status VARCHAR(30) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    picked_up_at TIMESTAMP WITH TIME ZONE,
    delivery_started_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    rejection_reason VARCHAR(500),
    failure_reason_code VARCHAR(40),
    failure_description VARCHAR(1000),
    active_assignment BOOLEAN,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_delivery_assignment_status CHECK (status IN (
        'ASSIGNED', 'ACCEPTED', 'REJECTED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'DELIVERY_FAILED'
    )),
    CONSTRAINT chk_delivery_assignment_active CHECK (
        (active_assignment = TRUE AND status IN ('ASSIGNED', 'ACCEPTED', 'PICKED_UP', 'IN_TRANSIT'))
        OR (active_assignment IS NULL AND status IN ('REJECTED', 'DELIVERED', 'DELIVERY_FAILED'))
    )
);

-- NULL terminal markers allow unlimited history, while TRUE permits only one active assignment per SubOrder.
CREATE UNIQUE INDEX uq_delivery_assignment_active
    ON delivery_assignments (sub_order_id, active_assignment);
CREATE INDEX idx_delivery_assignment_courier_status
    ON delivery_assignments (courier_id, status);
CREATE INDEX idx_delivery_assignment_sub_order
    ON delivery_assignments (sub_order_id);
CREATE INDEX idx_delivery_assignment_assigned_at
    ON delivery_assignments (assigned_at);
CREATE INDEX idx_delivery_assignment_status
    ON delivery_assignments (status);

CREATE TABLE delivery_notification_outbox_events (
    id UUID PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    assignment_id UUID NOT NULL REFERENCES delivery_assignments (id),
    sub_order_id UUID NOT NULL REFERENCES sub_orders (id),
    delivery_status VARCHAR(30) NOT NULL,
    publish_attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_delivery_notification_outbox_due
    ON delivery_notification_outbox_events (published_at, next_attempt_at);

-- V15's denormalized automatic pointers must not leak into the new manual workflow.
UPDATE sub_orders SET courier_id = NULL;
