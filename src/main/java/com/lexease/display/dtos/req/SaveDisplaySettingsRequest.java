package com.lexease.display.dtos.req;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SaveDisplaySettingsRequest(
        @NotBlank @Size(max = 80) String fontFamily,
        @Min(14) @Max(40) int fontSize,
        @NotNull @DecimalMin("1.00") @DecimalMax("3.00") @Digits(integer = 1, fraction = 2) BigDecimal lineHeight,
        @NotNull @DecimalMin("0.00") @DecimalMax("0.50") @Digits(integer = 1, fraction = 2) BigDecimal letterSpacing,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String backgroundColor,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String textColor,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String highlightBackgroundColor,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String highlightTextColor,
        @Size(max = 80) String themeName
) {
}
