package com.deathstar.vader.loom.core.domain;

import java.util.List;
import java.util.UUID;

/** Logical Projection (Filter + Group + Sort) for rendering Items on a UI. */
public record View(
        UUID viewId, String name, List<FilterCriteria> filters, List<SortCriteria> sort) {
    public record FilterCriteria(UUID fieldId, String operator, Object value) {}

    public record SortCriteria(UUID fieldId, SortDirection direction) {}

    public enum SortDirection {
        ASC,
        DESC
    }
}
