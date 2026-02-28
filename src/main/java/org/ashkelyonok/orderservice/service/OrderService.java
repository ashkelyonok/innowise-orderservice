package org.ashkelyonok.orderservice.service;

import org.ashkelyonok.orderservice.model.dto.request.OrderCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.OrderUpdateStatusDto;
import org.ashkelyonok.orderservice.model.dto.response.OrderPageResponseDto;
import org.ashkelyonok.orderservice.model.dto.response.OrderResponseDto;
import org.ashkelyonok.orderservice.model.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Service interface for managing Orders.
 * Handles the lifecycle of orders including creation, lookup, status updates, and cancellation.
 */
public interface OrderService {

    /**
     * Creates a new order.
     * Validates that items exist, calculates total price, and saves the order.
     *
     * @param dto Data for creating order (userId, items).
     * @return The created OrderResponseDto with User Info.
     */
    OrderResponseDto createOrder(OrderCreateDto dto);

    /**
     * Retrieves an order by ID.
     *
     * @param id Order ID.
     * @return OrderResponseDto.
     * @throws org.ashkelyonok.orderservice.exception.OrderNotFoundException if not found.
     */
    OrderResponseDto getOrderById(Long id);

    /**
     * Retrieves all orders for a specific user with optional filtering.
     *
     * @param userId User ID.
     * @param fromDate Start of date range (optional).
     * @param toDate End of date range (optional).
     * @param statuses Set of statuses to include (optional).
     * @param pageable Pagination options.
     * @return Paginated list of orders (filtered if needed).
     */
    OrderPageResponseDto getOrdersByUserId(
            Long userId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Set<OrderStatus> statuses,
            Pageable pageable
    );

    /**
     * Filters orders based on complex criteria (Date range, Statuses).
     *
     * @param fromDate Start of date range (optional).
     * @param toDate End of date range (optional).
     * @param statuses Set of statuses to include (optional).
     * @param pageable Pagination options.
     * @return Paginated list of filtered orders.
     */
    OrderPageResponseDto filterOrders(
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Set<OrderStatus> statuses,
            Pageable pageable
    );

    /**
     * Updates the status of an order.
     * Includes logic validation (e.g., cannot cancel a delivered order).
     *
     * @param id Order ID.
     * @param dto New status.
     * @return Updated OrderResponseDto.
     */
    OrderResponseDto updateOrderStatus(Long id, OrderUpdateStatusDto dto);

    /**
     * Updates order status based on payment event.
     * @param orderId The ID of the order to update.
     * @param paymentStatus The status received from Payment Service ("SUCCESS", "FAILED").
     */
    void updateOrderStatusByPayment(Long orderId, String paymentStatus);

    /**
     * Soft deletes an order.
     *
     * @param id Order ID.
     */
    void deleteOrder(Long id);
}
