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

package org.apache.ambari.logfeeder.input;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logfeeder.common.ConfigBlock;
import org.apache.ambari.logfeeder.common.LogfeederException;
import org.apache.ambari.logfeeder.filter.Filter;
import org.apache.ambari.logfeeder.metrics.MetricCount;
import org.apache.ambari.logfeeder.output.Output;
import org.apache.ambari.logfeeder.output.OutputMgr;
import org.apache.log4j.Logger;

public abstract class Input extends ConfigBlock implements Runnable {
  static private Logger logger = Logger.getLogger(Input.class);

  protected OutputMgr outputMgr;
  protected InputMgr inputMgr;

  private List<Output> outputList = new ArrayList<Output>();

  private Filter firstFilter = null;
  private Thread thread;
  private boolean isClosed = false;
  protected String filePath = null;
  private String type = null;

  protected boolean tail = true;
  private boolean useEventMD5 = false;
  private boolean genEventMD5 = true;

  protected MetricCount readBytesMetric = new MetricCount();

  /**
   * This method will be called from the thread spawned for the output. This
   * method should only exit after all data are read from the source or the
   * process is exiting
   */
  abstract void start() throws Exception;

  @Override
  public void init() throws Exception {
    super.init();
    tail = getBooleanValue("tail", tail);
    useEventMD5 = getBooleanValue("use_event_md5_as_id", useEventMD5);
    genEventMD5 = getBooleanValue("gen_event_md5", genEventMD5);

    if (firstFilter != null) {
      firstFilter.init();
    }
  }

  @Override
  public String getNameForThread() {
    if (filePath != null) {
      try {
        return (type + "=" + (new File(filePath)).getName());
      } catch (Throwable ex) {
        logger.warn("Couldn't get basename for filePath=" + filePath,
          ex);
      }
    }
    return super.getNameForThread() + ":" + type;
  }

  @Override
  public void run() {
    try {
      logger.info("Started to monitor. " + getShortDescription());
      start();
    } catch (Exception e) {
      logger.error("Error writing to output.", e);
    }
    logger.info("Exiting thread. " + getShortDescription());
  }

  protected void outputLine(String line, InputMarker marker) {
    statMetric.count++;
    readBytesMetric.count += (line.length());

    if (firstFilter != null) {
      try {
        firstFilter.apply(line, marker);
      } catch (LogfeederException e) {
        logger.error(e.getLocalizedMessage(),e);
      }
    } else {
      // TODO: For now, let's make filter mandatory, so that no one
      // accidently forgets to write filter
      // outputMgr.write(line, this);
    }
  }

  protected void flush() {
    if (firstFilter != null) {
      firstFilter.flush();
    }
  }

  public boolean monitor() {
    if (isReady()) {
      logger.info("Starting thread. " + getShortDescription());
      thread = new Thread(this, getNameForThread());
      thread.start();
      return true;
    } else {
      return false;
    }
  }

  public void checkIn(InputMarker inputMarker) {
    // Default implementation is to ignore.
  }

  /**
   * This is generally used by final checkin
   */
  public void checkIn() {
  }

  public boolean isReady() {
    return true;
  }

  public boolean isTail() {
    return tail;
  }

  public void setTail(boolean tail) {
    this.tail = tail;
  }

  public boolean isUseEventMD5() {
    return useEventMD5;
  }

  public void setUseEventMD5(boolean useEventMD5) {
    this.useEventMD5 = useEventMD5;
  }

  public boolean isGenEventMD5() {
    return genEventMD5;
  }

  public void setGenEventMD5(boolean genEventMD5) {
    this.genEventMD5 = genEventMD5;
  }

  @Override
  public void setDrain(boolean drain) {
    logger.info("Request to drain. " + getShortDescription());
    super.setDrain(drain);
    ;
    try {
      thread.interrupt();
    } catch (Throwable t) {
      // ignore
    }
  }

  public Filter getFirstFilter() {
    return firstFilter;
  }

  public void setFirstFilter(Filter filter) {
    firstFilter = filter;
  }

  public void setInputMgr(InputMgr inputMgr) {
    this.inputMgr = inputMgr;
  }

  public void setOutputMgr(OutputMgr outputMgr) {
    this.outputMgr = outputMgr;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public void close() {
    logger.info("Close called. " + getShortDescription());

    try {
      if (firstFilter != null) {
        firstFilter.close();
      } else {
        outputMgr.close();
      }
    } catch (Throwable t) {
      // Ignore
    }
    isClosed = true;
  }

  public void setClosed(boolean isClosed) {
    this.isClosed = isClosed;
  }

  public boolean isClosed() {
    return isClosed;
  }

  @Override
  public void loadConfig(Map<String, Object> map) {
    super.loadConfig(map);
    String typeValue = getStringValue("type");
    if (typeValue != null) {
      // Explicitly add type and value to field list
      contextFields.put("type", typeValue);
      @SuppressWarnings("unchecked")
      Map<String, Object> addFields = (Map<String, Object>) map
        .get("add_fields");
      if (addFields == null) {
        addFields = new HashMap<String, Object>();
        map.put("add_fields", addFields);
      }
      addFields.put("type", typeValue);
    }
  }

  @Override
  public String getShortDescription() {
    return null;
  }

  @Override
  public void logStat() {
    super.logStat();
    logStatForMetric(readBytesMetric, "Stat: Bytes Read");

    if (firstFilter != null) {
      firstFilter.logStat();
    }
  }

  @Override
  public String toString() {
    return getShortDescription();
  }

  public void rollOver() {
    // Only some inputs support it. E.g. InputFile
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Date getEventTime() {
    return null;
  }

  public List<Output> getOutputList() {
    return outputList;
  }

  public void addOutput(Output output) {
    outputList.add(output);
  }

  public void addMetricsContainers(List<MetricCount> metricsList) {
    super.addMetricsContainers(metricsList);
    if (firstFilter != null) {
      firstFilter.addMetricsContainers(metricsList);
    }
    metricsList.add(readBytesMetric);
  }
  
  public Thread getThread(){
    return thread;
  }

}
