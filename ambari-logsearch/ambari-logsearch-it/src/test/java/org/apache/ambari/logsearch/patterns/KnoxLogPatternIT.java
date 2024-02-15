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

import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;

public class KnoxLogPatternIT extends PatternITBase {

  @Test
  public void testKnoxGatewayAppenderLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "KNOX", "configuration", "gateway-log4j.xml").toString())).getLayout("drfa");
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testKnoxLdapAppenderLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "KNOX", "configuration", "ldap-log4j.xml").toString())).getLayout("drfa");
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testKnoxGateway() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "KNOX", "configuration", "gateway-log4j.xml").toString())).getLayout("drfa");

    testServiceLog("knox_gateway", layout, inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "KNOX/package/templates/input.config-knox.json.j2")));
  }

  @Test
  public void testKnoxLdap() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "KNOX", "configuration", "ldap-log4j.xml").toString())).getLayout("drfa");

    testServiceLog("knox_ldap", layout, inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "KNOX/package/templates/input.config-knox.json.j2")));
  }
}
