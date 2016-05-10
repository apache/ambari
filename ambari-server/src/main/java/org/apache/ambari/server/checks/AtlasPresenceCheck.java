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
package org.apache.ambari.server.checks;

import java.util.Arrays;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * Checks if Atlas service is present. Upgrade to stack HDP 2.5 can't pursuit
 * with existed on the cluster Atlas service.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.DEFAULT)
public class AtlasPresenceCheck extends AbstractCheckDescriptor{

  private static final Logger LOG = LoggerFactory.getLogger(AtlasPresenceCheck.class);
  private static final String serviceName = "ATLAS";

  public AtlasPresenceCheck(){
    super(CheckDescription.ATLAS_SERVICE_PRESENCE_CHECK);
  }

  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    return super.isApplicable(request, Arrays.asList(serviceName), true);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    prerequisiteCheck.getFailedOn().add(serviceName);
    prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
    prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
  }
}
