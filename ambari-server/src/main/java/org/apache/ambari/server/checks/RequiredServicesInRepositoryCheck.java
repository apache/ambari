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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

import com.google.inject.Singleton;

/**
 * The {@link RequiredServicesInRepositoryCheck} is used to ensure that if there
 * are any services which require other services to also be included in the
 * upgrade that they are included in the repository.
 * <p/>
 * This check is to prevent problems which can be caused by trying to patch
 * upgrade services which have known depdenencies on other services because of
 * things like hard coded versions.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.REPOSITORY_VERSION,
    order = 1.0f,
    required = { UpgradeType.ROLLING, UpgradeType.EXPRESS, UpgradeType.HOST_ORDERED },
    orchestration = { RepositoryType.PATCH, RepositoryType.MAINT, RepositoryType.SERVICE })
public class RequiredServicesInRepositoryCheck extends AbstractCheckDescriptor {

  /**
   * Constructor.
   */
  public RequiredServicesInRepositoryCheck() {
    super(CheckDescription.VALID_SERVICES_INCLUDED_IN_REPOSITORY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
  }
}
