/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.core.loadsimulator.data;

import org.apache.ambari.metrics.core.loadsimulator.util.Json;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertEquals;

public class TestMetric {
  private static final String SAMPLE_METRIC_IN_JSON = "{\n" +
    "  \"instanceid\" : \"\",\n" +
    "  \"hostname\" : \"localhost\",\n" +
    "  \"metrics\" : {\n" +
    "    \"0\" : \"5.35\",\n" +
    "    \"5000\" : \"5.35\",\n" +
    "    \"10000\" : \"5.35\",\n" +
    "    \"15000\" : \"5.35\"\n" +
    "  },\n" +
    "  \"starttime\" : \"0\",\n" +
    "  \"appid\" : \"HOST\",\n" +
    "  \"metricname\" : \"disk_free\"\n" +
    "}";

  @Test
  public void testSerializeToJson() throws IOException {
    Metric diskOnHostMetric = new Metric(new ApplicationInstance("localhost", AppID.HOST, ""), "disk_free", 0);

    long timestamp = 0;
    double value = 5.35;

    diskOnHostMetric.putMetric(timestamp, Double.toString(value));
    diskOnHostMetric.putMetric(timestamp + 5000, Double.toString(value));
    diskOnHostMetric.putMetric(timestamp + 10000, Double.toString(value));
    diskOnHostMetric.putMetric(timestamp + 15000, Double.toString(value));

    String expected = SAMPLE_METRIC_IN_JSON;
    String s = new Json(true).serialize(diskOnHostMetric);

    assertEquals("Json should match", expected, s);
  }

  @Test
  public void testDeserializeObjectFromString() throws IOException {
    String source = SAMPLE_METRIC_IN_JSON;

    Metric m = new Json().deserialize(source, Metric.class);

    assertEquals("localhost", m.getHostname());
    assertEquals("HOST", m.getAppid());
    assertEquals("", m.getInstanceid());
    assertEquals("disk_free", m.getMetricname());
    assertEquals("0", m.getStarttime());

    assertThat(m.getMetrics()).isNotEmpty().hasSize(4).contains(
      entry("0", "5.35"),
      entry("5000", "5.35"),
      entry("10000", "5.35"),
      entry("15000", "5.35"));
  }
}