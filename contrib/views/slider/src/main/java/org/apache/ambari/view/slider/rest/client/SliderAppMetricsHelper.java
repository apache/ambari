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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.slider.TemporalInfo;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SliderAppMetricsHelper {
  private static final Logger logger = LoggerFactory
      .getLogger(SliderAppMetricsHelper.class);
  private static ObjectMapper mapper;
  private final static ObjectReader timelineObjectReader;
  private final static String URI_PARAM_APP_ID = "appId";
  private final static String URI_PARAM_METRIC_NAMES = "metricNames";
  private final static String URI_PARAM_START_TIME = "startTime";
  private final static String URI_PARAM_END_TIME = "endTime";

  static {
    mapper = new ObjectMapper();
    AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    // no inspection deprecation
    mapper.getSerializationConfig().setSerializationInclusion(
        Inclusion.NON_NULL);
    timelineObjectReader = mapper.reader(TimelineMetrics.class);
  }

  public static Map<String, Number[][]> getMetrics(ViewContext context,
      String spec, String params) throws IOException {
    Map<String, Number[][]> receivedMetrics = new HashMap<String, Number[][]>();
    Map<String, String> headers = new HashMap<String, String>();

    BufferedReader reader = null;
    try {
      String fullUrl = spec + "?" + params;
      logger.debug("Metrics request url = " + fullUrl);
      reader = new BufferedReader(new InputStreamReader(context
          .getURLStreamProvider().readFrom(fullUrl, "GET", (String)null, headers)));

      TimelineMetrics timelineMetrics = timelineObjectReader.readValue(reader);
      logger.debug("Timeline metrics response => " + timelineMetrics);

      for (TimelineMetric tlMetric : timelineMetrics.getMetrics()) {
        if (tlMetric.getMetricName() != null
            && tlMetric.getMetricValues() != null) {
          Map<Long, Double> tlMetricValues = tlMetric.getMetricValues();
          Number[][] metricValues = transformMetricValues(tlMetricValues);
          receivedMetrics.put(tlMetric.getMetricName(), metricValues);
        }
      }

    } catch (IOException io) {
      logger.warn("Error getting timeline metrics.", io);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          if (logger.isDebugEnabled()) {
            logger.warn("Unable to close http input steam : spec=" + spec, e);
          }
        }
      }
    }

    return receivedMetrics;
  }

  private static Number[][] transformMetricValues(
      Map<Long, Double> tlMetricValues) {
    Number[][] metricValues = new Number[tlMetricValues.size()][2];
    int i = 0;
    for (Map.Entry<Long, Double> tlMetricValue : tlMetricValues.entrySet()) {
      // value goes to column 0
      metricValues[i][0] = tlMetricValue.getValue();
      // timestamp goes to column 1 - convert it from millis to sec
      metricValues[i][1] = tlMetricValue.getKey() / 1000;
      i++;
    }
    return metricValues;
  }

  public static String getUrlWithParams(String appName, String metricUrl,
      Set<String> metricSet, TemporalInfo temporalInfo) throws SystemException,
      URISyntaxException {
    String metrics = getSetString(metricSet, -1);
    URIBuilder uriBuilder = new URIBuilder(metricUrl);

    if (appName != null) {
      uriBuilder.setParameter(URI_PARAM_APP_ID, appName);
    }
    if (metrics.length() > 0) {
      uriBuilder.setParameter(URI_PARAM_METRIC_NAMES, metrics);
    }

    if (temporalInfo != null) {
      long startTime = temporalInfo.getStartTime();
      if (startTime != -1) {
        uriBuilder.setParameter(URI_PARAM_START_TIME, String.valueOf(startTime));
      }
      long endTime = temporalInfo.getEndTime();
      if (endTime != -1) {
        uriBuilder.setParameter(URI_PARAM_END_TIME, String.valueOf(endTime));
      }
    } else {
      long endTime = System.currentTimeMillis() / 1000;
      long startTime = System.currentTimeMillis() / 1000 - 60 * 60;
      uriBuilder.setParameter(URI_PARAM_END_TIME, String.valueOf(endTime));
      uriBuilder.setParameter(URI_PARAM_START_TIME, String.valueOf(startTime));
    }
    return uriBuilder.toString();
  }

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
}
