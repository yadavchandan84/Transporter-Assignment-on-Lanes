package com.freightfox.transporter.repository;

import com.freightfox.transporter.entity.LaneQuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaneQuoteRepository extends JpaRepository<LaneQuoteEntity, Long> {
}
