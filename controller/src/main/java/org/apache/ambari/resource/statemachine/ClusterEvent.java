/**
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
package org.apache.ambari.resource.statemachine;

import org.apache.ambari.event.AbstractEvent;

public class ClusterEvent extends AbstractEvent<ClusterEventType> {
  private Cluster cluster;
  private Service service;
  public ClusterEvent(ClusterEventType type, Cluster cluster) {
    super(type);
    this.cluster = cluster;
  }
  //Need this to create an event that has details about the service
  //that moved into a different state
  public ClusterEvent(ClusterEventType type, Cluster cluster, Service service) {
    super(type);
    this.cluster = cluster;
    this.service = service;
  }
  public Cluster getCluster() {
    return cluster;
  }
  public Service getServiceCausingTransition() {
    return service;
  }
}
