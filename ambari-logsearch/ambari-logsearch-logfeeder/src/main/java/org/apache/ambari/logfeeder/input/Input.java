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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logfeeder.input.cache.LRUCache;
import org.apache.ambari.logfeeder.common.ConfigBlock;
import org.apache.ambari.logfeeder.common.LogfeederException;
import org.apache.ambari.logfeeder.filter.Filter;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.apache.ambari.logfeeder.output.Output;
import org.apache.ambari.logfeeder.output.OutputManager;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.log4j.Logger;

public abstract class Input extends ConfigBlock implements Runnable {
  private static final Logger LOG = Logger.getLogger(Input.class);

  private static final boolean DEFAULT_TAIL = true;
  private static final boolean DEFAULT_USE_EVENT_MD5 = false;
  private static final boolean DEFAULT_GEN_EVENT_MD5 = true;
  private static final boolean DEFAULT_CACHE_ENABLED = false;
  private static final boolean DEFAULT_CACHE_DEDUP_LAST = false;
  private static final int DEFAULT_CACHE_SIZE = 100;
  private static final long DEFAULT_CACHE_DEDUP_INTERVAL = 1000;
  private static final String DEFAULT_CACHE_KEY_FIELD = "log_message";

  private static final String CACHE_ENABLED = "cache_enabled";
  private static final String CACHE_KEY_FIELD = "cache_key_field";
  private static final String CACHE_LAST_DEDUP_ENABLED = "cache_last_dedup_enabled";
  private static final String CACHE_SIZE = "cache_size";
  private static final String CACHE_DEDUP_INTERVAL = "cache_dedup_interval";

  protected InputManager inputManager;
  protected OutputManager outputManager;
  private List<Output> outputList = new ArrayList<Output>();

  private Thread thread;
  private String type;
  protected String filePath;
  private Filter firstFilter;
  private boolean isClosed;

  protected boolean tail;
  private boolean useEventMD5;
  private boolean genEventMD5;

  private LRUCache cache;
  private String cacheKeyField;

  protected MetricData readBytesMetric = new MetricData(getReadBytesMetricName(), false);
  protected String getReadBytesMetricName() {
    return null;
  }
  
  @Override
  public void loadConfig(Map<String, Object> map) {
    super.loadConfig(map);
    String typeValue = getStringValue("type");
    if (typeValue != null) {
      // Explicitly add type and value to field list
      contextFields.put("type", typeValue);
      @SuppressWarnings("unchecked")
      Map<String, Object> addFields = (Map<String, Object>) map.get("add_fields");
      if (addFields == null) {
        addFields = new HashMap<String, Object>();
        map.put("add_fields", addFields);
      }
      addFields.put("type", typeValue);
    }
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

  public void addOutput(Output output) {
    outputList.add(output);
  }

  @Override
  public void init() throws Exception {
    super.init();
    initCache();
    tail = getBooleanValue("tail", DEFAULT_TAIL);
    useEventMD5 = getBooleanValue("use_event_md5_as_id", DEFAULT_USE_EVENT_MD5);
    genEventMD5 = getBooleanValue("gen_event_md5", DEFAULT_GEN_EVENT_MD5);

    if (firstFilter != null) {
      firstFilter.init();
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

  protected void outputLine(String line, InputMarker marker) {
    statMetric.value++;
    readBytesMetric.value += (line.length());

    if (firstFilter != null) {
      try {
        firstFilter.apply(line, marker);
      } catch (LogfeederException e) {
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
      } else {
        outputManager.close();
      }
    } catch (Throwable t) {
      // Ignore
    }
    isClosed = true;
  }

  private void initCache() {
    boolean cacheEnabled = getConfigValue(CACHE_ENABLED) != null
      ? getBooleanValue(CACHE_ENABLED, DEFAULT_CACHE_ENABLED)
      : LogFeederUtil.getBooleanProperty("logfeeder.cache.enabled", DEFAULT_CACHE_ENABLED);
    if (cacheEnabled) {
      String cacheKeyField = getConfigValue(CACHE_KEY_FIELD) != null
        ? getStringValue(CACHE_KEY_FIELD)
        : LogFeederUtil.getStringProperty("logfeeder.cache.key.field", DEFAULT_CACHE_KEY_FIELD);

      setCacheKeyField(getStringValue(cacheKeyField));

      boolean cacheLastDedupEnabled = getConfigValue(CACHE_LAST_DEDUP_ENABLED) != null
        ? getBooleanValue(CACHE_LAST_DEDUP_ENABLED, DEFAULT_CACHE_DEDUP_LAST)
        : LogFeederUtil.getBooleanProperty("logfeeder.cache.last.dedup.enabled", DEFAULT_CACHE_DEDUP_LAST);

      int cacheSize = getConfigValue(CACHE_SIZE) != null
        ? getIntValue(CACHE_SIZE, DEFAULT_CACHE_SIZE)
        : LogFeederUtil.getIntProperty("logfeeder.cache.size", DEFAULT_CACHE_SIZE);

      long cacheDedupInterval = getConfigValue(CACHE_DEDUP_INTERVAL) != null
        ? getLongValue(CACHE_DEDUP_INTERVAL, DEFAULT_CACHE_DEDUP_INTERVAL)
        : Long.parseLong(LogFeederUtil.getStringProperty("logfeeder.cache.dedup.interval", String.valueOf(DEFAULT_CACHE_DEDUP_INTERVAL)));

      setCache(new LRUCache(cacheSize, filePath, cacheDedupInterval, cacheLastDedupEnabled));
    }
  }

  public boolean isTail() {
    return tail;
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
  public String toString() {
    return getShortDescription();
  }
}
