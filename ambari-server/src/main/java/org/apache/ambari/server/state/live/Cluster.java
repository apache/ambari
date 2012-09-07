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

package org.apache.ambari.server.state.live;

import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.node.NodeEvent;
import org.apache.ambari.server.state.live.node.NodeState;

public interface Cluster {

  /**
   * Get the State for a given Node
   * @param nodeName Node hostname for which to retrieve state
   * @return
   */
  public NodeState getNodeState(String nodeName);
  
  /**
   * Set the State for a given Node
   * @param nodeName Node's hostname for which state is to be set
   * @param state NodeState to set
   */
  public void setNodeState(String nodeName, NodeState state);
  
  /**
   * Send event to the given Node
   * @param nodeName Node's hostname
   * @param event Event to be handled
   */
  public void handleNodeEvent(String nodeName, NodeEvent event)
      throws InvalidStateTransitonException;
  
  /**
   * Get the State for a given ServiceComponentNode
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param nodeName Node name
   * @return ServiceComponentNodeState
   */
  public ServiceComponentNodeState getServiceComponentNodeState(String service,
      String serviceComponent, String nodeName);

  /**
   * Set the State for a given ServiceComponentNode
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param nodeName Node name
   * @param state State to set
   */
  public void setServiceComponentNodeState(String service,
      String serviceComponent, String nodeName,
      ServiceComponentNodeState state);

  /**
   * Send an Event to a given ServiceComponentNode
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param nodeName Node name
   * @param event Event to be handled
   */
  public void handleServiceComponentNodeEvent(String service,
      String serviceComponent, String nodeName,
      ServiceComponentNodeEvent event) throws InvalidStateTransitonException;
  
}
