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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * StageStatus tests.
 */
public class StageStatusTest {

  @Test
  public void testIsValidManualTransition() throws Exception {

    // PENDING
    assertFalse(StageStatus.PENDING.isValidManualTransition(StageStatus.PENDING));
    assertFalse(StageStatus.PENDING.isValidManualTransition(StageStatus.IN_PROGRESS));
    assertFalse(StageStatus.PENDING.isValidManualTransition(StageStatus.HOLDING));
    assertFalse(StageStatus.PENDING.isValidManualTransition(StageStatus.COMPLETED));
    assertFalse(StageStatus.PENDING.isValidManualTransition(StageStatus.FAILED));
    assertFalse(StageStatus.PENDING.isValidManualTransition(StageStatus.HOLDING_FAILED));
    assertFalse(StageStatus.PENDING.isValidManualTransition(StageStatus.TIMEDOUT));
    assertFalse(StageStatus.PENDING.isValidManualTransition(StageStatus.HOLDING_TIMEDOUT));
    assertFalse(StageStatus.PENDING.isValidManualTransition(StageStatus.ABORTED));

    // IN_PROGRESS
    assertFalse(StageStatus.IN_PROGRESS.isValidManualTransition(StageStatus.PENDING));
    assertFalse(StageStatus.IN_PROGRESS.isValidManualTransition(StageStatus.IN_PROGRESS));
    assertFalse(StageStatus.IN_PROGRESS.isValidManualTransition(StageStatus.HOLDING));
    assertFalse(StageStatus.IN_PROGRESS.isValidManualTransition(StageStatus.COMPLETED));
    assertFalse(StageStatus.IN_PROGRESS.isValidManualTransition(StageStatus.FAILED));
    assertFalse(StageStatus.IN_PROGRESS.isValidManualTransition(StageStatus.HOLDING_FAILED));
    assertFalse(StageStatus.IN_PROGRESS.isValidManualTransition(StageStatus.TIMEDOUT));
    assertFalse(StageStatus.IN_PROGRESS.isValidManualTransition(StageStatus.HOLDING_TIMEDOUT));
    assertFalse(StageStatus.IN_PROGRESS.isValidManualTransition(StageStatus.ABORTED));

    // HOLDING
    assertFalse(StageStatus.HOLDING.isValidManualTransition(StageStatus.PENDING));
    assertFalse(StageStatus.HOLDING.isValidManualTransition(StageStatus.IN_PROGRESS));
    assertFalse(StageStatus.HOLDING.isValidManualTransition(StageStatus.HOLDING));
    assertTrue(StageStatus.HOLDING.isValidManualTransition(StageStatus.COMPLETED));
    assertFalse(StageStatus.HOLDING.isValidManualTransition(StageStatus.FAILED));
    assertFalse(StageStatus.HOLDING.isValidManualTransition(StageStatus.HOLDING_FAILED));
    assertFalse(StageStatus.HOLDING.isValidManualTransition(StageStatus.TIMEDOUT));
    assertFalse(StageStatus.HOLDING.isValidManualTransition(StageStatus.HOLDING_TIMEDOUT));
    assertFalse(StageStatus.HOLDING.isValidManualTransition(StageStatus.ABORTED));

    // COMPLETED
    assertFalse(StageStatus.COMPLETED.isValidManualTransition(StageStatus.PENDING));
    assertFalse(StageStatus.COMPLETED.isValidManualTransition(StageStatus.IN_PROGRESS));
    assertFalse(StageStatus.COMPLETED.isValidManualTransition(StageStatus.HOLDING));
    assertFalse(StageStatus.COMPLETED.isValidManualTransition(StageStatus.COMPLETED));
    assertFalse(StageStatus.COMPLETED.isValidManualTransition(StageStatus.FAILED));
    assertFalse(StageStatus.COMPLETED.isValidManualTransition(StageStatus.HOLDING_FAILED));
    assertFalse(StageStatus.COMPLETED.isValidManualTransition(StageStatus.TIMEDOUT));
    assertFalse(StageStatus.COMPLETED.isValidManualTransition(StageStatus.HOLDING_TIMEDOUT));
    assertFalse(StageStatus.COMPLETED.isValidManualTransition(StageStatus.ABORTED));

    // FAILED
    assertFalse(StageStatus.FAILED.isValidManualTransition(StageStatus.PENDING));
    assertFalse(StageStatus.FAILED.isValidManualTransition(StageStatus.IN_PROGRESS));
    assertFalse(StageStatus.FAILED.isValidManualTransition(StageStatus.HOLDING));
    assertFalse(StageStatus.FAILED.isValidManualTransition(StageStatus.COMPLETED));
    assertFalse(StageStatus.FAILED.isValidManualTransition(StageStatus.FAILED));
    assertFalse(StageStatus.FAILED.isValidManualTransition(StageStatus.HOLDING_FAILED));
    assertFalse(StageStatus.FAILED.isValidManualTransition(StageStatus.TIMEDOUT));
    assertFalse(StageStatus.FAILED.isValidManualTransition(StageStatus.HOLDING_TIMEDOUT));
    assertFalse(StageStatus.FAILED.isValidManualTransition(StageStatus.ABORTED));

    // HOLDING_FAILED
    assertFalse(StageStatus.HOLDING_FAILED.isValidManualTransition(StageStatus.PENDING));
    assertTrue(StageStatus.HOLDING_FAILED.isValidManualTransition(StageStatus.IN_PROGRESS));
    assertFalse(StageStatus.HOLDING_FAILED.isValidManualTransition(StageStatus.HOLDING));
    assertFalse(StageStatus.HOLDING_FAILED.isValidManualTransition(StageStatus.COMPLETED));
    assertTrue(StageStatus.HOLDING_FAILED.isValidManualTransition(StageStatus.FAILED));
    assertFalse(StageStatus.HOLDING_FAILED.isValidManualTransition(StageStatus.HOLDING_FAILED));
    assertFalse(StageStatus.HOLDING_FAILED.isValidManualTransition(StageStatus.TIMEDOUT));
    assertFalse(StageStatus.HOLDING_FAILED.isValidManualTransition(StageStatus.HOLDING_TIMEDOUT));
    assertFalse(StageStatus.HOLDING_FAILED.isValidManualTransition(StageStatus.ABORTED));

    // TIMEDOUT
    assertFalse(StageStatus.TIMEDOUT.isValidManualTransition(StageStatus.PENDING));
    assertFalse(StageStatus.TIMEDOUT.isValidManualTransition(StageStatus.IN_PROGRESS));
    assertFalse(StageStatus.TIMEDOUT.isValidManualTransition(StageStatus.HOLDING));
    assertFalse(StageStatus.TIMEDOUT.isValidManualTransition(StageStatus.COMPLETED));
    assertFalse(StageStatus.TIMEDOUT.isValidManualTransition(StageStatus.FAILED));
    assertFalse(StageStatus.TIMEDOUT.isValidManualTransition(StageStatus.HOLDING_FAILED));
    assertFalse(StageStatus.TIMEDOUT.isValidManualTransition(StageStatus.TIMEDOUT));
    assertFalse(StageStatus.TIMEDOUT.isValidManualTransition(StageStatus.HOLDING_TIMEDOUT));
    assertFalse(StageStatus.TIMEDOUT.isValidManualTransition(StageStatus.ABORTED));

    // HOLDING_TIMEDOUT
    assertFalse(StageStatus.HOLDING_TIMEDOUT.isValidManualTransition(StageStatus.PENDING));
    assertTrue(StageStatus.HOLDING_TIMEDOUT.isValidManualTransition(StageStatus.IN_PROGRESS));
    assertFalse(StageStatus.HOLDING_TIMEDOUT.isValidManualTransition(StageStatus.HOLDING));
    assertFalse(StageStatus.HOLDING_TIMEDOUT.isValidManualTransition(StageStatus.COMPLETED));
    assertFalse(StageStatus.HOLDING_TIMEDOUT.isValidManualTransition(StageStatus.FAILED));
    assertFalse(StageStatus.HOLDING_TIMEDOUT.isValidManualTransition(StageStatus.HOLDING_FAILED));
    assertTrue(StageStatus.HOLDING_TIMEDOUT.isValidManualTransition(StageStatus.TIMEDOUT));
    assertFalse(StageStatus.HOLDING_TIMEDOUT.isValidManualTransition(StageStatus.HOLDING_TIMEDOUT));
    assertFalse(StageStatus.HOLDING_TIMEDOUT.isValidManualTransition(StageStatus.ABORTED));

    // ABORTED
    assertFalse(StageStatus.ABORTED.isValidManualTransition(StageStatus.PENDING));
    assertFalse(StageStatus.ABORTED.isValidManualTransition(StageStatus.IN_PROGRESS));
    assertFalse(StageStatus.ABORTED.isValidManualTransition(StageStatus.HOLDING));
    assertFalse(StageStatus.ABORTED.isValidManualTransition(StageStatus.COMPLETED));
    assertFalse(StageStatus.ABORTED.isValidManualTransition(StageStatus.FAILED));
    assertFalse(StageStatus.ABORTED.isValidManualTransition(StageStatus.HOLDING_FAILED));
    assertFalse(StageStatus.ABORTED.isValidManualTransition(StageStatus.TIMEDOUT));
    assertFalse(StageStatus.ABORTED.isValidManualTransition(StageStatus.HOLDING_TIMEDOUT));
    assertFalse(StageStatus.ABORTED.isValidManualTransition(StageStatus.ABORTED));
  }
}