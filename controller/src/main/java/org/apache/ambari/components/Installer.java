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
import java.util.List;

import org.apache.ambari.common.rest.entities.agent.Command;

/**
 * The generic interface for component installers.
 * Used for installing and removing cluster software deployments.
 */
public abstract class Installer {

  /**
   * Generate commands that will install the required software on
   * each node in the cluster.
   * @param cluster the information about the cluster.
   * @return a list of commands for each node's agent to execute.
   * @throws IOException
   */
  public abstract List<Command> install(ClusterContext cluster
                                        ) throws IOException;
  
  /**
   * Generate commands that will uninstall the software from each node in the
   * cluster
   * @param cluster the cluster definition to remove
   * @return the commands for each node's agent to execute
   * @throws IOException
   */
  public abstract List<Command> remove(ClusterContext cluster
                                       ) throws IOException;
  
}
