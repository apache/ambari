/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.core.loadsimulator.jmetertest.jmetertest;

import org.apache.commons.lang3.StringUtils;
import org.apache.ambari.metrics.core.loadsimulator.MetricsLoadSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AMSJMeterLoadTest {

  private final static Logger LOG = LoggerFactory.getLogger(AMSJMeterLoadTest.class);
  private static String PROPERTIES_FILE = "loadsimulator/ams-jmeter.properties";
  private ScheduledExecutorService scheduledExecutorService = null;
  private List<AppGetMetric> appGetMetrics;
  private Properties amsJmeterProperties = null;

  public AMSJMeterLoadTest(Map<String, String> args) {

    String testType = args.get("type");
    String userDefinedPropertiesFile = args.get("amsJmeterPropertiesFile");
    if (null == userDefinedPropertiesFile || userDefinedPropertiesFile.isEmpty()) {
      this.amsJmeterProperties = readProperties(PROPERTIES_FILE);
    } else {
      this.amsJmeterProperties = readProperties(userDefinedPropertiesFile);
    }

    if ("U".equals(testType)) { //GET metrics simulator
      int numInstances = Integer.valueOf(amsJmeterProperties.getProperty("num-ui-instances"));
      this.scheduledExecutorService = Executors.newScheduledThreadPool(numInstances);
      this.appGetMetrics = initializeGetMetricsPayload(amsJmeterProperties);
      this.runTest(numInstances);
    } else {                    //PUT Metrics simulator
      Map<String, String> mapArgs = new HashMap<String, String>();
      mapArgs.put("hostName", (args.get("host-prefix") != null) ? args.get("host-prefix") : amsJmeterProperties.getProperty("host-prefix"));
      mapArgs.put("minHostIndex", (args.get("min-host-index") != null) ? args.get("min-host-index") : amsJmeterProperties.getProperty("min-host-index"));
      mapArgs.put("numberOfHosts", (args.get("num-hosts") != null) ? args.get("num-hosts") : amsJmeterProperties.getProperty("num-hosts"));
      mapArgs.put("metricsHostName", (args.get("ams-host-port") != null) ? args.get("ams-host-port") : amsJmeterProperties.getProperty("ams-host-port"));
      mapArgs.put("collectInterval", (args.get("collection-interval") != null) ? args.get("collection-interval") : amsJmeterProperties.getProperty("collection-interval"));
      mapArgs.put("sendInterval", (args.get("send-interval") != null) ? args.get("send-interval") : amsJmeterProperties.getProperty("send-interval"));
      mapArgs.put("master", (args.get("create-master") != null) ? args.get("create-master") : amsJmeterProperties.getProperty("create-master"));
      System.out.println("AMS Load Simulation Parameters : " + mapArgs);
      MetricsLoadSimulator.startTest(mapArgs);
    }
  }

  public static Properties readProperties(String propertiesFile) {
    try {
      Properties properties = new Properties();
      InputStream inputStream = ClassLoader.getSystemResourceAsStream(propertiesFile);
      if (inputStream == null) {
        inputStream = new FileInputStream(propertiesFile);
      }
      properties.load(inputStream);
      return properties;
    } catch (IOException ioEx) {
      LOG.error("Error reading properties file for jmeter");
      return null;
    }
  }

  private static List<GetMetricRequestInfo> readMetricsFromFile(String app) {
    InputStream input = null;
    List<GetMetricRequestInfo> metricList = new ArrayList<>();
    String fileName = "ui_metrics_def/" + app + ".dat";

    try {
      input = ClassLoader.getSystemResourceAsStream(fileName);
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      String line;
      List<String> metrics = new ArrayList<>();
      while ((line = reader.readLine()) != null) {

        if (line.startsWith("|")) {
          boolean needsTimestamps = line.contains("startTime");
          boolean needsHost = line.contains("hostname");
          metricList.add(new GetMetricRequestInfo(metrics, needsTimestamps, needsHost));
          metrics.clear();
        } else {
          metrics.add(line);
        }
      }
      return metricList;
    } catch (IOException e) {
      LOG.error("Cannot read file " + fileName + " for appID " + app, e);
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException ex) {
        }
      }
    }
    return null;
  }

  private static List<AppGetMetric> initializeGetMetricsPayload(Properties amsJmeterProperties) {

    List<AppGetMetric> appGetMetrics = new ArrayList<AppGetMetric>();
    String appsToTest = amsJmeterProperties.getProperty("apps-to-test");
    String[] apps;

    if (appsToTest != null && !appsToTest.isEmpty()) {
      apps = StringUtils.split(appsToTest, ",");
    } else {
      apps = new String[JmeterTestPlanTask.ClientApp.values().length];
      int ctr = 0;
      for (JmeterTestPlanTask.ClientApp app : JmeterTestPlanTask.ClientApp.values())
        apps[ctr++] = app.getId();
    }

    for (String app : apps) {

      int interval = Integer.valueOf(amsJmeterProperties.getProperty("get-interval"));
      String intervalString = amsJmeterProperties.getProperty(app + "-get-interval");
      if (intervalString != null && !intervalString.isEmpty()) {
        interval = Integer.valueOf(intervalString);
      }
      appGetMetrics.add(new AppGetMetric(readMetricsFromFile(app), interval, app));
    }

    return appGetMetrics;
  }

  public void runTest(int numInstances) {

    int appRefreshRate = Integer.valueOf(amsJmeterProperties.getProperty("app-refresh-rate"));
    for (int i = 0; i < numInstances; i++) {
      ScheduledFuture future = scheduledExecutorService.scheduleAtFixedRate(new JmeterTestPlanTask(appGetMetrics,
        amsJmeterProperties), 0, appRefreshRate, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Sample Usage:
   * java -cp "lib/*":ambari-metrics-timelineservice-2.1.1.0.jar org.apache.ambari.metrics
   * .core.loadsimulator.jmeter.AMSJMeterLoadTest
   * -t UI -p ambari-metrics-timelineservice/src/main/resources/jmeter/ams-jmeter.properties
   */
  public static void main(String[] args) {
    Map<String, String> mapArgs = parseArgs(args);
    new AMSJMeterLoadTest(mapArgs);
  }

  private static Map<String, String> parseArgs(String[] args) {
    Map<String, String> mapProps = new HashMap<String, String>();
    if (args.length == 0) {
      printUsage();
      throw new RuntimeException("Unexpected argument, See usage message.");
    } else {
      for (int i = 0; i < args.length; i += 2) {
        String arg = args[i];
        mapProps.put(arg.substring(1), args[i+1]);
      }
    }
    return mapProps;
  }

  public static void printUsage() {
    System.err.println("Usage: java AMSJmeterLoadTest [OPTIONS]");
    System.err.println("Options: ");
    System.err.println("[--t type (S=>Sink/U=>UI)] [-ams-host-port localhost:6188] [-min-host-index 2] [-host-prefix TestHost.] [-num-hosts 2] " +
      "[-create-master true] [-collection-interval 10000 ] [-send-interval 60000 ] [-p amsJmeterPropertiesFile (Optional)]");
  }

}


