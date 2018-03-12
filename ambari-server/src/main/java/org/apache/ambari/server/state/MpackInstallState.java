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

import java.util.List;

/**
 * The {@link MpackInstallState} represents the installation state of a
 * repository on a particular host. Because hosts can contain a mixture of
 * components from different repositories and management packs, there can be any
 * combination of {@link MpackInstallState#INSTALLED}} entries for a single
 * host. A host may not have multiple entries for the same management pack.
 * <p/>
 *
 */
public enum MpackInstallState {
  /**
   *
   */
  NOT_REQUIRED(0),

  /**
   *
   */
  NOT_INSTALLED(2),

  /**
   *
   */
  INSTALLING(3),

  /**
   *
   */
  INSTALLED(1),

  /**
   *
   */
  INSTALL_FAILED(4);

  private final int weight;

  /**
   * Constructor.
   *
   * @param weight
   *          the weight of the state.
   */
  private MpackInstallState(int weight) {
    this.weight = weight;
  }

  /**
   * Gets a single representation of the repository state based on the supplied
   * states.
   *
   * @param states
   *          the states to calculate the aggregate for.
   * @return the "heaviest" state.
   */
  public static MpackInstallState getAggregateState(List<MpackInstallState> states) {
    if (null == states || states.isEmpty()) {
      return NOT_REQUIRED;
    }

    MpackInstallState heaviestState = states.get(0);
    for (MpackInstallState state : states) {
      if (state.weight > heaviestState.weight) {
        heaviestState = state;
      }
    }

    return heaviestState;
  }
}
