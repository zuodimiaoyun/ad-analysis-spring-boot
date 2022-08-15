package com.kiona.analysis.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

/**
 * @author yangshuaichao
 * @date 2022/08/15 15:48
 * @description TODO
 */
@Entity
@Table(name = "google_event")
public class GoogleEvent {
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid default random_uuid()")
    private UUID id;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "no", nullable = false)
    private String no;

    @Column(name = "purchase_event", nullable = false)
    private boolean purchaseEvent;

    @Column(name = "purchase_value", nullable = false)
    private double purchaseValue;

    public UUID getId() {return id;}

    public void setId(UUID id) {this.id = id;}

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public boolean isPurchaseEvent() {
        return purchaseEvent;
    }

    public void setPurchaseEvent(boolean purchaseEvent) {
        this.purchaseEvent = purchaseEvent;
    }

    public double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }
}
