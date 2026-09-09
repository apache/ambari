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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.server.controller.dependencies.security;

import java.util.Objects;
import java.util.regex.Pattern;

/** Identity selection and approved consumer mapping lineage from one persisted snapshot read. */
public record ManagedHBaseKerberosLivePlan(
    ManagedHBaseKerberosOverlaySpec overlaySpec,
    String approvedConsumerMappingProfileFingerprint) {
  private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

  public ManagedHBaseKerberosLivePlan {
    overlaySpec = Objects.requireNonNull(overlaySpec, "overlaySpec");
    if (approvedConsumerMappingProfileFingerprint == null
        || !HASH.matcher(approvedConsumerMappingProfileFingerprint).matches()) {
      throw new IllegalArgumentException(
          "approvedConsumerMappingProfileFingerprint must be a SHA-256 fingerprint");
    }
  }
}
