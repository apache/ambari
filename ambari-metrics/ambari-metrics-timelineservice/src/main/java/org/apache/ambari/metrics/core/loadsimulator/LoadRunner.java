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
package org.apache.ambari.metrics.core.loadsimulator;

import static org.apache.ambari.metrics.core.loadsimulator.data.AppID.MASTER_APPS;
import static org.apache.ambari.metrics.core.loadsimulator.data.AppID.SLAVE_APPS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.metrics.core.loadsimulator.data.ApplicationInstance;
import org.apache.ambari.metrics.core.loadsimulator.data.HostMetricsGenerator;
import org.apache.ambari.metrics.core.loadsimulator.data.MetricsGeneratorConfigurer;
import org.apache.ambari.metrics.core.loadsimulator.net.MetricsSender;
import org.apache.ambari.metrics.core.loadsimulator.net.RestMetricsSender;
import org.apache.ambari.metrics.core.loadsimulator.util.TimeStampProvider;
import org.apache.ambari.metrics.core.loadsimulator.data.AppID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class LoadRunner {
  private final static Logger LOG = LoggerFactory.getLogger(LoadRunner.class);

  private final ScheduledExecutorService timer;
  private final ExecutorService workersPool;
  private final Collection<Callable<String>> workers;
  private final long startTime = new Date().getTime();
  private final int collectIntervalMillis;
  private final int sendIntervalMillis;

  public LoadRunner(String hostName,
                    int threadCount,
                    String metricsHostName,
                    int minHostIndex,
                    int collectIntervalMillis,
                    int sendIntervalMillis,
                    boolean createMaster) {
    this.collectIntervalMillis = collectIntervalMillis;
    this.workersPool = Executors.newFixedThreadPool(threadCount);
    this.timer = Executors.newScheduledThreadPool(1);
    this.sendIntervalMillis = sendIntervalMillis;

    workers = prepareWorkers(hostName, threadCount, metricsHostName, createMaster, minHostIndex);
  }

  private Collection<Callable<String>> prepareWorkers(String hostName,
                                                      int threadCount,
                                                      String metricsHost,
                                                      Boolean createMaster, int minHostIndex) {
    Collection<Callable<String>> senderWorkers =
      new ArrayList<Callable<String>>(threadCount);

    int startIndex = minHostIndex;
    if (createMaster) {
      String simHost = hostName + startIndex;
      addMetricsWorkers(senderWorkers, simHost, metricsHost, MASTER_APPS);
      startIndex++;
    }

    for (int i = startIndex; i < threadCount + minHostIndex; i++) {
      String simHost = hostName + i;
      addMetricsWorkers(senderWorkers, simHost, metricsHost, SLAVE_APPS);
    }

    return senderWorkers;
  }

  private void addMetricsWorkers(Collection<Callable<String>> senderWorkers,
                                 String specificHostName,
                                 String metricsHostName,
                                 AppID[] apps) {
    for (AppID app : apps) {
      HostMetricsGenerator metricsGenerator =
        createApplicationMetrics(specificHostName, app);
      MetricsSender sender = new RestMetricsSender(metricsHostName);
      senderWorkers.add(new MetricsSenderWorker(sender, metricsGenerator));
    }
  }

  private HostMetricsGenerator createApplicationMetrics(String simHost, AppID host) {
    ApplicationInstance appInstance = new ApplicationInstance(simHost, host, "");
    TimeStampProvider timeStampProvider = new TimeStampProvider(startTime,
      collectIntervalMillis, sendIntervalMillis);

    return MetricsGeneratorConfigurer
      .createMetricsForHost(appInstance, timeStampProvider);
  }

  public void start() {
    timer.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        try {
          runOnce();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }, 0, sendIntervalMillis, TimeUnit.MILLISECONDS);
  }

  public void runOnce() throws InterruptedException {
    List<Future<String>> futures = workersPool.invokeAll(workers,
      sendIntervalMillis / 2,
      TimeUnit.MILLISECONDS);
    int done = 0;

    // TODO: correctly count the failed tasks
    for (Future<String> future : futures) {
      done += future.isDone() ? 1 : 0;
    }

    LOG.info("Finished successfully " + done + " tasks ");
  }

  public void shutdown() {
    timer.shutdownNow();
    workersPool.shutdownNow();
  }

  public static void main(String[] args) {
    LoadRunner runner =
      new LoadRunner("local", 0, "metrics", 0, 10000, 20000, false);

    runner.start();
  }

}
