package org.ashkelyonok.orderservice.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ashkelyonok.orderservice.model.dto.error.ErrorResponseDto;
import org.ashkelyonok.orderservice.model.dto.error.ValidationErrorResponseDto;
import org.ashkelyonok.orderservice.model.dto.request.ItemCreateDto;
import org.ashkelyonok.orderservice.model.dto.request.ItemUpdateDto;
import org.ashkelyonok.orderservice.model.dto.response.ItemResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Items", description = "Product Catalog Management")
public interface ItemControllerApi {

    @Operation(summary = "Create new item", description = "Adds a new product to the catalog.")
    @ApiResponse(responseCode = "201", description = "Item created", content = @Content(schema = @Schema(implementation = ItemResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ValidationErrorResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Item already exists", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    ResponseEntity<ItemResponseDto> createItem(ItemCreateDto dto);

    @Operation(summary = "Get all items", description = "Returns paginated list of items with optional name filter.")
    ResponseEntity<Page<ItemResponseDto>> getItems(
            @Parameter(description = "Filter by name (case insensitive)") String name,
            @Parameter(hidden = true) Pageable pageable);

    @Operation(summary = "Get item by ID")
    @ApiResponse(responseCode = "200", description = "Item found")
    @ApiResponse(responseCode = "404", description = "Item not found")
    ResponseEntity<ItemResponseDto> getItemById(Long id);

    @Operation(summary = "Update item", description = "Updates item details (Price, Name, Quantity).")
    @ApiResponse(responseCode = "200", description = "Item updated")
    @ApiResponse(responseCode = "404", description = "Item not found")
    ResponseEntity<ItemResponseDto> updateItem(Long id, ItemUpdateDto dto);

    @Operation(summary = "Delete item", description = "Permanently removes an item from the catalog.")
    @ApiResponse(responseCode = "204", description = "Item deleted")
    @ApiResponse(responseCode = "404", description = "Item not found")
    ResponseEntity<Void> deleteItem(Long id);
}