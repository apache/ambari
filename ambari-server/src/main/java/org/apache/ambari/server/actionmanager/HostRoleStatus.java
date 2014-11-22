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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum HostRoleStatus {
  /**
   * Not queued for a host.
   */
  PENDING,
  /**
   * Queued for a host, or has already been sent to host, but host did not answer yet.
   */
  QUEUED,
  /**
   * Host reported it is working, received an IN_PROGRESS command status from host.
   */
  IN_PROGRESS,
  /**
   * Host reported success
   */
  COMPLETED,
  /**
   * Failed
   */
  FAILED,
  /**
   * Host did not respond in time
   */
  TIMEDOUT,
  /**
   * Operation was abandoned
   */
  ABORTED;

  private static List<HostRoleStatus> COMPLETED_STATES = Arrays.asList(FAILED, TIMEDOUT, ABORTED, COMPLETED);
  private static List<HostRoleStatus> FAILED_STATES = Arrays.asList(FAILED, TIMEDOUT, ABORTED);


  /**
   * Indicates whether or not it is a valid failure state.
   *
   * @return true if this is a valid failure state.
   */
  public boolean isFailedState() {
    return FAILED_STATES.contains(this);
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
    return COMPLETED_STATES.contains(this);
  }

  /**
   *
   * @return list of completed states
   */
  public static List<HostRoleStatus> getCompletedStates() {
    return Collections.unmodifiableList(COMPLETED_STATES);
  }

  /**
   *
   * @return list of failed states
   */
  public static List<HostRoleStatus> getFailedStates() {
    return Collections.unmodifiableList(FAILED_STATES);
  }


}
