/*
 * Copyright © 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mbarcia.csv.common.mapper;

import io.github.mbarcia.csv.common.domain.PaymentRecord;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.github.mbarcia.pipeline.annotation.MapperForStep;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@MapperForStep(
    order = 8,
    grpc = PaymentStatusSvc.SendPaymentRequest.class
)
@Mapper(
    componentModel = "cdi",
    uses = {CommonConverters.class, PaymentRecordMapper.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface SendPaymentRequestMapper {

  SendPaymentRequestMapper INSTANCE = Mappers.getMapper( SendPaymentRequestMapper.class );

  @Mapping(source = "amount", target = "amount", qualifiedByName = "stringToBigDecimal")
  @Mapping(source = "currency", target = "currency", qualifiedByName = "stringToCurrency")
  @Mapping(source = "paymentRecordId", target = "paymentRecordId", qualifiedByName = "stringToUUID")
  @Mapping(target = "paymentRecord")
  SendPaymentRequest fromGrpc(PaymentStatusSvc.SendPaymentRequest grpcRequest);

  @Mapping(source = "amount", target = "amount", qualifiedByName = "bigDecimalToString")
  @Mapping(source = "currency", target = "currency", qualifiedByName = "currencyToString")
  @Mapping(source = "paymentRecordId", target = "paymentRecordId", qualifiedByName = "uuidToString")
  @Mapping(target = "paymentRecord")
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
