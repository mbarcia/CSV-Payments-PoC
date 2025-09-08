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

package io.github.mbarcia.pipeline.service;

import io.smallrye.mutiny.Multi;

public abstract class GrpcServiceStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

  protected abstract ReactiveStreamingService<DomainIn, DomainOut> getService();

  protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  protected abstract GrpcOut toGrpc(DomainOut domainOut);

  public Multi<GrpcOut> remoteProcess(GrpcIn grpcRequest) {

    return getService()
        .process(fromGrpc(grpcRequest)) // Multi<DomainOut>
        .onItem()
        .transform(this::toGrpc) // Multi<GrpcOut>
        .onFailure()
        .transform(new throwStatusRuntimeExceptionFunction());
  }
}
