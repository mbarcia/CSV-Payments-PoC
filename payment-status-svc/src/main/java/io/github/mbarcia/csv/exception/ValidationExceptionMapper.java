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

package io.github.mbarcia.csv.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = Logger.getLogger(ValidationExceptionMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        LOG.error("Validation error occurred: " + exception.getMessage());
        
        String errorMessage = exception.getConstraintViolations().stream()
            .map(this::formatViolation)
            .collect(Collectors.joining(", "));
            
        LOG.error("Validation violations: " + errorMessage);
        
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"error\": \"Validation failed\", \"message\": \"" + errorMessage + "\"}")
            .build();
    }
    
    private String formatViolation(ConstraintViolation<?> violation) {
        return String.format("Property '%s' %s (value: '%s')", 
            violation.getPropertyPath(), 
            violation.getMessage(),
            violation.getInvalidValue());
    }
}