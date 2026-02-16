package org.ashkelyonok.orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.orderservice.controller.api.ItemControllerApi;
import org.ashkelyonok.orderservice.model.dto.request.ItemCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.ItemUpdateDto;
import org.ashkelyonok.orderservice.model.dto.response.ItemResponseDto;
import org.ashkelyonok.orderservice.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
public class ItemController implements ItemControllerApi {

    private final ItemService itemService;

    @Override
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponseDto> createItem(@RequestBody @Valid ItemCreateDto dto) {
        log.info("Received request to create item: {}", dto.getName());
        return new ResponseEntity<>(itemService.createItem(dto), HttpStatus.CREATED);
    }

    @Override
    @GetMapping
    public ResponseEntity<Page<ItemResponseDto>> getItems(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Received request to fetch items. Filter: {}", name);
        return ResponseEntity.ok(itemService.getItems(name, pageable));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDto> getItemById(@PathVariable Long id) {
        log.debug("Received request to fetch item: {}", id);
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponseDto> updateItem(
            @PathVariable Long id,
            @RequestBody @Valid ItemUpdateDto dto) {
        log.info("Received request to update item: {}", id);
        return ResponseEntity.ok(itemService.updateItem(id, dto));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        log.info("Received request to delete item: {}", id);
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
