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
package org.apache.ambari.components;

import java.io.IOException;

import org.apache.ambari.common.rest.agent.Action;

/**
 * An interface for pluggable component definitions.
 */
public abstract class ComponentPlugin {
  
  public abstract String getProvides();
  
  public abstract String getPackage();
  
  /**
   * Get the active roles (ie. with servers) for this component.
   * @return the list of roles in the order that they should be started
   * @throws IOException
   */
  public abstract String[] getActiveRoles() throws IOException;
  
  /**
   * Get the components that this one depends on.
   * @return the list of components that must be installed for this one
   * @throws IOException
   */
  public abstract String[] getRequiredComponents() throws IOException;
  
  /**
   * Get the commands to write the configuration for this component.
   * @param cluster the cluster that is being configured
   * @return the commands to run on each node
   * @throws IOException
   */
  public abstract Action configure(String cluster,
                                   String role) throws IOException;
  
  /**
   * Get the commands to finalize the installation on the machine.
   * @param cluster the cluster that is being installed
   * @return the commands to execute
   * @throws IOException
   */
  public abstract Action install(String cluster,
                                 String role) throws IOException;
  
  /**
   * Get the commands to start a role's server.
   * @param cluster the cluster that is being installed
   * @param role the role that needs to start running its server
   * @return the commands to execute
   * @throws IOException
   */
  public abstract Action startServer(String cluster,
                                     String role
                                     ) throws IOException;
  
  /**
   * Get the role that should run the check availability command.
   * @return the role name
   * @throws IOException
   */
  public abstract String runCheckRole() throws IOException;

  /**
   * Get the role that should run the initialization command.
   * @return the role name
   * @throws IOException
   */
  public abstract String runPreStartRole() throws IOException;

  /**
   * Get the commands to check whether the service is up
   * @param cluster the name of the cluster
   * @param role the role that is being checked
   * @return the commands to run on the agent
   * @throws IOException
   */
  public abstract Action checkService(String cluster, 
                                      String role) throws IOException;

  /**
   * Get the commands to run before the software is uninstalled.
   * @param cluster the cluster that is being uninstalled
   * @return the commands to execute
   * @throws IOException
   */
  public abstract Action uninstall(String cluster,
                                   String role
                                   ) throws IOException;
  
  /**
   * Get the commands to run to preinstall a component
   * For example, MapReduce needs to have certain directories
   * on the HDFS before JobTracker can be started.
   * @param cluster the cluster that is being installed
   * @param role the role that will run the action
   */
  public abstract Action preStartAction(String cluster, 
                              String role) throws IOException;
}
