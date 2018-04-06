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
package org.apache.ambari.logfeeder.plugin.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ConfigItem<PROP_TYPE extends LogFeederProperties> implements Cloneable, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigItem.class);

  private final static String GSON_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  private static Gson gson = new GsonBuilder().setDateFormat(GSON_DATE_FORMAT).create();

  public static Gson getGson() {
    return gson;
  }

  private Map<String, Object> configs;
  private Map<String, String> contextFields = new HashMap<>();
  private boolean drain = false;
  public MetricData statMetric = new MetricData(getStatMetricName(), false);

  public abstract void init(PROP_TYPE logFeederProperties) throws Exception;

  /**
   * Used while logging. Keep it short and meaningful
   */
  public abstract String getShortDescription();

  public abstract String getStatMetricName();

  public abstract boolean logConfigs();

  public void loadConfig(Map<String, Object> map) {
    configs = cloneObject(map);

    Map<String, String> nvList = getNVList("add_fields");
    if (nvList != null) {
      contextFields.putAll(nvList);
    }
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getNVList(String key) {
    return (Map<String, String>) configs.get(key);
  }

  public Map<String, Object> getConfigs() {
    return configs;
  }

  public boolean isEnabled() {
    return getBooleanValue("is_enabled", true);
  }

  public void addMetricsContainers(List<MetricData> metricsList) {
    metricsList.add(statMetric);
  }

  public void incrementStat(int count) {
    statMetric.value += count;
  }

  public synchronized void logStat() {
    logStatForMetric(statMetric, "Stat");
  }

  public void logStatForMetric(MetricData metric, String prefixStr) {
    long currStat = metric.value;
    long currMS = System.currentTimeMillis();
    String postFix = ", key=" + getShortDescription();
    if (currStat > metric.prevLogValue) {
      LOG.info(prefixStr + ": total_count=" + metric.value + ", duration=" + (currMS - metric.prevLogTime) / 1000 +
        " secs, count=" + (currStat - metric.prevLogValue) + postFix);
    }
    metric.prevLogValue = currStat;
    metric.prevLogTime = currMS;
  }

  public boolean isDrain() {
    return drain;
  }

  public void setDrain(boolean drain) {
    this.drain = drain;
  }

  public String getStringValue(String property) {
    return getStringValue(property, null);
  }

  public String getStringValue(String property, String defaultValue) {
    Object strValue = configs.getOrDefault(property, defaultValue);
    if (strValue != null) {
      return strValue.toString();
    }
    return null;
  }

  public Boolean getBooleanValue(String property) {
    return getBooleanValue(property, false);
  }

  public Boolean getBooleanValue(String property, Boolean defaultValue) {
    Object booleanValue = configs.getOrDefault(property, defaultValue);
    if (booleanValue != null) {
      if (booleanValue.getClass().isAssignableFrom(Boolean.class)) {
        return (Boolean) booleanValue;
      } else {
        String strValue = booleanValue.toString();
        return strValue.equalsIgnoreCase("true") || strValue.equalsIgnoreCase("yes");
      }
    }
    return false;
  }

  public Long getLongValue(String property) {
    return getLongValue(property, null);
  }

  public Long getLongValue(String property, Long defaultValue) {
    Object longValue = configs.getOrDefault(property, defaultValue);
    if (longValue != null) {
      if (longValue.getClass().isAssignableFrom(Long.class)) {
        return (Long) longValue;
      } else {
        return Long.parseLong(longValue.toString());
      }
    }
    return null;
  }

  public Integer getIntValue(String property) {
    return getIntValue(property, null);
  }

  public Integer getIntValue(String property, Integer defaultValue) {
    Object intValue = configs.getOrDefault(property, defaultValue);
    if (intValue != null) {
      if (intValue.getClass().isAssignableFrom(Integer.class)) {
        return (Integer) intValue;
      } else {
        return Integer.parseInt(intValue.toString());
      }
    }
    return null;
  }

  private Map<String, Object> cloneObject(Map<String, Object> map) {
    if (map == null) {
      return null;
    }
    String jsonStr = gson.toJson(map);
    Type type = new TypeToken<Map<String, Object>>() {}.getType();
    return gson.fromJson(jsonStr, type);
  }

  private Object getValue(String property) {
    return configs.get(property);
  }

  private Object getValue(String property, Object defaultValue) {
    return configs.getOrDefault(property, defaultValue);
  }

}
