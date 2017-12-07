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

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * The {@link LZOCheck}
 * is used to check that the LZO codec enabled in the core-site config fnd warning if any hosts require LZO, it should be installed before starting the upgrade.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.INFORMATIONAL_WARNING)
public class LZOCheck extends AbstractCheckDescriptor {

  final static String IO_COMPRESSION_CODECS = "io.compression.codecs";
  final static String LZO_ENABLE_KEY = "io.compression.codec.lzo.class";
  final static String LZO_ENABLE_VALUE = "com.hadoop.compression.lzo.LzoCodec";

  /**
   * Constructor.
   */
  public LZOCheck() {
    super(CheckDescription.LZO_CONFIG_CHECK);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    if (config.getGplLicenseAccepted()){
      return;
    }
    List<String> errorMessages = new ArrayList<>();
    PrereqCheckStatus checkStatus = PrereqCheckStatus.WARNING;

    String codecs = getProperty(request, "core-site", IO_COMPRESSION_CODECS);
    if (codecs!= null && codecs.contains(LZO_ENABLE_VALUE)) {
      errorMessages.add(getFailReason(IO_COMPRESSION_CODECS, prerequisiteCheck, request));
    }
    String classValue = getProperty(request, "core-site", LZO_ENABLE_KEY);

    if (LZO_ENABLE_VALUE.equals(classValue)) {
      errorMessages.add(getFailReason(LZO_ENABLE_KEY, prerequisiteCheck, request));
    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, "You have LZO codec enabled in the core-site config of your cluster. " +
          "You have to accept GPL license during ambari-server setup to have LZO installed automatically. " +
          "If any hosts require LZO, it should be installed before starting the upgrade. " +
          "Consult Ambari documentation for instructions on how to do this."));
      prerequisiteCheck.getFailedOn().add("LZO");
      prerequisiteCheck.setStatus(checkStatus);
    }
  }
}
