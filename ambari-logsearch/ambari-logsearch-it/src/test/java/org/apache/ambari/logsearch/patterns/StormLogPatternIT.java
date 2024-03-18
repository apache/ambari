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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class StormLogPatternIT extends PatternITBase {

  @Test
  public void testStormClusterLogLayout() {
    String layout = Log4jXmlProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "STORM", "configuration", "storm-cluster-log4j.xml").toString())).getLayout("pattern");
    assertThat(layout.contains("yyyy-MM-dd HH:mm:ss.SSS"), is(true));
  }

  @Test
  public void testStormWorkerLogLayout() {
    String layout = Log4jXmlProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "STORM", "configuration", "storm-worker-log4j.xml").toString())).getLayout("pattern");
    assertThat(layout.contains("yyyy-MM-dd HH:mm:ss.SSS"), is(true));
  }

  @Test
  public void testStormLog() throws Exception {
    String layout = Log4jXmlProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "STORM", "configuration", "storm-cluster-log4j.xml").toString())).getLayout("pattern");

    testServiceLog("storm_drpc", layout, inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "STORM/package/templates/input.config-storm.json.j2")));
  }

  @Test
  public void testStormWorkerLog() throws Exception {
    String layout = Log4jXmlProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "STORM", "configuration", "storm-worker-log4j.xml").toString())).getLayout("pattern");

    testServiceLog("storm_worker", layout, inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "STORM/package/templates/input.config-storm.json.j2")));
  }

  @Test
  public void testStormWorkerLogEntry() throws Exception {
    String logEntry = "2018-05-04 05:10:00.120 o.a.s.d.executor main [INFO] Loaded executor tasks count:[5 5]";
    Map<String, Object> result = testLogEntry(logEntry, "storm_worker", inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "STORM/package/templates/input.config-storm.json.j2")));

    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("INFO"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("storm_worker"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Loaded executor tasks count:[5 5]"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    MatcherAssert.assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 4, 5, 10, 0, 120000000)));
  }
}
