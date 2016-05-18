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
package org.apache.ambari.logsearch.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class PropertiesUtil extends PropertyPlaceholderConfigurer {
  private static Map<String, String> propertiesMap;

  private PropertiesUtil() {

  }

  @Override
  protected void processProperties(
    ConfigurableListableBeanFactory beanFactory, Properties props)
    throws BeansException {
    super.processProperties(beanFactory, props);

    propertiesMap = new HashMap<String, String>();

    // First add the system properties
    Set<Object> keySet = System.getProperties().keySet();
    for (Object key : keySet) {
      String keyStr = key.toString();
      propertiesMap.put(keyStr, System.getProperties()
        .getProperty(keyStr).trim());
    }

    // add our properties now
    keySet = props.keySet();
    for (Object key : keySet) {
      String keyStr = key.toString();
      propertiesMap.put(keyStr, props.getProperty(keyStr).trim());
    }
  }

  public static String getProperty(String key, String defaultValue) {
    if (key == null) {
      return null;
    }
    String rtrnVal = propertiesMap.get(key);
    if (rtrnVal == null) {
      rtrnVal = defaultValue;
    }
    return rtrnVal;
  }

  public static String getProperty(String key) {
    if (key == null) {
      return null;
    }
    return propertiesMap.get(key);
  }

  public static String[] getPropertyStringList(String key) {
    if (key == null) {
      return null;
    }
    String value = propertiesMap.get(key);
    if (value == null || value.trim().equals("")) {
      return new String[0];
    } else {
      String[] splitValues = value.split(",");
      String[] returnValues = new String[splitValues.length];
      for (int i = 0; i < splitValues.length; i++) {
        returnValues[i] = splitValues[i].trim();
      }
      return returnValues;
    }
  }

  public static Integer getIntProperty(String key, int defaultValue) {
    if (key == null) {
      return null;
    }
    String rtrnVal = propertiesMap.get(key);
    if (rtrnVal == null) {
      return defaultValue;
    }
    return Integer.valueOf(rtrnVal);
  }

  public static Integer getIntProperty(String key) {
    if (key == null) {
      return null;
    }
    String rtrnVal = propertiesMap.get(key);
    if (rtrnVal == null) {
      return null;
    }
    return Integer.valueOf(rtrnVal);
  }

  public static Long getLongProperty(String key, long defaultValue) {
    if (key == null) {
      return null;
    }
    String rtrnVal = propertiesMap.get(key);
    if (rtrnVal == null) {
      return defaultValue;
    }
    return Long.valueOf(rtrnVal);
  }

  public static Long getLongProperty(String key) {
    if (key == null) {
      return null;
    }
    String rtrnVal = propertiesMap.get(key);
    if (rtrnVal == null) {
      return null;
    }
    return Long.valueOf(rtrnVal);
  }

  public static boolean getBooleanProperty(String key, boolean defaultValue) {
    if (key == null) {
      return defaultValue;
    }
    String value = getProperty(key);
    if (value == null) {
      return defaultValue;
    }
    return Boolean.parseBoolean(value);
  }
}
