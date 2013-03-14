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

package org.apache.ambari.server.state;

public enum State {
  /**
   * Initial/Clean state.
   */
  INIT(0),
  /**
   * In the process of installing.
   */
  INSTALLING(1),
  /**
   * Install failed.
   */
  INSTALL_FAILED(2),
  /**
   * State when install completed successfully.
   */
  INSTALLED(3),
  /**
   * In the process of starting.
   */
  STARTING(4),
  /**
   * Start failed.
   */
  START_FAILED(5),
  /**
   * State when start completed successfully.
   */
  STARTED(6),
  /**
   * In the process of stopping.
   */
  STOPPING(7),
  /**
   * Stop failed.
   */
  STOP_FAILED(8),
  /**
   * In the process of uninstalling.
   */
  UNINSTALLING(9),
  /**
   * Uninstall failed.
   */
  UNINSTALL_FAILED(10),
  /**
   * State when uninstall completed successfully.
   */
  UNINSTALLED(11),
  /**
   * In the process of wiping out the install.
   */
  WIPING_OUT(12),
  /**
   * State when wipeout fails.
   */
  WIPEOUT_FAILED(13),
  /**
   * In the process of upgrading the deployed bits.
   */
  UPGRADING(14),
  /**
   * Upgrade has failed.
   */
  UPGRADE_FAILED(15),
  /**
   * Disabled master's backup state
   */
  MAINTENANCE(16);

  private final int state;

  private State(int state) {
    this.state = state;
  }

  /**
   * Indicates whether or not it is a valid desired state.
   *
   * @return true if this is a valid desired state.
   */
  public boolean isValidDesiredState() {
    switch (State.values()[this.state]) {
      case INIT:
      case INSTALLED:
      case STARTED:
      case UNINSTALLED:
      case MAINTENANCE:
        return true;
      default:
        return false;
    }
  }

  /**
   * Indicates whether or not its a state indicating a task in progress.
   *
   * @return true if this is a state indicating progress.
   */
  public boolean isInProgressState() {
    switch (State.values()[this.state]) {
      case INSTALLING:
      case STARTING:
      case STOPPING:
      case UNINSTALLING:
      case WIPING_OUT:
      case UPGRADING:
        return true;
      default:
        return false;
    }
  }

  /**
   * Indicates whether or not it is a valid state for the client component.
   *
   * @return true if this is a valid state for a client component.
   */
  public boolean isValidClientComponentState() {
    switch (State.values()[this.state]) {
      case STARTING:
      case STARTED:
      case START_FAILED:
      case STOP_FAILED:
      case STOPPING:
        return false;
      default:
        return true;
    }
  }

  /**
   * Indicates whether or not the resource with this state can be removed.
   *
   * @return true if this is a removable state
   */
  public boolean isRemovableState() {
    switch (State.values()[this.state]) {
      case INIT:
      case INSTALLING:
      case INSTALLED:
      case INSTALL_FAILED:
      case UPGRADE_FAILED:
      case UNINSTALLED:
      case MAINTENANCE:
        return true;
      default:
        return false;
    }
  }
}