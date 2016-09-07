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

package org.apache.ambari.logfeeder.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ambari.logfeeder.LogFeeder;
import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * This class contains utility methods used by LogFeeder
 */
public class LogFeederUtil {
  private static final Logger LOG = Logger.getLogger(LogFeederUtil.class);

  private final static String GSON_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  private static Gson gson = new GsonBuilder().setDateFormat(GSON_DATE_FORMAT).create();
  
  public static Gson getGson() {
    return gson;
  }

  public static String hostName = null;
  public static String ipAddress = null;
  
  static{
    try {
      InetAddress ip = InetAddress.getLocalHost();
      ipAddress = ip.getHostAddress();
      String getHostName = ip.getHostName();
      String getCanonicalHostName = ip.getCanonicalHostName();
      if (!getCanonicalHostName.equalsIgnoreCase(ipAddress)) {
        LOG.info("Using getCanonicalHostName()=" + getCanonicalHostName);
        hostName = getCanonicalHostName;
      } else {
        LOG.info("Using getHostName()=" + getHostName);
        hostName = getHostName;
      }
      LOG.info("ipAddress=" + ipAddress + ", getHostName=" + getHostName + ", getCanonicalHostName=" + getCanonicalHostName +
          ", hostName=" + hostName);
    } catch (UnknownHostException e) {
      LOG.error("Error getting hostname.", e);
    }
  }
  
  private static Properties props;

  /**
   * This method will read the properties from System, followed by propFile and finally from the map
   */
  public static void loadProperties(String propFile, String[] propNVList) throws Exception {
    LOG.info("Loading properties. propFile=" + propFile);
    props = new Properties(System.getProperties());
    boolean propLoaded = false;

    // First get properties file path from environment value
    String propertiesFilePath = System.getProperty("properties");
    if (StringUtils.isNotEmpty(propertiesFilePath)) {
      File propertiesFile = new File(propertiesFilePath);
      if (propertiesFile.exists() && propertiesFile.isFile()) {
        LOG.info("Properties file path set in environment. Loading properties file=" + propertiesFilePath);
        try (FileInputStream fis = new FileInputStream(propertiesFile)) {
          props.load(fis);
          propLoaded = true;
        } catch (Throwable t) {
          LOG.error("Error loading properties file. properties file=" + propertiesFile.getAbsolutePath());
        }
      } else {
        LOG.error("Properties file path set in environment, but file not found. properties file=" + propertiesFilePath);
      }
    }

    if (!propLoaded) {
      try (BufferedInputStream bis = (BufferedInputStream) LogFeeder.class.getClassLoader().getResourceAsStream(propFile)) {
        // Properties not yet loaded, let's try from class loader
        if (bis != null) {
          LOG.info("Loading properties file " + propFile + " from classpath");
          props.load(bis);
          propLoaded = true;
        } else {
          LOG.fatal("Properties file not found in classpath. properties file name= " + propFile);
        }
      }
    }

    if (!propLoaded) {
      LOG.fatal("Properties file is not loaded.");
      throw new Exception("Properties not loaded");
    } else {
      updatePropertiesFromMap(propNVList);
    }
  }

  private static void updatePropertiesFromMap(String[] nvList) {
    if (nvList == null) {
      return;
    }
    LOG.info("Trying to load additional proeprties from argument paramters. nvList.length=" + nvList.length);
    for (String nv : nvList) {
      LOG.info("Passed nv=" + nv);
      if (nv.startsWith("-") && nv.length() > 1) {
        nv = nv.substring(1);
        LOG.info("Stripped nv=" + nv);
        int i = nv.indexOf("=");
        if (nv.length() > i) {
          LOG.info("Candidate nv=" + nv);
          String name = nv.substring(0, i);
          String value = nv.substring(i + 1);
          LOG.info("Adding property from argument to properties. name=" + name + ", value=" + value);
          props.put(name, value);
        }
      }
    }
  }

  public static String getStringProperty(String key) {
    return props == null ? null : props.getProperty(key);
  }

  public static String getStringProperty(String key, String defaultValue) {
    return props == null ? defaultValue : props.getProperty(key, defaultValue);
  }

  public static boolean getBooleanProperty(String key, boolean defaultValue) {
    String value = getStringProperty(key);
    return toBoolean(value, defaultValue);
  }

  private static boolean toBoolean(String value, boolean defaultValue) {
    if (StringUtils.isEmpty(value)) {
      return defaultValue;
    }
    
    return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
  }

  public static int getIntProperty(String key, int defaultValue) {
    String value = getStringProperty(key);
    int retValue = objectToInt(value, defaultValue, ", key=" + key);
    return retValue;
  }

  public static int objectToInt(Object objValue, int retValue, String errMessage) {
    if (objValue == null) {
      return retValue;
    }
    String strValue = objValue.toString();
    if (StringUtils.isNotEmpty(strValue)) {
      try {
        retValue = Integer.parseInt(strValue);
      } catch (Throwable t) {
        LOG.error("Error parsing integer value. str=" + strValue + ", " + errMessage);
      }
    }
    return retValue;
  }

  @SuppressWarnings("unchecked")
  public static boolean isEnabled(Map<String, Object> conditionConfigs, Map<String, Object> valueConfigs) {
    Map<String, Object> conditions = (Map<String, Object>) conditionConfigs.get("conditions");
    if (MapUtils.isEmpty(conditions)) {
      return toBoolean((String) valueConfigs.get("is_enabled"), true);
    }
    
    for (String conditionType : conditions.keySet()) {
      if (!conditionType.equalsIgnoreCase("fields")) {
        continue;
      }
      
      Map<String, Object> fields = (Map<String, Object>) conditions.get("fields");
      for (Map.Entry<String, Object> field : fields.entrySet()) {
        if (field.getValue() instanceof String) {
          if (isFieldConditionMatch(valueConfigs, field.getKey(), (String) field.getValue())) {
            return true;
          }
        } else {
          for (String stringValue : (List<String>) field.getValue()) {
            if (isFieldConditionMatch(valueConfigs, field.getKey(), stringValue)) {
              return true;
            }
          }
        }
      }
    }
    
    return false;
  }

  private static boolean isFieldConditionMatch(Map<String, Object> configs, String fieldName, String stringValue) {
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

  public static void logStatForMetric(MetricData metric, String prefixStr, String postFix) {
    long currStat = metric.value;
    long currMS = System.currentTimeMillis();
    if (currStat > metric.prevLogValue) {
      LOG.info(prefixStr + ": total_count=" + metric.value + ", duration=" + (currMS - metric.prevLogTime) / 1000 +
          " secs, count=" + (currStat - metric.prevLogValue) + postFix);
    }
    metric.prevLogValue = currStat;
    metric.prevLogTime = currMS;
  }

  public static Map<String, Object> cloneObject(Map<String, Object> map) {
    if (map == null) {
      return null;
    }
    String jsonStr = gson.toJson(map);
    Type type = new TypeToken<Map<String, Object>>() {}.getType();
    return gson.fromJson(jsonStr, type);
  }

  public static Map<String, Object> toJSONObject(String jsonStr) {
    if (StringUtils.isBlank(jsonStr)) {
      return new HashMap<String, Object>();
    }
    Type type = new TypeToken<Map<String, Object>>() {}.getType();
    return gson.fromJson(jsonStr, type);
  }

  private static class LogHistory {
    private long lastLogTime = 0;
    private int counter = 0;
  }

  private static Map<String, LogHistory> logHistoryList = new Hashtable<String, LogHistory>();

  public static boolean logErrorMessageByInterval(String key, String message, Throwable e, Logger callerLogger, Level level) {
    LogHistory log = logHistoryList.get(key);
    if (log == null) {
      log = new LogHistory();
      logHistoryList.put(key, log);
    }
    
    if ((System.currentTimeMillis() - log.lastLogTime) > 30 * 1000) {
      log.lastLogTime = System.currentTimeMillis();
      if (log.counter > 0) {
        message += ". Messages suppressed before: " + log.counter;
      }
      log.counter = 0;
      callerLogger.log(level, message, e);

      return true;
    } else {
      log.counter++;
      return false;
    }
  }

  public static boolean isListContains(List<String> list, String str, boolean caseSensitive) {
    if (list == null) {
      return false;
    }
    
    for (String value : list) {
      if (value == null) {
        continue;
      }
      
      if (caseSensitive ? value.equals(str) : value.equalsIgnoreCase(str) ||
          value.equalsIgnoreCase(LogFeederConstants.ALL)) {
        return true;
      }
    }
    return false;
  }
  
  private static String logfeederTempDir = null;
  
  public synchronized static String getLogfeederTempDir() {
    if (logfeederTempDir == null) {
      String tempDirValue = getStringProperty("logfeeder.tmp.dir", "/tmp/$username/logfeeder/");
      HashMap<String, String> contextParam = new HashMap<String, String>();
      String username = System.getProperty("user.name");
      contextParam.put("username", username);
      logfeederTempDir = PlaceholderUtil.replaceVariables(tempDirValue, contextParam);
    }
    return logfeederTempDir;
  }
}
