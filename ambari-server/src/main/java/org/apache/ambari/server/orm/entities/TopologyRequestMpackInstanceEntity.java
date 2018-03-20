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

package org.apache.ambari.server.orm.entities;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@DiscriminatorValue("TopologyRequest")
public class TopologyRequestMpackInstanceEntity extends MpackInstanceEntity {

  @ManyToOne
  @JoinColumn(name = "topology_request_id", referencedColumnName = "id")
  private TopologyRequestEntity topologyRequest;

  /**
   * @return the topology request
   */
  public TopologyRequestEntity getTopologyRequest() {
    return topologyRequest;
  }

  /**
   * @param topologyRequest the topology request
   */
  public void setTopologyRequest(TopologyRequestEntity topologyRequest) {
    this.topologyRequest = topologyRequest;
  }

  @Override
  public String getBlueprintName() {
    return topologyRequest.getBlueprintName();
  }
}
