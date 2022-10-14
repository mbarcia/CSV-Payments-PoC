package com.example.poc.repository;

import com.example.poc.domain.CsvPaymentsFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CsvPaymentsFileRepository extends JpaRepository<CsvPaymentsFile, Long> {
    @Override
// This actually worked but didn't actually hydrate the object
//
//    @Query(value = "select file, r, ack, pst from CsvPaymentsFile file " +
//            " inner join fetch file.records r" +
//            " inner join fetch r.ackPaymentSent ack" +
//            " left join fetch ack.paymentStatus pst" +
//            " where file.id = :id")
    Optional<CsvPaymentsFile> findById(Long id);
}
