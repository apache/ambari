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
 * The {@link HiveRollingPortChangeWarning} to see if Hive is installed and if
 * the upgrade type is {@link UpgradeType#ROLLING}. If so, then a
 * {@link PrereqCheckStatus#WARNING} is produced which will let the operator
 * know that the port for Hive must change in order to preserve the uptime of
 * the service.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.INFORMATIONAL_WARNING, required = true)
public class HiveRollingPortChangeWarning extends AbstractCheckDescriptor {

  /**
   * Constructor.
   */
  public HiveRollingPortChangeWarning() {
    super(CheckDescription.SERVICES_HIVE_ROLLING_PORT_WARNING);
  }

  /**
   * {@inheritDoc}
   * <p/>
   * This check is only applicable if Hive is installed and the upgrade type is
   * {@link UpgradeType#ROLLING}.
   */
  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    boolean isApplicable = super.isApplicable(request, Arrays.asList("HIVE"), true);
    return isApplicable && request.getUpgradeType() == UpgradeType.ROLLING;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    prerequisiteCheck.getFailedOn().add("HIVE");
    prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
    prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
  }
}
