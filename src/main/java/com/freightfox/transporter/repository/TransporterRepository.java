package com.freightfox.transporter.repository;

import com.freightfox.transporter.entity.TransporterEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransporterRepository extends JpaRepository<TransporterEntity, Integer> {

    /** All transporters with their quotes and lanes in a single query, ordered by transporter id. */
    @Query("select distinct t from TransporterEntity t left join fetch t.laneQuotes q left join fetch q.lane order by t.id")
    List<TransporterEntity> findAllWithQuotes();
}
