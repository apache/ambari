/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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
package org.apache.ambari.server.agent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecoveryTopologyManagerTest {
  @Test
  public void testSnapshotIsFreshOnlyForTheActiveAgentSession() {
    RecoveryTopologyManager manager = new RecoveryTopologyManager();
    Long hostId = 1L;

    manager.beginAgentSession(hostId, "session-1");
    assertFalse(manager.isFresh(hostId));
    assertTrue(manager.markSnapshotComplete(hostId, "session-1"));
    assertTrue(manager.isFresh(hostId));

    manager.beginAgentSession(hostId, "session-2");
    assertFalse(manager.isFresh(hostId));
    assertFalse(manager.markSnapshotComplete(hostId, "session-1"));
    assertFalse(manager.isFresh(hostId));
    assertTrue(manager.markSnapshotComplete(hostId, "session-2"));
    assertTrue(manager.isFresh(hostId));
  }

  @Test
  public void testEndingAgentSessionInvalidatesSnapshotAndVersion() {
    RecoveryTopologyManager manager = new RecoveryTopologyManager();
    Long hostId = 1L;

    manager.beginAgentSession(hostId, "session-1");
    manager.markSnapshotComplete(hostId, "session-1");
    long completeVersion = manager.getVersion();

    manager.endAgentSession(hostId);

    assertFalse(manager.isActiveSession(hostId, "session-1"));
    assertFalse(manager.isFresh(hostId));
    assertTrue(manager.getVersion() > completeVersion);
  }
}
