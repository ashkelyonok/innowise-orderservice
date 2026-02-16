package org.ashkelyonok.orderservice.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ashkelyonok.orderservice.model.dto.error.ErrorResponseDto;
import org.ashkelyonok.orderservice.model.dto.error.ValidationErrorResponseDto;
import org.ashkelyonok.orderservice.model.dto.request.OrderCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.OrderUpdateStatusDto;
import org.ashkelyonok.orderservice.model.dto.response.OrderPageResponseDto;
import org.ashkelyonok.orderservice.model.dto.response.OrderResponseDto;
import org.ashkelyonok.orderservice.model.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Set;

@Tag(name = "Orders", description = "Order Processing and Management")
public interface OrderControllerApi {

    @Operation(summary = "Create new order", description = "Resolves User by Email, validates items, and places an order.")
    @ApiResponse(responseCode = "201", description = "Order placed successfully", content = @Content(schema = @Schema(implementation = OrderResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed / Invalid Operation", content = @Content(schema = @Schema(implementation = ValidationErrorResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Item not found", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    @ApiResponse(responseCode = "503", description = "User Service unavailable or User not found", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    ResponseEntity<OrderResponseDto> createOrder(OrderCreateDto dto);

    @Operation(summary = "Get order by ID", description = "Fetches order details including enriched User Info.")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    ResponseEntity<OrderResponseDto> getOrderById(Long id);

    @Operation(summary = "Search orders", description = "Filter orders by Date Range and Statuses.")
    ResponseEntity<OrderPageResponseDto> filterOrders(
            @Parameter(description = "Start Date (ISO-8601)") LocalDateTime fromDate,
            @Parameter(description = "End Date (ISO-8601)") LocalDateTime toDate,
            @Parameter(description = "List of statuses") Set<OrderStatus> statuses,
            @Parameter(hidden = true) Pageable pageable);

    @Operation(summary = "Get user's orders", description = "Fetches history for a specific user ID with optional filtering.")
    ResponseEntity<OrderPageResponseDto> getOrdersByUserId(
            @Parameter(description = "User ID") Long userId,
            @Parameter(description = "Filter by creation date (start)") LocalDateTime fromDate,
            @Parameter(description = "Filter by creation date (end)") LocalDateTime toDate,
            @Parameter(description = "Filter by order statuses") Set<OrderStatus> statuses,
            @Parameter(hidden = true) Pageable pageable);

    @Operation(summary = "Update order status", description = "Transitions order state (e.g., PAID -> SHIPPED). Enforces state machine logic.")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "400", description = "Invalid status transition")
    @ApiResponse(responseCode = "404", description = "Order not found")
    ResponseEntity<OrderResponseDto> updateOrderStatus(Long id, OrderUpdateStatusDto dto);

    @Operation(summary = "Delete order", description = "Performs a soft delete.")
    @ApiResponse(responseCode = "204", description = "Order deleted")
    @ApiResponse(responseCode = "404", description = "Order not found")
    ResponseEntity<Void> deleteOrder(Long id);
}