package com.deathstar.vader.loom.service;

import com.deathstar.vader.loom.domain.Item;
import com.deathstar.vader.loom.infrastructure.ScopedValueIdentityResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemQueryServiceImpl implements ItemQueryService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ScopedValueIdentityResolver identityResolver;
    private final ObjectMapper objectMapper;

    private Item mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            Map<UUID, Object> staticAttrs = new HashMap<>();
            Map<UUID, Object> dynamicAttrs = new HashMap<>();

            String staticJson = rs.getString("attr_static");
            String dynamicJson = rs.getString("attr_dynamic");

            if (staticJson != null && !staticJson.isEmpty()) {
                Map<String, Object> map =
                        objectMapper.readValue(
                                staticJson, new TypeReference<Map<String, Object>>() {});
                map.forEach((k, v) -> staticAttrs.put(UUID.fromString(k), v));
            }
            if (dynamicJson != null && !dynamicJson.isEmpty()) {
                Map<String, Object> map =
                        objectMapper.readValue(
                                dynamicJson, new TypeReference<Map<String, Object>>() {});
                map.forEach((k, v) -> dynamicAttrs.put(UUID.fromString(k), v));
            }

            return new Item(
                    rs.getString("tenant_id"),
                    UUID.fromString(rs.getString("id")),
                    rs.getLong("version"),
                    staticAttrs,
                    dynamicAttrs);
        } catch (Exception e) {
            throw new SQLException("Failed to map Item row", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Item getItem(UUID id) {
        String sql =
                "SELECT id, tenant_id, version, attr_static, attr_dynamic FROM items WHERE id = :id AND tenant_id = :tenant_id";
        List<Item> items =
                jdbcTemplate.query(
                        sql,
                        new MapSqlParameterSource()
                                .addValue("id", id)
                                .addValue("tenant_id", identityResolver.currentTenantId()),
                        this::mapRow);
        return items.isEmpty() ? null : items.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> getItemsByStaticProperty(String propertyName, Object value) {
        String sql =
                "SELECT id, tenant_id, version, attr_static, attr_dynamic FROM items WHERE tenant_id = :tenant_id AND attr_static->>:property_name = :value";
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("tenant_id", identityResolver.currentTenantId())
                        .addValue("property_name", propertyName)
                        .addValue("value", value.toString()),
                this::mapRow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> getItemsByDynamicProperty(UUID propertyId, Object value) {
        String sql =
                "SELECT id, tenant_id, version, attr_static, attr_dynamic FROM items WHERE tenant_id = :tenant_id AND attr_dynamic->>:property_id = :value";
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("tenant_id", identityResolver.currentTenantId())
                        .addValue("property_id", propertyId.toString())
                        .addValue("value", value.toString()),
                this::mapRow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> getItemsByDynamicPropertyIn(UUID propertyId, List<?> values) {
        if (values == null || values.isEmpty()) return List.of();

        List<String> stringValues = values.stream().map(Object::toString).toList();
        String sql =
                "SELECT id, tenant_id, version, attr_static, attr_dynamic FROM items WHERE tenant_id = :tenant_id AND attr_dynamic->>:property_id IN (:values)";
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("tenant_id", identityResolver.currentTenantId())
                        .addValue("property_id", propertyId.toString())
                        .addValue("values", stringValues),
                this::mapRow);
    }
}
