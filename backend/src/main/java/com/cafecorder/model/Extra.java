package com.cafecorder.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

// Something ordered besides the main drink. An @Embeddable value, not an entity: it has no id
// of its own and only exists as part of a visit (a row in visit_extras).
@Embeddable
public class Extra {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal price;

    protected Extra() {
        // JPA needs a no-arg constructor
    }

    public Extra(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
}
