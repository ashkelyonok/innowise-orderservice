package org.ashkelyonok.orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.orderservice.controller.api.OrderControllerApi;
import org.ashkelyonok.orderservice.model.dto.request.OrderCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.OrderUpdateStatusDto;
import org.ashkelyonok.orderservice.model.dto.response.OrderPageResponseDto;
import org.ashkelyonok.orderservice.model.dto.response.OrderResponseDto;
import org.ashkelyonok.orderservice.model.enums.OrderStatus;
import org.ashkelyonok.orderservice.service.OrderService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController implements OrderControllerApi {

    private final OrderService orderService;

    @Override
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody @Valid OrderCreateDto dto) {
        log.info("Received request to create order for email: {}", dto.getUserEmail());
        return new ResponseEntity<>(orderService.createOrder(dto), HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long id) {
        log.debug("Received request to fetch order: {}", id);
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @Override
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderPageResponseDto> filterOrders(
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) Set<OrderStatus> statuses,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Received request to filter orders");
        return ResponseEntity.ok(orderService.filterOrders(fromDate, toDate, statuses, pageable));
    }

    @Override
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<OrderPageResponseDto> getOrdersByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) Set<OrderStatus> statuses,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Received request to fetch orders for user ID: {}", userId);
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId, fromDate, toDate, statuses, pageable));
    }

    @Override
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody @Valid OrderUpdateStatusDto dto) {
        log.info("Received request to update status for order: {}", id);
        return ResponseEntity.ok(orderService.updateOrderStatus(id, dto));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("Received request to delete order: {}", id);
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}