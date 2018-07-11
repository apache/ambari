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

package org.apache.ambari.metrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.http.HttpConfig;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
import org.apache.hadoop.metrics2.source.JvmMetrics;
import org.apache.hadoop.service.CompositeService;
import org.apache.hadoop.util.ExitUtil;
import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.yarn.YarnUncaughtExceptionHandler;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException;
import org.apache.ambari.metrics.core.timeline.HBaseTimelineMetricsService;
import org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration;
import org.apache.ambari.metrics.core.timeline.TimelineMetricStore;
import org.apache.ambari.metrics.webapp.AMSWebApp;
import org.apache.hadoop.yarn.webapp.WebApp;
import org.apache.hadoop.yarn.webapp.WebApps;

import static org.apache.hadoop.http.HttpServer2.HTTP_MAX_THREADS_KEY;

/**
 * Metrics collector web server
 */
public class AMSApplicationServer extends CompositeService {

  public static final int SHUTDOWN_HOOK_PRIORITY = 30;
  private static final Log LOG = LogFactory.getLog(AMSApplicationServer.class);

  TimelineMetricStore timelineMetricStore;
  private WebApp webApp;
  private TimelineMetricConfiguration metricConfiguration;

  public AMSApplicationServer() {
    super(AMSApplicationServer.class.getName());
  }

  @Override
  protected void serviceInit(Configuration conf) throws Exception {
    metricConfiguration = TimelineMetricConfiguration.getInstance();
    metricConfiguration.initialize();
    timelineMetricStore = createTimelineMetricStore(conf);
    addIfService(timelineMetricStore);
    super.serviceInit(conf);
  }

  @Override
  protected void serviceStart() throws Exception {
    DefaultMetricsSystem.initialize("AmbariMetricsSystem");
    JvmMetrics.initSingleton("AmbariMetricsSystem", null);

    startWebApp();
    super.serviceStart();
  }

  @Override
  protected void serviceStop() throws Exception {
    if (webApp != null) {
      webApp.stop();
    }

    DefaultMetricsSystem.shutdown();
    super.serviceStop();
  }
  
  static AMSApplicationServer launchAMSApplicationServer(String[] args) {
    Thread.setDefaultUncaughtExceptionHandler(new YarnUncaughtExceptionHandler());
    StringUtils.startupShutdownMessage(AMSApplicationServer.class, args, LOG);
    AMSApplicationServer amsApplicationServer = null;
    try {
      amsApplicationServer = new AMSApplicationServer();
      ShutdownHookManager.get().addShutdownHook(
        new CompositeServiceShutdownHook(amsApplicationServer),
        SHUTDOWN_HOOK_PRIORITY);
      YarnConfiguration conf = new YarnConfiguration();
      amsApplicationServer.init(conf);
      amsApplicationServer.start();
    } catch (Throwable t) {
      LOG.fatal("Error starting AMSApplicationServer", t);
      ExitUtil.terminate(-1, "Error starting AMSApplicationServer");
    }
    return amsApplicationServer;
  }

  public static void main(String[] args) {
    launchAMSApplicationServer(args);
  }

  protected TimelineMetricStore createTimelineMetricStore(Configuration conf) {
    LOG.info("Creating metrics store.");
    return new HBaseTimelineMetricsService(metricConfiguration);
  }

  protected void startWebApp() {
    String bindAddress = null;
    try {
      bindAddress = metricConfiguration.getWebappAddress();
    } catch (Exception e) {
      throw new ExceptionInInitializerError("Cannot find bind address");
    }
    LOG.info("Instantiating metrics collector at " + bindAddress);
    try {
      Configuration conf = metricConfiguration.getMetricsConf();
      conf.set(HTTP_MAX_THREADS_KEY, String.valueOf(metricConfiguration
        .getTimelineMetricsServiceHandlerThreadCount()));
      HttpConfig.Policy policy = HttpConfig.Policy.valueOf(
        conf.get(TimelineMetricConfiguration.TIMELINE_SERVICE_HTTP_POLICY,
          HttpConfig.Policy.HTTP_ONLY.name()));
      webApp =
          WebApps
            .$for("timeline", null, null, "ws")
            .withHttpPolicy(conf, policy)
            .at(bindAddress)
            .start(new AMSWebApp(timelineMetricStore));
    } catch (Exception e) {
      String msg = "AHSWebApp failed to start.";
      LOG.error(msg, e);
      throw new YarnRuntimeException(msg, e);
    }
  }
  
}
