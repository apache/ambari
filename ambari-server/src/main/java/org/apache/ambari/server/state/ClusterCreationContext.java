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
package org.apache.ambari.server.state;

/** Immutable server-validated identity for a cluster creation draft. */
public final class ClusterCreationContext {
  private final int creatorUserId;
  private final String creationDraftId;
  private final String workflowScopeKey;

  public ClusterCreationContext(int creatorUserId, String creationDraftId, String workflowScopeKey) {
    this.creatorUserId = creatorUserId;
    this.creationDraftId = creationDraftId;
    this.workflowScopeKey = workflowScopeKey;
  }

  public int getCreatorUserId() {
    return creatorUserId;
  }

  public String getCreationDraftId() {
    return creationDraftId;
  }

  public String getWorkflowScopeKey() {
    return workflowScopeKey;
  }
}
