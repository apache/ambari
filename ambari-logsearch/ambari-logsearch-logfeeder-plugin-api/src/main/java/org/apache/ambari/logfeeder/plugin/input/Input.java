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
package org.apache.ambari.logfeeder.plugin.input;

import org.apache.ambari.logfeeder.plugin.common.ConfigItem;
import org.apache.ambari.logfeeder.plugin.common.MetricData;
import org.apache.ambari.logfeeder.plugin.filter.Filter;
import org.apache.ambari.logfeeder.plugin.input.cache.LRUCache;
import org.apache.ambari.logfeeder.plugin.manager.InputManager;
import org.apache.ambari.logfeeder.plugin.manager.OutputManager;
import org.apache.ambari.logfeeder.plugin.common.LogFeederProperties;
import org.apache.ambari.logfeeder.plugin.output.Output;
import org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder;
import org.apache.ambari.logsearch.config.api.model.inputconfig.Conditions;
import org.apache.ambari.logsearch.config.api.model.inputconfig.Fields;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Input<PROP_TYPE extends LogFeederProperties, INPUT_MARKER extends InputMarker> extends ConfigItem<PROP_TYPE> implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(Input.class);

  private InputDescriptor inputDescriptor;
  private PROP_TYPE logFeederProperties;
  private LogSearchConfigLogFeeder logSearchConfig;
  private InputManager inputManager;
  private OutputManager outputManager;
  private final List<Output> outputList = new ArrayList<>();
  private Filter<PROP_TYPE> firstFilter;
  private boolean isClosed;
  private String type;
  private boolean useEventMD5 = false;
  private boolean genEventMD5 = true;
  private Thread thread;
  private LRUCache cache;
  private String cacheKeyField;
  private boolean initDefaultFields;
  protected MetricData readBytesMetric = new MetricData(getReadBytesMetricName(), false);

  public void loadConfigs(InputDescriptor inputDescriptor, PROP_TYPE logFeederProperties,
                          InputManager inputManager, OutputManager outputManager) {
    this.inputDescriptor = inputDescriptor;
    this.logFeederProperties = logFeederProperties;
    this.inputManager = inputManager;
    this.outputManager = outputManager;
  }

  public void setLogSearchConfig(LogSearchConfigLogFeeder logSearchConfig) {
    this.logSearchConfig = logSearchConfig;
  }

  public LogSearchConfigLogFeeder getLogSearchConfig() {
    return logSearchConfig;
  }

  public abstract boolean monitor();

  public abstract INPUT_MARKER getInputMarker();

  public abstract boolean isReady();

  public abstract void setReady(boolean isReady);

  public abstract void checkIn(INPUT_MARKER inputMarker);

  public abstract void lastCheckIn();

  public abstract String getReadBytesMetricName();

  public PROP_TYPE getLogFeederProperties() {
    return logFeederProperties;
  }

  public InputDescriptor getInputDescriptor() {
    return inputDescriptor;
  }

  public InputManager getInputManager() {
    return inputManager;
  }

  public OutputManager getOutputManager() {
    return outputManager;
  }

  public void setOutputManager(OutputManager outputManager) {
    this.outputManager = outputManager;
  }

  public void setInputManager(InputManager inputManager) {
    this.inputManager = inputManager;
  }

  public void addOutput(Output output) {
    outputList.add(output);
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

  public boolean isFilterRequired(FilterDescriptor filterDescriptor) {
    Conditions conditions = filterDescriptor.getConditions();
    Fields fields = conditions.getFields();
    return fields.getType().contains(inputDescriptor.getType());
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

  @Override
  public boolean isEnabled() {
    return inputDescriptor.isEnabled() != null ? inputDescriptor.isEnabled() : true;
  }

  @Override
  public void init(PROP_TYPE logFeederProperties) throws Exception {
    if (firstFilter != null) {
      firstFilter.init(logFeederProperties);
    }
  }

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
  public abstract void start() throws Exception;

  public void outputLine(String line, INPUT_MARKER marker) {
    statMetric.value++;
    readBytesMetric.value += (line.length());

    if (firstFilter != null) {
      try {
        firstFilter.apply(line, marker);
      } catch (Exception e) {
        LOG.error("Error during filter apply: {}", e);
      }
    } else {
      // TODO: For now, let's make filter mandatory, so that no one accidently forgets to write filter
      // outputManager.write(line, this);
    }
  }

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

  public void flush() {
    if (firstFilter != null) {
      firstFilter.flush();
    }
  }

  public void loadConfig(InputDescriptor inputDescriptor) {
    this.inputDescriptor = inputDescriptor;
  }

  public void setClosed(boolean isClosed) {
    this.isClosed = isClosed;
  }

  public boolean isClosed() {
    return isClosed;
  }

  public String getNameForThread() {
    return this.getClass().getSimpleName();
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isUseEventMD5() {
    return useEventMD5;
  }

  public boolean isGenEventMD5() {
    return genEventMD5;
  }

  public Filter getFirstFilter() {
    return this.firstFilter;
  }

  public Thread getThread() {
    return thread;
  }

  public void setThread(Thread thread) {
    this.thread = thread;
  }

  public void setUseEventMD5(boolean useEventMD5) {
    this.useEventMD5 = useEventMD5;
  }

  public void setGenEventMD5(boolean genEventMD5) {
    this.genEventMD5 = genEventMD5;
  }

  public LRUCache getCache() {
    return this.cache;
  }

  public String getCacheKeyField() {
    return this.cacheKeyField;
  }

  public void setCache(LRUCache cache) {
    this.cache = cache;
  }

  public void setCacheKeyField(String cacheKeyField) {
    this.cacheKeyField = cacheKeyField;
  }

  public List<? extends Output> getOutputList() {
    return outputList;
  }

  public void addMetricsContainers(List<MetricData> metricsList) {
    super.logStat();
    logStatForMetric(readBytesMetric, "Stat: Bytes Read");

    if (firstFilter != null) {
      firstFilter.logStat();
    }
  }

  public void logStat() {
    super.logStat();
    logStatForMetric(readBytesMetric, "Stat: Bytes Read");

    if (firstFilter != null) {
      firstFilter.logStat();
    }
  }

  public void initCache(boolean cacheEnabled, String cacheKeyField, int cacheSize,
                        boolean cacheLastDedupEnabled, String cacheDedupInterval, String fileName) {
    boolean enabled = getInputDescriptor().isCacheEnabled() != null
      ? getInputDescriptor().isCacheEnabled()
      : cacheEnabled;
    if (enabled) {
      String keyField = getInputDescriptor().getCacheKeyField() != null
        ? getInputDescriptor().getCacheKeyField()
        : cacheKeyField;

      setCacheKeyField(keyField);

      int size = getInputDescriptor().getCacheSize() != null
        ? getInputDescriptor().getCacheSize()
        : cacheSize;

      boolean lastDedupEnabled = getInputDescriptor().getCacheLastDedupEnabled() != null
        ? getInputDescriptor().getCacheLastDedupEnabled()
        : cacheLastDedupEnabled;

      long dedupInterval = getInputDescriptor().getCacheDedupInterval() != null
        ? getInputDescriptor().getCacheDedupInterval()
        : Long.parseLong(cacheDedupInterval);

      setCache(new LRUCache(size, fileName, dedupInterval, lastDedupEnabled));
    }
  }

  @Override
  public String toString() {
    return getShortDescription();
  }

  public void setFirstFilter(Filter<PROP_TYPE> firstFilter) {
    this.firstFilter = firstFilter;
  }

  public boolean isInitDefaultFields() {
    return initDefaultFields;
  }

  public void setInitDefaultFields(boolean initDefaultFields) {
    this.initDefaultFields = initDefaultFields;
  }
}
