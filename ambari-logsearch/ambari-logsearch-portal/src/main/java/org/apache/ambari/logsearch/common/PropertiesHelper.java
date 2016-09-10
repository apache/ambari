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
package org.apache.ambari.logsearch.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class PropertiesHelper extends PropertyPlaceholderConfigurer {
  private static final Logger logger = Logger.getLogger(PropertiesHelper.class);
  
  private static final String LOGSEARCH_PROP_FILE="logsearch.properties";
  
  private static Map<String, String> propertiesMap;

  private PropertiesHelper() {
  }
  
 static {
    propertiesMap = new HashMap<String, String>();
    Properties properties = new Properties();
    URL fileCompleteUrl = Thread.currentThread().getContextClassLoader().getResource(LOGSEARCH_PROP_FILE);
    FileInputStream fileInputStream = null;
    try {
      File file = new File(fileCompleteUrl.toURI());
      fileInputStream = new FileInputStream(file.getAbsoluteFile());
      properties.load(fileInputStream);
    } catch (IOException | URISyntaxException e) {
      logger.error("error loading prop for protocol config",e);
    } finally {
      if (fileInputStream != null) {
        try {
          fileInputStream.close();
        } catch (IOException e) {
        }
      }
    }
    for (String key : properties.stringPropertyNames()) {
      String value = properties.getProperty(key);
      propertiesMap.put(key, value);
    }
  }

  @Override
  protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) throws BeansException {
    super.processProperties(beanFactory, props);

    propertiesMap = new HashMap<String, String>();

    // First add the system properties
    Set<Object> keySet = System.getProperties().keySet();
    for (Object key : keySet) {
      String keyStr = key.toString();
      propertiesMap.put(keyStr, System.getProperties().getProperty(keyStr).trim());
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
