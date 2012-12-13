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
   * Initial/Clean state
   */
  INIT(0),
  /**
   * In the process of installing.
   */
  INSTALLING(1),
  /**
   * Install failed
   */
  INSTALL_FAILED(2),
  /**
   * State when install completed successfully
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
   * Stop failed
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
   * In the process of wiping out the install
   */
  WIPING_OUT(12),
  /**
   * State when wipeout fails
   */
  WIPEOUT_FAILED(13);

  private final int state;

  private State(int state) {
    this.state = state;
  }

  public boolean isValidDesiredState() {
    switch (State.values()[this.state]) {
      case INIT:
      case INSTALLED:
      case STARTED:
      case UNINSTALLED:
        return true;
      default:
        return false;
    }
  }

  public boolean isInProgressState() {
    switch (State.values()[this.state]) {
      case INSTALLING:
      case STARTING:
      case STOPPING:
      case UNINSTALLING:
      case WIPING_OUT:
        return true;
      default:
        return false;
    }
  }

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

}
