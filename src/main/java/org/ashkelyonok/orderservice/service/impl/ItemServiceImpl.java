package org.ashkelyonok.orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.orderservice.exception.ItemNotFoundException;
import org.ashkelyonok.orderservice.mapper.ItemMapper;
import org.ashkelyonok.orderservice.model.dto.request.ItemCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.ItemUpdateDto;
import org.ashkelyonok.orderservice.model.dto.response.ItemResponseDto;
import org.ashkelyonok.orderservice.model.entity.Item;
import org.ashkelyonok.orderservice.repository.ItemRepository;
import org.ashkelyonok.orderservice.repository.spec.SpecificationBuilder;
import org.ashkelyonok.orderservice.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Override
    public ItemResponseDto createItem(ItemCreateDto dto) {
        log.info("Attempting to create item with name: {}", dto.getName());

        Item item = itemMapper.toEntity(dto);
        Item savedItem = itemRepository.save(item);

        log.info("Item created successfully with ID: {}", savedItem.getId());
        return itemMapper.toDto(savedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemResponseDto> getItems(String name, Pageable pageable) {
        log.debug("Fetching items page {} with filter name='{}'", pageable.getPageNumber(), name);

        Specification<Item> spec = SpecificationBuilder.likeIgnoreCase("name", name);
        Specification<Item> notDeleted = (root, query, cb) -> cb.isFalse(root.get("deleted"));

        return itemRepository.findAll(spec.and(notDeleted), pageable)
                .map(itemMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponseDto getItemById(Long id) {
        return itemRepository.findByIdAndDeletedFalse(id)
                .map(itemMapper::toDto)
                .orElseThrow(() -> new ItemNotFoundException(id));
    }

    @Override
    public ItemResponseDto updateItem(Long id, ItemUpdateDto dto) {
        log.info("Updating item with ID: {}", id);

        Item item = itemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        itemMapper.updateItemFromDto(dto, item);
        Item updatedItem = itemRepository.save(item);

        return itemMapper.toDto(updatedItem);
    }

    @Override
    public void deleteItem(Long id) {
        log.info("Deleting item with ID: {}", id);

        int rows = itemRepository.softDeleteById(id);

        if (rows == 0) {
            throw new ItemNotFoundException(id);
        }
        log.info("Item {} soft deleted", id);
    }
}