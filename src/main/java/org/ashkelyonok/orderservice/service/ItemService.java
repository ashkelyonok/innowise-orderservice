package org.ashkelyonok.orderservice.service;

import org.ashkelyonok.orderservice.model.dto.request.ItemCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.ItemUpdateDto;
import org.ashkelyonok.orderservice.model.dto.response.ItemResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing Items.
 * Handles the lifecycle of items including creation, lookup, updates and deletion.
 */
public interface ItemService {

    /**
     * Creates a new item in the catalog.
     */
    ItemResponseDto createItem(ItemCreateDto dto);

    /**
     * Retrieves a paginated list of items.
     * Optionally filters by name (case-insensitive).
     */
    Page<ItemResponseDto> getItems(String name, Pageable pageable);

    /**
     * Retrieves a single item by ID.
     */
    ItemResponseDto getItemById(Long id);

    /**
     * Updates an existing item.
     */
    ItemResponseDto updateItem(Long id, ItemUpdateDto dto);

    /**
     * Deletes an item from the catalog.
     */
    void deleteItem(Long id);
}
