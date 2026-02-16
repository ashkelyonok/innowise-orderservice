package org.ashkelyonok.orderservice.service.impl;

import org.ashkelyonok.orderservice.exception.ItemNotFoundException;
import org.ashkelyonok.orderservice.mapper.ItemMapper;
import org.ashkelyonok.orderservice.model.dto.request.ItemCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.ItemUpdateDto;
import org.ashkelyonok.orderservice.model.dto.response.ItemResponseDto;
import org.ashkelyonok.orderservice.model.entity.Item;
import org.ashkelyonok.orderservice.repository.ItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Test
    @DisplayName("Create Item: Should return saved item DTO when valid data is provided")
    void createItem_ShouldReturnDto_WhenDataIsValid() {
        ItemCreateDto createDto = new ItemCreateDto();
        createDto.setName("Laptop");
        createDto.setPrice(new BigDecimal("1200.00"));

        Item itemEntity = new Item();
        itemEntity.setName("Laptop");
        itemEntity.setPrice(new BigDecimal("1200.00"));

        Item savedItem = new Item();
        savedItem.setId(1L);
        savedItem.setName("Laptop");
        savedItem.setPrice(new BigDecimal("1200.00"));

        ItemResponseDto responseDto = new ItemResponseDto();
        responseDto.setId(1L);
        responseDto.setName("Laptop");
        responseDto.setPrice(new BigDecimal("1200.00"));

        when(itemMapper.toEntity(createDto)).thenReturn(itemEntity);
        when(itemRepository.save(itemEntity)).thenReturn(savedItem);
        when(itemMapper.toDto(savedItem)).thenReturn(responseDto);

        ItemResponseDto result = itemService.createItem(createDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Laptop");

        verify(itemRepository).save(itemEntity);
    }

    @Test
    @DisplayName("Get Items: Should return paged list when items exist")
    @SuppressWarnings("unchecked")
    void getItems_ShouldReturnPagedList_WhenItemsExist() {
        Pageable pageable = PageRequest.of(0, 10);

        Item item = new Item();
        item.setId(1L);

        Page<Item> page = new PageImpl<>(List.of(item));

        ItemResponseDto responseDto = new ItemResponseDto();
        responseDto.setId(1L);
        responseDto.setName("Item");

        when(itemRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(itemMapper.toDto(item)).thenReturn(responseDto);

        Page<ItemResponseDto> result = itemService.getItems("filter", pageable);

        assertThat(result).isNotEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(itemRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Get Item By ID: Should return item when ID exists")
    void getItemById_ShouldReturnItem_WhenIdExists() {
        Long itemId = 1L;
        Item item = new Item();
        item.setId(itemId);

        ItemResponseDto responseDto = new ItemResponseDto();
        responseDto.setId(itemId);
        responseDto.setName("Item");

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(responseDto);

        ItemResponseDto result = itemService.getItemById(itemId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(itemId);
    }

    @Test
    @DisplayName("Get Item By ID: Should throw exception when ID does not exist")
    void getItemById_ShouldThrowException_WhenIdDoesNotExist() {
        Long itemId = 99L;
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItemById(itemId))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    @DisplayName("Update Item: Should return updated DTO when item exists")
    void updateItem_ShouldReturnUpdatedDto_WhenItemExists() {
        Long itemId = 1L;
        ItemUpdateDto updateDto = new ItemUpdateDto();
        updateDto.setName("New Name");
        updateDto.setPrice(BigDecimal.valueOf(200));

        Item existingItem = new Item();
        existingItem.setId(itemId);
        existingItem.setName("Old Name");

        Item savedItem = new Item();
        savedItem.setId(itemId);
        savedItem.setName("New Name");

        ItemResponseDto responseDto = new ItemResponseDto();
        responseDto.setId(itemId);
        responseDto.setName("New Name");
        responseDto.setPrice(BigDecimal.valueOf(200));

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(itemRepository.save(existingItem)).thenReturn(savedItem);
        when(itemMapper.toDto(savedItem)).thenReturn(responseDto);

        ItemResponseDto result = itemService.updateItem(itemId, updateDto);

        assertThat(result.getName()).isEqualTo("New Name");

        verify(itemMapper).updateItemFromDto(updateDto, existingItem);
        verify(itemRepository).save(existingItem);
    }

    @Test
    @DisplayName("Update Item: Should throw exception when item not found")
    void updateItem_ShouldThrowException_WhenItemNotFound() {
        Long itemId = 99L;
        ItemUpdateDto updateDto = new ItemUpdateDto();
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.updateItem(itemId, updateDto))
                .isInstanceOf(ItemNotFoundException.class);

        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete Item: Should verify deletion when item exists")
    void deleteItem_ShouldDelete_WhenItemExists() {
        Long itemId = 1L;
        when(itemRepository.existsById(itemId)).thenReturn(true);

        itemService.deleteItem(itemId);

        verify(itemRepository).deleteById(itemId);
    }

    @Test
    @DisplayName("Delete Item: Should throw exception when item does not exist")
    void deleteItem_ShouldThrowException_WhenItemDoesNotExist() {
        Long itemId = 99L;
        when(itemRepository.existsById(itemId)).thenReturn(false);

        assertThatThrownBy(() -> itemService.deleteItem(itemId))
                .isInstanceOf(ItemNotFoundException.class);

        verify(itemRepository, never()).deleteById(any());
    }
}