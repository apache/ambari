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

package org.apache.ambari.logfeeder.output;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.util.DateUtil;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputProperties;
import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputSolrProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.CollectionStateWatcher;
import org.apache.solr.common.cloud.DocCollection;

public class OutputSolr extends Output implements CollectionStateWatcher {

  private static final Logger LOG = Logger.getLogger(OutputSolr.class);

  private static final int OUTPUT_PROPERTIES_WAIT_MS = 10000;
  private static final int SHARDS_WAIT_MS = 10000;

  private static final int DEFAULT_MAX_BUFFER_SIZE = 5000;
  private static final int DEFAULT_MAX_INTERVAL_MS = 3000;
  private static final int DEFAULT_NUMBER_OF_WORKERS = 1;
  private static final boolean DEFAULT_SKIP_LOGTIME = false;

  private static final int RETRY_INTERVAL = 30;

  private String type;
  private String collection;
  private String splitMode;
  private int splitInterval;
  private List<String> shards;
  private String zkConnectString;
  private int maxIntervalMS;
  private int workers;
  private int maxBufferSize;
  private boolean isComputeCurrentCollection = false;
  private int lastSlotByMin = -1;
  private boolean skipLogtime = false;

  private final Object propertiesLock = new Object();

  private BlockingQueue<OutputData> outgoingBuffer = null;
  private List<SolrWorkerThread> workerThreadList = new ArrayList<>();

  @Override
  public boolean monitorConfigChanges() {
    return true;
  };

  @Override
  public String getOutputType() {
    return type;
  }

  @Override
  protected String getStatMetricName() {
    return "output.solr.write_logs";
  }

  @Override
  protected String getWriteBytesMetricName() {
    return "output.solr.write_bytes";
  }
  
  @Override
  public void init(LogFeederProps logFeederProps) throws Exception {
    super.init(logFeederProps);
    initParams();
    setupSecurity();
    createOutgoingBuffer();
    createSolrStateWatcher();
    createSolrWorkers();
  }

  private void initParams() throws Exception {
    type = getStringValue("type");
    while (true) {
      OutputSolrProperties outputSolrProperties = logSearchConfig.getOutputSolrProperties(type);
      if (outputSolrProperties == null) {
        LOG.info("Output solr properties for type " + type + " is not available yet.");
        try { Thread.sleep(OUTPUT_PROPERTIES_WAIT_MS); } catch (Exception e) { LOG.warn(e); }
      } else {
        initPropertiesFromLogSearchConfig(outputSolrProperties, true);
        break;
      }
    }

    zkConnectString = getStringValue("zk_connect_string");
    if (StringUtils.isEmpty(zkConnectString)) {
      throw new Exception("For solr output the zk_connect_string property need to be set");
    }

    skipLogtime = getBooleanValue("skip_logtime", DEFAULT_SKIP_LOGTIME);

    maxIntervalMS = getIntValue("idle_flush_time_ms", DEFAULT_MAX_INTERVAL_MS);
    workers = getIntValue("workers", DEFAULT_NUMBER_OF_WORKERS);

    maxBufferSize = getIntValue("flush_size", DEFAULT_MAX_BUFFER_SIZE);
    if (maxBufferSize < 1) {
      LOG.warn("maxBufferSize is less than 1. Making it 1");
      maxBufferSize = 1;
    }

    LOG.info(String.format("Config: Number of workers=%d, splitMode=%s, splitInterval=%d."
        + getShortDescription(), workers, splitMode, splitInterval));
  }

  @Override
  public void outputConfigChanged(OutputProperties outputProperties) {
    initPropertiesFromLogSearchConfig((OutputSolrProperties)outputProperties, false);
  }

  private void initPropertiesFromLogSearchConfig(OutputSolrProperties outputSolrProperties, boolean init) {
    synchronized (propertiesLock) {
      splitMode = outputSolrProperties.getSplitIntervalMins();
      if (!splitMode.equalsIgnoreCase("none")) {
        splitInterval = Integer.parseInt(splitMode);
      }
      isComputeCurrentCollection = !splitMode.equalsIgnoreCase("none");

      // collection can not be overwritten after initialization
      if (init) {
        collection = outputSolrProperties.getCollection();
        if (StringUtils.isEmpty(collection)) {
          throw new IllegalStateException("Collection property is mandatory");
        }
      }
    }
  }

  private void setupSecurity() {
    String jaasFile = getLogFeederProps().getLogFeederSecurityConfig().getSolrJaasFile();
    boolean securityEnabled = getLogFeederProps().getLogFeederSecurityConfig().isSolrKerberosEnabled();
    if (securityEnabled) {
      System.setProperty("java.security.auth.login.config", jaasFile);
      HttpClientUtil.addConfigurer(new Krb5HttpClientConfigurer());
      LOG.info("setupSecurity() called for kerberos configuration, jaas file: " + jaasFile);
    }
  }

  private void createOutgoingBuffer() {
    int bufferSize = maxBufferSize * (workers + 3);
    LOG.info("Creating blocking queue with bufferSize=" + bufferSize);
    outgoingBuffer = new LinkedBlockingQueue<OutputData>(bufferSize);
  }

  private void createSolrStateWatcher() throws Exception {
    if ("none".equals(splitMode)) {
      return;
    }
    
    CloudSolrClient stateWatcherClient = createSolrClient();
    stateWatcherClient.registerCollectionStateWatcher(collection, this);
    while (true) {
      if (shards == null) {
        LOG.info("Shards are not available yet, waiting ...");
        try { Thread.sleep(SHARDS_WAIT_MS); } catch (Exception e) { LOG.warn(e); }
      } else {
        break;
      }
    }
  }

  @Override
  public boolean onStateChanged(Set<String> liveNodes, DocCollection collectionState) {
    synchronized (propertiesLock) {
      if (collectionState != null) {
        List<String> shards = new ArrayList<>(collectionState.getSlicesMap().keySet());
        Collections.sort(shards);
        this.shards = shards;
      }
    }
    return false;
  }

  private void createSolrWorkers() throws Exception, MalformedURLException {
    for (int count = 0; count < workers; count++) {
      CloudSolrClient solrClient = getSolrClient(count);
      createSolrWorkerThread(count, solrClient);
    }
  }

  CloudSolrClient getSolrClient(int count) throws Exception, MalformedURLException {
    CloudSolrClient solrClient = createSolrClient();
    pingSolr(count, solrClient);

    return solrClient;
  }

  private CloudSolrClient createSolrClient() throws Exception {
    LOG.info("Using zookeepr. zkConnectString=" + zkConnectString);
    LOG.info("Using collection=" + collection);

    CloudSolrClient solrClient = new CloudSolrClient.Builder().withZkHost(zkConnectString).build();
    solrClient.setDefaultCollection(collection);
    return solrClient;
  }

  private void pingSolr(int count, CloudSolrClient solrClient) {
    try {
      LOG.info("Pinging Solr server. zkConnectString=" + zkConnectString);
      SolrPingResponse response = solrClient.ping();
      if (response.getStatus() == 0) {
        LOG.info("Ping to Solr server is successful for worker=" + count);
      } else {
        LOG.warn(
            String.format("Ping to Solr server failed. It would check again. worker=%d, zkConnectString=%s, collection=%s, " +
                "response=%s", count, zkConnectString, collection, response));
      }
    } catch (Throwable t) {
      LOG.warn(String.format(
          "Ping to Solr server failed. It would check again. worker=%d, zkConnectString=%s, collection=%s", count,
          zkConnectString, collection), t);
    }
  }

  private void createSolrWorkerThread(int count, SolrClient solrClient) {
    SolrWorkerThread solrWorkerThread = new SolrWorkerThread(solrClient);
    solrWorkerThread.setName(getNameForThread() + "," + collection + ",worker=" + count);
    solrWorkerThread.setDaemon(true);
    solrWorkerThread.start();
    workerThreadList.add(solrWorkerThread);
  }

  @Override
  public void write(Map<String, Object> jsonObj, InputMarker inputMarker) throws Exception {
    try {
      trimStrValue(jsonObj);
      useActualDateIfNeeded(jsonObj);
      outgoingBuffer.put(new OutputData(jsonObj, inputMarker));
    } catch (InterruptedException e) {
      // ignore
    }
  }

  private void useActualDateIfNeeded(Map<String, Object> jsonObj) {
    if (skipLogtime) {
      jsonObj.put("logtime", DateUtil.getActualDateStr());
      if (jsonObj.get("evtTime") != null) {
        jsonObj.put("evtTime", DateUtil.getActualDateStr());
      }
    }
  }

  public void flush() {
    LOG.info("Flush called...");
    setDrain(true);

    int wrapUpTimeSecs = 30;
    // Give wrapUpTimeSecs seconds to wrap up
    boolean isPending = false;
    for (int i = 0; i < wrapUpTimeSecs; i++) {
      for (SolrWorkerThread solrWorkerThread : workerThreadList) {
        if (solrWorkerThread.isDone()) {
          try {
            solrWorkerThread.interrupt();
          } catch (Throwable t) {
            // ignore
          }
        } else {
          isPending = true;
        }
      }
      if (isPending) {
        try {
          LOG.info("Will give " + (wrapUpTimeSecs - i) + " seconds to wrap up");
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // ignore
        }
      }
      isPending = false;
    }
  }

  @Override
  public void setDrain(boolean drain) {
    super.setDrain(drain);
  }

  @Override
  public long getPendingCount() {
    long pendingCount = 0;
    for (SolrWorkerThread solrWorkerThread : workerThreadList) {
      pendingCount += solrWorkerThread.localBuffer.size();
    }
    return pendingCount;
  }

  @Override
  public void close() {
    LOG.info("Closing Solr client...");
    flush();

    LOG.info("Closed Solr client");
    super.close();
  }

  @Override
  public String getShortDescription() {
    return "output:destination=solr,collection=" + collection;
  }

  class SolrWorkerThread extends Thread {
    private static final String ROUTER_FIELD = "_router_field_";

    private final SolrClient solrClient;
    private final Collection<SolrInputDocument> localBuffer = new ArrayList<>();
    private final Map<String, InputMarker> latestInputMarkers = new HashMap<>();

    private long localBufferBytesSize = 0;

    public SolrWorkerThread(SolrClient solrClient) {
      this.solrClient = solrClient;
    }

    @Override
    public void run() {
      LOG.info("SolrWorker thread started");
      long lastDispatchTime = System.currentTimeMillis();

      while (true) {
        long currTimeMS = System.currentTimeMillis();
        OutputData outputData = null;
        try {
          long nextDispatchDuration = maxIntervalMS - (currTimeMS - lastDispatchTime);
          outputData = getOutputData(nextDispatchDuration);

          if (outputData != null) {
            createSolrDocument(outputData);
          } else {
            if (isDrain() && outgoingBuffer.isEmpty()) {
              break;
            }
          }

          if (!localBuffer.isEmpty() &&
              (outputData == null && isDrain() || nextDispatchDuration <= 0 || localBuffer.size() >= maxBufferSize)
             ) {
            boolean response = sendToSolr(outputData);
            if (isDrain() && !response) {
              //Since sending to Solr response failed and it is in draining mode, let's break;
              LOG.warn("In drain mode and sending to Solr failed. So exiting. output=" + getShortDescription());
              break;
            }
          }
          if (localBuffer.isEmpty()) {
            //If localBuffer is empty, then reset the timer
            lastDispatchTime = currTimeMS;
          }
        } catch (InterruptedException e) {
          // Handle thread exiting
        } catch (Throwable t) {
          String logMessageKey = this.getClass().getSimpleName() + "_SOLR_MAINLOOP_EXCEPTION";
          LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Caught exception in main loop. " + outputData, t, LOG,
                Level.ERROR);
        }
      }

      closeSolrClient();

      resetLocalBuffer();
      LOG.info("Exiting Solr worker thread. output=" + getShortDescription());
    }
    
    /**
     * This will loop till Solr is available and LogFeeder is
     * successfully able to write to the collection or shard. It will block till
     * it can write. The outgoingBuffer is a BlockingQueue and when it is full, it
     * will automatically stop parsing the log files.
     */
    private boolean sendToSolr(OutputData outputData) {
      boolean result = false;
      while (!isDrain()) {
        try {
          synchronized (propertiesLock) {
            if (isComputeCurrentCollection) {
              // Compute the current router value
              addRouterField();
            }
          }
          addToSolr(outputData);
          resetLocalBuffer();
          //Send successful, will return 
          result = true;
          break;
        } catch (IOException | SolrException exception) {
          // Transient error, lets block till it is available
          try {
            LOG.warn("Solr is not reachable. Going to retry after " + RETRY_INTERVAL + " seconds. " + "output="
                + getShortDescription(), exception);
            Thread.sleep(RETRY_INTERVAL * 1000);
          } catch (Throwable t) {
            // ignore
          }
        } catch (Throwable serverException) {
          // Something unknown happened. Let's not block because of this error. 
          // Clear the buffer
          String logMessageKey = this.getClass().getSimpleName() + "_SOLR_UPDATE_EXCEPTION";
          LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Error sending log message to server. Dropping logs",
              serverException, LOG, Level.ERROR);
          resetLocalBuffer();
          break;
        }
      } 
      return result;
    }

    private OutputData getOutputData(long nextDispatchDuration) throws InterruptedException {
      OutputData outputData = outgoingBuffer.poll();
      if (outputData == null && !isDrain() && nextDispatchDuration > 0) {
        outputData = outgoingBuffer.poll(nextDispatchDuration, TimeUnit.MILLISECONDS);
      }
      if (outputData != null && outputData.jsonObj.get("id") == null) {
        outputData.jsonObj.put("id", UUID.randomUUID().toString());
      }
      return outputData;
    }

    private void createSolrDocument(OutputData outputData) {
      SolrInputDocument document = new SolrInputDocument();
      for (String name : outputData.jsonObj.keySet()) {
        Object obj = outputData.jsonObj.get(name);
        document.addField(name, obj);
        try {
          localBufferBytesSize += obj.toString().length();
        } catch (Throwable t) {
          String logMessageKey = this.getClass().getSimpleName() + "_BYTE_COUNT_ERROR";
          LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Error calculating byte size. object=" + obj, t, LOG,
              Level.ERROR);
        }
      }
      latestInputMarkers.put(outputData.inputMarker.base64FileKey, outputData.inputMarker);
      localBuffer.add(document);
    }

    private void addRouterField() {
      Calendar cal = Calendar.getInstance();
      int weekDay = cal.get(Calendar.DAY_OF_WEEK);
      int currHour = cal.get(Calendar.HOUR_OF_DAY);
      int currMin = cal.get(Calendar.MINUTE);

      int minOfWeek = (weekDay - 1) * 24 * 60 + currHour * 60 + currMin;
      int slotByMin = minOfWeek / splitInterval % shards.size();

      String shard = shards.get(slotByMin);

      if (lastSlotByMin != slotByMin) {
        LOG.info("Switching to shard " + shard + ", output=" + getShortDescription());
        lastSlotByMin = slotByMin;
      }

      for (SolrInputDocument solrInputDocument : localBuffer) {
        solrInputDocument.setField(ROUTER_FIELD, shard);
      }
    }

    private void addToSolr(OutputData outputData) throws SolrServerException, IOException {
      UpdateResponse response = solrClient.add(localBuffer);
      if (response.getStatus() != 0) {
        String logMessageKey = this.getClass().getSimpleName() + "_SOLR_UPDATE_ERROR";
        LogFeederUtil.logErrorMessageByInterval(logMessageKey,
            String.format("Error writing to Solr. response=%s, log=%s", response, outputData), null, LOG, Level.ERROR);
      }
      statMetric.value += localBuffer.size();
      writeBytesMetric.value += localBufferBytesSize;
      for (InputMarker inputMarker : latestInputMarkers.values()) {
        inputMarker.input.checkIn(inputMarker);
      }
    }

    private void closeSolrClient() {
      if (solrClient != null) {
        try {
          solrClient.close();
        } catch (IOException e) {
          // Ignore
        }
      }
    }

    public void resetLocalBuffer() {
      localBuffer.clear();
      localBufferBytesSize = 0;
      latestInputMarkers.clear();
    }

    public boolean isDone() {
      return localBuffer.isEmpty();
    }
  }

  @Override
  public void write(String block, InputMarker inputMarker) throws Exception {
  }

  @Override
  public void copyFile(File inputFile, InputMarker inputMarker) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("copyFile method is not yet supported for output=solr");
  }
}
