package com.nexamarket.catalog.storage;

public interface ObjectStorage {
    void put(String objectKey, byte[] content, String contentType);

    StoredObject get(String objectKey);
}
