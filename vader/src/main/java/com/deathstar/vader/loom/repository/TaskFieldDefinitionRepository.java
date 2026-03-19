package com.deathstar.vader.loom.repository;

import com.deathstar.vader.loom.core.domain.BucketType;
import com.deathstar.vader.loom.core.domain.FieldDefinition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TaskFieldDefinitionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<FieldDefinition> findById(UUID id) {
        String sql =
                "SELECT id, name, field_type, bucket_type FROM task_field_definitions WHERE id = :id";

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("id", id),
                rs -> {
                    if (rs.next()) {
                        String targetBucket = rs.getString("bucket_type");
                        return Optional.of(
                                new FieldDefinition(
                                        rs.getObject("id", UUID.class),
                                        rs.getString("name"),
                                        FieldDefinition.FieldType.valueOf(
                                                rs.getString("field_type")),
                                        targetBucket != null
                                                ? BucketType.valueOf(targetBucket)
                                                : null));
                    }
                    return Optional.empty();
                });
    }

    public List<FieldDefinition> findAllByTenantId(String tenantId) {
        String sql =
                "SELECT id, name, field_type, bucket_type FROM task_field_definitions WHERE tenant_id = :tenantId";

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("tenantId", tenantId),
                (rs, rowNum) -> {
                    String targetBucket = rs.getString("bucket_type");
                    return new FieldDefinition(
                            rs.getObject("id", UUID.class),
                            rs.getString("name"),
                            FieldDefinition.FieldType.valueOf(rs.getString("field_type")),
                            targetBucket != null ? BucketType.valueOf(targetBucket) : null);
                });
    }

    public void save(String tenantId, FieldDefinition definition) {
        String sql =
                "INSERT INTO task_field_definitions (id, tenant_id, name, field_type, bucket_type) "
                        + "VALUES (:id, :tenantId, :name, :fieldType, :bucketType)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", definition.fieldId());
        params.addValue("tenantId", tenantId);
        params.addValue("name", definition.name());
        params.addValue("fieldType", definition.type().name());
        params.addValue(
                "bucketType",
                definition.targetBucket() != null ? definition.targetBucket().name() : null);

        jdbcTemplate.update(sql, params);
    }
}
