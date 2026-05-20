package com.ddd.webbb.category.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Long> {

    Optional<BoardCategory> findFirstByIsActiveTrueOrderBySortOrderAsc();
}
