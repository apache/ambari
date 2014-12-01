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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.loadsimulator.data;

import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.loadsimulator.util.Json;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestAppMetrics {
  private static final String SAMPLE_SINGLE_METRIC_HOST_JSON = "{\n" +
    "  \"metrics\" : [ {\n" +
    "    \"instanceid\" : \"\",\n" +
    "    \"hostname\" : \"localhost\",\n" +
    "    \"metrics\" : {\n" +
    "      \"0\" : \"5.35\",\n" +
    "      \"5000\" : \"5.35\",\n" +
    "      \"10000\" : \"5.35\",\n" +
    "      \"15000\" : \"5.35\"\n" +
    "    },\n" +
    "    \"starttime\" : \"1411663170112\",\n" +
    "    \"appid\" : \"HOST\",\n" +
    "    \"metricname\" : \"disk_free\"\n" +
    "  } ]\n" +
    "}";

  private static final String SAMPLE_TWO_METRIC_HOST_JSON = "{\n" +
    "  \"metrics\" : [ {\n" +
    "    \"instanceid\" : \"\",\n" +
    "    \"hostname\" : \"localhost\",\n" +
    "    \"metrics\" : {\n" +
    "      \"0\" : \"5.35\",\n" +
    "      \"5000\" : \"5.35\",\n" +
    "      \"10000\" : \"5.35\",\n" +
    "      \"15000\" : \"5.35\"\n" +
    "    },\n" +
    "    \"starttime\" : \"0\",\n" +
    "    \"appid\" : \"HOST\",\n" +
    "    \"metricname\" : \"disk_free\"\n" +
    "  }, {\n" +
    "    \"instanceid\" : \"\",\n" +
    "    \"hostname\" : \"localhost\",\n" +
    "    \"metrics\" : {\n" +
    "      \"0\" : \"94.0\",\n" +
    "      \"5000\" : \"94.0\",\n" +
    "      \"10000\" : \"94.0\",\n" +
    "      \"15000\" : \"94.0\"\n" +
    "    },\n" +
    "    \"starttime\" : \"0\",\n" +
    "    \"appid\" : \"HOST\",\n" +
    "    \"metricname\" : \"mem_cached\"\n" +
    "  } ]\n" +
    "}";

  private long[] timestamps;

  @Before
  public void setUp() throws Exception {
    timestamps = new long[4];
    timestamps[0] = 0;
    timestamps[1] = timestamps[0] + 5000;
    timestamps[2] = timestamps[1] + 5000;
    timestamps[3] = timestamps[2] + 5000;

  }

  @Test
  public void testHostDiskMetricsSerialization() throws IOException {
    long timestamp = 1411663170112L;
    AppMetrics appMetrics = new AppMetrics(new ApplicationInstance("localhost", AppID.HOST, ""), timestamp);

    Metric diskFree = appMetrics.createMetric("disk_free");
    double value = 5.35;

    diskFree.putMetric(timestamps[0], Double.toString(value));
    diskFree.putMetric(timestamps[1], Double.toString(value));
    diskFree.putMetric(timestamps[2], Double.toString(value));
    diskFree.putMetric(timestamps[3], Double.toString(value));

    appMetrics.addMetric(diskFree);

    String expected = SAMPLE_SINGLE_METRIC_HOST_JSON;
    String s = new Json(true).serialize(appMetrics);

    assertEquals("Serialized Host Metrics", expected, s);
  }


  @Test
  public void testSingleHostManyMetricsSerialization() throws IOException {
    AppMetrics appMetrics = new AppMetrics(new ApplicationInstance("localhost", AppID.HOST, ""), timestamps[0]);

    Metric diskFree = appMetrics.createMetric("disk_free");
    double value = 5.35;
    diskFree.putMetric(timestamps[0], Double.toString(value));
    diskFree.putMetric(timestamps[1], Double.toString(value));
    diskFree.putMetric(timestamps[2], Double.toString(value));
    diskFree.putMetric(timestamps[3], Double.toString(value));

    appMetrics.addMetric(diskFree);

    Metric memCache = appMetrics.createMetric("mem_cached");
    double memVal = 94;
    memCache.putMetric(timestamps[0], Double.toString(memVal));
    memCache.putMetric(timestamps[1], Double.toString(memVal));
    memCache.putMetric(timestamps[2], Double.toString(memVal));
    memCache.putMetric(timestamps[3], Double.toString(memVal));

    appMetrics.addMetric(memCache);

    String expected = SAMPLE_TWO_METRIC_HOST_JSON;
    String s = new Json(true).serialize(appMetrics);

    assertEquals("Serialized Host Metrics", expected, s);
  }
}