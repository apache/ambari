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

import static org.apache.ambari.infra.solr.metrics.reporters.MetricsUtils.NAME_PREFIX;
import static org.apache.ambari.infra.solr.metrics.reporters.MetricsUtils.toTimelineMetric;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

public class ScheduledAMSReporter<T> extends ScheduledReporter {

  private static final Logger LOG = LoggerFactory.getLogger(ScheduledAMSReporter.class);

  private final SolrMetricsSink amsClient;
  private final String namePrefix;
  private final GaugeConverter<T> gaugeConverter;

  protected ScheduledAMSReporter(String registryName,
                                 MetricRegistry registry,
                                 String name,
                                 MetricFilter filter,
                                 TimeUnit rateUnit,
                                 TimeUnit durationUnit,
                                 ScheduledExecutorService executor,
                                 boolean shutdownExecutorOnStop, Set<MetricAttribute> disabledMetricAttributes,
                                 SolrMetricsSink amsClient,
                                 GaugeConverter<T> gaugeConverter) {
    super(registry, name, filter, rateUnit, durationUnit, executor, shutdownExecutorOnStop, disabledMetricAttributes);
    this.amsClient = amsClient;
    namePrefix = String.format("%s%s.", NAME_PREFIX, registryName);
    this.gaugeConverter = gaugeConverter;
  }

  @Override
  public void report(SortedMap<String, Gauge> gauges,
                     SortedMap<String, Counter> counters,
                     SortedMap<String, Histogram> histograms,
                     SortedMap<String, Meter> meters,
                     SortedMap<String, Timer> timers) {
    try {
      long currentMillis = System.currentTimeMillis();
      List<TimelineMetric> timelineMetricList = new ArrayList<>();
      gauges.forEach((metricName, gauge) ->
              addTimelineMetrics(namePrefix + metricName, gauge, currentMillis, timelineMetricList));
      counters.forEach((metricName, counter) ->
              timelineMetricList.add(toTimelineMetric(namePrefix + metricName, counter.getCount(), currentMillis)));
      timers.forEach((metricName, timer) ->
              addTimelineMetrics(namePrefix + metricName, timer, currentMillis, timelineMetricList));

      if (timelineMetricList.isEmpty())
        return;

      TimelineMetrics timelineMetrics = new TimelineMetrics();
      timelineMetrics.setMetrics(timelineMetricList);
      amsClient.emitMetrics(timelineMetrics);
    }
    catch (Exception ex) {
      LOG.error("Unable to collect and send metrics", ex);
    }
  }

  private void addTimelineMetrics(String metricName, Gauge<T> gauge, long currentMillis, List<TimelineMetric> timelineMetricList) {
    try {
      timelineMetricList.addAll(gaugeConverter.convert(metricName, gauge, currentMillis));
    } catch (Exception ex) {
      LOG.error("Unable to get value of gauge metric " + metricName, ex);
    }
  }

  private void addTimelineMetrics(String metricName, Timer timer, long currentTime, List<TimelineMetric> timelineMetricList) {
    try {
      timelineMetricList.add(toTimelineMetric(metricName + ".avgRequestsPerSecond", timer.getMeanRate(), currentTime));
      Snapshot snapshot = timer.getSnapshot();
      timelineMetricList.add(toTimelineMetric(metricName + ".avgTimePerRequest", snapshot.getMean(), currentTime));
      timelineMetricList.add(toTimelineMetric(metricName + ".medianRequestTime", snapshot.getMedian(), currentTime));
    } catch (Exception ex) {
      LOG.error("Unable to get value of timer metric " + metricName, ex);
    }
  }
}
