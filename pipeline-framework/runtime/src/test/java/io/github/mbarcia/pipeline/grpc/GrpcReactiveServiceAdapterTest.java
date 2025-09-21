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

package io.github.mbarcia.pipeline.grpc;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.pipeline.service.ReactiveService;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class GrpcReactiveServiceAdapterTest {

    @Mock private ReactiveService<DomainIn, DomainOut> mockReactiveService;

    private GrpcReactiveServiceAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> adapter;

    private static class GrpcIn {}

    private static class GrpcOut {}

    private static class DomainIn {}

    private static class DomainOut {}

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        adapter =
                new GrpcReactiveServiceAdapter<>() {
                    @Override
                    protected ReactiveService<DomainIn, DomainOut> getService() {
                        return mockReactiveService;
                    }

                    @Override
                    protected DomainIn fromGrpc(GrpcIn grpcIn) {
                        return new DomainIn();
                    }

                    @Override
                    protected GrpcOut toGrpc(DomainOut domainOut) {
                        return new GrpcOut();
                    }
                };
    }

    @Test
    void remoteProcess_SuccessPath() {
        GrpcIn grpcRequest = new GrpcIn();
        DomainOut domainOut = new DomainOut();

        Mockito.when(mockReactiveService.process(ArgumentMatchers.any(DomainIn.class)))
                .thenReturn(Uni.createFrom().item(domainOut));

        Uni<GrpcOut> resultUni = adapter.remoteProcess(grpcRequest);

        UniAssertSubscriber<GrpcOut> subscriber =
                resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem();

        assertNotNull(subscriber.getItem());
        Mockito.verify(mockReactiveService).process(ArgumentMatchers.any(DomainIn.class));
    }

    @Test
    void remoteProcess_FailurePath() {
        GrpcIn grpcRequest = new GrpcIn();
        RuntimeException testException = new RuntimeException("Processing failed");

        Mockito.when(mockReactiveService.process(ArgumentMatchers.any(DomainIn.class)))
                .thenReturn(Uni.createFrom().failure(testException));

        Uni<GrpcOut> resultUni = adapter.remoteProcess(grpcRequest);

        UniAssertSubscriber<GrpcOut> subscriber =
                resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitFailure();

        Throwable failure = subscriber.getFailure();
        assertNotNull(failure);
        assertInstanceOf(StatusRuntimeException.class, failure);
        assertEquals("INTERNAL: Processing failed", failure.getMessage());
        assertEquals(testException.getMessage(), failure.getCause().getMessage());
    }
}
