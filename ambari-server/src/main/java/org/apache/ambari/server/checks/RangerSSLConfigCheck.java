/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.checks;


import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Singleton;


/**
 * This service check will mainly be for 2.6 stacks so as to encourage user
 * to move the certificate, keystore and truststore from the default conf dir to
 * an external directory untoched while  RU/EU during upgrades/downgrades.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.INFORMATIONAL_WARNING)
public class RangerSSLConfigCheck extends AbstractCheckDescriptor {

  private static final Logger LOG = LoggerFactory.getLogger(RangerSSLConfigCheck.class);
  private static final String serviceName = "RANGER";


  /**
   * Constructor
   */
  public RangerSSLConfigCheck() {
    super(CheckDescription.RANGER_SSL_CONFIG_CHECK);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getApplicableServices() {
    return Sets.newHashSet(serviceName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    String isRangerHTTPEnabled = getProperty(request, "ranger-admin-site", "ranger.service.http.enabled");
    String isRangerSSLEnabled = getProperty(request, "ranger-admin-site", "ranger.service.https.attrib.ssl.enabled");
    String rangerSSLKeystoreFile = getProperty(request, "ranger-admin-site", "ranger.https.attrib.keystore.file");

    if (("false").equalsIgnoreCase(isRangerHTTPEnabled) && ("true").equalsIgnoreCase(isRangerSSLEnabled) && rangerSSLKeystoreFile.contains("/etc/ranger/admin/conf") ) {
      LOG.info("Ranger is SSL enabled, need to show Configuration changes warning before upragade proceeds.");
      prerequisiteCheck.getFailedOn().add(serviceName);
      prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
      prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
    } else {
      LOG.info("Ranger is not SSL enabled, no need to show Configuration changes warning before upragade proceeds.");
    }

  }
}
