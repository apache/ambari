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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.ambari.server.controller.dependencies.ManagedHBaseConsumerLocalMapping;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;

/** Immutable output of a request-local managed HBase Kerberos calculation. */
public final class ManagedHBaseKerberosCalculation {
  private final KerberosDescriptor descriptor;
  private final Map<String, Map<String, String>> configurations;
  private final ManagedHBaseConsumerLocalMapping consumerLocalMapping;

  public ManagedHBaseKerberosCalculation(KerberosDescriptor descriptor,
      Map<String, Map<String, String>> configurations,
      ManagedHBaseConsumerLocalMapping consumerLocalMapping) {
    KerberosDescriptorFactory factory = new KerberosDescriptorFactory();
    this.descriptor = factory.createInstance(
        Objects.requireNonNull(descriptor, "descriptor").toMap());
    this.configurations = immutableDeepCopy(
        Objects.requireNonNull(configurations, "configurations"));
    this.consumerLocalMapping = Objects.requireNonNull(consumerLocalMapping,
        "consumerLocalMapping");
  }

  /** Returns a fresh descriptor because descriptor model instances are mutable. */
  public KerberosDescriptor detachedDescriptor() {
    return new KerberosDescriptorFactory().createInstance(descriptor.toMap());
  }

  public Map<String, Map<String, String>> configurations() {
    return configurations;
  }

  public ManagedHBaseConsumerLocalMapping consumerLocalMapping() {
    return consumerLocalMapping;
  }

  public String realm() {
    return consumerLocalMapping.realm();
  }

  public String effectiveShortUser() {
    return consumerLocalMapping.proof().effectiveShortUser();
  }

  public String rolePrincipalPattern() {
    return consumerLocalMapping.rolePrincipalPattern();
  }

  public String headlessPrincipal() {
    return consumerLocalMapping.headlessPrincipal();
  }

  public String smokePrincipal() {
    return consumerLocalMapping.smokePrincipal();
  }

  public String mappingProfileFingerprint() {
    return consumerLocalMapping.profileFingerprint();
  }

  private static Map<String, Map<String, String>> immutableDeepCopy(
      Map<String, Map<String, String>> source) {
    Map<String, Map<String, String>> copy = new TreeMap<>();
    for (Map.Entry<String, Map<String, String>> entry : source.entrySet()) {
      if (entry.getKey() == null || entry.getValue() == null) {
        throw new IllegalArgumentException("configurations must not contain nulls");
      }
      copy.put(entry.getKey(), Collections.unmodifiableMap(new TreeMap<>(entry.getValue())));
    }
    return Collections.unmodifiableMap(copy);
  }

  @Override
  public String toString() {
    return "ManagedHBaseKerberosCalculation[descriptor=redacted, configurations=redacted, "
        + "consumerLocalMapping=" + consumerLocalMapping + "]";
  }
}
