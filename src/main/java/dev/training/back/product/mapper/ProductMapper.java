package dev.training.back.product.mapper;

import dev.training.back.product.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<Product> findAll();

    Product findById(@Param("id") Long id);

    int decreaseQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);
}
