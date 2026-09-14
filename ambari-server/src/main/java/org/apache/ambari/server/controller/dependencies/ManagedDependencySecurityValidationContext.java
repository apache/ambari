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
package org.apache.ambari.server.controller.dependencies;

import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.PolicySource;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Transient trusted inputs for secure snapshot construction. */
public record ManagedDependencySecurityValidationContext(
    ManagedHBaseConsumerLocalMapping consumerMapping,
    ManagedDependencyProviderSecurityProof providerPolicy,
    @JsonIgnore PolicySource hdfsPolicySource) {

  public static ManagedDependencySecurityValidationContext none() {
    return new ManagedDependencySecurityValidationContext(null, null, null);
  }

  public boolean isEmpty() {
    return consumerMapping == null && providerPolicy == null && hdfsPolicySource == null;
  }

  @Override
  public String toString() {
    return "ManagedDependencySecurityValidationContext[consumerMapping="
        + (consumerMapping == null ? "absent" : consumerMapping.profileFingerprint())
        + ", providerPolicy=" + providerPolicy + ", hdfsPolicySource=redacted]";
  }
}
