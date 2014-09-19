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

package org.apache.ambari.view.slider.rest.client;

import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.slider.GangliaMetric;
import org.apache.ambari.view.slider.TemporalInfo;
import org.apache.log4j.Logger;
import org.apache.http.client.utils.URIBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SliderAppGangliaHelper {

  private static final Logger logger = Logger
      .getLogger(SliderAppGangliaHelper.class);

  private static String getSetString(Set<String> set, int limit) {
    StringBuilder sb = new StringBuilder();

    if (limit == -1 || set.size() <= limit) {
      for (String cluster : set) {
        if (sb.length() > 0) {
          sb.append(",");
        }
        sb.append(cluster);
      }
    }
    return sb.toString();
  }

  private static Number convertToNumber(String s) {
    return s.contains(".") ? Double.parseDouble(s) : Long.parseLong(s);
  }

  public static Map<String, GangliaMetric> getGangliaMetrics(ViewContext context,
                                                             String spec,
                                                             String params) throws IOException {
    Map<String, GangliaMetric> receivedMetrics = new HashMap<String, GangliaMetric>();
    Map<String, String> headers = new HashMap<String, String>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        context.getURLStreamProvider().readFrom(spec, "POST", params, headers)));

    String feedStart = reader.readLine();
    if (feedStart == null || feedStart.isEmpty()) {
      logger.info("Empty feed while getting ganglia metrics for spec => " +
                  spec);
      return null;
    }
    int startTime = convertToNumber(feedStart).intValue();

    String dsName = reader.readLine();
    if (dsName == null || dsName.isEmpty()) {
      logger.info("Feed without body while reading ganglia metrics for spec " +
                  "=> " + spec);
      return null;
    }

    while (!"[~EOF]".equals(dsName)) {
      GangliaMetric metric = new GangliaMetric();
      List<GangliaMetric.TemporalMetric> listTemporalMetrics =
          new ArrayList<GangliaMetric.TemporalMetric>();

      metric.setDs_name(dsName);
      metric.setCluster_name(reader.readLine());
      metric.setHost_name(reader.readLine());
      metric.setMetric_name(reader.readLine());

      String timeStr = reader.readLine();
      String stepStr = reader.readLine();
      if (timeStr == null || timeStr.isEmpty() || stepStr == null
          || stepStr.isEmpty()) {
        logger.info("Unexpected end of stream reached while getting ganglia " +
                    "metrics for spec => " + spec);
        return null;
      }
      int time = convertToNumber(timeStr).intValue();
      int step = convertToNumber(stepStr).intValue();

      String val = reader.readLine();
      String lastVal = null;

      while (val != null && !"[~EOM]".equals(val)) {
        if (val.startsWith("[~r]")) {
          Integer repeat = Integer.valueOf(val.substring(4)) - 1;
          for (int i = 0; i < repeat; ++i) {
            if (!"[~n]".equals(lastVal)) {
              GangliaMetric.TemporalMetric tm = new GangliaMetric.TemporalMetric(lastVal, time);
              if (tm.isValid()) listTemporalMetrics.add(tm);
            }
            time += step;
          }
        } else {
          if (!"[~n]".equals(val)) {
            GangliaMetric.TemporalMetric tm = new GangliaMetric.TemporalMetric(val, time);
            if (tm.isValid()) listTemporalMetrics.add(tm);
          }
          time += step;
        }
        lastVal = val;
        val = reader.readLine();
      }

      metric.setDatapointsFromList(listTemporalMetrics);
      receivedMetrics.put(metric.getMetric_name(), metric);

      dsName = reader.readLine();
      if (dsName == null || dsName.isEmpty()) {
        logger.info("Unexpected end of stream reached while getting ganglia " +
                    "metrics for spec => " + spec);
        return null;
      }
    }
    String feedEnd = reader.readLine();
    if (feedEnd == null || feedEnd.isEmpty()) {
      logger.info("Error reading end of feed while getting ganglia metrics " +
                  "for spec => " + spec);
    } else {

      int endTime = convertToNumber(feedEnd).intValue();
      int totalTime = endTime - startTime;
      if (logger.isInfoEnabled() && totalTime > 3) {
        logger.info("Ganglia resource population time: " + totalTime);
      }
    }
    return receivedMetrics;
  }

  public static String getSpec(String gangliaUrl,
                               Set<String> metricSet,
                               TemporalInfo temporalInfo) throws SystemException, URISyntaxException {

    String metrics = getSetString(metricSet, -1);

    URIBuilder uriBuilder = new URIBuilder(gangliaUrl);

    uriBuilder.setParameter("h", "__SummaryInfo__");

    if (metrics.length() > 0) {
      uriBuilder.setParameter("m", metrics);
    } else {
      // get all metrics
      uriBuilder.setParameter("m", ".*");
    }

    if (temporalInfo != null) {
      long startTime = temporalInfo.getStartTime();
      if (startTime != -1) {
        uriBuilder.setParameter("s", String.valueOf(startTime));
      }

      long endTime = temporalInfo.getEndTime();
      if (endTime != -1) {
        uriBuilder.setParameter("e", String.valueOf(endTime));
      }

      long step = temporalInfo.getStep();
      if (step != -1) {
        uriBuilder.setParameter("r", String.valueOf(step));
      }
    } else {
      long endTime = System.currentTimeMillis() / 1000;
      long startTime = System.currentTimeMillis() / 1000 - 60 * 60;
      uriBuilder.setParameter("e", String.valueOf(endTime));
      uriBuilder.setParameter("s", String.valueOf(startTime));
      uriBuilder.setParameter("r", "15");
    }

    return uriBuilder.toString();
  }
}
