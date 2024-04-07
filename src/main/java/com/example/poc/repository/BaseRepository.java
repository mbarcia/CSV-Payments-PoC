package com.example.poc.repository;

import com.example.poc.domain.BasePersistable;
import org.springframework.data.repository.CrudRepository;

public interface BaseRepository<T extends BasePersistable> extends CrudRepository<T, Long> {}
