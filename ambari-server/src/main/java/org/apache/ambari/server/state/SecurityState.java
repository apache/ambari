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

package org.apache.ambari.server.state;

import java.util.EnumSet;

/**
 * Indicates the security state of a service or component.
 */
public enum SecurityState {
  /**
   * Indicates that it is not known whether the service or component is secured or not
   */
  UNKNOWN,
  /**
   * Indicates service or component is not or should not be secured
   */
  UNSECURED,
  /**
   * Indicates component is or should be secured using Kerberos
   */
  SECURED_KERBEROS,
  /**
   * Indicates the component is in the process of being secured
   */
  SECURING,
  /**
   * Indicates the component is in the process of being unsecured
   */
  UNSECURING,
  /**
   * Indicates the component is not secured due to an error condition
   */
  ERROR;

  /**
   * The subset of states that are considered endpoints, meaning they do not indicate the state is
   * in transition.
   */
  public static final EnumSet<SecurityState> ENDPOINT_STATES =
      EnumSet.of(UNKNOWN, UNSECURED, ERROR, SECURED_KERBEROS);

  /**
   * The subset of states that are considered transitional, meaning they indicate a task is in
   * process to reach some endpoint state
   */
  public static final EnumSet<SecurityState> TRANSITIONAL_STATES =
      EnumSet.of(SECURING, UNSECURING);

  /**
   * Tests this SecurityState to see if it is an endpoint state.
   *
   * @return true if this state is an endpoint state; otherwise false
   */
  public boolean isEndpoint() {
    return ENDPOINT_STATES.contains(this);
  }

  /**
   * Tests this SecurityState to see if it is a transitional state.
   *
   * @return true if this state is a transitional state; otherwise false
   */
  public boolean isTransitional() {
    return TRANSITIONAL_STATES.contains(this);
  }
}
