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
import org.ashkelyonok.orderservice.model.dto.response.UserPageResponse;
import org.ashkelyonok.orderservice.model.dto.response.UserResponseDto;
import org.ashkelyonok.orderservice.model.entity.Item;
import org.ashkelyonok.orderservice.model.entity.Order;
import org.ashkelyonok.orderservice.model.entity.OrderItem;
import org.ashkelyonok.orderservice.model.enums.OrderStatus;
import org.ashkelyonok.orderservice.model.event.OrderCreatedEvent;
import org.ashkelyonok.orderservice.repository.ItemRepository;
import org.ashkelyonok.orderservice.repository.OrderRepository;
import org.ashkelyonok.orderservice.repository.spec.OrderSpecification;
import org.ashkelyonok.orderservice.repository.spec.SpecificationBuilder;
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

        Specification<Order> userConstraint = SpecificationBuilder.attributeEquals("userId", userId);
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

        Order order = orderRepository.findByIdAndDeletedFalse(orderId)
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
        int rows = orderRepository.softDeleteById(id);
        if (rows == 0) {
            throw new OrderNotFoundException(id);
        }
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

        UserResponseDto user = userServiceClient.getUserById(order.getUserId());
        responseDto.setUserInfo(user);

        return responseDto;
    }

    private UserResponseDto resolveUser(String email) {
        UserPageResponse page = userServiceClient.getUserByEmail(email);

        if (page == null || page.getContent() == null || page.getContent().isEmpty()) {
            log.error("User resolution failed for email: {}", email);
            throw new ServiceUnavailableException("Cannot create order: User Service unavailable or User not found.");
        }
        return page.getContent().getFirst();
    }

    private Map<Long, Item> fetchAndValidateItems(List<OrderItemCreateDto> itemDtos) {
        Set<Long> itemIds = itemDtos.stream()
                .map(OrderItemCreateDto::getItemId)
                .collect(Collectors.toSet());

        Map<Long, Item> itemMap = itemRepository.findByIdInAndDeletedFalse(itemIds).stream()
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
        if (page.isEmpty()) {
            return OrderPageResponseDto.builder()
                    .content(List.of())
                    .pageNumber(page.getNumber())
                    .pageSize(page.getSize())
                    .totalElements(0L)
                    .totalPages(0)
                    .build();
        }

        Set<Long> uniqueUserIds = page.getContent().stream()
                .map(Order::getUserId)
                .collect(Collectors.toSet());

        Map<Long, UserResponseDto> userCache = new java.util.HashMap<>();

        if (uniqueUserIds.size() == 1) {
            Long singleUserId = uniqueUserIds.iterator().next();
            UserResponseDto user = userServiceClient.getUserById(singleUserId);
            if (user != null) {
                userCache.put(singleUserId, user);
            }
        } else {
            UserPageResponse usersPage = userServiceClient.getUsersByIds(uniqueUserIds);

            if (usersPage != null && usersPage.getContent() != null) {
                usersPage.getContent().forEach(u -> userCache.put(u.getId(), u));
            } else {
                log.warn("Batch user fetch failed (Circuit Breaker open). Order history will display without user info.");
            }
        }

        List<OrderResponseDto> content = page.getContent().stream()
                .map(order -> {
                    OrderResponseDto dto = orderMapper.toDto(order);
                    dto.setUserInfo(userCache.get(order.getUserId()));
                    return dto;
                })
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