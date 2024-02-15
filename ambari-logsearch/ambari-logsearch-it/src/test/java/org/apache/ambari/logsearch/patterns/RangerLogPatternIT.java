/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.patterns;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;

public class RangerLogPatternIT extends PatternITBase {

  @Test
  public void testRangerAdminLogLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "RANGER", "configuration", "admin-log4j.xml").toString())).getLayout("xa_log_appender");
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testRangerUserSynchLogLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "RANGER", "configuration", "usersync-log4j.xml").toString())).getLayout("logFile");
    assertThat(layout.contains("%d{dd MMM yyyy HH:mm:ss}"), is(true));
  }

  @Test
  public void testRangerAdminLog() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "RANGER", "configuration", "admin-log4j.xml").toString())).getLayout("xa_log_appender");

    testServiceLog("ranger_admin", layout, inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "RANGER/package/templates/input.config-ranger.json.j2")));
  }

  @Test
  public void testRangerUserSynchLog() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "RANGER", "configuration", "usersync-log4j.xml").toString())).getLayout("logFile");

    testServiceLog("ranger_usersync", layout, inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "RANGER/package/templates/input.config-ranger.json.j2")));
  }

  @Test
  public void testRangerKMSLogLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "RANGER_KMS", "configuration", "kms-log4j.xml").toString())).getLayout("kms");
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testRangerKMSLog() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "RANGER_KMS", "configuration", "kms-log4j.xml").toString())).getLayout("kms");

    testServiceLog("ranger_kms", layout, inputConfigTemplate(new File(HDP_SERVICES_FOLDER, "RANGER_KMS/package/templates/input.config-ranger-kms.json.j2")));
  }
}
