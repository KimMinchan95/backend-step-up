package dev.training.back.purchase.mapper;

import dev.training.back.purchase.model.Purchase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PurchaseMapper {
    
    int insert(Purchase purchase);

    Purchase findById(@Param("id") Long id);
}
