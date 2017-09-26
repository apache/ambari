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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ambari.logfeeder.common.ConfigBlock;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder;
import org.apache.ambari.logsearch.config.api.OutputConfigMonitor;
import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputProperties;

public abstract class Output extends ConfigBlock implements OutputConfigMonitor {
  private String destination = null;

  protected MetricData writeBytesMetric = new MetricData(getWriteBytesMetricName(), false);
  protected String getWriteBytesMetricName() {
    return null;
  }

  public boolean monitorConfigChanges() {
    return false;
  };
  
  @Override
  public String getOutputType() {
    throw new IllegalStateException("This method should be overriden if the Output wants to monitor the configuration");
  }
  
  @Override
  public void outputConfigChanged(OutputProperties outputProperties) {
    throw new IllegalStateException("This method should be overriden if the Output wants to monitor the configuration");
  };

  @Override
  public String getShortDescription() {
    return null;
  }

  @Override
  public String getNameForThread() {
    if (destination != null) {
      return destination;
    }
    return super.getNameForThread();
  }

  public abstract void write(String block, InputMarker inputMarker) throws Exception;
  
  public abstract void copyFile(File inputFile, InputMarker inputMarker) throws UnsupportedOperationException;

  public void write(Map<String, Object> jsonObj, InputMarker inputMarker) throws Exception {
    write(LogFeederUtil.getGson().toJson(jsonObj), inputMarker);
  }

  boolean isClosed = false;

  /**
   * Extend this method to clean up
   */
  public void close() {
    LOG.info("Calling base close()." + getShortDescription());
    isClosed = true;
  }

  /**
   * This is called on shutdown. All output should extend it.
   */
  public boolean isClosed() {
    return isClosed;
  }

  public long getPendingCount() {
    return 0;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  protected LogSearchConfigLogFeeder logSearchConfig;

  public void setLogSearchConfig(LogSearchConfigLogFeeder logSearchConfig) {
    this.logSearchConfig = logSearchConfig;
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
  
  public void trimStrValue(Map<String, Object> jsonObj) {
    if (jsonObj != null) {
      for (Entry<String, Object> entry : jsonObj.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if (value != null && value instanceof String) {
          String valueStr = value.toString().trim();
          jsonObj.put(key, valueStr);
        }
      }
    }
  }
}
