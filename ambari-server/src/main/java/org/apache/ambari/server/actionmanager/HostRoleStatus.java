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
package org.apache.ambari.server.actionmanager;

public enum HostRoleStatus {
  PENDING(0), //Not queued for a host
  QUEUED(1), //Queued for a host
  IN_PROGRESS(2), //Host reported it is working
  COMPLETED(3), //Host reported success
  FAILED(4), //Failed
  TIMEDOUT(5), //Host did not respond in time
  ABORTED(6); //Operation was abandoned
  private final int status;

  private HostRoleStatus(int status) {
    this.status = status;
  }

  /**
   * Indicates whether or not it is a valid failure state.
   *
   * @return true if this is a valid failure state.
   */
  public boolean isFailedState() {
    switch (HostRoleStatus.values()[this.status]) {
      case FAILED:
      case TIMEDOUT:
      case ABORTED:
        return true;
      default:
        return false;
    }
  }

  /**
   * Indicates whether or not this is a completed state.
   * Completed means that the associated task has stopped
   * running because it has finished successfully or has
   * failed.
   *
   * @return true if this is a completed state.
   */
  public boolean isCompletedState() {
    switch (HostRoleStatus.values()[this.status]) {
      case COMPLETED:
      case FAILED:
      case TIMEDOUT:
      case ABORTED:
        return true;
      default:
        return false;
    }
  }
}
