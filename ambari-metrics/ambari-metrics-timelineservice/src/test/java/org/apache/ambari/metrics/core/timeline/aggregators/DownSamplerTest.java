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

package org.apache.ambari.metrics.core.timeline.aggregators;

import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class DownSamplerTest {

  @Test
  public void testGetDownsampleMetricPatterns() throws Exception {

    Configuration configuration = new Configuration();
    configuration.setIfUnset("timeline.metrics.downsampler.topn.metric.patterns", "pattern1,pattern2");
    configuration.setIfUnset("timeline.metrics.downsampler.lastvalue.metric.patterns", "pattern3");

    List<String> patterns = DownSamplerUtils.getDownsampleMetricPatterns(configuration);
    Assert.assertEquals(patterns.size(), 3);
    Assert.assertTrue(patterns.contains("pattern1"));
    Assert.assertTrue(patterns.contains("pattern2"));
    Assert.assertTrue(patterns.contains("pattern3"));

    Configuration configuration2 = new Configuration();
    patterns = DownSamplerUtils.getDownsampleMetricPatterns(configuration2);
    Assert.assertEquals(patterns.size(), 0);
  }

  @Test
  public void testGetDownSamplers() throws Exception {

    Configuration configuration = new Configuration();
    configuration.setIfUnset("timeline.metrics.downsampler.topn.metric.patterns", "pattern1,pattern2");
    configuration.setIfUnset("timeline.metrics.downsampler.test.metric.patterns", "pattern3");

    List<CustomDownSampler> downSamplers = DownSamplerUtils.getDownSamplers(configuration);
    Assert.assertEquals(downSamplers.size(), 1);
    Assert.assertTrue(downSamplers.get(0) instanceof TopNDownSampler);
  }

  @Ignore
  @Test
  public void testPrepareTopNDownSamplingStatement() throws Exception {
    Configuration configuration = new Configuration();
    configuration.setIfUnset("timeline.metrics.downsampler.topn.metric.patterns", "pattern1,pattern2");
    configuration.setIfUnset("timeline.metrics.downsampler.topn.value", "3");

    Map<String, String> conf = configuration.getValByRegex(DownSamplerUtils.downSamplerConfigPrefix);

    TopNDownSampler topNDownSampler = TopNDownSampler.fromConfig(conf);
    List<String> stmts = topNDownSampler.prepareDownSamplingStatement(14000000l, 14100000l, "METRIC_RECORD_UUID");
    Assert.assertEquals(stmts.size(),2);
    Assert.assertTrue(stmts.contains("SELECT METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, 14100000 AS SERVER_TIME, UNITS, " +
      "MAX(METRIC_MAX), 1, MAX(METRIC_MAX), MAX(METRIC_MAX) FROM METRIC_RECORD_UUID WHERE " +
      "METRIC_NAME LIKE 'pattern1' AND SERVER_TIME > 14000000 AND SERVER_TIME <= 14100000 " +
      "GROUP BY METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, UNITS ORDER BY MAX(METRIC_MAX) DESC LIMIT 3"));

    Assert.assertTrue(stmts.contains("SELECT METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, 14100000 AS SERVER_TIME, UNITS, " +
      "MAX(METRIC_MAX), 1, MAX(METRIC_MAX), MAX(METRIC_MAX) FROM METRIC_RECORD_UUID WHERE " +
      "METRIC_NAME LIKE 'pattern2' AND SERVER_TIME > 14000000 AND SERVER_TIME <= 14100000 " +
      "GROUP BY METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, UNITS ORDER BY MAX(METRIC_MAX) DESC LIMIT 3"));

    configuration.clear();
    configuration.setIfUnset("timeline.metrics.downsampler.topn.metric.patterns", "pattern1");
    configuration.setIfUnset("timeline.metrics.downsampler.topn.value", "4");
    configuration.setIfUnset("timeline.metrics.downsampler.topn.function", "sum");
    conf = configuration.getValByRegex(DownSamplerUtils.downSamplerConfigPrefix);
    topNDownSampler = TopNDownSampler.fromConfig(conf);
    stmts = topNDownSampler.prepareDownSamplingStatement(14000000l, 14100000l, "METRIC_AGGREGATE_MINUTE_UUID");
    Assert.assertEquals(stmts.size(),1);

    Assert.assertTrue(stmts.contains("SELECT METRIC_NAME, APP_ID, INSTANCE_ID, 14100000 AS SERVER_TIME, UNITS, " +
      "SUM(METRIC_SUM), 1, SUM(METRIC_SUM), SUM(METRIC_SUM) FROM METRIC_AGGREGATE_MINUTE_UUID WHERE " +
      "METRIC_NAME LIKE 'pattern1' AND SERVER_TIME > 14000000 AND SERVER_TIME <= 14100000 " +
      "GROUP BY METRIC_NAME, APP_ID, INSTANCE_ID, UNITS ORDER BY SUM(METRIC_SUM) DESC LIMIT 4"));
  }

  @Test
  public void testPrepareEventDownSamplingStatement() throws Exception {
    Configuration configuration = new Configuration();
    configuration.setIfUnset("timeline.metrics.downsampler.event.metric.patterns", "pattern1,pattern2");

    Map<String, String> conf = configuration.getValByRegex(DownSamplerUtils.downSamplerConfigPrefix);

    EventMetricDownSampler eventMetricDownSampler = EventMetricDownSampler.fromConfig(conf);
    List<String> stmts = eventMetricDownSampler.prepareDownSamplingStatement(14000000l, 14100000l, "METRIC_RECORD_UUID");
    Assert.assertEquals(stmts.size(),2);

    Assert.assertTrue(stmts.get(0).equals("SELECT METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, 14100000 AS SERVER_TIME, " +
      "UNITS, SUM(METRIC_SUM), SUM(METRIC_COUNT), MAX(METRIC_MAX), MIN(METRIC_MIN) FROM METRIC_RECORD_UUID WHERE METRIC_NAME " +
      "LIKE 'pattern1' AND SERVER_TIME > 14000000 AND SERVER_TIME <= 14100000 GROUP BY METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, UNITS"));

    Assert.assertTrue(stmts.get(1).equals("SELECT METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, 14100000 AS SERVER_TIME, " +
      "UNITS, SUM(METRIC_SUM), SUM(METRIC_COUNT), MAX(METRIC_MAX), MIN(METRIC_MIN) FROM METRIC_RECORD_UUID WHERE METRIC_NAME " +
      "LIKE 'pattern2' AND SERVER_TIME > 14000000 AND SERVER_TIME <= 14100000 GROUP BY METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, UNITS"));
  }
}
