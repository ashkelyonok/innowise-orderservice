package org.ashkelyonok.orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.ashkelyonok.orderservice.model.entity.OrderItem;
import org.ashkelyonok.orderservice.model.enums.OrderStatus;
import org.ashkelyonok.orderservice.model.event.OrderCreatedEvent;
import org.ashkelyonok.orderservice.repository.ItemRepository;
import org.ashkelyonok.orderservice.repository.OrderRepository;
import org.ashkelyonok.orderservice.repository.spec.OrderSpecification;
import org.ashkelyonok.orderservice.security.SecurityUtil;
import org.ashkelyonok.orderservice.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;
    private final OrderEventProducer orderEventProducer;
    private final SecurityUtil securityUtil;

    @Override
    public OrderResponseDto createOrder(OrderCreateDto dto) {
        log.info("Creating order for user email: {}", dto.getUserEmail());

        UserResponseDto user = resolveUser(dto.getUserEmail());

        securityUtil.checkOwnership(user.getId());

        Map<Long, Item> itemMap = fetchAndValidateItems(dto.getItems());

        Order order = orderMapper.toEntity(dto);
        order.setUserId(user.getId());

        processOrderItems(order, dto.getItems(), itemMap);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());

        sendOrderCreatedEvent(savedOrder);

        return buildResponse(savedOrder, user);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        securityUtil.checkOwnership(order.getUserId());

        return enrichWithUserInfo(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPageResponseDto getOrdersByUserId(Long userId, LocalDateTime fromDate, LocalDateTime toDate, Set<OrderStatus> statuses, Pageable pageable) {
        securityUtil.checkOwnership(userId);

        log.debug("Fetching orders for userId: {} with filters: [Date: {}-{}, Statuses: {}]", userId, fromDate, toDate, statuses);

        Specification<Order> userConstraint = (root, query, cb) -> cb.and(
                cb.equal(root.get("userId"), userId),
                cb.isFalse(root.get("deleted"))
        );

        Specification<Order> filters = OrderSpecification.filterBy(fromDate, toDate, statuses);

        Page<Order> page = orderRepository.findAll(userConstraint.and(filters), pageable);

        return buildPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPageResponseDto filterOrders(LocalDateTime fromDate, LocalDateTime toDate, Set<OrderStatus> statuses, Pageable pageable) {
        log.debug("Filtering orders with range: {} - {}", fromDate, toDate);
        Specification<Order> spec = OrderSpecification.filterBy(fromDate, toDate, statuses);
        Page<Order> page = orderRepository.findAll(spec, pageable);
        return buildPageResponse(page);
    }

    @Override
    public OrderResponseDto updateOrderStatus(Long id, OrderUpdateStatusDto dto) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        validateStatusTransition(order.getStatus(), dto.getStatus());

        log.info("Updating status for order {} from {} to {}", id, order.getStatus(), dto.getStatus());
        order.setStatus(dto.getStatus());

        Order savedOrder = orderRepository.save(order);
        return enrichWithUserInfo(savedOrder);
    }

    @Override
    public void updateOrderStatusByPayment(Long orderId, String paymentStatus) {
        log.info("Updating status for orderId: {} based on payment status: {}", orderId, paymentStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
            order.setStatus(OrderStatus.PAID);
            log.info("Order {} marked as PAID", orderId);
        } else if ("FAILED".equalsIgnoreCase(paymentStatus)) {
            log.warn("Payment failed for Order {}. Manual intervention required.", orderId);
        }

        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException(id);
        }

        orderRepository.softDeleteById(id);
        log.info("Order {} soft deleted", id);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        if (current == OrderStatus.CANCELLED) {
            throw new InvalidOrderOperationException("Cannot update status of a CANCELLED order");
        }
        if (current == OrderStatus.DELIVERED) {
            throw new InvalidOrderOperationException("Cannot update status of a DELIVERED order");
        }

        if (target == OrderStatus.CANCELLED && current == OrderStatus.SHIPPED) {
            throw new InvalidOrderOperationException("Cannot cancel an order that has already been shipped");
        }
    }

    private OrderResponseDto enrichWithUserInfo(Order order) {
        OrderResponseDto responseDto = orderMapper.toDto(order);
        try {
            UserResponseDto user = userServiceClient.getUserById(order.getUserId());
            responseDto.setUserInfo(user);
        } catch (Exception e) {
            log.error("Unexpected error fetching user info for order {}", order.getId(), e);
        }
        return responseDto;
    }

    private UserResponseDto resolveUser(String email) {
        UserResponseDto user = userServiceClient.getUserByEmail(email);
        if (user == null || user.getId() == null) {
            log.error("User resolution failed for email: {}", email);
            throw new ServiceUnavailableException("Cannot create order: User not found or Service unavailable.");
        }
        return user;
    }

    private Map<Long, Item> fetchAndValidateItems(List<OrderItemCreateDto> itemDtos) {
        Set<Long> itemIds = itemDtos.stream()
                .map(OrderItemCreateDto::getItemId)
                .collect(Collectors.toSet());

        Map<Long, Item> itemMap = itemRepository.findByIdIn(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));

        List<Long> missingIds = itemIds.stream()
                .filter(id -> !itemMap.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new ItemNotFoundException(missingIds.getFirst());
        }
        return itemMap;
    }

    private void processOrderItems(Order order, List<OrderItemCreateDto> itemDtos, Map<Long, Item> itemMap) {
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemCreateDto itemDto : itemDtos) {
            Item item = itemMap.get(itemDto.getItemId());

            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalPrice = totalPrice.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .item(item)
                    .quantity(itemDto.getQuantity())
                    .build();
            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);
    }

    private void sendOrderCreatedEvent(Order order) {
        try {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .totalAmount(order.getTotalPrice())
                    .createdAt(LocalDateTime.now())
                    .build();

            orderEventProducer.sendOrderCreatedEvent(event);
        } catch (Exception e) {
            log.error("Failed to send Kafka event for order ID: {}", order.getId(), e);
        }
    }

    private OrderResponseDto buildResponse(Order order, UserResponseDto user) {
        OrderResponseDto response = orderMapper.toDto(order);
        response.setUserInfo(user);
        return response;
    }

    private OrderPageResponseDto buildPageResponse(Page<Order> page) {
        List<OrderResponseDto> content = page.getContent().stream()
                .map(this::enrichWithUserInfo)
                .toList();

        return OrderPageResponseDto.builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}