package com.example.poc.common.domain;

import com.opencsv.bean.CsvIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

    @Id
    @CsvIgnore
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    public UUID id;

    public BaseEntity() {
        id = UUID.randomUUID();
    }
}
