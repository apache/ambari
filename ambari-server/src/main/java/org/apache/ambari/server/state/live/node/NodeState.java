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

package org.apache.ambari.server.state.live.node;

public enum NodeState {
  /**
   * New node state
   */
  INIT,
  /**
   * State when a registration request is received from the Node but
   * the node has not been verified/authenticated.
   */
  WAITING_FOR_VERIFICATION,
  /**
   * State when the node has been verified/authenticated
   */
  VERIFIED,
  /**
   * State when the server is receiving heartbeats regularly from the Node
   * and the state of the Node is healthy
   */
  HEALTHY,
  /**
   * State when the server has not received a heartbeat from the Node in the
   * configured heartbeat expiry window.
   */
  HEARTBEAT_LOST,
  /**
   * Node is in unhealthy state as reported either by the Node itself or via
   * any other additional means ( monitoring layer )
   */
  UNHEALTHY
}
