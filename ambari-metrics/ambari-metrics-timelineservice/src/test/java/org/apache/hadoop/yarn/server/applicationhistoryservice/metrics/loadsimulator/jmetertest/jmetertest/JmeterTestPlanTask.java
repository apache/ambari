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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.loadsimulator.jmetertest.jmetertest;

import org.apache.commons.io.IOUtils;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.protocol.http.util.HTTPConstants;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.timers.ConstantTimer;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class JmeterTestPlanTask implements Runnable {

  private static StandardJMeterEngine jmeterEngine = null;
  private final static Logger LOG = LoggerFactory.getLogger(JmeterTestPlanTask.class);
  private List<AppGetMetric> appGetMetrics;
  private Properties amsJmeterProperties;
  private HashTree amsTestPlanTree;
  private TestPlan amsTestPlan;
  private static final String JMETER_HOME = "loadsimulator";
  private static final String JMETER_PROPERTIES_FILE = JMETER_HOME + "/jmeter.properties";
  private static final String SAVESERVICE_PROPERTIES_FILE = JMETER_HOME + "/saveservice.properties";

  public enum ClientApp {
    HOST("HOST"),
    NAMENODE("NAMENODE"),
    HBASE("HBASE"),
    NIMBUS("NIMBUS"),
    KAFKA_BROKER("KAFKA_BROKER"),
    FLUME_HANDLER("FLUME_HANDLER"),
    AMS_HBASE("AMS-HBASE"),
    NODEMANAGER("NODEMANAGER"),
    RESOURCEMANAGER("RESOURCEMANAGER"),
    DATANODE("DATANODE");

    private String id;

    private ClientApp(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }
  }

  public JmeterTestPlanTask(List<AppGetMetric> appGetMetrics, Properties amsJmeterProperties) {
    this.appGetMetrics = appGetMetrics;
    this.amsJmeterProperties = amsJmeterProperties;
    amsTestPlanTree = new HashTree();
    amsTestPlan = new TestPlan("AMS JMeter Load Test plan");
    System.out.println("Starting AMS Jmeter load testing");
  }

  public void run() {
    if (jmeterEngine != null) {

      Object[] threadGroups = amsTestPlanTree.getArray(amsTestPlan);
      for (Object threadGroupObj : threadGroups) {
        if (threadGroupObj instanceof ThreadGroup) {
          ThreadGroup threadGroup = (ThreadGroup) threadGroupObj;
          threadGroup.stop();
        }
      }
      amsTestPlanTree.clear();
      jmeterEngine.askThreadsToStop();
      jmeterEngine.stopTest();
      JMeterContextService.endTest();
    }

    //Start the new test plan for the new app.
    try {
      //Initialize Jmeter essentials
      jmeterEngine = new StandardJMeterEngine();
      JMeterContextService.getContext().setEngine(jmeterEngine);

      //Workaround to supply JMeterUtils with jmeter.prooperties from JAR.
      JMeterUtils.setJMeterHome("");
      Field f = new JMeterUtils().getClass().getDeclaredField("appProperties");
      f.setAccessible(true);
      f.set(null, AMSJMeterLoadTest.readProperties(JMETER_PROPERTIES_FILE));

      //Copy saveservices.properties file to tmp dir for JMeter to consume.
      InputStream inputStream = ClassLoader.getSystemResourceAsStream(SAVESERVICE_PROPERTIES_FILE);
      if (inputStream == null) {
        inputStream = new FileInputStream(SAVESERVICE_PROPERTIES_FILE);
      }
      String tmpDir = System.getProperty("java.io.tmpdir");
      OutputStream outputStream = new FileOutputStream(tmpDir + "/saveservice.properties");
      IOUtils.copy(inputStream, outputStream);
      outputStream.close();
      JMeterUtils.setProperty("saveservice_properties", tmpDir + "/saveservice.properties");

      //Initialize Test plan
      amsTestPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
      amsTestPlanTree.add("AMS Test plan", amsTestPlan);

      //Choose a random APP to run the perform GET metrics request.
      int currentAppIndex = new Random().nextInt(appGetMetrics.size());

      //Create ThreadGroup for the App
      createThreadGroupHashTree(currentAppIndex, amsJmeterProperties, amsTestPlanTree, amsTestPlan);

      //Geneates the JMX file that you can use through the GUI mode.
      //SaveService.saveTree(amsTestPlanTree, new FileOutputStream(JMETER_HOME + "/" + "amsTestPlan.jmx"));

      //Summarizer output to get test progress in stdout like.
      Summariser summariser = null;
      String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
      if (summariserName.length() > 0) {
        summariser = new Summariser(summariserName);
      }

      //Store execution results into a .jtl file
      String jmeterLogFile = tmpDir + "/amsJmeterTestResults.jtl";
      ResultCollector resultCollector = new ResultCollector(summariser);
      resultCollector.setFilename(jmeterLogFile);
      amsTestPlanTree.add(amsTestPlanTree.getArray()[0], resultCollector);
      jmeterEngine.configure(amsTestPlanTree);
      jmeterEngine.run();

      LOG.info("AMS Jmeter Test started up successfully");

    } catch (Exception ioEx) {
      amsTestPlanTree.clear();
      jmeterEngine.askThreadsToStop();
      jmeterEngine.stopTest();
      JMeterContextService.endTest();
      LOG.error("Error occurred while running AMS load test : " + ioEx.getMessage());
      ioEx.printStackTrace();
    }
  }

  private ConstantTimer createConstantTimer(int delay) {
    ConstantTimer timer = new ConstantTimer();
    timer.setDelay("" + delay);
    return timer;
  }

  private Map<String, String> getAppSpecificParameters(String app, GetMetricRequestInfo request, Properties amsJmeterProperties) {

    Map<String, String> parametersMap = new HashMap<String, String>();
    String hostPrefix = amsJmeterProperties.getProperty("host-prefix");
    String hostSuffix = amsJmeterProperties.getProperty("host-suffix");
    int minHostIndex = Integer.valueOf(amsJmeterProperties.getProperty("min-host-index"));
    int numHosts = Integer.valueOf(amsJmeterProperties.getProperty("num-hosts"));

    parametersMap.put("appId", app);

    if (request.needsTimestamps()) {
      long currentTime = System.currentTimeMillis();
      long oneHourBack = currentTime - 3600 * 1000;
      parametersMap.put("startTime", String.valueOf(oneHourBack));
      parametersMap.put("endTime", String.valueOf(currentTime));
    }

    if (request.needsHost()) {
      if (ClientApp.AMS_HBASE.getId().equals(app)) {
        parametersMap.put("hostname", amsJmeterProperties.getProperty("ams-host"));
      } else if (ClientApp.HOST.getId().equals(app) || ClientApp.NODEMANAGER.getId().equals(app)) {
        int randomHost = minHostIndex + new Random().nextInt(numHosts);
        parametersMap.put("hostname", hostPrefix + randomHost + hostSuffix);
      } else {
        parametersMap.put("hostname", hostPrefix + amsJmeterProperties.getProperty(app + "-host") + hostSuffix);
      }
    }
    parametersMap.put("metricNames", request.getMetricStringPayload());
    return parametersMap;
  }

  private void createThreadGroupHashTree(int appIndex, Properties amsJmeterProperties, HashTree amsTestPlanTree, TestPlan amsTestPlan) {

    AppGetMetric appGetMetric = appGetMetrics.get(appIndex);
    String app = appGetMetric.getApp();
    int interval = appGetMetric.getInterval();

    //Read and validate AMS information.
    String[] amsHostPort = amsJmeterProperties.getProperty("ams-host-port").split(":");
    String amsHost = amsHostPort[0];
    String amsPath = amsJmeterProperties.getProperty("ams-path");
    int amsPort = Integer.valueOf(amsHostPort[1]);
    int numLoops = Integer.valueOf(amsJmeterProperties.getProperty("num-get-calls-per-app"));

    LoopController loopController = createLoopController(app + " GET loop controller", numLoops, false);
    for (GetMetricRequestInfo request : appGetMetric.getMetricRequests()) {

      ThreadGroup threadGroup = createThreadGroup(app + " GET threadGroup", 1, 0, loopController);

      HashTree threadGroupHashTree = amsTestPlanTree.add(amsTestPlan, threadGroup);
      Map<String, String> parametersMap = getAppSpecificParameters(app, request, amsJmeterProperties);

      HTTPSampler sampler = createGetSampler("GET " + app + " metrics", amsHost, amsPort, amsPath, null, parametersMap);

      if (numLoops > 1) {
        threadGroupHashTree.add(createConstantTimer(interval));
      }

      threadGroupHashTree.add(sampler);
    }
  }

  private HTTPSampler createGetSampler(String name, String domain, int port, String path, String encoding, Map<String, String> parameters) {

    HTTPSampler sampler = new HTTPSampler();
    sampler.setDomain(domain);
    sampler.setPort(port);
    sampler.setPath(path);
    sampler.setMethod(HTTPConstants.GET);

    if (encoding != null)
      sampler.setContentEncoding(encoding);

    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      sampler.addArgument(entry.getKey(), entry.getValue());
    }
    sampler.setName(name);
    return sampler;
  }

  private LoopController createLoopController(String name, int numLoops, boolean continueForever) {
    LoopController loopController = new LoopController();
    loopController.setLoops(numLoops);
    loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
    loopController.initialize();
    loopController.setContinueForever(continueForever);
    loopController.setName(name);
    return loopController;
  }

  private ThreadGroup createThreadGroup(String name, int numThreads, int rampUp, LoopController loopController) {
    ThreadGroup threadGroup = new ThreadGroup();
    threadGroup.setName(name);
    threadGroup.setNumThreads(numThreads);
    threadGroup.setRampUp(rampUp);
    threadGroup.setSamplerController(loopController);
    threadGroup.setProperty(TestElement.TEST_CLASS, ThreadGroup.class.getName());
    return threadGroup;
  }

}
