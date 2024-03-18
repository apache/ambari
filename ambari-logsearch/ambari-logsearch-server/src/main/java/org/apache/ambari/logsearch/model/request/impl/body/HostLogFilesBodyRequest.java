/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.model.request.impl.body;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.model.request.impl.HostLogFilesRequest;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public class HostLogFilesBodyRequest implements HostLogFilesRequest {
  @NotNull
  @JsonProperty(LogSearchConstants.REQUEST_PARAM_HOST_NAME)
  private String hostName;

  @JsonProperty(LogSearchConstants.REQUEST_PARAM_COMPONENT_NAME)
  private String componentName;

  @Nullable
  @JsonProperty(LogSearchConstants.REQUEST_PARAM_CLUSTER_NAMES)
  private String clusters;

  @Override
  public String getHostName() {
    return hostName;
  }

  @Override
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @Override
  public String getComponentName() {
    return componentName;
  }

  @Override
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  @Override
  public String getClusters() {
    return clusters;
  }

  @Override
  public void setClusters(String clusters) {
    this.clusters = clusters;
  }
}
