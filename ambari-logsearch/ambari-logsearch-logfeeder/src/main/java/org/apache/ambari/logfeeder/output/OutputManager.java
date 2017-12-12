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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.annotations.VisibleForTesting;
import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.loglevelfilter.LogLevelFilterHandler;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logfeeder.util.MurmurHash;
import org.apache.ambari.logsearch.config.api.OutputConfigMonitor;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.inject.Inject;

public class OutputManager {
  private static final Logger LOG = Logger.getLogger(OutputManager.class);

  private static final int HASH_SEED = 31174077;
  private static final int MAX_OUTPUT_SIZE = 32765; // 32766-1

  private List<Output> outputs = new ArrayList<Output>();

  private boolean addMessageMD5 = true;

  private static long docCounter = 0;
  private MetricData messageTruncateMetric = new MetricData(null, false);

  @Inject
  private LogLevelFilterHandler logLevelFilterHandler;

  @Inject
  private LogFeederProps logFeederProps;

  private OutputLineFilter outputLineFilter = new OutputLineFilter();

  public List<Output> getOutputs() {
    return outputs;
  }

  public List<? extends OutputConfigMonitor> getOutputsToMonitor() {
    List<Output> outputsToMonitor = new ArrayList<>();
    for (Output output : outputs) {
      if (output.monitorConfigChanges()) {
        outputsToMonitor.add(output);
      }
    }
    return outputsToMonitor;
  }

  public void add(Output output) {
    this.outputs.add(output);
  }

  public void init() throws Exception {
    for (Output output : outputs) {
      output.init(logFeederProps);
    }
  }

  public void write(Map<String, Object> jsonObj, InputMarker inputMarker) {
    Input input = inputMarker.input;

    // Update the block with the context fields
    for (Map.Entry<String, String> entry : input.getInputDescriptor().getAddFields().entrySet()) {
      if (jsonObj.get(entry.getKey()) == null || entry.getKey().equals("cluster") && "null".equals(jsonObj.get(entry.getKey()))) {
        jsonObj.put(entry.getKey(), entry.getValue());
      }
    }

    // TODO: Ideally most of the overrides should be configurable

    if (jsonObj.get("type") == null) {
      jsonObj.put("type", input.getInputDescriptor().getType());
    }
    if (jsonObj.get("path") == null && input.getFilePath() != null) {
      jsonObj.put("path", input.getFilePath());
    }
    if (jsonObj.get("path") == null && input.getInputDescriptor().getPath() != null) {
      jsonObj.put("path", input.getInputDescriptor().getPath());
    }
    if (jsonObj.get("host") == null && LogFeederUtil.hostName != null) {
      jsonObj.put("host", LogFeederUtil.hostName);
    }
    if (jsonObj.get("ip") == null && LogFeederUtil.ipAddress != null) {
      jsonObj.put("ip", LogFeederUtil.ipAddress);
    }
    if (jsonObj.get("level") == null) {
      jsonObj.put("level", LogFeederConstants.LOG_LEVEL_UNKNOWN);
    }
    
    if (input.isUseEventMD5() || input.isGenEventMD5()) {
      String prefix = "";
      Object logtimeObj = jsonObj.get("logtime");
      if (logtimeObj != null) {
        if (logtimeObj instanceof Date) {
          prefix = "" + ((Date) logtimeObj).getTime();
        } else {
          prefix = logtimeObj.toString();
        }
      }
      
      Long eventMD5 = MurmurHash.hash64A(LogFeederUtil.getGson().toJson(jsonObj).getBytes(), HASH_SEED);
      if (input.isGenEventMD5()) {
        jsonObj.put("event_md5", prefix + eventMD5.toString());
      }
      if (input.isUseEventMD5()) {
        jsonObj.put("id", prefix + eventMD5.toString());
      }
    }

    jsonObj.put("seq_num", new Long(docCounter++));
    if (jsonObj.get("id") == null) {
      jsonObj.put("id", UUID.randomUUID().toString());
    }
    if (jsonObj.get("event_count") == null) {
      jsonObj.put("event_count", new Integer(1));
    }
    if (inputMarker.lineNumber > 0) {
      jsonObj.put("logfile_line_number", new Integer(inputMarker.lineNumber));
    }
    if (jsonObj.containsKey("log_message")) {
      // TODO: Let's check size only for log_message for now
      String logMessage = (String) jsonObj.get("log_message");
      logMessage = truncateLongLogMessage(jsonObj, input, logMessage);
      if (addMessageMD5) {
        jsonObj.put("message_md5", "" + MurmurHash.hash64A(logMessage.getBytes(), 31174077));
      }
    }
    if (logLevelFilterHandler.isAllowed(jsonObj, inputMarker)
      && !outputLineFilter.apply(jsonObj, inputMarker.input)) {
      for (Output output : input.getOutputList()) {
        try {
          output.write(jsonObj, inputMarker);
        } catch (Exception e) {
          LOG.error("Error writing. to " + output.getShortDescription(), e);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private String truncateLongLogMessage(Map<String, Object> jsonObj, Input input, String logMessage) {
    if (logMessage != null && logMessage.getBytes().length > MAX_OUTPUT_SIZE) {
      messageTruncateMetric.value++;
      String logMessageKey = this.getClass().getSimpleName() + "_MESSAGESIZE";
      LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Message is too big. size=" + logMessage.getBytes().length +
          ", input=" + input.getShortDescription() + ". Truncating to " + MAX_OUTPUT_SIZE + ", first upto 100 characters=" +
          StringUtils.abbreviate(logMessage, 100), null, LOG, Level.WARN);
      logMessage = new String(logMessage.getBytes(), 0, MAX_OUTPUT_SIZE);
      jsonObj.put("log_message", logMessage);
      List<String> tagsList = (List<String>) jsonObj.get("tags");
      if (tagsList == null) {
        tagsList = new ArrayList<String>();
        jsonObj.put("tags", tagsList);
      }
      tagsList.add("error_message_truncated");
    }
    return logMessage;
  }

  public void write(String jsonBlock, InputMarker inputMarker) {
    if (logLevelFilterHandler.isAllowed(jsonBlock, inputMarker)) {
      for (Output output : inputMarker.input.getOutputList()) {
        try {
          output.write(jsonBlock, inputMarker);
        } catch (Exception e) {
          LOG.error("Error writing. to " + output.getShortDescription(), e);
        }
      }
    }
  }

  public void copyFile(File inputFile, InputMarker inputMarker) {
    Input input = inputMarker.input;
    for (Output output : input.getOutputList()) {
      try {
        output.copyFile(inputFile, inputMarker);
      }catch (Exception e) {
        LOG.error("Error coyping file . to " + output.getShortDescription(), e);
      }
    }
  }

  public void logStats() {
    for (Output output : outputs) {
      output.logStat();
    }
    LogFeederUtil.logStatForMetric(messageTruncateMetric, "Stat: Messages Truncated", "");
  }

  public void addMetricsContainers(List<MetricData> metricsList) {
    metricsList.add(messageTruncateMetric);
    for (Output output : outputs) {
      output.addMetricsContainers(metricsList);
    }
  }

  public void close() {
    LOG.info("Close called for outputs ...");
    for (Output output : outputs) {
      try {
        output.setDrain(true);
        output.close();
      } catch (Exception e) {
        // Ignore
      }
    }
    
    // Need to get this value from property
    int iterations = 30;
    int waitTimeMS = 1000;
    for (int i = 0; i < iterations; i++) {
      boolean allClosed = true;
      for (Output output : outputs) {
        if (!output.isClosed()) {
          try {
            allClosed = false;
            LOG.warn("Waiting for output to close. " + output.getShortDescription() + ", " + (iterations - i) + " more seconds");
            Thread.sleep(waitTimeMS);
          } catch (Throwable t) {
            // Ignore
          }
        }
      }
      if (allClosed) {
        LOG.info("All outputs are closed. Iterations=" + i);
        return;
      }
    }

    LOG.warn("Some outpus were not closed after " + iterations + "  iterations");
    for (Output output : outputs) {
      if (!output.isClosed()) {
        LOG.warn("Output not closed. Will ignore it." + output.getShortDescription() + ", pendingCound=" + output.getPendingCount());
      }
    }
  }

  public LogLevelFilterHandler getLogLevelFilterHandler() {
    return logLevelFilterHandler;
  }

  @VisibleForTesting
  public void setLogLevelFilterHandler(LogLevelFilterHandler logLevelFilterHandler) {
    this.logLevelFilterHandler = logLevelFilterHandler;
  }

  public LogFeederProps getLogFeederProps() {
    return logFeederProps;
  }

  @VisibleForTesting
  public void setLogFeederProps(LogFeederProps logFeederProps) {
    this.logFeederProps = logFeederProps;
  }
}
