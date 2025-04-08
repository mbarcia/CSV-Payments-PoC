package com.example.poc.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opencsv.bean.CsvIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @CsvIgnore
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    @GeneratedValue
    // UUID version 7 (Hibernate 6.5+)
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @JsonIgnore
    public UUID getId() {
        return id;
    }

    protected void setId(UUID id) {
        this.id = id;
    }
}
