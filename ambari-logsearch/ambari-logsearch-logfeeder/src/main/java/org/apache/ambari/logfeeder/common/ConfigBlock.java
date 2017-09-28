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
import java.util.Map;

import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Priority;


public abstract class ConfigBlock extends ConfigItem {
  protected Map<String, Object> configs;
  protected Map<String, String> contextFields = new HashMap<String, String>();
  public ConfigBlock() {
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

  public boolean isFieldConditionMatch(String fieldName, String stringValue) {
    boolean allow = false;
    String fieldValue = (String) configs.get(fieldName);
    if (fieldValue != null && fieldValue.equalsIgnoreCase(stringValue)) {
      allow = true;
    } else {
      @SuppressWarnings("unchecked")
      Map<String, Object> addFields = (Map<String, Object>) configs.get("add_fields");
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
      retValue = (strValue.equalsIgnoreCase("true") || strValue.equalsIgnoreCase("yes"));
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
        LOG.error("Error parsing integer value. key=" + key + ", value=" + strValue);
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
        LOG.error("Error parsing long value. key=" + key + ", value=" + strValue);
      }
    }
    return retValue;
  }

  @Override
  public boolean isEnabled() {
    return getBooleanValue("is_enabled", true);
  }

  public Map<String, String> getContextFields() {
    return contextFields;
  }

  public boolean logConfigs(Priority level) {
    if (!super.logConfigs(level)) {
      return false;
    }
    LOG.log(level, "Printing configuration Block=" + getShortDescription());
    LOG.log(level, "configs=" + configs);
    LOG.log(level, "contextFields=" + contextFields);
    return true;
  }
}
