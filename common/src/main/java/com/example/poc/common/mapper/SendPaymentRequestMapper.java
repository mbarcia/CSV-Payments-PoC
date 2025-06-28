package com.example.poc.common.mapper;

import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.grpc.PaymentStatusSvc;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Mapper(componentModel = "cdi", uses = {CommonConverters.class, PaymentStatusMapper.class}, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface SendPaymentRequestMapper {

    @Mapping(source = "amount", target = "amount", qualifiedByName = "stringToBigDecimal")
    @Mapping(source = "currency", target = "currency", qualifiedByName = "stringToCurrency")
    @Mapping(source = "paymentRecordId", target = "paymentRecordId", qualifiedByName = "stringToUUID")
    @Mapping(source = "paymentRecord", target = "paymentRecord")

    SendPaymentRequest fromGrpc(PaymentStatusSvc.SendPaymentRequest grpcRequest);

    @Mapping(source = "amount", target = "amount", qualifiedByName = "bigDecimalToString")
    @Mapping(source = "currency", target = "currency", qualifiedByName = "currencyToString")
    @Mapping(source = "paymentRecordId", target = "paymentRecordId", qualifiedByName = "uuidToString")
    @Mapping(source = "paymentRecord", target = "paymentRecord")
    PaymentStatusSvc.SendPaymentRequest toGrpc(SendPaymentRequest domainIn);

    @Setter
    @Getter
    @Accessors(chain = true)
    class SendPaymentRequest {
        private String msisdn;
        private BigDecimal amount;
        private Currency currency;
        private String reference;
        private String url;
        private UUID paymentRecordId;
        private PaymentRecord paymentRecord;
    }
}