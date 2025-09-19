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
import io.github.mbarcia.csv.common.domain.PaymentRecord;
import java.util.UUID;
import lombok.*;

@Value
@Builder
@JsonDeserialize(builder = AckPaymentSentDto.AckPaymentSentDtoBuilder.class)
public class AckPaymentSentDto {
  UUID id;
  UUID conversationId;
  UUID paymentRecordId;
  PaymentRecord paymentRecord;
  String message;
  Long status;

  // Lombok will generate the builder, but Jackson needs to know how to interpret it
  @JsonPOJOBuilder(withPrefix = "")
  public static class AckPaymentSentDtoBuilder {}
}