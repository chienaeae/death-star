package com.deathstar.vader.asset.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    
    private String provider; // 's3' or 'minio'
    private String bucket;
    private MinioProperties minio = new MinioProperties();

    @Data
    public static class MinioProperties {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String publicUrl; // The external URL used by the client for MinIO (e.g. localhost:9000 vs minio:9000)
    }
}
