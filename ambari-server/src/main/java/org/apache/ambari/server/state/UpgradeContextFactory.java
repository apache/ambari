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

import java.util.Map;

import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

/**
 * The {@link UpgradeContextFactory} is used to create dependency-injected
 * instances of {@link UpgradeContext}s.
 */
public interface UpgradeContextFactory {

  /**
   * Creates an {@link UpgradeContext} which is injected with dependencies.
   *
   * @param cluster
   *          the cluster that the upgrade is for
   * @param type
   *          the type of upgrade, either rolling or non_rolling
   * @param direction
   *          the direction for the upgrade
   * @param upgradeRequestMap
   *          the original map of paramters used to create the upgrade
   *
   * @return an initialized {@link UpgradeContext}.
   */
  UpgradeContext create(Cluster cluster, UpgradeType type, Direction direction,
      Map<String, Object> upgradeRequestMap);
}
