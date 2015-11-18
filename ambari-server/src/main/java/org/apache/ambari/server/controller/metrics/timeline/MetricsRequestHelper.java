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
package org.apache.ambari.server.controller.metrics.timeline;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Map;

/**
 * Helper class to call AMS backend that is utilized by @AMSPropertyProvider
 * and @AMSReportPropertyProvider as well as @TimelineMetricCacheEntryFactory
 */
public class MetricsRequestHelper {
  private final static Logger LOG = LoggerFactory.getLogger(MetricsRequestHelper.class);
  private final static ObjectMapper mapper;
  private final static ObjectReader timelineObjectReader;
  private final URLStreamProvider streamProvider;

  static {
    mapper = new ObjectMapper();
    AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    //noinspection deprecation
    mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    timelineObjectReader = mapper.reader(TimelineMetrics.class);
  }

  public MetricsRequestHelper(URLStreamProvider streamProvider) {
    this.streamProvider = streamProvider;
  }

  public TimelineMetrics fetchTimelineMetrics(String spec) throws IOException {
    LOG.debug("Metrics request url = " + spec);
    BufferedReader reader = null;
    TimelineMetrics timelineMetrics = null;
    try {

      HttpURLConnection connection = streamProvider.processURL(spec, HttpMethod.GET, (String)null, null);
      if (connection.getResponseCode() == HttpStatus.SC_BAD_REQUEST) {
        InputStream errorStream = connection.getErrorStream();
        reader = new BufferedReader(new InputStreamReader(errorStream));
        throw new IOException(reader.readLine());
      }

      InputStream inputStream = connection.getInputStream();
      reader = new BufferedReader(new InputStreamReader(inputStream));
      timelineMetrics = timelineObjectReader.readValue(reader);
      if (LOG.isTraceEnabled()) {
        for (TimelineMetric metric : timelineMetrics.getMetrics()) {
          LOG.trace("metric: " + metric.getMetricName() +
            ", size = " + metric.getMetricValues().size() +
            ", host = " + metric.getHostName() +
            ", app = " + metric.getAppId() +
            ", instance = " + metric.getInstanceId() +
            ", time = " + metric.getTimestamp() +
            ", startTime = " + new Date(metric.getStartTime()));
        }
      }
    } catch (IOException io) {
      String errorMsg = "Error getting timeline metrics : " + io.getMessage();
      LOG.error(errorMsg);
      if (LOG.isDebugEnabled()) {
        LOG.debug(errorMsg, io);
      }

      if (io instanceof SocketTimeoutException) {
        errorMsg += " Can not connect to collector, socket error.";
        LOG.error(errorMsg);
        throw io;
      }

    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          if (LOG.isWarnEnabled()) {
            if (LOG.isDebugEnabled()) {
              LOG.warn("Unable to close http input stream : spec=" + spec, e);
            } else {
              LOG.warn("Unable to close http input stream : spec=" + spec);
            }
          }
        }
      }
    }
    return timelineMetrics;
  }
}
