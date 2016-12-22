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
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

import com.google.inject.Singleton;

/**
 * The {@link StormShutdownWarning} to see if Storm is installed and if the
 * upgrade type is {@link UpgradeType#ROLLING}. If so, then a
 * {@link PrereqCheckStatus#WARNING} is produced which will let the operator
 * know that Storm cannot be rolling on ceratin versions of the HDP stack.
 * <p/>
 * The upgrade packs must include this check where it is applicable. It contains
 * no logic for determine stack versions and only checks for the presence of
 * Storm and the type of upgrade.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.INFORMATIONAL_WARNING, required = UpgradeType.ROLLING)
public class StormShutdownWarning extends AbstractCheckDescriptor {

  /**
   * Constructor.
   */
  public StormShutdownWarning() {
    super(CheckDescription.SERVICES_STORM_ROLLING_WARNING);
  }

  /**
   * {@inheritDoc}
   * <p/>
   * This check is only applicable if Storm is installed and the upgrade type is
   * {@link UpgradeType#ROLLING}.
   */
  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    boolean isApplicable = super.isApplicable(request, Arrays.asList("STORM"), true);
    return isApplicable && request.getUpgradeType() == UpgradeType.ROLLING;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    prerequisiteCheck.getFailedOn().add("STORM");
    prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
    prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
  }
}
