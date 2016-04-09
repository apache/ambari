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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logfeeder.ConfigBlock;
import org.apache.ambari.logfeeder.LogFeederUtil;
import org.apache.ambari.logfeeder.MetricCount;
import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

public abstract class Output extends ConfigBlock {
  static private Logger logger = Logger.getLogger(Output.class);

  String destination = null;

  Type jsonType = new TypeToken<Map<String, String>>() {
  }.getType();

  public MetricCount writeBytesMetric = new MetricCount();

  @Override
  public String getShortDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getNameForThread() {
    if (destination != null) {
      return destination;
    }
    return super.getNameForThread();
  }

  public void write(String block, InputMarker inputMarker) throws Exception {
    // No-op. Please implement in sub classes
  }

  /**
   * @param jsonObj
   * @param input
   * @throws Exception
   */
  public void write(Map<String, Object> jsonObj, InputMarker inputMarker)
    throws Exception {
    write(LogFeederUtil.getGson().toJson(jsonObj), inputMarker);
  }

  boolean isClosed = false;

  /**
   * Extend this method to clean up
   */
  public void close() {
    logger.info("Calling base close()." + getShortDescription());
    isClosed = true;
  }

  /**
   * This is called on shutdown. All output should extend it.
   *
   * @return
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

  @Override
  public void addMetricsContainers(List<MetricCount> metricsList) {
    super.addMetricsContainers(metricsList);
    metricsList.add(writeBytesMetric);
  }

  @Override
  public synchronized void logStat() {
    super.logStat();

    //Printing stat for writeBytesMetric
    logStatForMetric(writeBytesMetric, "Stat: Bytes Written");

  }

}
