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

public enum ServiceComponentNodeLiveState {
  /**
   * Initial/Clean state
   */
  INIT,
  /**
   * In the process of installing.
   */
  INSTALLING,
  /**
   * Install failed
   */
  INSTALL_FAILED,
  /**
   * State when install completed successfully
   */
  INSTALLED,
  /**
   * In the process of starting.
   */
  STARTING,
  /**
   * Start failed.
   */
  START_FAILED,
  /**
   * State when start completed successfully.
   */
  STARTED,
  /**
   * In the process of stopping.
   */
  STOPPING,
  /**
   * Stop failed
   */
  STOP_FAILED,
  /**
   * In the process of uninstalling.
   */
  UNINSTALLING,
  /**
   * Uninstall failed.
   */
  UNINSTALL_FAILED,
  /**
   * State when uninstall completed successfully.
   */
  UNINSTALLED,
  /**
   * In the process of wiping out the install
   */
  WIPING_OUT,
  /**
   * State when wipeout fails
   */
  WIPEOUT_FAILED
}
