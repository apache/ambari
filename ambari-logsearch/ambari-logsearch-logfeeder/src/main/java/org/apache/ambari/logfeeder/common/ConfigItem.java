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

package org.apache.ambari.logfeeder.common;

import java.util.List;

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public abstract class ConfigItem {

  protected static final Logger LOG = Logger.getLogger(ConfigBlock.class);
  private boolean drain = false;
  private LogFeederProps logFeederProps;
  public MetricData statMetric = new MetricData(getStatMetricName(), false);

  public ConfigItem() {
    super();
  }

  protected String getStatMetricName() {
    return null;
  }

  /**
   * Used while logging. Keep it short and meaningful
   */
  public abstract String getShortDescription();

  /**
   * Every implementor need to give name to the thread they create
   */
  public String getNameForThread() {
    return this.getClass().getSimpleName();
  }

  public void addMetricsContainers(List<MetricData> metricsList) {
    metricsList.add(statMetric);
  }

  /**
   * This method needs to be overwritten by deriving classes.
   */
  public void init(LogFeederProps logFeederProps) throws Exception {
    this.logFeederProps = logFeederProps;
  }

  public abstract boolean isEnabled();

  public void incrementStat(int count) {
    statMetric.value += count;
  }

  public void logStatForMetric(MetricData metric, String prefixStr) {
    LogFeederUtil.logStatForMetric(metric, prefixStr, ", key=" + getShortDescription());
  }

  public synchronized void logStat() {
    logStatForMetric(statMetric, "Stat");
  }

  public boolean logConfigs(Priority level) {
    if (level.toInt() == Priority.INFO_INT && !LOG.isInfoEnabled()) {
      return false;
    }
    if (level.toInt() == Priority.DEBUG_INT && !LOG.isDebugEnabled()) {
      return false;
    }
    return true;
  }

  public boolean isDrain() {
    return drain;
  }

  public void setDrain(boolean drain) {
    this.drain = drain;
  }

  public LogFeederProps getLogFeederProps() {
    return logFeederProps;
  }
}