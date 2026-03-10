CREATE TABLE IF NOT EXISTS default.audit_events (
    `Timestamp` DateTime64(3) CODEC(Delta(8), ZSTD(1)),
    `EventId` UUID,
    `TraceId` String CODEC(ZSTD(1)),
    `SpanId` String CODEC(ZSTD(1)),
    
    `ActorId` String CODEC(ZSTD(1)),
    `Action` LowCardinality(String) CODEC(ZSTD(1)),
    `ResourceType` LowCardinality(String) CODEC(ZSTD(1)),
    `ResourceId` String CODEC(ZSTD(1)),
    `Status` LowCardinality(String) CODEC(ZSTD(1)),
    
    `ClientIp` IPv4 CODEC(Delta(4), ZSTD(1)),
    `UserAgent` String CODEC(ZSTD(1)),
    
    `Metadata` Map(LowCardinality(String), String) CODEC(ZSTD(1)),
    
    INDEX idx_audit_trace_id TraceId TYPE bloom_filter(0.001) GRANULARITY 1
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(Timestamp)
ORDER BY (ActorId, Action, Timestamp)
SETTINGS index_granularity = 8192;
