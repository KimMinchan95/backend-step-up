package dev.training.back.product.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product {

    private Long id;
    private String name;
    private Integer price;
    private Integer stock;
}
