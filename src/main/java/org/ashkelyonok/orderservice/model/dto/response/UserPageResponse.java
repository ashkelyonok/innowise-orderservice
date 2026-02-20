package org.ashkelyonok.orderservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Wrapper DTO for paginated user responses fetched via Feign")
public class UserPageResponse {

    @Schema(description = "List of user details retrieved from User Service")
    private List<UserResponseDto> content;
}
