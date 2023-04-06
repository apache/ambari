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
package org.apache.ambari.server.checks;

import java.util.Collections;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * The {@link AutoStartDisabledCheck} class is used to check that the cluster does
 * not have auto-restart enabled.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.CONFIGURATION_WARNING,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class AutoStartDisabledCheck extends AbstractCheckDescriptor {

  static final String CLUSTER_ENV_TYPE = "cluster-env";
  static final String RECOVERY_ENABLED_KEY = "recovery_enabled";
  static final String RECOVERY_TYPE_KEY = "recovery_type";
  static final String RECOVERY_AUTO_START = "AUTO_START";

  /**
   * Constructor.
   */
  public AutoStartDisabledCheck() {
    super(CheckDescription.AUTO_START_DISABLED);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getApplicableServices() {
    return Collections.emptySet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {

    String autoStartEnabled = getProperty(request, CLUSTER_ENV_TYPE, RECOVERY_ENABLED_KEY);

    // !!! auto-start is already disabled
    if (!Boolean.valueOf(autoStartEnabled)) {
      return;
    }

    // !!! double check the value is AUTO_START.  it's the only supported value (and there's no enum for it)
    String recoveryType = getProperty(request, CLUSTER_ENV_TYPE, RECOVERY_TYPE_KEY);
    if (StringUtils.equals(recoveryType, RECOVERY_AUTO_START)) {

      prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.getFailedOn().add(request.getClusterName());

    }
  }
}
