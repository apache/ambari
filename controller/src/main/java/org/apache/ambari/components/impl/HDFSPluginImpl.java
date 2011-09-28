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

import java.io.IOException;
import java.util.List;

import org.apache.ambari.common.rest.entities.agent.Action;
import org.apache.ambari.common.rest.entities.agent.Command;
import org.apache.ambari.components.ClusterContext;
import org.apache.ambari.components.ComponentPlugin;

public class HDFSPluginImpl extends ComponentPlugin {

  @Override
  public String[] getRoles() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getRequiredComponents() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isService() throws IOException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<Action> writeConfiguration(ClusterContext cluster)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Action> install(ClusterContext cluster) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Action> uninstall(ClusterContext cluster) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Action> startRoleServer(ClusterContext cluster, String role)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Action> stopRoleServer(ClusterContext cluster, String role)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

}
