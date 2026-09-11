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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScopedWorkflowState {
  private long revision;
  private String owner;
  private String workflow;
  private String phase;
  private Map<String, Object> values;

  public ScopedWorkflowState() {
    values = new LinkedHashMap<>();
  }

  public ScopedWorkflowState(long revision, String owner, String workflow, String phase,
      Map<String, Object> values) {
    this.revision = revision;
    this.owner = owner;
    this.workflow = workflow;
    this.phase = phase;
    this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
  }

  public static ScopedWorkflowState empty() {
    return new ScopedWorkflowState(0, null, "IDLE", "IDLE", Collections.emptyMap());
  }

  public long getRevision() {
    return revision;
  }

  public void setRevision(long revision) {
    this.revision = revision;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getWorkflow() {
    return workflow;
  }

  public void setWorkflow(String workflow) {
    this.workflow = workflow;
  }

  public String getPhase() {
    return phase;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

  public Map<String, Object> getValues() {
    return values;
  }

  public void setValues(Map<String, Object> values) {
    this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
  }

  public Map<String, Object> toResponse(boolean summary) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("revision", revision);
    response.put("owner", owner);
    response.put("workflow", workflow);
    response.put("phase", phase);
    if (!summary) {
      response.put("values", values == null ? Collections.emptyMap() : values);
    }
    return response;
  }
}
