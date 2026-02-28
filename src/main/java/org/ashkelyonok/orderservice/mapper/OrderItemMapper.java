package org.ashkelyonok.orderservice.mapper;

import org.ashkelyonok.orderservice.model.dto.response.OrderItemResponseDto;
import org.ashkelyonok.orderservice.model.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "name", source = "item.name")
    @Mapping(target = "price", source = "item.price")
    @Mapping(target = "totalItemPrice", source = ".", qualifiedByName = "calculateTotal")
    OrderItemResponseDto toDto(OrderItem orderItem);


    @Named("calculateTotal")
    default BigDecimal calculateTotal(OrderItem orderItem) {
        if (orderItem.getItem() == null || orderItem.getItem().getPrice() == null || orderItem.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return orderItem.getItem().getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
    }
}