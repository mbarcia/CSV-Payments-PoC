package com.example.poc.repository;

import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    @Query("select r, ack, pst from PaymentRecord r" +
            " inner join fetch r.ackPaymentSent ack" +
            " left join fetch ack.paymentStatus pst" +
            " where r.csvPaymentsFile = ?1")
    List<PaymentRecord> findAllByFile(CsvPaymentsFile csvPaymentsFile);
}
