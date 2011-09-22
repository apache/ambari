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

import org.apache.ambari.common.rest.entities.Blueprint;
import org.apache.ambari.common.rest.entities.ClusterDefinition;

public interface ClusterContext {

  /**
   * Get the cluster name.
   * @return the name of the cluster
   */
  String getClusterName();
  
  /**
   * Get all of the roles for this component that will be installed on this
   * node.
   * @return a list of all of the roles for this node
   */
  String[] getAllRoles();
  
  /**
   * Get the list of all roles for this component that should be started 
   * running on this node.
   * @return the lists of roles that should be running
   */
  String[] getActiveRoles();
  
  /**
   * Get the directory name for the directory that should contain the software.
   * @return the full pathname of the directory
   */
  String getInstallDirectory();
  
  /**
   * Get the directory name for the configuration directory.
   * @return the full pathname for the directory
   */
  String getConfigDirectory();
  
  /**
   * Get the definition for this cluster.
   * @return the cluster definition
   */
  ClusterDefinition getClusterDefinition();
  
  /**
   * Get the blueprint for this cluster.
   * @return the cluster blueprint
   */
  Blueprint getBlueprint();
  
}
