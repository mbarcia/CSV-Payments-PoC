package com.example.poc.domain;

import com.opencsv.bean.CsvIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @CsvIgnore
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    @GeneratedValue
    // UUID version 7 (Hibernate 6.5+)
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    protected void setId(UUID id) {
        this.id = id;
    }
}
