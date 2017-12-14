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
import java.util.List;
import java.util.Map;

import org.apache.ambari.logfeeder.conf.LogEntryCacheConfig;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.cache.LRUCache;
import org.apache.ambari.logfeeder.common.ConfigItem;
import org.apache.ambari.logfeeder.common.LogFeederException;
import org.apache.ambari.logfeeder.filter.Filter;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.apache.ambari.logfeeder.output.Output;
import org.apache.ambari.logfeeder.output.OutputManager;
import org.apache.ambari.logsearch.config.api.model.inputconfig.Conditions;
import org.apache.ambari.logsearch.config.api.model.inputconfig.Fields;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;
import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Priority;

public abstract class Input extends ConfigItem implements Runnable {
  
  private static final boolean DEFAULT_TAIL = true;
  private static final boolean DEFAULT_USE_EVENT_MD5 = false;
  private static final boolean DEFAULT_GEN_EVENT_MD5 = true;

  protected InputDescriptor inputDescriptor;
  
  protected InputManager inputManager;
  protected OutputManager outputManager;
  private List<Output> outputList = new ArrayList<>();

  private Thread thread;
  private String type;
  protected String filePath;
  private Filter firstFilter;
  protected boolean isClosed;

  protected boolean tail;
  private boolean useEventMD5;
  private boolean genEventMD5;

  private LRUCache cache;
  private String cacheKeyField;

  protected MetricData readBytesMetric = new MetricData(getReadBytesMetricName(), false);
  protected String getReadBytesMetricName() {
    return null;
  }
  
  public void loadConfig(InputDescriptor inputDescriptor) {
    this.inputDescriptor = inputDescriptor;
  }

  public InputDescriptor getInputDescriptor() {
    return inputDescriptor;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setInputManager(InputManager inputManager) {
    this.inputManager = inputManager;
  }

  public void setOutputManager(OutputManager outputManager) {
    this.outputManager = outputManager;
  }

  public boolean isFilterRequired(FilterDescriptor filterDescriptor) {
    Conditions conditions = filterDescriptor.getConditions();
    Fields fields = conditions.getFields();
    return fields.getType().contains(inputDescriptor.getType());
  }

  public void addFilter(Filter filter) {
    if (firstFilter == null) {
      firstFilter = filter;
    } else {
      Filter f = firstFilter;
      while (f.getNextFilter() != null) {
        f = f.getNextFilter();
      }
      f.setNextFilter(filter);
    }
  }

  @SuppressWarnings("unchecked")
  public boolean isOutputRequired(Output output) {
    Map<String, Object> conditions = (Map<String, Object>) output.getConfigs().get("conditions");
    if (conditions == null) {
      return false;
    }
    
    Map<String, Object> fields = (Map<String, Object>) conditions.get("fields");
    if (fields == null) {
      return false;
    }
    
    List<String> types = (List<String>) fields.get("rowtype");
    return types.contains(inputDescriptor.getRowtype());
  }

  public void addOutput(Output output) {
    outputList.add(output);
  }

  @Override
  public void init(LogFeederProps logFeederProps) throws Exception {
    super.init(logFeederProps);
    initCache(logFeederProps.getLogEntryCacheConfig());
    tail = BooleanUtils.toBooleanDefaultIfNull(inputDescriptor.isTail(), DEFAULT_TAIL);
    useEventMD5 = BooleanUtils.toBooleanDefaultIfNull(inputDescriptor.isUseEventMd5AsId(), DEFAULT_USE_EVENT_MD5);
    genEventMD5 = BooleanUtils.toBooleanDefaultIfNull(inputDescriptor.isGenEventMd5(), DEFAULT_GEN_EVENT_MD5);

    if (firstFilter != null) {
      firstFilter.init(logFeederProps);
    }

  }

  boolean monitor() {
    if (isReady()) {
      LOG.info("Starting thread. " + getShortDescription());
      thread = new Thread(this, getNameForThread());
      thread.start();
      return true;
    } else {
      return false;
    }
  }

  public abstract boolean isReady();

  @Override
  public void run() {
    try {
      LOG.info("Started to monitor. " + getShortDescription());
      start();
    } catch (Exception e) {
      LOG.error("Error writing to output.", e);
    }
    LOG.info("Exiting thread. " + getShortDescription());
  }

  /**
   * This method will be called from the thread spawned for the output. This
   * method should only exit after all data are read from the source or the
   * process is exiting
   */
  abstract void start() throws Exception;

  public void outputLine(String line, InputMarker marker) {
    statMetric.value++;
    readBytesMetric.value += (line.length());

    if (firstFilter != null) {
      try {
        firstFilter.apply(line, marker);
      } catch (LogFeederException e) {
        LOG.error(e.getLocalizedMessage(), e);
      }
    } else {
      // TODO: For now, let's make filter mandatory, so that no one accidently forgets to write filter
      // outputManager.write(line, this);
    }
  }

  protected void flush() {
    if (firstFilter != null) {
      firstFilter.flush();
    }
  }

  @Override
  public void setDrain(boolean drain) {
    LOG.info("Request to drain. " + getShortDescription());
    super.setDrain(drain);
    try {
      thread.interrupt();
    } catch (Throwable t) {
      // ignore
    }
  }

  public void addMetricsContainers(List<MetricData> metricsList) {
    super.addMetricsContainers(metricsList);
    if (firstFilter != null) {
      firstFilter.addMetricsContainers(metricsList);
    }
    metricsList.add(readBytesMetric);
  }

  @Override
  public void logStat() {
    super.logStat();
    logStatForMetric(readBytesMetric, "Stat: Bytes Read");

    if (firstFilter != null) {
      firstFilter.logStat();
    }
  }

  public abstract void checkIn(InputMarker inputMarker);

  public abstract void lastCheckIn();

  public void close() {
    LOG.info("Close called. " + getShortDescription());

    try {
      if (firstFilter != null) {
        firstFilter.close();
      }
    } catch (Throwable t) {
      // Ignore
    }
  }

  private void initCache(LogEntryCacheConfig cacheConfig) {
    boolean cacheEnabled = inputDescriptor.isCacheEnabled() != null
      ? inputDescriptor.isCacheEnabled()
      : cacheConfig.isCacheEnabled();
    if (cacheEnabled) {
      String cacheKeyField = inputDescriptor.getCacheKeyField() != null
        ? inputDescriptor.getCacheKeyField()
        : cacheConfig.getCacheKeyField();

      setCacheKeyField(cacheKeyField);

      int cacheSize = inputDescriptor.getCacheSize() != null
        ? inputDescriptor.getCacheSize()
        : cacheConfig.getCacheSize();

      boolean cacheLastDedupEnabled = inputDescriptor.getCacheLastDedupEnabled() != null
        ? inputDescriptor.getCacheLastDedupEnabled()
        : cacheConfig.isCacheLastDedupEnabled();

      long cacheDedupInterval = inputDescriptor.getCacheDedupInterval() != null
        ? inputDescriptor.getCacheDedupInterval()
        : Long.parseLong(cacheConfig.getCacheDedupInterval());

      setCache(new LRUCache(cacheSize, filePath, cacheDedupInterval, cacheLastDedupEnabled));
    }
  }

  public boolean isUseEventMD5() {
    return useEventMD5;
  }

  public boolean isGenEventMD5() {
    return genEventMD5;
  }

  public Filter getFirstFilter() {
    return firstFilter;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public void setClosed(boolean isClosed) {
    this.isClosed = isClosed;
  }

  public boolean isClosed() {
    return isClosed;
  }

  public List<Output> getOutputList() {
    return outputList;
  }
  
  public Thread getThread(){
    return thread;
  }

  public LRUCache getCache() {
    return cache;
  }

  public void setCache(LRUCache cache) {
    this.cache = cache;
  }

  public String getCacheKeyField() {
    return cacheKeyField;
  }

  public void setCacheKeyField(String cacheKeyField) {
    this.cacheKeyField = cacheKeyField;
  }

  @Override
  public boolean isEnabled() {
    return BooleanUtils.isNotFalse(inputDescriptor.isEnabled());
  }

  @Override
  public String getNameForThread() {
    if (filePath != null) {
      try {
        return (type + "=" + (new File(filePath)).getName());
      } catch (Throwable ex) {
        LOG.warn("Couldn't get basename for filePath=" + filePath, ex);
      }
    }
    return super.getNameForThread() + ":" + type;
  }

  @Override
  public boolean logConfigs(Priority level) {
    if (!super.logConfigs(level)) {
      return false;
    }
    LOG.log(level, "Printing Input=" + getShortDescription());
    LOG.log(level, "description=" + inputDescriptor.getPath());
    return true;
  }

  @Override
  public String toString() {
    return getShortDescription();
  }
}