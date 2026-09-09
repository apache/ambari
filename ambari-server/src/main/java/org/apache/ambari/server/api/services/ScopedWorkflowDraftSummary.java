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

import java.util.LinkedHashMap;
import java.util.Map;

/** Credential-free summary used by the owned creation draft directory. */
public class ScopedWorkflowDraftSummary {
  private final String draftId;
  private final long revision;
  private final String workflow;
  private final String phase;
  private final Long clusterId;
  private final String clusterName;

  public ScopedWorkflowDraftSummary(String draftId, long revision, String workflow, String phase,
      Long clusterId, String clusterName) {
    this.draftId = draftId;
    this.revision = revision;
    this.workflow = workflow;
    this.phase = phase;
    this.clusterId = clusterId;
    this.clusterName = clusterName;
  }

  public Map<String, Object> toResponse() {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("draft_id", draftId);
    response.put("revision", revision);
    response.put("workflow", workflow);
    response.put("phase", phase);
    if (clusterId != null) {
      response.put("cluster_id", clusterId);
      response.put("cluster_name", clusterName);
    }
    return response;
  }
}
