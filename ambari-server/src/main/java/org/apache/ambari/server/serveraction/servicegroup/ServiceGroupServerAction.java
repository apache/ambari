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

package org.apache.ambari.server.serveraction.servicegroup;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;


public abstract class ServiceGroupServerAction extends AbstractServerAction {

  protected final static Logger LOG = LoggerFactory.getLogger(ServiceGroupServerAction.class);

  /**
   * Gson
   */
  @Inject
  protected Gson gson;

  /**
   * The Cluster that this ServerAction implementation is executing on
   */
  @Inject
  private Clusters clusters = null;

  /**
   * Returns the relevant Cluster object
   *
   * @return the relevant Cluster
   * @throws AmbariException if the Cluster object cannot be retrieved
   */
  protected Cluster getCluster() throws AmbariException {
    Cluster cluster = clusters.getCluster(getClusterName());

    if (cluster == null) {
      throw new AmbariException(String.format("Failed to retrieve cluster for %s", getClusterName()));
    }

    return cluster;
  }

  /**
   * The Clusters object for this KerberosServerAction
   *
   * @return a Clusters object
   */
  protected Clusters getClusters() {
    return clusters;
  }

  /**
   * Returns the relevant cluster's name
   * <p/>
   * Using the data from the execution command, retrieve the relevant cluster's name.
   *
   * @return a String declaring the relevant cluster's name
   * @throws AmbariException if the cluster's name is not available
   */
  protected String getClusterName() throws AmbariException {
    ExecutionCommand executionCommand = getExecutionCommand();
    String clusterName = (executionCommand == null) ? null : executionCommand.getClusterName();

    if ((clusterName == null) || clusterName.isEmpty()) {
      throw new AmbariException("Failed to retrieve the cluster name from the execution command");
    }

    return clusterName;
  }
}