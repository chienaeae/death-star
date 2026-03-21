package com.deathstar.vader.loom.service;

import com.deathstar.vader.loom.domain.Item;
import java.util.List;
import java.util.UUID;

public interface ItemQueryService {

    Item getItem(UUID id);

    List<Item> getItemsByStaticProperty(String propertyName, Object value);

    List<Item> getItemsByDynamicProperty(UUID propertyId, Object value);

    List<Item> getItemsByDynamicPropertyIn(UUID propertyId, List<?> values);
}
