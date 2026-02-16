package org.ashkelyonok.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ashkelyonok.orderservice.AbstractIntegrationTest;
import org.ashkelyonok.orderservice.model.dto.request.ItemCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.ItemUpdateDto;
import org.ashkelyonok.orderservice.model.entity.Item;
import org.ashkelyonok.orderservice.repository.ItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        itemRepository.deleteAll();
    }

    @Test
    @DisplayName("Create Item: Success (201)")
    void createItem_Success() throws Exception {
        ItemCreateDto dto = new ItemCreateDto();
        dto.setName("Integration GPU");
        dto.setPrice(new BigDecimal("999.99"));

        String token = generateTestToken(1L, "admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Integration GPU")));
    }

    @Test
    @DisplayName("Create Item: Fail Validation (400) on Negative Price")
    void createItem_ValidationFail() throws Exception {
        ItemCreateDto dto = new ItemCreateDto();
        dto.setName("Bad Item");
        dto.setPrice(new BigDecimal("-10.00"));

        String token = generateTestToken(1L, "admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Get Items: Success with Pagination and Filtering")
    void getItems_WithFilter() throws Exception {
        itemRepository.save(new Item(null, "Apple iPhone", BigDecimal.TEN, LocalDateTime.now(), LocalDateTime.now()));
        itemRepository.save(new Item(null, "Samsung Galaxy", BigDecimal.TEN, LocalDateTime.now(), LocalDateTime.now()));

        String token = generateTestToken(1L, "user@test.com", "ROLE_USER");

        mockMvc.perform(get("/api/v1/items")
                        .header("Authorization", token)
                        .param("name", "Apple")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Apple iPhone")));
    }

    @Test
    @DisplayName("Get Item By ID: Success (200)")
    void getItemById_Success() throws Exception {
        Item item = itemRepository.save(new Item(null, "Existing Item", BigDecimal.TEN, LocalDateTime.now(), LocalDateTime.now()));
        String token = generateTestToken(2L, "user@test.com", "ROLE_USER");

        mockMvc.perform(get("/api/v1/items/" + item.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Existing Item")));
    }

    @Test
    @DisplayName("Get Item By ID: Not Found (404)")
    void getItemById_NotFound() throws Exception {
        String token = generateTestToken(1L, "user@test.com", "ROLE_USER");

        mockMvc.perform(get("/api/v1/items/99999")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update Item: Success (200)")
    void updateItem_Success() throws Exception {
        Item saved = itemRepository.save(new Item(null, "Old Name", BigDecimal.ONE, LocalDateTime.now(), LocalDateTime.now()));
        String token = generateTestToken(1L, "admin@test.com", "ROLE_ADMIN");

        ItemUpdateDto updateDto = new ItemUpdateDto();
        updateDto.setName("New Name");
        updateDto.setPrice(BigDecimal.TEN);

        mockMvc.perform(put("/api/v1/items/" + saved.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")));
    }

    @Test
    @DisplayName("Update Item: Not Found (404)")
    void updateItem_NotFound() throws Exception {
        String token = generateTestToken(1L, "admin@test.com", "ROLE_ADMIN");
        ItemUpdateDto updateDto = new ItemUpdateDto();
        updateDto.setName("New Name");
        updateDto.setPrice(BigDecimal.TEN);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/items/99999")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Delete Item: Success (204)")
    void deleteItem_Success() throws Exception {
        Item saved = itemRepository.save(new Item(null, "Delete Me", BigDecimal.ONE, LocalDateTime.now(), LocalDateTime.now()));
        String token = generateTestToken(1L, "admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(delete("/api/v1/items/" + saved.getId())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        assertThat(itemRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("Delete Item: Not Found (404)")
    void deleteItem_NotFound() throws Exception {
        String token = generateTestToken(1L, "admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/items/99999")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }
}