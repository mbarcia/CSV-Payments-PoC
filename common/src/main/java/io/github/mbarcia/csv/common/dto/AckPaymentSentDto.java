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

import io.github.mbarcia.csv.common.domain.PaymentRecord;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AckPaymentSentDto {
  public UUID id;
  public UUID conversationId;
  public UUID paymentRecordId;
  public PaymentRecord paymentRecord;
  public String message;
  public Long status;
}
