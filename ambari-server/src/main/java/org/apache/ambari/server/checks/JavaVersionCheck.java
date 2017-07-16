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

import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * The {@link JavaVersionCheck} checks that JDK version used by Ambari meets the minimal version requirement
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.DEFAULT)
public class JavaVersionCheck extends AbstractCheckDescriptor {

  private static final Logger LOG = LoggerFactory.getLogger(JavaVersionCheck.class);
  static final String JAVA_VERSION_PROPERTY_NAME = "java-version";

  /**
   * Constructor.
   */
  public JavaVersionCheck() {
    super(CheckDescription.JAVA_VERSION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {

    PrerequisiteCheckConfig prerequisiteCheckConfig = request.getPrerequisiteCheckConfig();
    Map<String, String> checkProperties = null;
    if(prerequisiteCheckConfig != null) {
      checkProperties = prerequisiteCheckConfig.getCheckProperties(this.getClass().getName());
    }
    String javaVersion = null;
    if(checkProperties != null && checkProperties.containsKey(JAVA_VERSION_PROPERTY_NAME)) {
      javaVersion = checkProperties.get(JAVA_VERSION_PROPERTY_NAME);
      LOG.debug(String.format("JDK version defined in the upgrade pack: %s ", javaVersion));
    }

    if(null == javaVersion){
      LOG.debug(String.format("No JDK version provided in the upgrade pack. Use default version %.1f", Configuration.JDK_MIN_VERSION));
      javaVersion = String.valueOf(Configuration.JDK_MIN_VERSION);
    }

    int version = parseJavaVersion(javaVersion);
    boolean meetVersion = meetJavaVersion(version);

    if(!meetVersion){
      prerequisiteCheck.getFailedOn().add("Java");
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      String reason = getFailReason(prerequisiteCheck, request);
      reason = String.format(reason, javaVersion);
      prerequisiteCheck.setFailReason(reason);
      return;
    }
  }

  private boolean meetJavaVersion(int expectedVersion){
    String version = System.getProperty("java.version");
    int currentJavaVersion = parseJavaVersion(version);
    return (currentJavaVersion >= expectedVersion) && (currentJavaVersion != -1);
  }

  private int parseJavaVersion(String javaVersion) {
    if (javaVersion.startsWith("1.6")) {
      return 6;
    } else if (javaVersion.startsWith("1.7")) {
      return 7;
    } else if (javaVersion.startsWith("1.8")) {
      return 8;
    } else { // Some unsupported java version
      return -1;
    }
  }
}
