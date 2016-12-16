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

import java.util.Arrays;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.BooleanUtils;

import com.google.inject.Singleton;

/**
 * The {@link YarnRMHighAvailabilityCheck} checks that YARN has HA mode enabled
 * for ResourceManager..
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.MULTIPLE_COMPONENT_WARNING, order = 17.2f)
public class YarnRMHighAvailabilityCheck extends AbstractCheckDescriptor {

  /**
   * Constructor.
   */
  public YarnRMHighAvailabilityCheck() {
    super(CheckDescription.SERVICES_YARN_RM_HA);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    return super.isApplicable(request, Arrays.asList("YARN"), true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    // pretty weak sauce here; probably should do a bit more, like query JMX to
    // see that there is at least 1 RM active and 1 in standby
    String propertyValue = getProperty(request, "yarn-site", "yarn.resourcemanager.ha.enabled");

    if (null == propertyValue || !BooleanUtils.toBoolean(propertyValue)) {
      prerequisiteCheck.getFailedOn().add("YARN");
      prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
      prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
    }
  }
}
