package com.deathstar.vader.loom.infrastructure;

import com.deathstar.vader.loom.domain.BucketType;
import com.deathstar.vader.loom.spi.StateRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Vader's implementation of the Loom StateRepository using PostgreSQL's native JSONB operators. */
@Repository
@RequiredArgsConstructor
public class PostgresStateRepository implements StateRepository {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(PostgresStateRepository.class);

    private static final String UPSERT_SQL =
            "INSERT INTO items (id, tenant_id, version, attr_static, attr_dynamic) "
                    + "VALUES (:id, :tenant_id, 1, :patch_attr_static::jsonb, :patch_attr_dynamic::jsonb) "
                    + "ON CONFLICT (id) DO UPDATE "
                    + "SET version = items.version + 1, "
                    + "attr_static = items.attr_static || :patch_attr_static::jsonb, "
                    + "attr_dynamic = items.attr_dynamic || :patch_attr_dynamic::jsonb "
                    + "WHERE items.version = :base_version";

    private static final String UPDATE_SQL =
            "UPDATE items SET version = version + 1"
                    + ", attr_static = attr_static || :patch_attr_static::jsonb"
                    + ", attr_dynamic = attr_dynamic || :patch_attr_dynamic::jsonb"
                    + " WHERE id = :id AND version = :base_version";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public boolean partialUpdate(
            UUID itemId,
            String tenantId,
            Map<BucketType, Map<UUID, Object>> bucketedPatches,
            long baseVersion) {
        if (bucketedPatches.isEmpty() && baseVersion != 0) return true;

        Map<UUID, Object> staticPatch =
                bucketedPatches.getOrDefault(BucketType.STATIC, new java.util.HashMap<>());
        Map<UUID, Object> dynamicPatch =
                bucketedPatches.getOrDefault(BucketType.DYNAMIC, new java.util.HashMap<>());

        String staticJson = staticPatch.isEmpty() ? "{}" : toJsonString(staticPatch);
        String dynamicJson = dynamicPatch.isEmpty() ? "{}" : toJsonString(dynamicPatch);

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("id", itemId)
                        .addValue("tenant_id", tenantId)
                        .addValue("base_version", baseVersion)
                        .addValue("patch_attr_static", staticJson)
                        .addValue("patch_attr_dynamic", dynamicJson);

        int updatedRows;
        if (baseVersion == 0) {
            // UPSERT for initial creation event
            updatedRows = jdbcTemplate.update(UPSERT_SQL, params);
        } else {
            // Standard CAS Update
            updatedRows = jdbcTemplate.update(UPDATE_SQL, params);
        }

        if (updatedRows == 0) {
            log.warn("OCC Failure: Expected version {} for item {}", baseVersion, itemId);
            return false;
        }

        return true;
    }

    @Override
    public long getCurrentVersion(UUID itemId) {
        String sql = "SELECT version FROM items WHERE id = :id";
        try {
            return jdbcTemplate.queryForObject(
                    sql, new MapSqlParameterSource("id", itemId), Long.class);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return 0L;
        }
    }

    private String toJsonString(Map<UUID, Object> map) {
        // Simplified mockup for JSON conversion to feed to Postgres `::jsonb`
        // Reality would use `objectMapper.writeValueAsString(...)`
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<UUID, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"")
                    .append(entry.getKey())
                    .append("\":\"")
                    .append(entry.getValue())
                    .append("\"");
            first = false;
        }
        return sb.append("}").toString();
    }
}
