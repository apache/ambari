/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.api.services;

import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.ambari.server.mpack.MpackException;
import org.apache.ambari.server.mpack.MpackJson;

@Provider
public class MpackExceptionMapper implements ExceptionMapper<MpackException> {
  @Override
  public Response toResponse(MpackException exception) {
    int status = switch (exception.getCode()) {
      case NOT_FOUND -> 404;
      case UPLOAD_LIMIT -> 413;
      case STALE_PLAN, IDEMPOTENCY_CONFLICT, OPERATION_CONFLICT, RELEASE_CONFLICT,
          RESOURCE_CONFLICT, RESOURCE_IN_USE, MAINTENANCE_REQUIRED, RECOVERY_REQUIRED -> 409;
      case STORAGE_FAILURE -> 503;
      default -> 400;
    };
    return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE)
        .entity(MpackJson.tree(Map.of("schema_version", 1, "error", Map.of(
            "code", exception.getCode().name(), "message", exception.getMessage(),
            "details", exception.getDetails())))).build();
  }
}
