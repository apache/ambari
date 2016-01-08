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

package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.util.ExitUtil;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Acts as the single TimetineMetricStore Watcher.
 */
public class TimelineMetricStoreWatcher implements Runnable {

  private static final Log LOG = LogFactory.getLog(TimelineMetricStoreWatcher.class);
  private static final String FAKE_METRIC_NAME = "TimelineMetricStoreWatcher.FakeMetric";
  private static final String FAKE_HOSTNAME = "fakehostname";
  private static final String FAKE_APP_ID = "timeline_metric_store_watcher";

  private static int failures = 0;
  private final TimelineMetricConfiguration configuration;

  private TimelineMetricStore timelineMetricStore;

  //used to call timelineMetricStore blocking methods with timeout
  private ExecutorService executor = Executors.newSingleThreadExecutor();


  public TimelineMetricStoreWatcher(TimelineMetricStore timelineMetricStore,
                                    TimelineMetricConfiguration configuration) {
    this.timelineMetricStore = timelineMetricStore;
    this.configuration = configuration;
  }

  @Override
  public void run() {
    if (checkMetricStore()) {
      failures = 0;
      if (LOG.isDebugEnabled()) {
        LOG.debug("Successfully got metrics from TimelineMetricStore");
      }
    } else {
      LOG.info("Failed to get metrics from TimelineMetricStore, attempt = " + failures);
      failures++;
    }

    if (failures >= configuration.getTimelineMetricsServiceWatcherMaxFailures()) {
      String msg = "Error getting metrics from TimelineMetricStore. " +
        "Shutting down by TimelineMetricStoreWatcher.";
      LOG.fatal(msg);
      ExitUtil.terminate(-1, msg);
    }

  }

  /**
   * Checks TimelineMetricStore functionality by adding and getting
   * a fake metric to/from HBase
   * @return if check was successful
   */
  private boolean checkMetricStore() {
    final long startTime = System.currentTimeMillis();
    final int delay = configuration.getTimelineMetricsServiceWatcherDelay();
    final int timeout = configuration.getTimelineMetricsServiceWatcherTimeout();

    TimelineMetric fakeMetric = new TimelineMetric();
    fakeMetric.setMetricName(FAKE_METRIC_NAME);
    fakeMetric.setHostName(FAKE_HOSTNAME);
    fakeMetric.setAppId(FAKE_APP_ID);
    fakeMetric.setStartTime(startTime);
    fakeMetric.setTimestamp(startTime);
    fakeMetric.getMetricValues().put(startTime, 0.0);

    final TimelineMetrics metrics = new TimelineMetrics();
    metrics.setMetrics(Collections.singletonList(fakeMetric));

    Callable<TimelineMetric> task = new Callable<TimelineMetric>() {
      public TimelineMetric call() throws Exception {
        timelineMetricStore.putMetrics(metrics);
        return timelineMetricStore.getTimelineMetric(
          FAKE_METRIC_NAME, Collections.singletonList(FAKE_HOSTNAME),
          FAKE_APP_ID, null, startTime - delay * 2 * 1000,
          startTime + delay * 2 * 1000, Precision.SECONDS, 1);
      }
    };

    Future<TimelineMetric> future = executor.submit(task);
    TimelineMetric timelineMetric = null;
    try {
      timelineMetric = future.get(timeout, TimeUnit.SECONDS);
    // Phoenix might throw RuntimeExeption's
    } catch (Exception e) {
      return false;
    } finally {
      future.cancel(true);
    }

    return timelineMetric != null;
  }

}
