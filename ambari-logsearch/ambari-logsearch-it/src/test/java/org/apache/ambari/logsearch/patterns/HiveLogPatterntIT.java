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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class HiveLogPatterntIT extends PatternITBase {

  @Test
  public void testHiveServerLogEntry() throws Exception {
    String logEntry = "2018-05-11T07:46:01,087 WARN  [main]: metastore.HiveMetaStoreClient (:()) - Failed to connect to the MetaStore Server...";
    Map<String, Object> result = testLogEntry(logEntry,"hive_server", inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "HIVE/package/templates/input.config-hive.json.j2")));

    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("WARN"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("hive_server"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Failed to connect to the MetaStore Server..."));
    assertThat(result.get("logger_name"), is("metastore.HiveMetaStoreClient "));
    assertThat(result.get("host"), is("HW13201.local"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    MatcherAssert.assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 11, 7, 46, 1, 87000000)));
  }

  @Test
  public void testHiveServerInteractiveLogEntry() throws Exception {
    String logEntry = "2018-05-11T08:48:02,973 WARN  [main]: conf.HiveConf (HiveConf.java:initialize(5193)) - HiveConf of name hive.hook.proto.base-directory does not exist";
    Map<String, Object> result = testLogEntry(logEntry,"hive_server_interactive", inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "HIVE/package/templates/input.config-hive.json.j2")));

    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("WARN"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("hive_server_interactive"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("HiveConf of name hive.hook.proto.base-directory does not exist"));
    assertThat(result.get("logger_name"), is("conf.HiveConf "));
    assertThat(result.get("host"), is("HW13201.local"));
    assertThat(result.get("file"), is("HiveConf.java"));
    assertThat(result.get("method"), is("initialize"));
    assertThat(result.get("line_number"), is("5193"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    MatcherAssert.assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 11, 8, 48, 2, 973000000)));
  }

  @Test
  public void testHiveMetastoreLogEntry() throws Exception {
    String logEntry = "2018-05-11T09:13:14,706 INFO  [pool-7-thread-6]: txn.TxnHandler (TxnHandler.java:performWriteSetGC(1588)) - Deleted 0 obsolete rows from WRTIE_SET";
    Map<String, Object> result = testLogEntry(logEntry,"hive_metastore", inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "HIVE/package/templates/input.config-hive.json.j2")));

    assertThat(result.isEmpty(), is(false));
    assertThat(result.get("cluster"), is(CLUSTER));
    assertThat(result.get("level"), is("INFO"));
    assertThat(result.get("event_count"), is(1));
    assertThat(result.get("type"), is("hive_metastore"));
    assertThat(result.containsKey("seq_num"), is(true));
    assertThat(result.containsKey("id"), is(true));
    assertThat(result.containsKey("message_md5"), is(true));
    assertThat(result.containsKey("event_md5"), is(true));
    assertThat(result.containsKey("ip"), is(true));
    assertThat(result.containsKey("host"), is(true));
    assertThat(result.get("log_message"), is("Deleted 0 obsolete rows from WRTIE_SET"));
    assertThat(result.get("logger_name"), is("txn.TxnHandler "));
    assertThat(result.get("host"), is("HW13201.local"));
    assertThat(result.get("line_number"), is("1588"));
    assertThat(result.get("file"), is("TxnHandler.java"));
    assertThat(result.get("method"), is("performWriteSetGC"));
    Date logTime = (Date) result.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    MatcherAssert.assertThat(localDateTime, is(LocalDateTime.of(2018, 5, 11, 9, 13, 14, 706000000)));
  }
}

