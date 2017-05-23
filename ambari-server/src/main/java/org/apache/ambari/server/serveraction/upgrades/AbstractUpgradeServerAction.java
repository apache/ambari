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
package org.apache.ambari.server.serveraction.upgrades;

import java.util.Collections;
import java.util.Set;

import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * Abstract class that reads values from command params in a consistent way.
 */
public abstract class AbstractUpgradeServerAction extends AbstractServerAction {

  public static final String CLUSTER_NAME_KEY = UpgradeContext.COMMAND_PARAM_CLUSTER_NAME;
  public static final String UPGRADE_DIRECTION_KEY = UpgradeContext.COMMAND_PARAM_DIRECTION;
  public static final String VERSION_KEY = UpgradeContext.COMMAND_PARAM_VERSION;
  protected static final String REQUEST_ID = UpgradeContext.COMMAND_PARAM_REQUEST_ID;

  /**
   * The original "current" stack of the cluster before the upgrade started.
   * This is the same regardless of whether the current direction is
   * {@link Direction#UPGRADE} or {@link Direction#DOWNGRADE}.
   */
  protected static final String ORIGINAL_STACK_KEY = UpgradeContext.COMMAND_PARAM_ORIGINAL_STACK;

  /**
   * The target upgrade stack before the upgrade started. This is the same
   * regardless of whether the current direction is {@link Direction#UPGRADE} or
   * {@link Direction#DOWNGRADE}.
   */
  protected static final String TARGET_STACK_KEY = UpgradeContext.COMMAND_PARAM_TARGET_STACK;

  protected static final String SUPPORTED_SERVICES_KEY = UpgradeResourceProvider.COMMAND_PARAM_SUPPORTED_SERVICES;

  @Inject
  protected Clusters m_clusters;

  /**
   * @return the set of supported services
   */
  protected Set<String> getSupportedServices() {
    String services = getCommandParameterValue(SUPPORTED_SERVICES_KEY);
    if (StringUtils.isBlank(services)) {
      return Collections.emptySet();
    } else {
      return Sets.newHashSet(StringUtils.split(services, ','));
    }
  }

}
