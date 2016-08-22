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

package org.apache.ambari.logfeeder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.logconfig.LogFeederConstants;
import org.apache.ambari.logfeeder.logconfig.filter.FilterLogData;
import org.apache.ambari.logfeeder.output.Output;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class OutputMgr {
  private static final Logger logger = Logger.getLogger(OutputMgr.class);

  private Collection<Output> outputList = new ArrayList<Output>();

  private boolean addMessageMD5 = true;

  private int MAX_OUTPUT_SIZE = 32765; // 32766-1
  private static long doc_counter = 0;
  private MetricCount messageTruncateMetric = new MetricCount();

  
  public Collection<Output> getOutputList() {
    return outputList;
  }

  public void setOutputList(Collection<Output> outputList) {
    this.outputList = outputList;
  }

  public void write(Map<String, Object> jsonObj, InputMarker inputMarker) {
    Input input = inputMarker.input;

    // Update the block with the context fields
    for (Map.Entry<String, String> entry : input.getContextFields()
      .entrySet()) {
      if (jsonObj.get(entry.getKey()) == null) {
        jsonObj.put(entry.getKey(), entry.getValue());
      }
    }

    // TODO: Ideally most of the overrides should be configurable

    // Add the input type
    if (jsonObj.get("type") == null) {
      jsonObj.put("type", input.getStringValue("type"));
    }
    if (jsonObj.get("path") == null && input.getFilePath() != null) {
      jsonObj.put("path", input.getFilePath());
    }
    if (jsonObj.get("path") == null && input.getStringValue("path") != null) {
      jsonObj.put("path", input.getStringValue("path"));
    }

    // Add host if required
    if (jsonObj.get("host") == null && LogFeederUtil.hostName != null) {
      jsonObj.put("host", LogFeederUtil.hostName);
    }
    // Add IP if required
    if (jsonObj.get("ip") == null && LogFeederUtil.ipAddress != null) {
      jsonObj.put("ip", LogFeederUtil.ipAddress);
    }
    
    //Add level
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
      Long eventMD5 = LogFeederUtil.genHash(LogFeederUtil.getGson()
        .toJson(jsonObj));
      if (input.isGenEventMD5()) {
        jsonObj.put("event_md5", prefix + eventMD5.toString());
      }
      if (input.isUseEventMD5()) {
        jsonObj.put("id", prefix + eventMD5.toString());
      }
    }

    // jsonObj.put("@timestamp", new Date());
    jsonObj.put("seq_num", new Long(doc_counter++));
    if (jsonObj.get("id") == null) {
      jsonObj.put("id", UUID.randomUUID().toString());
    }
    if (jsonObj.get("event_count") == null) {
      jsonObj.put("event_count", new Integer(1));
    }
    if (inputMarker.lineNumber > 0) {
      jsonObj.put("logfile_line_number", new Integer(
        inputMarker.lineNumber));
    }
    if (jsonObj.containsKey("log_message")) {
      // TODO: Let's check size only for log_message for now
      String logMessage = (String) jsonObj.get("log_message");
      if (logMessage != null
        && logMessage.getBytes().length > MAX_OUTPUT_SIZE) {
        messageTruncateMetric.count++;
        final String LOG_MESSAGE_KEY = this.getClass().getSimpleName()
          + "_MESSAGESIZE";
        LogFeederUtil.logErrorMessageByInterval(LOG_MESSAGE_KEY,
          "Message is too big. size="
            + logMessage.getBytes().length + ", input="
            + input.getShortDescription()
            + ". Truncating to " + MAX_OUTPUT_SIZE
            + ", first upto 100 characters="
            + LogFeederUtil.subString(logMessage, 100),
          null, logger, Level.WARN);
        logMessage = new String(logMessage.getBytes(), 0,
          MAX_OUTPUT_SIZE);
        jsonObj.put("log_message", logMessage);
        // Add error tags
        @SuppressWarnings("unchecked")
        List<String> tagsList = (List<String>) jsonObj.get("tags");
        if (tagsList == null) {
          tagsList = new ArrayList<String>();
          jsonObj.put("tags", tagsList);
        }
        tagsList.add("error_message_truncated");

      }
      if (addMessageMD5) {
        jsonObj.put("message_md5",
          "" + LogFeederUtil.genHash(logMessage));
      }
    }
    //check log is allowed to send output
    if (FilterLogData.INSTANCE.isAllowed(jsonObj)) {
      for (Output output : input.getOutputList()) {
        try {
          output.write(jsonObj, inputMarker);
        } catch (Exception e) {
          logger.error("Error writing. to " + output.getShortDescription(), e);
        }
      }
    }
  }

  public void write(String jsonBlock, InputMarker inputMarker) {
    //check log is allowed to send output
    if (FilterLogData.INSTANCE.isAllowed(jsonBlock)) {
      for (Output output : inputMarker.input.getOutputList()) {
        try {
          output.write(jsonBlock, inputMarker);
        } catch (Exception e) {
          logger.error("Error writing. to " + output.getShortDescription(), e);
        }
      }
    }
  }

  public void close() {
    logger.info("Close called for outputs ...");
    for (Output output : outputList) {
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
    int i;
    boolean allClosed = true;
    for (i = 0; i < iterations; i++) {
      allClosed = true;
      for (Output output : outputList) {
        if (!output.isClosed()) {
          try {
            allClosed = false;
            logger.warn("Waiting for output to close. "
              + output.getShortDescription() + ", "
              + (iterations - i) + " more seconds");
            Thread.sleep(waitTimeMS);
          } catch (Throwable t) {
            // Ignore
          }
        }
      }
      if (allClosed) {
        break;
      }
    }

    if (!allClosed) {
      logger.warn("Some outpus were not closed. Iterations=" + i);
      for (Output output : outputList) {
        if (!output.isClosed()) {
          logger.warn("Output not closed. Will ignore it."
            + output.getShortDescription() + ", pendingCound="
            + output.getPendingCount());
        }
      }
    } else {
      logger.info("All outputs are closed. Iterations=" + i);
    }
  }

  public void logStats() {
    for (Output output : outputList) {
      output.logStat();
    }
    LogFeederUtil.logStatForMetric(messageTruncateMetric,
      "Stat: Messages Truncated", null);
  }

  public void addMetricsContainers(List<MetricCount> metricsList) {
    metricsList.add(messageTruncateMetric);
    for (Output output : outputList) {
      output.addMetricsContainers(metricsList);
    }
  }

  
  public void copyFile(File inputFile, InputMarker inputMarker) {
    Input input = inputMarker.input;
    for (Output output : input.getOutputList()) {
      try {
        output.copyFile(inputFile, inputMarker);
      }catch (Exception e) {
        logger.error("Error coyping file . to " + output.getShortDescription(),
            e);
      }
    }
  }
}
