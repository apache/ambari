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

public class MetricsLogPatternIT extends PatternITBase {

  @Test
  public void testMetricsLogLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "AMBARI_METRICS", "configuration", "ams-log4j.xml").toString())).getLayout("file");
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testMetrics() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "AMBARI_METRICS", "configuration", "ams-log4j.xml").toString())).getLayout("file");

    testServiceLog("ams_collector", layout, inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "AMBARI_METRICS/package/templates/input.config-ambari-metrics.json.j2")));
//    testServiceLog("ams_monitor", layout, Paths.get("AMBARI_METRICS", "package", "templates", "input.config-ambari-metrics.json.j2"));
  }

//  @Test
//  public void testMetricsGrafana() throws Exception {
//    testServiceLog("ams_grafana", "%d{ISO8601} %-5p [%t] %c{2}: %m%n", Paths.get("AMBARI_METRICS", "package", "templates", "input.config-ambari-metrics.json.j2"));
//  }

  @Test
  public void testMetricsHBaseLogLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "AMBARI_METRICS", "configuration", "ams-hbase-log4j.xml").toString())).getLayout("DRFA");
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testMetricsHBase() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "AMBARI_METRICS", "configuration", "ams-hbase-log4j.xml").toString())).getLayout("DRFA");

    testServiceLog("ams_hbase_master", layout, inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "AMBARI_METRICS/package/templates/input.config-ambari-metrics.json.j2")));
    testServiceLog("ams_hbase_regionserver", layout, inputConfigTemplate(
            new File(HDP_SERVICES_FOLDER, "AMBARI_METRICS/package/templates/input.config-ambari-metrics.json.j2")));
  }
}
