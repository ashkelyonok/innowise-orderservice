package org.ashkelyonok.orderservice.service.impl;

import org.ashkelyonok.orderservice.client.UserServiceClient;
import org.ashkelyonok.orderservice.exception.InvalidOrderOperationException;
import org.ashkelyonok.orderservice.exception.ItemNotFoundException;
import org.ashkelyonok.orderservice.exception.OrderNotFoundException;
import org.ashkelyonok.orderservice.exception.ServiceUnavailableException;
import org.ashkelyonok.orderservice.kafka.producer.OrderEventProducer;
import org.ashkelyonok.orderservice.mapper.OrderMapper;
import org.ashkelyonok.orderservice.model.dto.request.OrderCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.OrderItemCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.OrderUpdateStatusDto;
import org.ashkelyonok.orderservice.model.dto.response.OrderPageResponseDto;
import org.ashkelyonok.orderservice.model.dto.response.OrderResponseDto;
import org.ashkelyonok.orderservice.model.dto.response.UserResponseDto;
import org.ashkelyonok.orderservice.model.entity.Item;
import org.ashkelyonok.orderservice.model.entity.Order;
import org.ashkelyonok.orderservice.model.enums.OrderStatus;
import org.ashkelyonok.orderservice.model.event.OrderCreatedEvent;
import org.ashkelyonok.orderservice.repository.ItemRepository;
import org.ashkelyonok.orderservice.repository.OrderRepository;
import org.ashkelyonok.orderservice.security.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private OrderEventProducer orderEventProducer;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("Create Order: Should succeed and publish event when data is valid")
    void createOrder_ShouldSucceed_WhenDataIsValid() {
        String email = "test@example.com";
        Long userId = 100L;
        Long itemId = 1L;

        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setUserEmail(email);
        OrderItemCreateDto itemDto = new OrderItemCreateDto();
        itemDto.setItemId(itemId);
        itemDto.setQuantity(2);
        createDto.setItems(List.of(itemDto));

        UserResponseDto userResponse = new UserResponseDto();
        userResponse.setId(userId);
        userResponse.setEmail(email);

        Item item = new Item();
        item.setId(itemId);
        item.setPrice(BigDecimal.TEN);

        Order orderEntity = new Order();
        orderEntity.setId(500L);
        orderEntity.setUserId(userId);
        orderEntity.setTotalPrice(BigDecimal.valueOf(20));

        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setId(500L);
        responseDto.setStatus(OrderStatus.CREATED);

        when(userServiceClient.getUserByEmail(email)).thenReturn(userResponse);
        doNothing().when(securityUtil).checkOwnership(userId);
        when(itemRepository.findByIdIn(Set.of(itemId))).thenReturn(List.of(item));
        when(orderMapper.toEntity(createDto)).thenReturn(orderEntity);
        when(orderRepository.save(orderEntity)).thenReturn(orderEntity);
        when(orderMapper.toDto(orderEntity)).thenReturn(responseDto);

        OrderResponseDto result = orderService.createOrder(createDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(500L);

        verify(orderEventProducer).sendOrderCreatedEvent(any(OrderCreatedEvent.class));
        verify(orderRepository).save(orderEntity);
        verify(securityUtil).checkOwnership(userId);
    }

    @Test
    @DisplayName("Create Order: Should throw ServiceUnavailableException when user not found")
    void createOrder_ShouldThrowException_WhenUserNotFound() {
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setUserEmail("unknown@example.com");

        when(userServiceClient.getUserByEmail(anyString())).thenReturn(null);

        assertThatThrownBy(() -> orderService.createOrder(createDto))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Create Order: Should throw exception when User ID is null")
    void createOrder_ShouldThrowException_WhenUserIdIsNull() {
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setUserEmail("test@example.com");

        UserResponseDto userResponse = new UserResponseDto();
        userResponse.setEmail("test@example.com");
        userResponse.setId(null);

        when(userServiceClient.getUserByEmail(anyString())).thenReturn(userResponse);

        assertThatThrownBy(() -> orderService.createOrder(createDto))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("Create Order: Should throw ItemNotFoundException when item missing")
    void createOrder_ShouldThrowException_WhenItemNotFound() {
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setUserEmail("test@example.com");
        OrderItemCreateDto itemDto = new OrderItemCreateDto();
        itemDto.setItemId(99L);
        createDto.setItems(List.of(itemDto));

        UserResponseDto userResponse = new UserResponseDto();
        userResponse.setId(1L);

        when(userServiceClient.getUserByEmail(anyString())).thenReturn(userResponse);
        when(itemRepository.findByIdIn(anySet())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> orderService.createOrder(createDto))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    @DisplayName("Create Order: Should log error but NOT fail when Kafka is down")
    void createOrder_ShouldNotFail_WhenKafkaFails() {
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setUserEmail("test@example.com");
        OrderItemCreateDto itemDto = new OrderItemCreateDto();
        itemDto.setItemId(1L);
        itemDto.setQuantity(1);
        createDto.setItems(List.of(itemDto));

        UserResponseDto user = new UserResponseDto();
        user.setId(1L);
        Item item = new Item();
        item.setId(1L);
        item.setPrice(BigDecimal.ONE);
        Order order = new Order();
        order.setId(1L);

        when(userServiceClient.getUserByEmail(anyString())).thenReturn(user);
        when(itemRepository.findByIdIn(anySet())).thenReturn(List.of(item));
        when(orderMapper.toEntity(any())).thenReturn(order);
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toDto(any())).thenReturn(new OrderResponseDto());

        doThrow(new RuntimeException("Kafka unreachable")).when(orderEventProducer).sendOrderCreatedEvent(any());

        OrderResponseDto result = orderService.createOrder(createDto);

        assertThat(result).isNotNull();
        verify(orderRepository).save(any());
        verify(orderEventProducer).sendOrderCreatedEvent(any());
    }

    @Test
    @DisplayName("Get Order: Should return enriched order when found")
    void getOrderById_ShouldReturnEnrichedOrder_WhenFound() {
        Long id = 1L;
        Long userId = 100L;
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);

        UserResponseDto user = new UserResponseDto();
        user.setId(userId);

        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(id);

        when(orderRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(order));
        doNothing().when(securityUtil).checkOwnership(userId);
        when(orderMapper.toDto(order)).thenReturn(dto);
        when(userServiceClient.getUserById(userId)).thenReturn(user);

        OrderResponseDto result = orderService.getOrderById(id);

        assertThat(result.getUserInfo()).isEqualTo(user);
        verify(securityUtil).checkOwnership(userId);
    }

    @Test
    @DisplayName("Get Order: Should return order with null user info when User Service fails")
    void getOrderById_ShouldReturnPartial_WhenUserServiceFails() {
        Long id = 1L;
        Order order = new Order();
        order.setId(id);
        order.setUserId(100L);

        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(id);

        when(orderRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(order));
        doNothing().when(securityUtil).checkOwnership(100L);
        when(orderMapper.toDto(order)).thenReturn(dto);
        when(userServiceClient.getUserById(anyLong())).thenThrow(new RuntimeException("Service Down"));

        OrderResponseDto result = orderService.getOrderById(id);

        assertThat(result).isNotNull();
        assertThat(result.getUserInfo()).isNull();
    }

    @Test
    @DisplayName("Get Order: Should throw exception when order not found")
    void getOrderById_ShouldThrowException_WhenNotFound() {
        when(orderRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderById(1L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Update Status: Should update when transition is valid")
    void updateOrderStatus_ShouldUpdate_WhenTransitionValid() {
        Long id = 1L;
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.CREATED);

        OrderUpdateStatusDto dto = new OrderUpdateStatusDto();
        dto.setStatus(OrderStatus.SHIPPED);

        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toDto(order)).thenReturn(responseDto);

        OrderResponseDto result = orderService.updateOrderStatus(id, dto);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("Update Status: Should throw exception when trying to cancel a SHIPPED order")
    void updateOrderStatus_ShouldThrowException_WhenCancellingShipped() {
        Long id = 1L;
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.SHIPPED);

        OrderUpdateStatusDto dto = new OrderUpdateStatusDto();
        dto.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(id, dto))
                .isInstanceOf(InvalidOrderOperationException.class)
                .hasMessageContaining("Cannot cancel an order that has already been shipped");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Status: Should throw exception when updating a delivered order")
    void updateOrderStatus_ShouldThrowException_WhenDelivered() {
        Order order = new Order();
        order.setStatus(OrderStatus.DELIVERED);

        OrderUpdateStatusDto dto = new OrderUpdateStatusDto();
        dto.setStatus(OrderStatus.CREATED);

        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, dto))
                .isInstanceOf(InvalidOrderOperationException.class)
                .hasMessageContaining("Cannot update status of a DELIVERED order");
    }

    @Test
    @DisplayName("Update Status: Should throw exception when updating a CANCELLED order")
    void updateOrderStatus_ShouldThrowException_WhenCancelled() {
        Order order = new Order();
        order.setStatus(OrderStatus.CANCELLED);

        OrderUpdateStatusDto dto = new OrderUpdateStatusDto();
        dto.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, dto))
                .isInstanceOf(InvalidOrderOperationException.class)
                .hasMessageContaining("Cannot update status of a CANCELLED order");
    }

    @Test
    @DisplayName("Update Status: Should throw exception when Order Not Found")
    void updateOrderStatus_ShouldThrowException_WhenNotFound() {
        OrderUpdateStatusDto dto = new OrderUpdateStatusDto();
        when(orderRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(99L, dto))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Payment Update: Should mark PAID when payment SUCCESS")
    void updateOrderStatusByPayment_ShouldMarkPaid_WhenSuccess() {
        Long id = 1L;
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.CREATED);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        orderService.updateOrderStatusByPayment(id, "SUCCESS");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Payment Update: Should do nothing to status when payment FAILED")
    void updateOrderStatusByPayment_ShouldLogOnly_WhenFailed() {
        Long id = 1L;
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.CREATED);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        orderService.updateOrderStatusByPayment(id, "FAILED");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED); // Unchanged
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Payment Update: Should ignore unknown payment status")
    void updateOrderStatusByPayment_ShouldIgnore_WhenStatusUnknown() {
        Long id = 1L;
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.CREATED);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        orderService.updateOrderStatusByPayment(id, "UNKNOWN_STATUS");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED); // Should remain unchanged
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Payment Update: Should throw exception when Order Not Found")
    void updateOrderStatusByPayment_ShouldThrowException_WhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatusByPayment(99L, "SUCCESS"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Get User Orders: Should return paged list")
    void getOrdersByUserId_ShouldReturnPagedList() {
        Long userId = 1L;
        Pageable pageable = Pageable.unpaged();
        Order order = new Order();
        order.setUserId(userId);
        Page<Order> page = new PageImpl<>(List.of(order));

        doNothing().when(securityUtil).checkOwnership(userId);
        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(orderMapper.toDto(any())).thenReturn(new OrderResponseDto());

        OrderPageResponseDto result = orderService.getOrdersByUserId(userId, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(securityUtil).checkOwnership(userId);
    }

    @Test
    @DisplayName("Get User Orders: Should throw exception when access denied")
    void getOrdersByUserId_ShouldThrowException_WhenAccessDenied() {
        Long userId = 99L;
        Pageable pageable = Pageable.unpaged();

        doThrow(new org.springframework.security.access.AccessDeniedException("Denied"))
                .when(securityUtil).checkOwnership(userId);

        assertThatThrownBy(() -> orderService.getOrdersByUserId(userId, null, null, null, pageable))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("Filter Orders: Should return filtered list")
    @SuppressWarnings("unchecked")
    void filterOrders_ShouldReturnFilteredList() {
        Pageable pageable = Pageable.unpaged();
        Page<Order> page = new PageImpl<>(List.of(new Order()));

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(orderMapper.toDto(any())).thenReturn(new OrderResponseDto());

        OrderPageResponseDto result = orderService.filterOrders(null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Delete Order: Should soft delete when order exists")
    void deleteOrder_ShouldSoftDelete_WhenExists() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        orderService.deleteOrder(1L);

        verify(orderRepository).softDeleteById(1L);
    }

    @Test
    @DisplayName("Delete Order: Should throw exception when order does not exist")
    void deleteOrder_ShouldThrowException_WhenNotFound() {
        when(orderRepository.existsById(1L)).thenReturn(false);
        assertThatThrownBy(() -> orderService.deleteOrder(1L))
                .isInstanceOf(OrderNotFoundException.class);
    }
}