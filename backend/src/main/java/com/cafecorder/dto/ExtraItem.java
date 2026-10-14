package com.cafecorder.dto;

import com.cafecorder.model.Extra;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

// One extra item, e.g. { "name": "Almond croissant", "price": 4.25 }. Used in both requests and responses.
public record ExtraItem(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.00") @Digits(integer = 5, fraction = 2) BigDecimal price
) {
    public static List<ExtraItem> from(List<Extra> extras) {
        return extras.stream().map(e -> new ExtraItem(e.getName(), e.getPrice())).toList();
    }
}
