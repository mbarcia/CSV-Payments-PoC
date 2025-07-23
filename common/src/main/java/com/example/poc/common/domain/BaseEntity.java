package com.example.poc.common.domain;

import static io.quarkus.hibernate.reactive.panache.Panache.withTransaction;

import com.opencsv.bean.CsvIgnore;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

  @Id
  @CsvIgnore
  @Column(name = "id", updatable = false, nullable = false)
  public UUID id;

  public BaseEntity() {
    id = UUID.randomUUID();
  }

  public Uni<Void> save() {
    return withTransaction(() -> this.persist().replaceWithVoid());
  }
}
