package com.example.poc.mapper;

import com.example.poc.grpc.PaymentStatusSvc;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Mapper
public interface SendPaymentRequestMapper {

    @Mapping(source = "amount", target = "amount", qualifiedByName = "stringToBigDecimal")
    @Mapping(source = "currency", target = "currency", qualifiedByName = "stringToCurrency")
    @Mapping(source = "recordId", target = "recordId", qualifiedByName = "stringToUUID")
    SendPaymentRequest fromGrpc(PaymentStatusSvc.SendPaymentRequest grpcRequest);

    @Mapping(source = "amount", target = "amount", qualifiedByName = "bigDecimalToString")
    @Mapping(source = "currency", target = "currency", qualifiedByName = "currencyToString")
    @Mapping(source = "recordId", target = "recordId", qualifiedByName = "uuidToString")
    PaymentStatusSvc.SendPaymentRequest toGrpc(SendPaymentRequest domainIn);

    @Named("stringToBigDecimal")
    static BigDecimal stringToBigDecimal(String amount) {
        return amount != null ? new BigDecimal(amount) : null;
    }

    @Named("bigDecimalToString")
    static String bigDecimalToString(BigDecimal value) {
        return value != null ? value.toPlainString() : "0";
    }

    @Named("stringToCurrency")
    static Currency stringToCurrency(String currency) {
        return currency != null ? Currency.getInstance(currency) : null;
    }

    @Named("currencyToString")
    static String currencyToString(Currency currency) {
        return currency != null ? currency.getCurrencyCode() : "";
    }

    @Named("stringToUUID")
    static UUID stringToUUID(String recordId) {
        return recordId != null ? UUID.fromString(recordId) : null;
    }

    @Named("uuidToString")
    static String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : "";
    }

    @Setter
    @Getter
    @Accessors(chain = true)
    class SendPaymentRequest {
        private String msisdn;
        private BigDecimal amount;
        private Currency currency;
        private String reference;
        private String url;
        private UUID recordId;
    }
}