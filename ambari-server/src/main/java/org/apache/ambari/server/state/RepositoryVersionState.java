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

/**
 * There must be exactly one repository version that is in a CURRENT state for a particular cluster or host.
 * There may be 0 or more repository versions in an INSTALLED state.
 * A repository version state transitions from UPGRADING -> CURRENT | UPGRADE_FAILED
 * The operation to transition a repository version state into CURRENT must be atomic and change the existing
 * relation between repository version and cluster or host from CURRENT to INSTALLED.
 *
 * <pre>
 * Step 1: Initial Configuration
 * Version 1 is CURRENT
 *
 * Step 2: Add another repository and start an upgrade from Version 1 to Version 2
 * Version 1: CURRENT
 * Version 2: UPGRADING
 *
 * Step 3: Upgrade can either complete successfully or fail
 * Version 1: CURRENT
 * Version 2: UPGRADE_FAILED (a retry can set this back to UPGRADING)
 *
 * or
 *
 * Version 1: INSTALLED
 * Version 2: CURRENT
 *
 * Step 4: May revert to the original version via a downgrade, which is technically still an upgrade to a version.
 * Version 1: UPGRADING
 * Version 2: CURRENT
 *
 * and eventually becomes
 *
 * Version 1: CURRENT
 * Version 2: INSTALLED
 *
 * *********************************************
 * Start states: CURRENT, UPGRADING
 * Allowed Transitions:
 * UPGRADING -> CURRENT | UPGRADE_FAILED
 * UPGRADE_FAILED -> UPGRADING
 * CURRENT -> INSTALLED
 * INSTALLED -> UPGRADING
 * </pre>
 */
public enum RepositoryVersionState {
  /**
   * Repository version that is installed and supported but not the active version.
   */
  INSTALLED,
  /**
   * Repository version that is installed and supported and is the active version.
   */
  CURRENT,
  /**
   * Repository version that is in the process of upgrading to become the CURRENT active version,
   * and the previous active version transitions to an INSTALLED state.
   */
  UPGRADING,
  /**
   * Repository version that during the upgrade process failed to become the active version and must be remedied.
   */
  UPGRADE_FAILED;
}
