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
package org.apache.ambari.logfeeder.plugin.output;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ambari.logfeeder.plugin.common.ConfigItem;
import org.apache.ambari.logfeeder.plugin.common.LogFeederProperties;
import org.apache.ambari.logfeeder.plugin.common.MetricData;
import org.apache.ambari.logfeeder.plugin.input.InputMarker;
import org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder;
import org.apache.ambari.logsearch.config.api.OutputConfigMonitor;
import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

public abstract class Output<PROP_TYPE extends LogFeederProperties, INPUT_MARKER extends InputMarker> extends ConfigItem<PROP_TYPE> implements OutputConfigMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(Output.class);

  private final static String GSON_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  private static Gson gson = new GsonBuilder().setDateFormat(GSON_DATE_FORMAT).create();

  private LogSearchConfigLogFeeder logSearchConfig;
  private String destination = null;
  private boolean isClosed;
  protected MetricData writeBytesMetric = new MetricData(getWriteBytesMetricName(), false);

  public abstract String getOutputType();

  public abstract void outputConfigChanged(OutputProperties outputProperties);

  public abstract void copyFile(File inputFile, InputMarker inputMarker) throws Exception;

  public abstract void write(String jsonStr, INPUT_MARKER inputMarker) throws Exception;

  public abstract Long getPendingCount();

  public abstract String getWriteBytesMetricName();

  public String getNameForThread() {
    return this.getClass().getSimpleName();
  }

  public boolean monitorConfigChanges() {
    return false;
  };

  public void setLogSearchConfig(LogSearchConfigLogFeeder logSearchConfig) {
    this.logSearchConfig = logSearchConfig;
  }

  public LogSearchConfigLogFeeder getLogSearchConfig() {
    return logSearchConfig;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public boolean isClosed() {
    return isClosed;
  }

  public void setClosed(boolean closed) {
    isClosed = closed;
  }

  public void write(Map<String, Object> jsonObj, INPUT_MARKER inputMarker) throws Exception {
    write(gson.toJson(jsonObj), inputMarker);
  }

  @Override
  public void addMetricsContainers(List<MetricData> metricsList) {
    super.addMetricsContainers(metricsList);
    metricsList.add(writeBytesMetric);
  }

  @Override
  public synchronized void logStat() {
    super.logStat();
    logStatForMetric(writeBytesMetric, "Stat: Bytes Written");
  }

  @Override
  public boolean logConfigs() {
    // TODO: log something about the configs
    return true;
  }

  public void trimStrValue(Map<String, Object> jsonObj) {
    if (jsonObj != null) {
      for (Map.Entry<String, Object> entry : jsonObj.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if (value != null && value instanceof String) {
          String valueStr = value.toString().trim();
          jsonObj.put(key, valueStr);
        }
      }
    }
  }

  public void close() {
    LOG.info("Calling base close()." + getShortDescription());
    isClosed = true;
  }
}
