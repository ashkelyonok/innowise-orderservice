package org.ashkelyonok.orderservice.repository.spec;

import lombok.experimental.UtilityClass;
import org.ashkelyonok.orderservice.model.entity.Order;
import org.ashkelyonok.orderservice.model.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Set;

@UtilityClass
public class OrderSpecification {

    public static Specification<Order> filterBy(LocalDateTime fromDate, LocalDateTime toDate, Set<OrderStatus> statuses) {
        return Specification.<Order>where(null)
                .and(SpecificationBuilder.between("createdAt", fromDate, toDate))
                .and(SpecificationBuilder.attributeIn("status", statuses))
                .and(SpecificationBuilder.attributeEquals("deleted", false));
    }
}