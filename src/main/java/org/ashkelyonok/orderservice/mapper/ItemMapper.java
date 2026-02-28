package org.ashkelyonok.orderservice.mapper;

import org.ashkelyonok.orderservice.model.dto.request.ItemCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.ItemUpdateDto;
import org.ashkelyonok.orderservice.model.dto.response.ItemResponseDto;
import org.ashkelyonok.orderservice.model.entity.Item;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemResponseDto toDto(Item item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Item toEntity(ItemCreateDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateItemFromDto(ItemUpdateDto dto, @MappingTarget Item item);
}