package com.freightfox.transporter.repository;

import com.freightfox.transporter.entity.LaneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaneRepository extends JpaRepository<LaneEntity, Integer> {
}
