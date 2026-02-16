package org.ashkelyonok.orderservice.repository.spec;

import jakarta.persistence.criteria.Path;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collection;

@UtilityClass
public class SpecificationBuilder {

    public static <T> Specification<T> likeIgnoreCase(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.trim().isEmpty()) {
                return cb.conjunction();
            }
            Path<String> fieldPath = root.get(field);
            return cb.like(cb.lower(fieldPath), "%" + value.toLowerCase().trim() + "%");
        };
    }

    public static <T> Specification<T> attributeEquals(String field, Object value) {
        return (root, query, cb) -> {
            if (value == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get(field), value);
        };
    }

    public static <T> Specification<T> attributeIn(String field, Collection<?> values) {
        return (root, query, cb) -> {
            if (values == null || values.isEmpty()) {
                return cb.conjunction();
            }
            return root.get(field).in(values);
        };
    }

    public static <T> Specification<T> between(String field, LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) {
                return cb.conjunction();
            }
            if (start != null && end != null) {
                return cb.between(root.get(field), start, end);
            }
            if (start != null) {
                return cb.greaterThanOrEqualTo(root.get(field), start);
            }
            return cb.lessThanOrEqualTo(root.get(field), end);
        };
    }
}
