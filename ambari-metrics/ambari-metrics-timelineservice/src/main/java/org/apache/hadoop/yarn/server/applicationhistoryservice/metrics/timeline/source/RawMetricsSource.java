/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.source;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.sink.ExternalMetricsSink;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.source.cache.InternalMetricsCache;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.source.cache.InternalMetricsCacheProvider;

public class RawMetricsSource implements InternalMetricsSource {
  private static final Log LOG = LogFactory.getLog(RawMetricsSource.class);
  private final int internalCacheInterval;
  private final ExternalMetricsSink rawMetricsSink;
  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
  private final InternalMetricsCache cache;
  static final String RAW_METRICS_CACHE = "RAW_METRICS_CACHE_INSTANCE";

  public RawMetricsSource(int internalCacheInterval, ExternalMetricsSink rawMetricsSink) {
    this.internalCacheInterval = internalCacheInterval;
    this.rawMetricsSink = rawMetricsSink;
    this.cache = InternalMetricsCacheProvider.getInstance().getCacheInstance(RAW_METRICS_CACHE);
    if (rawMetricsSink.getFlushSeconds() > internalCacheInterval) {
      initializeFixedRateScheduler();
    }
  }

  @Override
  public void publishTimelineMetrics(Collection<TimelineMetrics> metrics) {
    // TODO: Adjust default flush to reasonable defaults > 3 seconds
    if (rawMetricsSink.getFlushSeconds() > internalCacheInterval) {
      // Need to cache only if external sink cannot keep up and thereby has
      // different flush interval as compared to HBase flush
      cache.putAll(metrics); // Scheduler initialized already for flush
    } else {
      submitDataWithTimeout(metrics);
    }
  }

  private void initializeFixedRateScheduler() {
    executorService.scheduleAtFixedRate(() -> rawMetricsSink.sinkMetricData(cache.evictAll()),
      rawMetricsSink.getFlushSeconds(), rawMetricsSink.getFlushSeconds(), TimeUnit.SECONDS);
  }

  private void submitDataWithTimeout(final Collection<TimelineMetrics> metrics) {
    Future f = executorService.submit(() -> {
      rawMetricsSink.sinkMetricData(metrics);
      return null;
    });
    try {
      f.get(rawMetricsSink.getSinkTimeOutSeconds(), TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOG.warn("Raw metrics sink interrupted.");
    } catch (ExecutionException e) {
      LOG.warn("Exception on sinking metrics", e);
    } catch (TimeoutException e) {
      LOG.warn("Timeout exception on sinking metrics", e);
    }
  }

}
