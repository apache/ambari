/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller;

import java.util.Map;

import io.swagger.annotations.ApiModelProperty;

/**
 * Response schema for endpoint {@link org.apache.ambari.server.api.services.ServiceService#getArtifact}
 *
 * The interface is not actually implemented, it only carries swagger annotations.
 */
public interface ClusterServiceArtifactResponse {

  @ApiModelProperty(name = "Artifacts")
  public ClusterServiceArtifactResponseInfo getClusterServiceArtifactResponseInfo();

  @ApiModelProperty(name = "artifact_data")
  public Map<String, Object> getArtifactData();

  public interface ClusterServiceArtifactResponseInfo {
    @ApiModelProperty(name = "artifact_name")
    public String getArtifactName();

    @ApiModelProperty(name = "cluster_name")
    public String getClusterName();

    @ApiModelProperty(name = "service_name")
    public String getServiceName();
  }

}
