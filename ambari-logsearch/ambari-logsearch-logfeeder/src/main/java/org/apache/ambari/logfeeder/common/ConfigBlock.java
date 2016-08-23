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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logfeeder.metrics.MetricCount;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;


public abstract class ConfigBlock {
  static private Logger logger = Logger.getLogger(ConfigBlock.class);

  private boolean drain = false;

  protected Map<String, Object> configs;
  protected Map<String, String> contextFields = new HashMap<String, String>();
  public MetricCount statMetric = new MetricCount();

  /**
   *
   */
  public ConfigBlock() {
    super();
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

  /**
   * @param metricsList
   */
  public void addMetricsContainers(List<MetricCount> metricsList) {
    metricsList.add(statMetric);
  }

  /**
   * This method needs to be overwritten by deriving classes.
   */
  public void init() throws Exception {
  }

  public void loadConfig(Map<String, Object> map) {
    configs = LogFeederUtil.cloneObject(map);

    Map<String, String> nvList = getNVList("add_fields");
    if (nvList != null) {
      contextFields.putAll(nvList);
    }
  }

  public Map<String, Object> getConfigs() {
    return configs;
  }

  @SuppressWarnings("unchecked")
  public boolean isEnabled() {
    boolean isEnabled = getBooleanValue("is_enabled", true);
    if (isEnabled) {
      // Let's check for static conditions
      Map<String, Object> conditions = (Map<String, Object>) configs
        .get("conditions");
      boolean allow = true;
      if (conditions != null && conditions.size() > 0) {
        allow = false;
        for (String conditionType : conditions.keySet()) {
          if (conditionType.equalsIgnoreCase("fields")) {
            Map<String, Object> fields = (Map<String, Object>) conditions
              .get("fields");
            for (String fieldName : fields.keySet()) {
              Object values = fields.get(fieldName);
              if (values instanceof String) {
                allow = isFieldConditionMatch(fieldName,
                  (String) values);
              } else {
                List<String> listValues = (List<String>) values;
                for (String stringValue : listValues) {
                  allow = isFieldConditionMatch(fieldName,
                    stringValue);
                  if (allow) {
                    break;
                  }
                }
              }
              if (allow) {
                break;
              }
            }
          }
          if (allow) {
            break;
          }
        }
        isEnabled = allow;
      }
    }
    return isEnabled;
  }

  public boolean isFieldConditionMatch(String fieldName, String stringValue) {
    boolean allow = false;
    String fieldValue = (String) configs.get(fieldName);
    if (fieldValue != null && fieldValue.equalsIgnoreCase(stringValue)) {
      allow = true;
    } else {
      @SuppressWarnings("unchecked")
      Map<String, Object> addFields = (Map<String, Object>) configs
        .get("add_fields");
      if (addFields != null && addFields.get(fieldName) != null) {
        String addFieldValue = (String) addFields.get(fieldName);
        if (stringValue.equalsIgnoreCase(addFieldValue)) {
          allow = true;
        }
      }

    }
    return allow;
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getNVList(String key) {
    return (Map<String, String>) configs.get(key);
  }

  public String getStringValue(String key) {
    Object value = configs.get(key);
    if (value != null && value.toString().equalsIgnoreCase("none")) {
      value = null;
    }
    if (value != null) {
      return value.toString();
    }
    return null;
  }

  public String getStringValue(String key, String defaultValue) {
    Object value = configs.get(key);
    if (value != null && value.toString().equalsIgnoreCase("none")) {
      value = null;
    }

    if (value != null) {
      return value.toString();
    }
    return defaultValue;
  }

  public Object getConfigValue(String key) {
    return configs.get(key);
  }

  public boolean getBooleanValue(String key, boolean defaultValue) {
    String strValue = getStringValue(key);
    boolean retValue = defaultValue;
    if (!StringUtils.isEmpty(strValue)) {
      if (strValue.equalsIgnoreCase("true")
        || strValue.equalsIgnoreCase("yes")) {
        retValue = true;
      } else {
        retValue = false;
      }
    }
    return retValue;
  }

  public int getIntValue(String key, int defaultValue) {
    String strValue = getStringValue(key);
    int retValue = defaultValue;
    if (!StringUtils.isEmpty(strValue)) {
      try {
        retValue = Integer.parseInt(strValue);
      } catch (Throwable t) {
        logger.error("Error parsing integer value. key=" + key
          + ", value=" + strValue);
      }
    }
    return retValue;
  }
  
  public long getLongValue(String key, long defaultValue) {
    String strValue = getStringValue(key);
    Long retValue = defaultValue;
    if (!StringUtils.isEmpty(strValue)) {
      try {
        retValue = Long.parseLong(strValue);
      } catch (Throwable t) {
        logger.error("Error parsing long value. key=" + key + ", value="
            + strValue);
      }
    }
    return retValue;
  }

  public Map<String, String> getContextFields() {
    return contextFields;
  }

  public void incrementStat(int count) {
    statMetric.count += count;
  }

  public void logStatForMetric(MetricCount metric, String prefixStr) {
    LogFeederUtil.logStatForMetric(metric, prefixStr, ", key="
      + getShortDescription());
  }

  synchronized public void logStat() {
    logStatForMetric(statMetric, "Stat");
  }

  public boolean logConfgs(Priority level) {
    if (level.toInt() == Priority.INFO_INT && !logger.isInfoEnabled()) {
      return false;
    }
    if (level.toInt() == Priority.DEBUG_INT && !logger.isDebugEnabled()) {
      return false;
    }
    logger.log(level, "Printing configuration Block="
      + getShortDescription());
    logger.log(level, "configs=" + configs);
    logger.log(level, "contextFields=" + contextFields);
    return true;
  }

  public boolean isDrain() {
    return drain;
  }

  public void setDrain(boolean drain) {
    this.drain = drain;
  }
}
