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
package org.apache.ambari.server.mpack;

import java.util.Map;

/** Stable machine-readable lifecycle errors, independent of diagnostic text. */
public class MpackException extends RuntimeException {
  public enum Code {
    INVALID_MANIFEST, UNSUPPORTED_SCHEMA, INVALID_ARCHIVE, UPLOAD_LIMIT,
    DIGEST_MISMATCH, RELEASE_CONFLICT, RESOURCE_CONFLICT, DEPENDENCY_MISSING,
    DEPENDENCY_AMBIGUOUS, DEPENDENCY_CYCLE, VERSION_INCOMPATIBLE, INVALID_TARGET,
    STALE_PLAN, IDEMPOTENCY_CONFLICT, OPERATION_CONFLICT, RESOURCE_IN_USE,
    NOT_FOUND, INVALID_RECEIPT, RECOVERY_REQUIRED, MAINTENANCE_REQUIRED,
    UNSUPPORTED_OPERATION, STORAGE_FAILURE
  }

  private final Code code;
  private final Map<String, Object> details;

  public MpackException(Code code, String message) {
    this(code, message, Map.of());
  }

  public MpackException(Code code, String message, Map<String, Object> details) {
    super(message);
    this.code = code;
    this.details = Map.copyOf(details);
  }

  public Code getCode() {
    return code;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}
