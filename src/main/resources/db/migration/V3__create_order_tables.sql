CREATE TABLE orders (
    id UUID PRIMARY KEY,
    source_cart_id UUID NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL CHECK (total_amount >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_orders_customer_created_at ON orders (customer_id, created_at);

CREATE TABLE sub_orders (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (id),
    seller_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    subtotal NUMERIC(19, 2) NOT NULL CHECK (subtotal >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_sub_orders_order_id ON sub_orders (order_id);
CREATE INDEX idx_sub_orders_seller_id ON sub_orders (seller_id);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    sub_order_id UUID NOT NULL REFERENCES sub_orders (id),
    product_variant_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19, 2) NOT NULL CHECK (unit_price >= 0),
    stock_reservation_id UUID NOT NULL,
    reserved_until TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_order_items_sub_order_id ON order_items (sub_order_id);
