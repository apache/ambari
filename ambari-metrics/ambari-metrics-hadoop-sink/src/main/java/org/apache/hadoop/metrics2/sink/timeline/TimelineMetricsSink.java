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
package org.apache.hadoop.metrics2.sink.timeline;

import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.ClassUtils;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.metrics2.AbstractMetric;
import org.apache.hadoop.metrics2.MetricsException;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.MetricsTag;
import org.apache.hadoop.metrics2.impl.MsInfo;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricsCache;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@InterfaceAudience.Public
@InterfaceStability.Evolving
public class TimelineMetricsSink extends AbstractTimelineMetricsSink {
  private static ObjectMapper mapper;
  private Map<String, Set<String>> useTagsMap = new HashMap<String, Set<String>>();
  private static final String TAGS_FOR_PREFIX_PROPERTY_PREFIX = "tagsForPrefix.";
  private static final String MAX_METRIC_ROW_CACHE_SIZE = "maxRowCacheSize";
  private static final String METRICS_SEND_INTERVAL = "sendInterval";
  protected HttpClient httpClient = new HttpClient();
  private TimelineMetricsCache metricsCache;

  static {
    mapper = new ObjectMapper();
    AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    mapper.getSerializationConfig()
      .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
  }

  @Override
  public void init(SubsetConfiguration conf) {
    super.init(conf);

    int maxRowCacheSize = conf.getInt(MAX_METRIC_ROW_CACHE_SIZE,
      TimelineMetricsCache.MAX_RECS_PER_NAME_DEFAULT);
    int metricsSendInterval = conf.getInt(METRICS_SEND_INTERVAL,
      TimelineMetricsCache.MAX_EVICTION_TIME_MILLIS); // ~ 1 min
    metricsCache = new TimelineMetricsCache(maxRowCacheSize, metricsSendInterval);

    conf.setListDelimiter(',');
    Iterator<String> it = (Iterator<String>) conf.getKeys();
    while (it.hasNext()) {
      String propertyName = it.next();
      if (propertyName != null && propertyName.startsWith(TAGS_FOR_PREFIX_PROPERTY_PREFIX)) {
        String contextName = propertyName.substring(TAGS_FOR_PREFIX_PROPERTY_PREFIX.length());
        String[] tags = conf.getStringArray(propertyName);
        boolean useAllTags = false;
        Set<String> set = null;
        if (tags.length > 0) {
          set = new HashSet<String>();
          for (String tag : tags) {
            tag = tag.trim();
            useAllTags |= tag.equals("*");
            if (tag.length() > 0) {
              set.add(tag);
            }
          }
          if (useAllTags) {
            set = null;
          }
        }
        useTagsMap.put(contextName, set);
      }
    }
  }

  @Override
  public void putMetrics(MetricsRecord record) {
    try {
      String recordName = record.name();
      String contextName = record.context();

      StringBuilder sb = new StringBuilder();
      sb.append(contextName);
      sb.append('.');
      sb.append(recordName);

      appendPrefix(record, sb);
      sb.append(".");
      int sbBaseLen = sb.length();

      Collection<AbstractMetric> metrics =
        (Collection<AbstractMetric>) record.metrics();

      List<TimelineMetric> metricList = new ArrayList<TimelineMetric>();

      for (AbstractMetric metric : metrics) {
        sb.append(metric.name());
        String name = sb.toString();
        TimelineMetric timelineMetric = new TimelineMetric();
        timelineMetric.setMetricName(name);
        timelineMetric.setHostName(getHostName());
        timelineMetric.setAppId(getServiceName());
        timelineMetric.setStartTime(record.timestamp());
        timelineMetric.setType(ClassUtils.getShortCanonicalName(
          metric.value(), "Number"));
        timelineMetric.getMetricValues().put(record.timestamp(),
          metric.value().doubleValue());
        // Put intermediate values into the cache until it is time to send
        metricsCache.putTimelineMetric(timelineMetric);

        // Retrieve all values from cache if it is time to send
        TimelineMetric cachedMetric = metricsCache.getTimelineMetric(name);

        if (cachedMetric != null) {
          metricList.add(cachedMetric);
        }

        sb.setLength(sbBaseLen);
      }

      TimelineMetrics timelineMetrics = new TimelineMetrics();
      timelineMetrics.setMetrics(metricList);

      if (!metricList.isEmpty()) {
        emitMetrics(timelineMetrics);
      }


    } catch (IOException io) {
      throw new MetricsException("Failed to putMetrics", io);
    }
  }

  private void emitMetrics(TimelineMetrics metrics) throws IOException {
    String jsonData = mapper.writeValueAsString(metrics);

    SocketAddress socketAddress = getServerSocketAddress();

    if (socketAddress != null) {
      StringRequestEntity requestEntity = new StringRequestEntity(
        jsonData, "application/json", "UTF-8");

      PostMethod postMethod = new PostMethod(getCollectorUri());
      postMethod.setRequestEntity(requestEntity);
      int statusCode = httpClient.executeMethod(postMethod);
      if (statusCode != 200) {
        LOG.info("Unable to POST metrics to collector, " + getCollectorUri());
      }
    }
  }

  // Taken as is from Ganglia30 implementation
  @InterfaceAudience.Private
  public void appendPrefix(MetricsRecord record, StringBuilder sb) {
    String contextName = record.context();
    Collection<MetricsTag> tags = record.tags();
    if (useTagsMap.containsKey(contextName)) {
      Set<String> useTags = useTagsMap.get(contextName);
      for (MetricsTag t : tags) {
        if (useTags == null || useTags.contains(t.name())) {

          // the context is always skipped here because it is always added

          // the hostname is always skipped to avoid case-mismatches
          // from different DNSes.

          if (t.info() != MsInfo.Context && t.info() != MsInfo.Hostname && t.value() != null) {
            sb.append('.').append(t.name()).append('=').append(t.value());
          }
        }
      }
    }
  }

  @Override
  public void flush() {
    // TODO: Buffering implementation
  }

}
