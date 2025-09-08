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

package io.github.mbarcia.csv.common.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public abstract class GrpcServiceClientStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

  protected abstract ReactiveStreamingClientService<DomainIn, DomainOut> getService();

  protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  protected abstract GrpcOut toGrpc(DomainOut domainOut);

  public Uni<GrpcOut> remoteProcess(Multi<GrpcIn> requestStream) {

    return getService()
        .process(
            requestStream.onItem().transform(this::fromGrpc)) // Multi<DomainIn> → Uni<DomainOut>
        .onItem()
        .transform(this::toGrpc) // Uni<GrpcOut>
        .onFailure()
        .transform(new throwStatusRuntimeExceptionFunction());
  }
}
