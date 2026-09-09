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

package org.apache.ambari.server.topology;

/**
 * Wrapper around a TopologyRequest which adds an id that can be used
 * to refer to the persisted entity.
 */
public class PersistedTopologyRequest {
  private final long id;
  private final TopologyRequest request;
  private final Long repositoryVersionId;
  private final String specificationHash;
  private final String provisioningState;

  public PersistedTopologyRequest(long id, TopologyRequest request) {
    this(id, request, null, null, null);
  }

  public PersistedTopologyRequest(long id, TopologyRequest request,
      Long repositoryVersionId, String specificationHash) {
    this(id, request, repositoryVersionId, specificationHash, null);
  }

  public PersistedTopologyRequest(long id, TopologyRequest request,
      Long repositoryVersionId, String specificationHash, String provisioningState) {
    this.id = id;
    this.request = request;
    this.repositoryVersionId = repositoryVersionId;
    this.specificationHash = specificationHash;
    this.provisioningState = provisioningState;
  }

  public long getId() {
    return id;
  }

  public TopologyRequest getRequest() {
    return request;
  }

  public Long getRepositoryVersionId() {
    return repositoryVersionId;
  }

  public String getSpecificationHash() {
    return specificationHash;
  }

  public boolean isCancelled() {
    return org.apache.ambari.server.orm.entities.TopologyRequestEntity.PROVISIONING_STATE_CANCELLED
        .equals(provisioningState);
  }
}
