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

import java.util.Optional;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;

/**
 * Reads the persisted, approved identity, namespace, and consumer mapping lineage for a live
 * cluster in one consistent operation.
 *
 * <p>An empty result means that the cluster has no active secure managed HBase binding. An
 * active binding with missing, stale, unapproved, or inconsistent security facts must fail with
 * {@link AmbariException}; it must never be represented by an empty result. Implementations must
 * not call {@code KerberosHelper}, descriptor resolution, or configuration calculation.</p>
 */
@FunctionalInterface
public interface ManagedHBaseKerberosLivePlanProvider {
  Optional<ManagedHBaseKerberosLivePlan> findApprovedLivePlan(Cluster cluster)
      throws AmbariException;
}
