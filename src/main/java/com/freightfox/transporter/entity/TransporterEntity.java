package com.freightfox.transporter.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transporters")
public class TransporterEntity {

    @Id
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lane.id")
    private List<LaneQuoteEntity> laneQuotes = new ArrayList<>();

    protected TransporterEntity() {
    }

    public TransporterEntity(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addQuote(LaneEntity lane, long quote) {
        laneQuotes.add(new LaneQuoteEntity(this, lane, quote));
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LaneQuoteEntity> getLaneQuotes() {
        return laneQuotes;
    }

    public void setLaneQuotes(List<LaneQuoteEntity> laneQuotes) {
        this.laneQuotes = laneQuotes;
    }
}
