package org.ashkelyonok.orderservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wrapper DTO for paginated order responses")
public class OrderPageResponseDto {

    @Schema(description = "List of orders on the current page")
    private List<OrderResponseDto> content;

    @Schema(description = "Current page number (0-indexed)",
            example = "0")
    private int pageNumber;

    @Schema(description = "Number of items per page",
            example = "10")
    private int pageSize;

    @Schema(description = "Total number of elements across all pages",
            example = "50")
    private long totalElements;

    @Schema(description = "Total number of pages",
            example = "5")
    private int totalPages;
}