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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;
import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.loglevelfilter.LogLevelFilterHandler;
import org.apache.ambari.logfeeder.input.InputFile;
import org.apache.ambari.logfeeder.plugin.common.MetricData;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.ambari.logfeeder.plugin.input.InputMarker;
import org.apache.ambari.logfeeder.plugin.manager.OutputManager;
import org.apache.ambari.logfeeder.plugin.output.Output;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.OutputConfigMonitor;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OutputManagerImpl extends OutputManager {
  private static final Logger LOG = Logger.getLogger(OutputManagerImpl.class);

  private static final int MAX_OUTPUT_SIZE = 32765; // 32766-1

  private List<Output> outputs = new ArrayList<>();

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

  @Override
  public void init() throws Exception {
    for (Output output : outputs) {
      output.init(logFeederProps);
    }
  }

  public void write(Map<String, Object> jsonObj, InputMarker inputMarker) {
    Input input = inputMarker.getInput();

    // Update the block with the context fields
    for (Map.Entry<String, String> entry : input.getInputDescriptor().getAddFields().entrySet()) {
      if (jsonObj.get(entry.getKey()) == null || entry.getKey().equals("cluster") && "null".equals(jsonObj.get(entry.getKey()))) {
        jsonObj.put(entry.getKey(), entry.getValue());
      }
    }

    // TODO: Ideally most of the overrides should be configurable

    LogFeederUtil.fillMapWithFieldDefaults(jsonObj, inputMarker, true);
    jsonObj.putIfAbsent("level", LogFeederConstants.LOG_LEVEL_UNKNOWN);

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


      byte[] bytes = LogFeederUtil.getGson().toJson(jsonObj).getBytes();
      Long eventMD5 = Hashing.md5().hashBytes(bytes).asLong();
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
    if (StringUtils.isNotBlank(input.getInputDescriptor().getGroup())) {
      jsonObj.put("group", input.getInputDescriptor().getGroup());
    }
    if (inputMarker.getAllProperties().containsKey("line_number") &&
      (Integer) inputMarker.getAllProperties().get("line_number") > 0) {
      jsonObj.put("logfile_line_number", inputMarker.getAllProperties().get("line_number"));
    }
    if (jsonObj.containsKey("log_message")) {
      // TODO: Let's check size only for log_message for now
      String logMessage = (String) jsonObj.get("log_message");
      logMessage = truncateLongLogMessage(jsonObj, input, logMessage);
      if (addMessageMD5) {
        jsonObj.put("message_md5", "" + Hashing.md5().hashBytes(logMessage.getBytes()).asLong());
      }
    }
    if (logLevelFilterHandler.isAllowed(jsonObj, inputMarker)
      && !outputLineFilter.apply(jsonObj, inputMarker.getInput())) {
      List<? extends Output> outputList = input.getOutputList();
      for (Output output : outputList) {
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
      List<? extends Output> outputList = inputMarker.getInput().getOutputList();
      for (Output output : outputList) {
        try {
          output.write(jsonBlock, inputMarker);
        } catch (Exception e) {
          LOG.error("Error writing. to " + output.getShortDescription(), e);
        }
      }
    }
  }

  public void copyFile(File inputFile, InputMarker inputMarker) {
    Input input = inputMarker.getInput();
    List<? extends Output> outputList = input.getOutputList();
    for (Output output : outputList) {
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
