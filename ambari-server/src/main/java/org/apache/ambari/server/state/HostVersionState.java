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
 * There must be exactly one stack version that is in the CURRENT state.
 * There may be 0 or more stack versions in an INSTALLED state.
 * Installing a new stack version on a host transitions from NOT_INSTALLED to CURRENT once the upgrade finishes.
 * The operation to transition a new stack version on the host from the NOT_INSTALLED state to CURRENT must be atomic
 * and change the existing stack version on the host from CURRENT to INSTALLED.
 *
 * <pre>
 * Start states: CURRENT, NOT_INSTALLED
 * Allowed Transitions:
 * NOT_INSTALLED -> CURRENT
 * CURRENT -> INSTALLED
 * INSTALLED -> CURRENT
 * </pre>
 */
public enum HostVersionState {
  /**
   * Stack version that is installed on this host for all of its components but is not the active stack version on the host.
   */
  INSTALLED,
  /**
   * Stack version that is installed on this host for all of its components and is the active stack version on the host.
   */
  CURRENT,
  /**
   * Stack version that remains to be installed on this host, potentially because it is in the process of being installed.
   */
  NOT_INSTALLED;
}
