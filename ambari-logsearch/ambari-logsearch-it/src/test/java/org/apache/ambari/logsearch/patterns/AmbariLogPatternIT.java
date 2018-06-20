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
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

public class AmbariLogPatternIT extends PatternITBase {

  public static File AMBARI_CONF;

  @BeforeClass
  public static void setupAmbariConfig() throws Exception {
    setupGlobal();
    AMBARI_CONF = new File(AMBARI_FOLDER, Paths.get("ambari-server", "conf", "unix").toString());
  }

  @Test
  public void testAmbariAgentLogEntry() throws Exception {
    // given
    String logEntry = "INFO 2018-05-02 09:29:12,359 DataCleaner.py:39 - Data cleanup thread started";
    // when
    Map<String, Object> result = testLogEntry(logEntry, "ambari_agent", ambariInputConfigTemplate());
    // then
    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("ambari_agent"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Data cleanup thread started"));
    assertThat(result.get("file"), is("DataCleaner.py"));
    assertThat(result.get("line_number"), is("39"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 2, 9, 29, 12, 359000000)));
  }

  @Test
  public void testAmbariAgentMultilineLogEntry() throws Exception {
    // given
    String logEntry = "INFO 2018-05-02 09:31:52,227 RecoveryManager.py:572 - RecoverConfig = {u'components': u'',\n" +
            " u'maxCount': u'6',\n" +
            " u'maxLifetimeCount': u'1024',\n" +
            " u'retryGap': u'5',\n" +
            " u'type': u'AUTO_START',\n" +
            " u'windowInMinutes': u'60'}";
    // when
    Map<String, Object> result = testLogEntry(logEntry, "ambari_agent", ambariInputConfigTemplate());
    // then
    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("ambari_agent"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("RecoverConfig = {u'components': u'',\n" +
    " u'maxCount': u'6',\n" +
            " u'maxLifetimeCount': u'1024',\n" +
            " u'retryGap': u'5',\n" +
            " u'type': u'AUTO_START',\n" +
            " u'windowInMinutes': u'60'}"));
    assertThat(result.get("file"), is("RecoveryManager.py"));
    assertThat(result.get("line_number"), is("572"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 2, 9, 31, 52, 227000000)));
  }

  @Test
  public void testAmbariServerLogLayout() {
    testAmbariServerLogLayout("file");
  }

  @Test
  public void testAmbariAlertsLogLayout() {
    testAmbariServerLogLayout("alerts");
  }

  @Test
  public void testAmbariConfigChangesLogLayout() {
    testAmbariServerLogLayout("configchange");
  }

  @Test
  public void testAmbariDbCheckLogLayout() {
    testAmbariServerLogLayout("dbcheckhelper");
  }

  public void testAmbariServerLogLayout(String appenderName) {
    String layout = Log4jProperties.loadFrom(new File(AMBARI_CONF, "log4j.properties")).getLayout(appenderName);
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testAmbariServerLog() throws Exception {
    testAmbariServerLog("file", "ambari_server");
  }

  @Test
  public void testAmbariConfigChangesLog() throws Exception {
    testAmbariServerLog("configchange", "ambari_config_changes");
  }

  @Test
  public void testAmbariDBCheckLog() throws Exception {
    testAmbariServerLog("dbcheckhelper", "ambari_server_check_database");
  }

  public void testAmbariServerLog(String appenderName, String logId) throws Exception {
    String layout = Log4jProperties.loadFrom(new File(AMBARI_CONF, "log4j.properties")).getLayout(appenderName);
    testServiceLog(logId, layout, ambariInputConfigTemplate());
  }

  private String ambariInputConfigTemplate() throws IOException {
    return inputConfigTemplate(
            new File(AMBARI_STACK_DEFINITIONS, "LOGSEARCH/0.5.0/properties/input.config-ambari.json.j2"));
  }

  @Test
  public void testAmbariAlertsLog() throws Exception {
    // given
    String layout = Log4jProperties.loadFrom(new File(AMBARI_CONF, "log4j.properties")).getLayout("alerts");
    String logEntry = generateLogEntry(layout);
    // when
    Map<String, Object> result = testLogEntry(logEntry, "ambari_alerts", ambariInputConfigTemplate());
    // then
    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("ambari_alerts"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message").toString().contains("This is a test message"), is(true));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime.toLocalDate(), is(LocalDate.now()));
  }

  @Test
  public void testAmbariEclipseLinkSevereEntry() throws Exception {
    testAmbariEclipseLinkEntry("Severe", "ERROR");
  }

  @Test
  public void testAmbariEclipseLinkWarningEntry() throws Exception {
    testAmbariEclipseLinkEntry("Warning", "WARN");
  }

  @Test
  public void testAmbariEclipseLinkInfoEntry() throws Exception {
    testAmbariEclipseLinkEntry("Info", "INFO");
  }

  @Test
  public void testAmbariEclipseLinkConfigEntry() throws Exception {
    testAmbariEclipseLinkEntry("Config", "INFO");
  }

  private void testAmbariEclipseLinkEntry(String logLevel, String expectedLogLevel) throws Exception {
    // given
    String logEntry = "[EL " + logLevel + "]: 2018-05-02 09:27:17.79--ServerSession(1657512321)-- EclipseLink, version: Eclipse Persistence Services - 2.6.2.v20151217-774c696";
    // when
    Map<String, Object> result = testLogEntry(logEntry, "ambari_eclipselink", ambariInputConfigTemplate());
    // then
    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("level"), is(expectedLogLevel));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("ambari_eclipselink"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("--ServerSession(1657512321)-- EclipseLink, version: Eclipse Persistence Services - 2.6.2.v20151217-774c696"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 2, 9, 27, 17, 79000000)));

  }

  @Test
  public void testAmbariAuditLogEntry() throws Exception {
    // given
    String logEntry = "2018-05-02T09:28:10.302Z, User(null), RemoteIp(192.175.27.2), Operation(User login), Roles(\n" +
            "), Status(Failed), Reason(Authentication required), Consecutive failures(UNKNOWN USER)\n" +
            "2018-05-02T09:28:10.346Z, User(admin), RemoteIp(192.175.27.2), Operation(User login), Roles(\n" +
            "    Ambari: Ambari Administrator\n" +
            "), Status(Success)";
    // when
    Map<String, Object> result = testLogEntry(logEntry, "ambari_audit", ambariInputConfigTemplate());
    // then
    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("ambari_audit"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("User(null), RemoteIp(192.175.27.2), Operation(User login), Roles(\n" +
            "), Status(Failed), Reason(Authentication required), Consecutive failures(UNKNOWN USER)\n" +
                    "2018-05-02T09:28:10.346Z, User(admin), RemoteIp(192.175.27.2), Operation(User login), Roles(\n" +
                    "    Ambari: Ambari Administrator\n" +
                    "), Status(Success)"));
    Date logTime = (Date) result.get("evtTime");
    ZonedDateTime localDateTime = ZonedDateTime.ofInstant(logTime.toInstant(), ZoneId.of("Z"));
    assertThat(localDateTime, is(ZonedDateTime.parse("2018-05-02T09:28:10.302Z")));
  }
}
