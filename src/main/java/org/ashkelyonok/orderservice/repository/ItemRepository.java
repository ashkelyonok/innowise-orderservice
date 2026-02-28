package org.ashkelyonok.orderservice.repository;

import org.ashkelyonok.orderservice.model.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    @Modifying
    @Query("UPDATE Item i SET i.deleted = true, i.updatedAt = CURRENT_TIMESTAMP WHERE i.id = :id")
    int softDeleteById(@Param("id") Long id);

    Optional<Item> findByIdAndDeletedFalse(Long id);

    @Query("SELECT i FROM Item i WHERE i.deleted = false AND i.id IN :ids")
    List<Item> findByIdInAndDeletedFalse(@Param("ids") Collection<Long> ids);
}