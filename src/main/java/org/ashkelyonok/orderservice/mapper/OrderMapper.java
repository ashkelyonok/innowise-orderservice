package org.ashkelyonok.orderservice.mapper;

import org.ashkelyonok.orderservice.model.dto.request.OrderCreateDto;
import org.ashkelyonok.orderservice.model.dto.response.OrderResponseDto;
import org.ashkelyonok.orderservice.model.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    @Mapping(target = "userInfo", ignore = true)
    @Mapping(target = "items", source = "orderItems")
    OrderResponseDto toDto(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", constant = "CREATED")
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(OrderCreateDto dto);
}