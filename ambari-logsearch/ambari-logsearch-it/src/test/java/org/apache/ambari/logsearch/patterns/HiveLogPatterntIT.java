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

import org.junit.Test;

public class HiveLogPatterntIT extends PatternITBase {
// TODO: use hdp_ambari_definitions
  @Test
  public void testHiveLogLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(AMBARI_STACK_DEFINITIONS, "HIVE/0.12.0.2.0/configuration/hive-log4j.xml")).getLayout("DRFA");
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testHiveServer2() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(AMBARI_STACK_DEFINITIONS, "HIVE/0.12.0.2.0/configuration/hive-log4j.xml")).getLayout("DRFA");
    testServiceLog("hive_hiveserver2", layout, inputConfigTemplate(new File(AMBARI_STACK_DEFINITIONS, "HIVE/0.12.0.2.0/package/templates/input.config-hive.json.j2")));
  }

  @Test
  public void testHiveMetastore() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(AMBARI_STACK_DEFINITIONS, "HIVE/0.12.0.2.0/configuration/hive-log4j.xml")).getLayout("DRFA");
    testServiceLog("hive_metastore", layout, inputConfigTemplate(new File(AMBARI_STACK_DEFINITIONS, "HIVE/0.12.0.2.0/package/templates/input.config-hive.json.j2")));
  }
}

