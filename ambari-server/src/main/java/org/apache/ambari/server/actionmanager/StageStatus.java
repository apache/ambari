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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Stage status enum.
 */
public enum StageStatus {

  /**
   * Stage contains tasks not yet queued for a host.
   */
  PENDING,

  /**
   * Stage contains tasks that are reported to be in progress.
   */
  IN_PROGRESS,

  /**
   * Stage is holding, waiting for command to proceed to next stage.
   */
  HOLDING,

  /**
   * All tasks for this stage have completed.
   */
  COMPLETED,

  /**
   * At least one task for this stage has reported a failure.
   */
  FAILED,

  /**
   * Stage is holding after a failure, waiting for command to proceed to next stage.
   */
  HOLDING_FAILED,

  /**
   * At least one task for this stage has timed out.
   */
  TIMEDOUT,

  /**
   * Stage is holding after a time-out, waiting for command to proceed to next stage.
   */
  HOLDING_TIMEDOUT,

  /**
   * Operation was abandoned.
   */
  ABORTED;

  /**
   * Mapping of valid status transitions that that are driven by manual input.
   */
  private static Map<StageStatus, EnumSet<StageStatus>> manualTransitionMap = new HashMap<StageStatus, EnumSet<StageStatus>>();

  static {
    manualTransitionMap.put(HOLDING, EnumSet.of(COMPLETED));
    manualTransitionMap.put(HOLDING_FAILED, EnumSet.of(IN_PROGRESS, FAILED));
    manualTransitionMap.put(HOLDING_TIMEDOUT, EnumSet.of(IN_PROGRESS, TIMEDOUT));
  }

  /**
   * Determine whether or not it is valid to transition from this stage status to the given status.
   *
   * @param status  the stage status being transitioned to
   *
   * @return true if it is valid to transition to the given stage status
   */
  public boolean isValidManualTransition(StageStatus status) {
    EnumSet<StageStatus> stageStatusSet = manualTransitionMap.get(this);
    return stageStatusSet != null && stageStatusSet.contains(status);
  }
}
