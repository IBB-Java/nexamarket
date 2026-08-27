package com.nexamarket.stock.application;

import java.util.function.Supplier;

/** Abstraction keeps stock code testable while production uses a Redis lock. */
public interface StockMutationLock {
    <T> T execute(Long variantId, Supplier<T> action);
}
