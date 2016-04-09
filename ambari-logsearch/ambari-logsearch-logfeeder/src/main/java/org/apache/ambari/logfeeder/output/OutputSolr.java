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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.logfeeder.LogFeederUtil;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

public class OutputSolr extends Output {
  static private Logger logger = Logger.getLogger(OutputSolr.class);

  private static final String ROUTER_FIELD = "_router_field_";

  String solrUrl = null;
  String zkHosts = null;
  String collection = null;
  String splitMode = "none";
  int splitInterval = 0;
  int numberOfShards = 1;
  boolean isComputeCurrentCollection = false;

  int maxBufferSize = 5000;
  int maxIntervalMS = 3000;
  int workers = 1;

  BlockingQueue<OutputData> outgoingBuffer = null;
  List<SolrWorkerThread> writerThreadList = new ArrayList<SolrWorkerThread>();
  private static final int RETRY_INTERVAL = 30;

  int lastSlotByMin = -1;

  @Override
  public void init() throws Exception {
    super.init();
    statMetric.metricsName = "output.solr.write_logs";
    writeBytesMetric.metricsName = "output.solr.write_bytes";

    solrUrl = getStringValue("url");
    zkHosts = getStringValue("zk_hosts");
    splitMode = getStringValue("splits_interval_mins", splitMode);
    if (!splitMode.equalsIgnoreCase("none")) {
      splitInterval = getIntValue("split_interval_mins", 30);
    }
    numberOfShards = getIntValue("number_of_shards", numberOfShards);

    maxBufferSize = getIntValue("flush_size", maxBufferSize);
    if (maxBufferSize < 1) {
      logger.warn("maxBufferSize is less than 1. Making it 1");
    }
    maxIntervalMS = getIntValue("idle_flush_time_ms", maxIntervalMS);
    workers = getIntValue("workers", workers);

    logger.info("Config: Number of workers=" + workers + ", splitMode="
        + splitMode + ", splitInterval=" + splitInterval
        + ", numberOfShards=" + numberOfShards + ". "
        + getShortDescription());

    if (StringUtils.isEmpty(solrUrl) && StringUtils.isEmpty(zkHosts)) {
      throw new Exception(
          "For solr output, either url or zk_hosts property need to be set");
    }

    int bufferSize = maxBufferSize * (workers + 3);
    logger.info("Creating blocking queue with bufferSize=" + bufferSize);
    // outgoingBuffer = new ArrayBlockingQueue<OutputData>(bufferSize);
    outgoingBuffer = new LinkedBlockingQueue<OutputData>(bufferSize);

    for (int count = 0; count < workers; count++) {
      SolrClient solrClient = null;
      CloudSolrClient solrClouldClient = null;
      if (zkHosts != null) {
        logger.info("Using zookeepr. zkHosts=" + zkHosts);
        collection = getStringValue("collection");
        if (StringUtils.isEmpty(collection)) {
          throw new Exception(
              "For solr cloud property collection is mandatory");
        }
        logger.info("Using collection=" + collection);
        solrClouldClient = new CloudSolrClient(zkHosts);
        solrClouldClient.setDefaultCollection(collection);
        solrClient = solrClouldClient;
        if (splitMode.equalsIgnoreCase("none")) {
          isComputeCurrentCollection = false;
        } else {
          isComputeCurrentCollection = true;
        }
      } else {
        String[] solrUrls = StringUtils.split(solrUrl, ",");
        if (solrUrls.length == 1) {
          logger.info("Using SolrURL=" + solrUrl);
          solrClient = new HttpSolrClient(solrUrl);
        } else {
          logger.info("Using load balance solr client. solrUrls="
              + solrUrl);
          logger.info("Initial URL for LB solr=" + solrUrls[0]);
          @SuppressWarnings("resource")
          LBHttpSolrClient lbSolrClient = new LBHttpSolrClient(
              solrUrls[0]);
          for (int i = 1; i < solrUrls.length; i++) {
            logger.info("Adding URL for LB solr=" + solrUrls[i]);
            lbSolrClient.addSolrServer(solrUrls[i]);
          }
          solrClient = lbSolrClient;
        }
      }
      try {
        logger.info("Pinging Solr server. zkHosts=" + zkHosts
            + ", urls=" + solrUrl);
        SolrPingResponse response = solrClient.ping();
        if (response.getStatus() == 0) {
          logger.info("Ping to Solr server is successful for writer="
              + count);
        } else {
          logger.warn("Ping to Solr server failed. It would check again. writer="
              + count
              + ", solrUrl="
              + solrUrl
              + ", zkHosts="
              + zkHosts
              + ", collection="
              + collection
              + ", response=" + response);
        }
      } catch (Throwable t) {
        logger.warn(
            "Ping to Solr server failed. It would check again. writer="
                + count + ", solrUrl=" + solrUrl + ", zkHosts="
                + zkHosts + ", collection=" + collection, t);
      }

      // Let's start the thread
      SolrWorkerThread solrWriterThread = new SolrWorkerThread(solrClient);
      solrWriterThread.setName(getNameForThread() + "," + collection
          + ",writer=" + count);
      solrWriterThread.setDaemon(true);
      solrWriterThread.start();
      writerThreadList.add(solrWriterThread);
    }
  }

  @Override
  public void setDrain(boolean drain) {
    super.setDrain(drain);
  }

  /**
   * Flush document buffer
   */
  public void flush() {
    logger.info("Flush called...");
    setDrain(true);

    int wrapUpTimeSecs = 30;
    // Give wrapUpTimeSecs seconds to wrap up
    boolean isPending = false;
    for (int i = 0; i < wrapUpTimeSecs; i++) {
      for (SolrWorkerThread solrWorkerThread : writerThreadList) {
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
          logger.info("Will give " + (wrapUpTimeSecs - i)
              + " seconds to wrap up");
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // ignore
        }
      }
      isPending = false;
    }
  }

  @Override
  public long getPendingCount() {
    long totalCount = 0;
    for (SolrWorkerThread solrWorkerThread : writerThreadList) {
      totalCount += solrWorkerThread.localBuffer.size();
    }
    return totalCount;
  }

  @Override
  public void close() {
    logger.info("Closing Solr client...");
    flush();

    logger.info("Closed Solr client");
    super.close();
  }

  @Override
  public void write(Map<String, Object> jsonObj, InputMarker inputMarker)
      throws Exception {
    try {
      outgoingBuffer.put(new OutputData(jsonObj, inputMarker));
    } catch (InterruptedException e) {
      // ignore
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.ambari.logfeeder.ConfigBlock#getShortDescription()
   */
  @Override
  public String getShortDescription() {
    return "output:destination=solr,collection=" + collection;
  }

  class SolrWorkerThread extends Thread {
    /**
     * 
     */
    SolrClient solrClient = null;
    Collection<SolrInputDocument> localBuffer = new ArrayList<SolrInputDocument>();
    long localBufferBytesSize = 0;
    Map<String, InputMarker> latestInputMarkerList = new HashMap<String, InputMarker>();

    /**
     * 
     */
    public SolrWorkerThread(SolrClient solrClient) {
      this.solrClient = solrClient;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      logger.info("SolrWriter thread started");
      long lastDispatchTime = System.currentTimeMillis();

      //long totalWaitTimeMS = 0;
      while (true) {
        long currTimeMS = System.currentTimeMillis();
        OutputData outputData = null;
        try {
          long nextDispatchDuration = maxIntervalMS
              - (currTimeMS - lastDispatchTime);
          outputData = outgoingBuffer.poll();
          if (outputData == null && !isDrain()
              && nextDispatchDuration > 0) {
            outputData = outgoingBuffer.poll(nextDispatchDuration,
                TimeUnit.MILLISECONDS);
//            long diffTimeMS = System.currentTimeMillis()
//                - currTimeMS;
            // logger.info("Waited for " + diffTimeMS +
            // " ms, planned for "
            // + nextDispatchDuration + " ms, localBuffer.size="
            // + localBuffer.size() + ", timedOut="
            // + (outputData == null ? "true" : "false"));
          }

          if (isDrain() && outputData == null
              && outgoingBuffer.size() == 0) {
            break;
          }
          if (outputData != null) {
            if (outputData.jsonObj.get("id") == null) {
              outputData.jsonObj.put("id", UUID.randomUUID()
                  .toString());
            }
            SolrInputDocument document = new SolrInputDocument();
            for (String name : outputData.jsonObj.keySet()) {
              Object obj = outputData.jsonObj.get(name);
              document.addField(name, obj);
              try {
                localBufferBytesSize += obj.toString().length();
              } catch (Throwable t) {
                final String LOG_MESSAGE_KEY = this.getClass()
                    .getSimpleName() + "_BYTE_COUNT_ERROR";
                LogFeederUtil.logErrorMessageByInterval(
                    LOG_MESSAGE_KEY,
                    "Error calculating byte size. object="
                        + obj, t, logger, Level.ERROR);

              }
            }
            latestInputMarkerList.put(
                outputData.inputMarker.base64FileKey,
                outputData.inputMarker);
            localBuffer.add(document);
          }

          if (localBuffer.size() > 0
              && ((outputData == null && isDrain()) || (nextDispatchDuration <= 0 || localBuffer
                  .size() >= maxBufferSize))) {
            try {
              if (isComputeCurrentCollection) {
                // Compute the current router value

                int weekDay = Calendar.getInstance().get(
                    Calendar.DAY_OF_WEEK);
                int currHour = Calendar.getInstance().get(
                    Calendar.HOUR_OF_DAY);
                int currMin = Calendar.getInstance().get(
                    Calendar.MINUTE);

                int minOfWeek = (weekDay - 1) * 24 * 60
                    + currHour * 60 + currMin;
                int slotByMin = minOfWeek / splitInterval
                    % numberOfShards;

                String shard = "shard" + slotByMin;

                if (lastSlotByMin != slotByMin) {
                  logger.info("Switching to shard " + shard
                      + ", output="
                      + getShortDescription());
                  lastSlotByMin = slotByMin;
                }

                for (SolrInputDocument solrInputDocument : localBuffer) {
                  solrInputDocument.addField(ROUTER_FIELD,
                      shard);
                }
              }

//              long beginTime = System.currentTimeMillis();
              UpdateResponse response = solrClient
                  .add(localBuffer);
//              long endTime = System.currentTimeMillis();
//              logger.info("Adding to Solr. Document count="
//                  + localBuffer.size() + ". Took "
//                  + (endTime - beginTime) + " ms");

              if (response.getStatus() != 0) {
                final String LOG_MESSAGE_KEY = this.getClass()
                    .getSimpleName() + "_SOLR_UPDATE_ERROR";
                LogFeederUtil
                    .logErrorMessageByInterval(
                        LOG_MESSAGE_KEY,
                        "Error writing to Solr. response="
                            + response.toString()
                            + ", log="
                            + (outputData == null ? null
                                : outputData
                                    .toString()),
                        null, logger, Level.ERROR);
              }
              statMetric.count += localBuffer.size();
              writeBytesMetric.count += localBufferBytesSize;
              for (InputMarker inputMarker : latestInputMarkerList
                  .values()) {
                inputMarker.input.checkIn(inputMarker);
              }

              resetLocalBuffer();
              lastDispatchTime = System.currentTimeMillis();
            } catch (IOException ioException) {
              // Transient error, lets block till it is available
              while (!isDrain()) {
                try {
                  logger.warn("Solr is down. Going to sleep for "
                      + RETRY_INTERVAL
                      + " seconds. output="
                      + getShortDescription());
                  Thread.sleep(RETRY_INTERVAL * 1000);
                } catch (Throwable t) {
                  // ignore
                  break;
                }
                if (isDrain()) {
                  break;
                }
                try {
                  SolrPingResponse pingResponse = solrClient
                      .ping();
                  if (pingResponse.getStatus() == 0) {
                    logger.info("Solr seems to be up now. Resuming... output="
                        + getShortDescription());
                    break;
                  }
                } catch (Throwable t) {
                  // Ignore
                }
              }
            } catch (Throwable serverException) {
              // Clear the buffer
              resetLocalBuffer();
              final String LOG_MESSAGE_KEY = this.getClass()
                  .getSimpleName() + "_SOLR_UPDATE_EXCEPTION";
              LogFeederUtil.logErrorMessageByInterval(
                  LOG_MESSAGE_KEY,
                  "Error sending log message to server. "
                      + (outputData == null ? null
                          : outputData.toString()),
                  serverException, logger, Level.ERROR);
            }
          }
        } catch (InterruptedException e) {
          // Handle thread exiting
        } catch (Throwable t) {
          final String LOG_MESSAGE_KEY = this.getClass()
              .getSimpleName() + "_SOLR_MAINLOOP_EXCEPTION";
          LogFeederUtil.logErrorMessageByInterval(LOG_MESSAGE_KEY,
              "Caught exception in main loop. " + outputData, t,
              logger, Level.ERROR);
        }
      }

      if (solrClient != null) {
        try {
          solrClient.close();
        } catch (IOException e) {
          // Ignore
        }
      }

      resetLocalBuffer();
      logger.info("Exiting Solr writer thread. output="
          + getShortDescription());
    }

    public boolean isDone() {
      return localBuffer.size() == 0;
    }

    public void resetLocalBuffer() {
      localBuffer.clear();
      localBufferBytesSize = 0;
      latestInputMarkerList.clear();
    }
  }
}
