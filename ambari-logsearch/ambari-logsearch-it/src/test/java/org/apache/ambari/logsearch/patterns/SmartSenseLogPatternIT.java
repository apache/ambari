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

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class SmartSenseLogPatternIT extends PatternITBase {

  // TODO: read input config from hdp-ambari-definitions when available

  @Test
  public void testHSTServerLogEntry() throws Exception {
    //given
    String logEntry = "2018-05-02 09:40:14,740  INFO [main] SupportToolServer:143 - Starting HST Server.";
    // when
    Map<String, Object> result = testLogEntry(logEntry, "hst_server", inputConfigTemplate());
    // then
    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("INFO"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("hst_server"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.get("logger_name"), is("SupportToolServer"));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Starting HST Server."));
    assertThat(result.get("line_number"), is("143"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 2, 9, 40,  14, 740000000)));
  }

  private String inputConfigTemplate() throws IOException {
    return IOUtils.toString(getClass().getClassLoader().getResourceAsStream("test-input-config/input.config-smartsense.json.j2"), Charset.defaultCharset());
  }

  @Test
  public void testHSTAgentLogEntry() throws Exception {
    // given
    String logEntry = "INFO 2018-05-02 09:32:47,197 security.py:177 - Server certificate not exists, downloading";
    // when
    Map<String, Object> result = testLogEntry(logEntry, "hst_agent", inputConfigTemplate());
    // then
    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("INFO"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("hst_agent"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.get("file"), is("security.py"));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Server certificate not exists, downloading"));
    assertThat(result.get("line_number"), is("177"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 2, 9, 32,  47, 197000000)));
  }

  @Test
  public void testActivityAnalyserLogEntry() throws Exception {
    // given
    String logEntry = "2018-05-02 10:23:49,592  INFO [main] ActivityUtil:410 - Could not find valid SmartSense ID. Will recheck every 5 minutes for next 5 minutes.";
    // when
    Map<String, Object> result = testLogEntry(logEntry, "activity_analyser", inputConfigTemplate());
    // then
    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("INFO"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("activity_analyser"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.get("logger_name"), is("ActivityUtil"));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Could not find valid SmartSense ID. Will recheck every 5 minutes for next 5 minutes."));
    assertThat(result.get("line_number"), is("410"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 2, 10, 23,  49, 592000000)));
  }

  @Test
  public void testActivityExplorerLogEntry() throws Exception {
    // given
    String logEntry = "2018-05-02 09:44:26,883  INFO [main] FileSystemConfigStorage:74 - Creating filesystem: org.apache.hadoop.fs.RawLocalFileSystem";
    // when
    Map<String, Object> result = testLogEntry(logEntry, "activity_explorer", inputConfigTemplate());
    // then
    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("INFO"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("activity_explorer"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.get("logger_name"), is("FileSystemConfigStorage"));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Creating filesystem: org.apache.hadoop.fs.RawLocalFileSystem"));
    assertThat(result.get("line_number"), is("74"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 2, 9, 44,  26, 883000000)));
  }
}
