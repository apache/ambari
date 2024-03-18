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
package org.apache.ambari.infra.solr.metrics.reporters;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TreeMap;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsUtils {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsUtils.class);
  private static final String APPID = "ambari-infra-solr";
  public static final String NAME_PREFIX = "infra.";

  private static final String hostName = initHostName();

  private static String initHostName() {
    String hostName = null;
    try {
      InetAddress ip = InetAddress.getLocalHost();
      String ipAddress = ip.getHostAddress();
      String ipHostName = ip.getHostName();
      String canonicalHostName = ip.getCanonicalHostName();
      if (!canonicalHostName.equalsIgnoreCase(ipAddress)) {
        LOG.info("Using InetAddress.getCanonicalHostName()={}", canonicalHostName);
        hostName = canonicalHostName;
      } else {
        LOG.info("Using InetAddress.getHostName()={}", ipHostName);
        hostName = ipHostName;
      }
      LOG.info("ipAddress={}, ipHostName={}, canonicalHostName={}, hostName={}", ipAddress, ipHostName, canonicalHostName, hostName);
    } catch (UnknownHostException e) {
      LOG.error("Error getting hostname.", e);
    }

    return hostName;
  }

  public static String getHostName() {
    return hostName;
  }

  public static TimelineMetric toTimelineMetric(String name, double value, long currentMillis) {
    TimelineMetric timelineMetric = newTimelineMetric();
    timelineMetric.setMetricName(name);
    timelineMetric.setStartTime(currentMillis);
    timelineMetric.setType("Long");
    TreeMap<Long, Double> metricValues = new TreeMap<>();
    metricValues.put(currentMillis, value);
    timelineMetric.setMetricValues(metricValues);
    return timelineMetric;
  }

  private static TimelineMetric newTimelineMetric() {
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setAppId(APPID);
    timelineMetric.setHostName(getHostName());
    return timelineMetric;
  }
}
