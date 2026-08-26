package com.nexamarket.catalog.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "catalog.storage.type", havingValue = "memory")
public class InMemoryObjectStorage implements ObjectStorage {

    private final Map<String, StoredObject> objects = new ConcurrentHashMap<>();

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        objects.put(objectKey, new StoredObject(content.clone(), contentType));
    }

    @Override
    public StoredObject get(String objectKey) {
        StoredObject stored = objects.get(objectKey);
        if (stored == null) {
            throw new ObjectStorageException("Nesne bulunamadı: " + objectKey, new IllegalStateException("missing object"));
        }
        return new StoredObject(stored.content().clone(), stored.contentType());
    }
}
