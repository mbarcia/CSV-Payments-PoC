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

package io.github.mbarcia.csv.common.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.github.mbarcia.csv.common.domain.AckPaymentSent;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = PaymentStatusDto.PaymentStatusDtoBuilder.class)
public class PaymentStatusDto {
  public UUID id;
  public String reference;
  public String status;
  public String message;
  public BigDecimal fee;
  public UUID ackPaymentSentId;
  public AckPaymentSent ackPaymentSent;

  // Lombok will generate the builder, but Jackson needs to know how to interpret it
  @JsonPOJOBuilder(withPrefix = "")
  public static class PaymentStatusDtoBuilder {}
}
