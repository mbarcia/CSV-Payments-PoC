package com.example.poc.mapper;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.grpc.PaymentsProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "cdi", uses = {PaymentRecord.class, PaymentStatusMapper.class}, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface AckPaymentSentMapper {

    @Mapping(source = "id", target = "id", qualifiedByName = "toUUID")
    @Mapping(source = "paymentStatusId", target = "paymentStatusId", qualifiedByName = "toUUID")
    @Mapping(source = "paymentRecordId", target = "paymentRecordId", qualifiedByName = "toUUID")
    @Mapping(source = "paymentRecord", target = "paymentRecord")
    @Mapping(source = "paymentStatus", target = "paymentStatus")
    AckPaymentSent fromGrpc(PaymentsProcessingSvc.AckPaymentSent proto);

    @Mapping(source = "id", target = "id", qualifiedByName = "toString")
    @Mapping(source = "paymentRecordId", target = "paymentRecordId", qualifiedByName = "toString")
    @Mapping(source = "paymentStatusId", target = "paymentStatusId", qualifiedByName = "toString")
    @Mapping(source = "paymentRecord", target = "paymentRecord")
    @Mapping(source = "paymentStatus", target = "paymentStatus")
    PaymentsProcessingSvc.AckPaymentSent toGrpc(AckPaymentSent domain);

    @Named("toUUID")
    static UUID toUUID(String id) {
        return id == null || id.isBlank() ? null : UUID.fromString(id);
    }

    @Named("toString")
    static String toString(UUID id) {
        return id == null ? "" : id.toString();
    }
}
