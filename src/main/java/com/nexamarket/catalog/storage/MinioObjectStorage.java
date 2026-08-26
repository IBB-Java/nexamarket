package com.nexamarket.catalog.storage;

import com.nexamarket.catalog.config.CatalogStorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
@ConditionalOnProperty(name = "catalog.storage.type", havingValue = "minio", matchIfMissing = true)
public class MinioObjectStorage implements ObjectStorage {

    private final CatalogStorageProperties properties;
    private final MinioClient client;
    private volatile boolean bucketReady;

    public MinioObjectStorage(CatalogStorageProperties properties) {
        this.properties = properties;
        this.client = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(content), (long) content.length, -1L)
                    .contentType(contentType)
                    .build());
        } catch (Exception exception) {
            throw new ObjectStorageException("Nesne depolamaya yazılamadı: " + objectKey, exception);
        }
    }

    @Override
    public StoredObject get(String objectKey) {
        try (GetObjectResponse response = client.getObject(GetObjectArgs.builder()
                .bucket(properties.bucket())
                .object(objectKey)
                .build())) {
            String contentType = response.headers().get("Content-Type");
            return new StoredObject(response.readAllBytes(), contentType);
        } catch (Exception exception) {
            throw new ObjectStorageException("Nesne depolamadan okunamadı: " + objectKey, exception);
        }
    }

    private synchronized void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
        }
        bucketReady = true;
    }
}
