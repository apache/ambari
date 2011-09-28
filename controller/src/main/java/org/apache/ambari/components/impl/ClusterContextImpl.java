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
package org.apache.ambari.components.impl;

import java.util.List;

import org.apache.ambari.common.rest.entities.Blueprint;
import org.apache.ambari.common.rest.entities.Cluster;
import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.components.ClusterContext;

public class ClusterContextImpl implements ClusterContext {

  Cluster cluster;
  Node node;
  
  public ClusterContextImpl(Cluster cluster, Node node) {
    this.cluster = cluster;
    this.node = node;
  }
  
  @Override
  public String getClusterName() {
    return cluster.getClusterDefinition().getName();
  }

  @Override
  public String[] getAllRoles() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getInstallDirectory() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getConfigDirectory() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ClusterDefinition getClusterDefinition() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Blueprint getBlueprint() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getClusterComponents() {
    List<String> roles = cluster.getClusterDefinition().getActiveServices();
    return roles.toArray(new String[1]);
  }

}
