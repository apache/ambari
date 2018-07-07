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

import org.junit.Test;

public class YarnLogPatternIT extends PatternITBase {

  @Test
  public void testYarnJobSummaryLogLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "YARN", "configuration", "yarn-log4j.xml").toString())).getLayout("RMSUMMARY");
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testYarnJobSummaryLog() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "YARN", "configuration", "yarn-log4j.xml").toString())).getLayout("RMSUMMARY");

    testServiceLog("yarn_jobsummary", layout,
            inputConfigTemplate(new File(HDP_SERVICES_FOLDER, "YARN/package/templates/input.config-yarn.json.j2")));
  }

  @Test
  public void testYarnNodemanagerLogEntry() throws Exception {
    Map<String, Object> result = testLogEntry("2018-05-02 09:43:46,898 INFO  zookeeper.ZooKeeper (Environment.java:logEnv(100)) - Client environment:zookeeper.version=3.4.6-1173--1,\n" +
                    " built on 04/10/2018 11:42 GMT",
            "yarn_nodemanager",
            inputConfigTemplate(new File(HDP_SERVICES_FOLDER, "YARN/package/templates/input.config-yarn.json.j2")));

    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("INFO"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("yarn_nodemanager"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.get("logger_name"), is("zookeeper.ZooKeeper "));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Client environment:zookeeper.version=3.4.6-1173--1,\n built on 04/10/2018 11:42 GMT"));
    assertThat(result.get("line_number"), is("100"));
    assertThat(result.get("file"), is("Environment.java"));
    assertThat(result.get("method"), is("logEnv"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 2, 9, 43, 46, 898000000)));
  }

  @Test
  public void testYarnResourcemanagerLogEntry() throws Exception {
    Map<String, Object> result = testLogEntry("2018-05-02 09:41:43,917 INFO  placement.UserGroupMappingPlacementRule (UserGroupMappingPlacementRule.java:get(232)) - Initialized queue mappings, override: false",
            "yarn_resourcemanager",
            inputConfigTemplate(new File(HDP_SERVICES_FOLDER, "YARN/package/templates/input.config-yarn.json.j2")));

    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("INFO"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("yarn_resourcemanager"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.get("logger_name"), is("placement.UserGroupMappingPlacementRule "));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Initialized queue mappings, override: false"));
    assertThat(result.get("line_number"), is("232"));
    assertThat(result.get("file"), is("UserGroupMappingPlacementRule.java"));
    assertThat(result.get("method"), is("get"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 2, 9, 41, 43, 917000000)));
  }

  @Test
  public void testYarnTimelineServerLogEntry() throws Exception {
    Map<String, Object> result = testLogEntry("2018-05-02 10:36:27,868 INFO  timeline.RollingLevelDB (RollingLevelDB.java:evictOldDBs(345)) - Evicting entity-ldb DBs scheduled for eviction",
            "yarn_timelineserver",
            inputConfigTemplate(new File(HDP_SERVICES_FOLDER, "YARN/package/templates/input.config-yarn.json.j2")));

    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("INFO"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("yarn_timelineserver"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.get("logger_name"), is("timeline.RollingLevelDB "));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Evicting entity-ldb DBs scheduled for eviction"));
    assertThat(result.get("line_number"), is("345"));
    assertThat(result.get("file"), is("RollingLevelDB.java"));
    assertThat(result.get("method"), is("evictOldDBs"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 2, 10, 36, 27, 868000000)));
  }

  @Test
  public void testYarnHistoryServerLogEntry() throws Exception {
    Map<String, Object> result = testLogEntry("2018-05-02 10:02:54,215 INFO  webapp.View (HsJobsBlock.java:render(74)) - Getting list of all Jobs.",
            "mapred_historyserver",
            inputConfigTemplate(new File(HDP_SERVICES_FOLDER, "YARN/package/templates/input.config-mapreduce2.json.j2")));

    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("INFO"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("mapred_historyserver"));
    assertThat(result.get("logger_name"), is("webapp.View "));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Getting list of all Jobs."));
    assertThat(result.get("line_number"), is("74"));
    assertThat(result.get("file"), is("HsJobsBlock.java"));
    assertThat(result.get("method"), is("render"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 2, 10, 2, 54, 215000000)));
  }
}
