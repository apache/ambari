/*
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
import java.util.EnumSet;
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
   * Task is holding, waiting for command to proceed to completion.
   */
  HOLDING,

  /**
   * Host reported success
   */
  COMPLETED,

  /**
   * Failed
   */
  FAILED,

  /**
   * Task is holding after a failure, waiting for command to skip or retry.
   */
  HOLDING_FAILED,

  /**
   * Host did not respond in time
   */
  TIMEDOUT,

  /**
   * Task is holding after a time-out, waiting for command to skip or retry.
   */
  HOLDING_TIMEDOUT,

  /**
   * Operation was abandoned
   */
  ABORTED,

  /**
   * The operation failed and was automatically skipped.
   */
  SKIPPED_FAILED;

  private static List<HostRoleStatus> COMPLETED_STATES = Arrays.asList(FAILED, TIMEDOUT, ABORTED,
      COMPLETED, SKIPPED_FAILED);

  private static List<HostRoleStatus> HOLDING_STATES = Arrays.asList(HOLDING, HOLDING_FAILED,
      HOLDING_TIMEDOUT);

  public static List<HostRoleStatus> SCHEDULED_STATES = Arrays.asList(PENDING, QUEUED, IN_PROGRESS);

  /**
   * The {@link HostRoleStatus}s that represent any commands which are
   * considered to be "Failed".
   */
  public static EnumSet<HostRoleStatus> FAILED_STATUSES = EnumSet.of(FAILED, TIMEDOUT, ABORTED,
      SKIPPED_FAILED);

  /**
   * The {@link HostRoleStatus}s that represent any commands which are
   * considered to be "Failed" and next commands can not be executed.
   */
  public static EnumSet<HostRoleStatus> NOT_SKIPPABLE_FAILED_STATUSES = EnumSet.of(FAILED, TIMEDOUT, ABORTED);

  /**
   * The {@link HostRoleStatus}s that represent the current commands that failed during stack upgrade.
   * This is not used to indicate commands that failed and then skipped.
   */
  public static EnumSet<HostRoleStatus> STACK_UPGRADE_FAILED_STATUSES = EnumSet.of(FAILED, HOLDING_FAILED,
      HOLDING_TIMEDOUT);

  /**
   * The {@link HostRoleStatus}s that represent any commands which are
   * considered to be "In Progress".
   */
  public static final EnumSet<HostRoleStatus> IN_PROGRESS_STATUSES = EnumSet.of(
      HostRoleStatus.QUEUED, HostRoleStatus.IN_PROGRESS,
      HostRoleStatus.PENDING, HostRoleStatus.HOLDING,
      HostRoleStatus.HOLDING_FAILED, HostRoleStatus.HOLDING_TIMEDOUT);

  /**
   * The {@link HostRoleStatus}s that represent all non-completed states.
   */
  public static final EnumSet<HostRoleStatus> NOT_COMPLETED_STATUSES = EnumSet.complementOf(EnumSet.of(COMPLETED));

  /**
   * Indicates whether or not it is a valid failure state.
   *
   * @return true if this is a valid failure state.
   */
  public boolean isFailedState() {
    return FAILED_STATUSES.contains(this);
  }

  /**
   * Indicates whether or not it is a valid failure state without ability to be skipped.
   *
   * @return true if this is a valid failure state.
   */
  public boolean isFailedAndNotSkippableState() {
    return NOT_SKIPPABLE_FAILED_STATUSES.contains(this);
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
   * Indicates whether or not this is a holding state.
   * Holding means that the associated task is waiting for
   * a command to transition to a completion state.
   *
   * @return true if this is a holding state.
   */
  public boolean isHoldingState() {
    return HOLDING_STATES.contains(this);
  }

  /**
   *
   * @return list of completed states
   */
  public static List<HostRoleStatus> getCompletedStates() {
    return Collections.unmodifiableList(COMPLETED_STATES);
  }

  /**
   * @return {@code true} if this is a status that is in progress
   */
  public boolean isInProgress() {
    return IN_PROGRESS_STATUSES.contains(this);
  }
}
