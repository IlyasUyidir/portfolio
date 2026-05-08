package com.gc2026.portfolio.dto.request;

import com.gc2026.portfolio.domain.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color must be a valid hex code (e.g. #FF0000)")
    private String color; // nullable

    @NotNull(message = "type is required")
    private CategoryType type;
}
