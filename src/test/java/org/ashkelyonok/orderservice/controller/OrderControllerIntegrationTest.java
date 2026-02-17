package org.ashkelyonok.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.ashkelyonok.orderservice.AbstractIntegrationTest;
import org.ashkelyonok.orderservice.model.dto.request.OrderCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.OrderItemCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.OrderUpdateStatusDto;
import org.ashkelyonok.orderservice.model.entity.Item;
import org.ashkelyonok.orderservice.model.entity.Order;
import org.ashkelyonok.orderservice.model.enums.OrderStatus;
import org.ashkelyonok.orderservice.model.event.PaymentStatusEvent;
import org.ashkelyonok.orderservice.repository.ItemRepository;
import org.ashkelyonok.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    @DisplayName("Create Order: Full Flow")
    void createOrder_ShouldSucceed() throws Exception {
        Item item = itemRepository.save(new Item(null, "RTX 4090", new BigDecimal("1500.00"), LocalDateTime.now(), LocalDateTime.now()));

        stubFor(WireMock.get(urlPathMatching("/api/v1/users/search"))
                .withQueryParam("email", matching(".*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                            { "id": 101, "email": "lanelloyd@gmail.com", "name": "Lane", "surname": "Lloyd" }
                        """)));

        OrderCreateDto dto = new OrderCreateDto();
        dto.setUserEmail("lanelloyd@gmail.com");
        OrderItemCreateDto itemDto = new OrderItemCreateDto();
        itemDto.setItemId(item.getId());
        itemDto.setQuantity(2);
        dto.setItems(List.of(itemDto));

        String token = generateTestToken(101L, "lanelloyd@gmail.com", "ROLE_USER");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CREATED")))
                .andExpect(jsonPath("$.totalPrice", is(3000.00)));
    }

    @Test
    @DisplayName("Exception: Feign Client 404 (User Not Found)")
    void createOrder_UserNotFound_Feign404() throws Exception {
        Item item = itemRepository.save(new Item(null, "Item", BigDecimal.TEN, LocalDateTime.now(), LocalDateTime.now()));

        stubFor(WireMock.get(urlPathMatching("/api/v1/users/search"))
                .willReturn(aResponse().withStatus(404)));

        OrderCreateDto dto = new OrderCreateDto();
        dto.setUserEmail("unknown@test.com");
        dto.setItems(List.of(new OrderItemCreateDto(item.getId(), 1)));

        String token = generateTestToken(1L, "user@test.com", "ROLE_USER");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("External Resource Not Found")));
    }

    @Test
    @DisplayName("Exception: Feign Client 400 (Bad Request)")
    void createOrder_UserBadRequest_Feign400() throws Exception {
        Item item = itemRepository.save(new Item(null, "Item", BigDecimal.TEN, LocalDateTime.now(), LocalDateTime.now()));

        stubFor(WireMock.get(urlPathMatching("/api/v1/users/search"))
                .willReturn(aResponse().withStatus(400)));

        OrderCreateDto dto = new OrderCreateDto();
        dto.setUserEmail("bad-request@test.com");
        dto.setItems(List.of(new OrderItemCreateDto(item.getId(), 1)));

        String token = generateTestToken(1L, "user@test.com", "ROLE_USER");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid External Request")));
    }

    @Test
    @DisplayName("Exception: Feign Client 503 (Service Unavailable)")
    void createOrder_UserServiceDown_Feign503() throws Exception {
        Item item = itemRepository.save(new Item(null, "Item", BigDecimal.TEN, LocalDateTime.now(), LocalDateTime.now()));

        stubFor(WireMock.get(urlPathMatching("/api/v1/users/search"))
                .willReturn(aResponse().withStatus(500)));

        OrderCreateDto dto = new OrderCreateDto();
        dto.setUserEmail("server-error@test.com");
        dto.setItems(List.of(new OrderItemCreateDto(item.getId(), 1)));

        String token = generateTestToken(1L, "user@test.com", "ROLE_USER");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error", is("External Dependency Error")));
    }

    @Test
    @DisplayName("Exception: Validation Error (Missing Fields)")
    void createOrder_ValidationFail() throws Exception {
        OrderCreateDto dto = new OrderCreateDto();

        String token = generateTestToken(1L, "user@test.com", "ROLE_USER");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }

    @Test
    @DisplayName("Get Orders By User ID: Success")
    void getOrdersByUserId_Success() throws Exception {
        Order o1 = new Order();
        o1.setUserId(101L);
        o1.setStatus(OrderStatus.CREATED);
        o1.setTotalPrice(BigDecimal.TEN);
        orderRepository.save(o1);

        Order o2 = new Order();
        o2.setUserId(101L);
        o2.setStatus(OrderStatus.PAID);
        o2.setTotalPrice(BigDecimal.TEN);
        orderRepository.save(o2);

        stubFor(WireMock.get(urlPathMatching("/api/v1/users/101"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{ \"id\": 101, \"name\": \"User101\" }")));

        String token = generateTestToken(101L, "user@test.com", "ROLE_USER");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/user/101")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("Get Orders By User ID: With Filtering")
    void getOrdersByUserId_WithFilter() throws Exception {
        Order paidOrder = new Order();
        paidOrder.setUserId(101L);
        paidOrder.setStatus(OrderStatus.PAID);
        paidOrder.setTotalPrice(BigDecimal.TEN);
        orderRepository.save(paidOrder);

        Order cancelledOrder = new Order();
        cancelledOrder.setUserId(101L);
        cancelledOrder.setStatus(OrderStatus.CANCELLED);
        cancelledOrder.setTotalPrice(BigDecimal.TEN);
        orderRepository.save(cancelledOrder);

        stubFor(WireMock.get(urlPathMatching("/api/v1/users/101"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{ \"id\": 101, \"name\": \"Lane\" }")));

        String token = generateTestToken(101L, "user@test.com", "ROLE_USER");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/user/101")
                        .header("Authorization", token)
                        .param("statuses", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("PAID")));
    }

    @Test
    @DisplayName("Get Orders By User ID: Should Return 403 When User Is Not Owner")
    void getOrdersByUserId_ShouldReturnForbidden_WhenUserIsNotOwner() throws Exception {
        String token = generateTestToken(101L, "user@test.com", "ROLE_USER");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/user/202")
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Global Exception Handler: Type Mismatch")
    void getOrder_TypeMismatch() throws Exception {
        String token = generateTestToken(1L, "user@test.com", "ROLE_USER");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/abc")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Get Order: Should Enrich with User Info")
    void getOrderById_ShouldEnrich() throws Exception {
        Order order = new Order();
        order.setUserId(101L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(BigDecimal.TEN);
        order = orderRepository.save(order);

        stubFor(WireMock.get(urlPathMatching("/api/v1/users/101"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{ \"id\": 101, \"name\": \"EnrichedUser\" }")));

        String token = generateTestToken(101L, "user@test.com", "ROLE_USER");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/" + order.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userInfo.name", is("EnrichedUser")));
    }

    @Test
    @DisplayName("Filter Orders: By Date and Status")
    void filterOrders_ShouldReturnMatching() throws Exception {
        Order match = new Order();
        match.setUserId(1L);
        match.setStatus(OrderStatus.PAID);
        match.setTotalPrice(BigDecimal.TEN);
        match.setCreatedAt(LocalDateTime.now().minusDays(1));
        orderRepository.save(match);

        Order noMatch = new Order();
        noMatch.setUserId(1L);
        noMatch.setStatus(OrderStatus.CANCELLED);
        noMatch.setTotalPrice(BigDecimal.TEN);
        noMatch.setCreatedAt(LocalDateTime.now().minusDays(10));
        orderRepository.save(noMatch);

        String token = generateTestToken(1L, "admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders")
                        .header("Authorization", token)
                        .param("statuses", "PAID")
                        .param("fromDate", LocalDateTime.now().minusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("PAID")));
    }

    @Test
    @DisplayName("Update Status: Success")
    void updateStatus_Success() throws Exception {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(BigDecimal.TEN);
        order = orderRepository.save(order);

        OrderUpdateStatusDto dto = new OrderUpdateStatusDto();
        dto.setStatus(OrderStatus.SHIPPED);

        String token = generateTestToken(1L, "admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + order.getId() + "/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SHIPPED")));
    }

    @Test
    @DisplayName("Update Status: Invalid Transition (Shipped -> Cancelled)")
    void updateOrderStatus_InvalidTransition() throws Exception {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.SHIPPED);
        order.setTotalPrice(BigDecimal.TEN);
        order = orderRepository.save(order);

        OrderUpdateStatusDto dto = new OrderUpdateStatusDto();
        dto.setStatus(OrderStatus.CANCELLED);

        String token = generateTestToken(1L, "admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + order.getId() + "/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Delete Order: Success (Soft Delete)")
    void deleteOrder_Success() throws Exception {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(BigDecimal.TEN);
        order = orderRepository.save(order);

        String token = generateTestToken(1L, "admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/orders/" + order.getId())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/" + order.getId())
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Delete Order: Not Found")
    void deleteOrder_NotFound() throws Exception {
        String token = generateTestToken(1L, "admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/orders/99999")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Kafka Consumer: Process Payment Event")
    void shouldProcessPaymentEvent() {
        Order order = new Order();
        order.setUserId(101L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(BigDecimal.TEN);
        order = orderRepository.save(order);
        Long orderId = order.getId();

        PaymentStatusEvent event = new PaymentStatusEvent(
                orderId, "SUCCESS", "pay_123", LocalDateTime.now()
        );

        kafkaTemplate.send("payment-status-topic", event);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
                    assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
                });
    }
}