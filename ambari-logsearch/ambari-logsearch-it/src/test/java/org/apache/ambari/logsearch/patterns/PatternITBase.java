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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.logfeeder.common.LogEntryParseTester;
import org.apache.ambari.logsearch.config.json.model.inputconfig.impl.InputAdapter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;

public class PatternITBase {
  protected final static Logger LOG = Logger.getLogger(PatternITBase.class);

  public static File HDP_AMBARI_DEFINITIONS;
  public static File AMBARI_STACK_DEFINITIONS;
  public static File AMBARI_FOLDER;
  public static File HDP_SERVICES_FOLDER;
  public static final String CLUSTER = "cl1";
  public static final String GLOBAL_CONFIG = "[\n" +
          "    {\n" +
          "      \"add_fields\": {\n" +
          "        \"cluster\": \""+ CLUSTER +"\"\n" +
          "      },\n" +
          "      \"source\": \"file\",\n" +
          "      \"tail\": \"true\",\n" +
          "      \"gen_event_md5\": \"true\"\n" +
          "    }\n" +
          "]";

  private Jinjava jinjava = new Jinjava();
  protected ListAppender listAppender;


  @BeforeClass
  public static void setupGlobal() throws Exception {
    String hdpAmbariDefinitionsPath = System.getProperty("hdp.ambari.definitions.path");
    if (isNotBlank(hdpAmbariDefinitionsPath)) {
      HDP_AMBARI_DEFINITIONS = new File(hdpAmbariDefinitionsPath);
      HDP_SERVICES_FOLDER = new File(HDP_AMBARI_DEFINITIONS, Paths.get( "src", "main", "resources", "stacks", "HDP", "3.0", "services").toString());
    }

    assumeTrue(HDP_SERVICES_FOLDER != null && HDP_SERVICES_FOLDER.exists());

    URL location = PatternITBase.class.getProtectionDomain().getCodeSource().getLocation();

    AMBARI_FOLDER = new File(new File(location.toURI()).getParentFile().getParentFile().getParentFile().getParent());
    AMBARI_STACK_DEFINITIONS = new File(AMBARI_FOLDER, Paths.get("ambari-server", "src", "main", "resources", "common-services").toString());
  }

  @Before
  public void setUp() throws Exception {
    JsonParser jsonParser = new JsonParser();
    JsonElement globalConfigJsonElement = jsonParser.parse(GLOBAL_CONFIG);

    InputAdapter.setGlobalConfigs(globalConfigJsonElement.getAsJsonArray());
    jinjava.getGlobalContext().registerFunction(new ELFunctionDefinition("", "default", JinjaFunctions.class, "defaultFunc", Object.class, Object.class));

    listAppender = new ListAppender();
    LOG.addAppender(listAppender);
  }

  protected String inputConfigTemplate(File templateFile) throws IOException {
    return FileUtils.readFileToString(templateFile, Charset.defaultCharset());
  }

  protected void testServiceLog(String logId, String log4jLayout, String inputConfigTemplate) throws Exception {
    String logEntry = generateLogEntry(log4jLayout);
    Map<String, Object> resultEntry = testLogEntry(logEntry, logId, inputConfigTemplate);
    assertServiceLog(logId, resultEntry);
  }

  protected String generateLogEntry(String log4jLayout) {
    return generateLogEntry(log4jLayout, "This is a test message");
  }

  protected String generateLogEntry(String log4jLayout, String message) {
    listAppender.setLayout(new PatternLayout(log4jLayout));
    listAppender.activateOptions();
    LOG.error(message, new Exception("TEST"));
    return listAppender.getLogList().get(0);
  }

  protected Map<String, Object> testLogEntry(String logEntry, String logId, String inputConfigTemplate) throws Exception {
    String grokFilter = jinjava.render(inputConfigTemplate, new HashMap<>());

    LogEntryParseTester tester = new LogEntryParseTester(logEntry, grokFilter, GLOBAL_CONFIG, logId);
    return tester.parse();
  }

  private void assertServiceLog(String logId, Map<String, Object> resultEntry) {
    assertThat(resultEntry.isEmpty(), is(false));
    assertThat(resultEntry.get("cluster"), is(CLUSTER));
    assertThat(resultEntry.get("level"), is("ERROR"));
    assertThat(resultEntry.get("event_count"), is(1));
    assertThat(resultEntry.get("type"), is(logId));
    assertThat(resultEntry.containsKey("seq_num"), is(true));
//    assertThat(LOG.getName().contains(resultEntry.get("logger_name").toString()), is(true));
    assertThat(resultEntry.containsKey("id"), is(true));
    assertThat(resultEntry.containsKey("message_md5"), is(true));
    assertThat(resultEntry.containsKey("event_md5"), is(true));
    assertThat(resultEntry.containsKey("ip"), is(true));
    assertThat(resultEntry.containsKey("host"), is(true));
    assertThat(resultEntry.get("log_message").toString().contains("This is a test message"), is(true));
    assertThat(resultEntry.get("log_message").toString().contains("java.lang.Exception: TEST"), is(true));
    Date logTime = (Date) resultEntry.get("logtime");
    LocalDateTime localDateTime = LocalDateTime.ofInstant(logTime.toInstant(), ZoneId.systemDefault());
    assertThat(localDateTime.toLocalDate(), is(LocalDate.now()));
  }

  protected void assertThatDateIsISO8601(String layout) {
    assertThat(layout.toLowerCase().contains("%d{iso8601}"), is(true));
  }
}
