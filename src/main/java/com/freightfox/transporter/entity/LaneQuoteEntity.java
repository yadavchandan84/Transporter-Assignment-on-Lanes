package com.freightfox.transporter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "lane_quotes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"transporter_id", "lane_id"}))
public class LaneQuoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id")
    private TransporterEntity transporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lane_id")
    private LaneEntity lane;

    @Column(nullable = false)
    private long quote;

    protected LaneQuoteEntity() {
    }

    public LaneQuoteEntity(TransporterEntity transporter, LaneEntity lane, long quote) {
        this.transporter = transporter;
        this.lane = lane;
        this.quote = quote;
    }

    public Long getId() {
        return id;
    }

    public TransporterEntity getTransporter() {
        return transporter;
    }

    public void setTransporter(TransporterEntity transporter) {
        this.transporter = transporter;
    }

    public LaneEntity getLane() {
        return lane;
    }

    public void setLane(LaneEntity lane) {
        this.lane = lane;
    }

    public long getQuote() {
        return quote;
    }

    public void setQuote(long quote) {
        this.quote = quote;
    }
}
