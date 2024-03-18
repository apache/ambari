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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.PatternLayout;
import org.junit.Test;

public class HdfsAuditLogPatternIT extends PatternITBase {

  @Test
  public void testHDFSAudit() throws Exception {
    // given
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "HDFS", "configuration", "hdfs-log4j.xml").toString())).getLayout("RFAS");
    listAppender.setLayout(new PatternLayout(layout));
    listAppender.activateOptions();

    // when
    LOG.info("allowed=true\tugi=hdfs (auth:SIMPLE)\tip=/192.168.73.101\tcmd=getfileinfo\tsrc=/user\tdst=null\tperm=null\tproto=rpc");

    // then
    String logEntry = listAppender.getLogList().get(0);
    Map<String, Object> result = testLogEntry(logEntry, "hdfs_audit", inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "HDFS/package/templates/input.config-hdfs.json.j2")));

    assertAuditLog(result);
  }

  private void assertAuditLog(Map<String, Object> resultEntry) {
    assertThat(resultEntry.isEmpty(), is(false));
    assertThat(resultEntry.get("logType"), is("HDFSAudit"));
    assertThat(resultEntry.get("cluster"), is(CLUSTER));
    assertThat(resultEntry.get("dst"), is("null"));
    assertThat(resultEntry.get("perm"), is("null"));
    assertThat(resultEntry.get("event_count"), is(1));
    assertThat(resultEntry.get("repo"), is("hdfs"));
    assertThat(resultEntry.get("reqUser"), is("hdfs"));
    assertThat(resultEntry.get("type"), is("hdfs_audit"));
    assertThat(resultEntry.get("level"), is("INFO"));
    assertThat(resultEntry.containsKey("seq_num"), is(true));
    assertThat(LOG.getName().contains(resultEntry.get("logger_name").toString()), is(true));
    assertThat(resultEntry.containsKey("id"), is(true));
    assertThat(resultEntry.get("authType"), is("SIMPLE"));
    assertThat(resultEntry.get("action"), is("getfileinfo"));
    assertThat(resultEntry.containsKey("message_md5"), is(true));
    assertThat(resultEntry.containsKey("event_md5"), is(true));
    assertThat(resultEntry.containsKey("ip"), is(true));
    assertThat(resultEntry.containsKey("host"), is(true));
    Date logTime = (Date) resultEntry.get("evtTime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime.toLocalDate(), is(LocalDate.now()));
  }
}
