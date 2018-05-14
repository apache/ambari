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

public class Spark2LogPatternIT extends PatternITBase {

  @Test
  public void testSpark2LogLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "SPARK2", "configuration", "spark2-log4j-properties.xml").toString())).getLayout("console");
    assertThat(layout.contains("%d{yy/MM/dd HH:mm:ss}"), is(true));
  }

  @Test
  public void testSpark2Livy2LogLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "SPARK2", "configuration", "livy2-log4j-properties.xml").toString())).getLayout("console");
    assertThat(layout.contains("%d{yy/MM/dd HH:mm:ss}"), is(true));
  }

  @Test
  public void testSpark2Log() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "SPARK2", "configuration", "spark2-log4j-properties.xml").toString())).getLayout("console");

    testServiceLog("spark2_jobhistory_server", layout, inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "SPARK2/package/templates/input.config-spark2.json.j2")));
  }

  @Test
  public void testSpark2Livy2Log() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "SPARK2", "configuration", "livy2-log4j-properties.xml").toString())).getLayout("console");

    testServiceLog("livy2_server", layout, inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "SPARK2/package/templates/input.config-spark2.json.j2")));
  }
}
