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

public class KafkaLogPatternIT extends PatternITBase {

  @Test
  public void testKafkaRequestAppenderLayout() {
    testKafkaAppenderLayout("requestAppender");
  }

  @Test
  public void testKafkaControllerAppenderLayout() {
    testKafkaAppenderLayout("controllerAppender");
  }

  @Test
  public void testKafkaLogCleanerAppenderLayout() {
    testKafkaAppenderLayout("cleanerAppender");
  }

  @Test
  public void testKafkaStateChangeAppenderLayout() {
    testKafkaAppenderLayout("stateChangeAppender");
  }

  @Test
  public void testKafkaServerAppenderLayout() {
    testKafkaAppenderLayout("kafkaAppender");
  }

  private void testKafkaAppenderLayout(String appender) {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "KAFKA", "configuration", "kafka-log4j.xml").toString())).getLayout(appender);
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testKafkaRequestAppender() throws Exception {
    testKafka("requestAppender", "kafka_request");
  }

  @Test
  public void testKafkaControllerAppender() throws Exception {
    testKafka("controllerAppender", "kafka_controller");
  }

  @Test
  public void testKafkaLogCleanerAppender() throws Exception {
    testKafka("cleanerAppender", "kafka_logcleaner");
  }

  @Test
  public void testKafkaStateChangeAppender() throws Exception {
    testKafka("stateChangeAppender", "kafka_statechange");
  }

  @Test
  public void testKafkaServerAppender() throws Exception {
    testKafka("kafkaAppender", "kafka_server");
  }

  private void testKafka(String appender, String logId) throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "KAFKA", "configuration", "kafka-log4j.xml").toString())).getLayout(appender);

    testServiceLog(logId, layout, inputConfigTemplate(new File(HDP_SERVICES_FOLDER, "KAFKA/package/templates/input.config-kafka.json.j2")));
  }
}
