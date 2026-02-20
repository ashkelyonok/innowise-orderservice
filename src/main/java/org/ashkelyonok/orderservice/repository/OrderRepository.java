package org.ashkelyonok.orderservice.repository;

import org.ashkelyonok.orderservice.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdAndDeletedFalse(Long id);

    @Modifying
    @Query("UPDATE Order o SET o.deleted = true, o.updatedAt = CURRENT_TIMESTAMP WHERE o.id = :id")
    int softDeleteById(@Param("id") Long id);
}
