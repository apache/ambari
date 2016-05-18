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

package org.apache.ambari.logfeeder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.ambari.logfeeder.filter.Filter;
import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.logconfig.LogFeederConstants;
import org.apache.ambari.logfeeder.mapper.Mapper;
import org.apache.ambari.logfeeder.output.Output;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * This class contains utility methods used by LogFeeder
 */
public class LogFeederUtil {
  static Logger logger = Logger.getLogger(LogFeederUtil.class);

  final static int HASH_SEED = 31174077;
  public final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  public final static String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  static Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();

  static Properties props;

  private static Map<String, LogHistory> logHistoryList = new Hashtable<String, LogHistory>();
  private static int logInterval = 30000; // 30 seconds

  public static Gson getGson() {
    return gson;
  }

  /**
   * This method will read the properties from System, followed by propFile
   * and finally from the map
   *
   * @param propFile
   * @param propNVList
   * @throws Exception
   */
  static public void loadProperties(String propFile, String[] propNVList)
    throws Exception {
    logger.info("Loading properties. propFile=" + propFile);
    props = new Properties(System.getProperties());
    boolean propLoaded = false;

    // First get properties file path from environment value
    String propertiesFilePath = System.getProperty("properties");
    if (propertiesFilePath != null && !propertiesFilePath.isEmpty()) {
      File propertiesFile = new File(propertiesFilePath);
      if (propertiesFile.exists() && propertiesFile.isFile()) {
        logger.info("Properties file path set in environment. Loading properties file="
          + propertiesFilePath);
        FileInputStream fileInputStream = null;
        try {
          fileInputStream = new FileInputStream(propertiesFile);
          props.load(fileInputStream);
          propLoaded = true;
        } catch (Throwable t) {
          logger.error("Error loading properties file. properties file="
            + propertiesFile.getAbsolutePath());
        } finally {
          if (fileInputStream != null) {
            try {
              fileInputStream.close();
            } catch (Throwable t) {
              // Ignore error
            }
          }
        }
      } else {
        logger.error("Properties file path set in environment, but file not found. properties file="
          + propertiesFilePath);
      }
    }

    if (!propLoaded) {
      // Properties not yet loaded, let's try from class loader
      BufferedInputStream fileInputStream = (BufferedInputStream) LogFeeder.class
        .getClassLoader().getResourceAsStream(propFile);
      if (fileInputStream != null) {
        logger.info("Loading properties file " + propFile
          + " from classpath");
        props.load(fileInputStream);
        propLoaded = true;
      } else {
        logger.fatal("Properties file not found in classpath. properties file name= "
          + propFile);
      }
    }

    if (!propLoaded) {
      logger.fatal("Properties file is not loaded.");
      throw new Exception("Properties not loaded");
    } else {
      // Let's load properties from argument list
      updatePropertiesFromMap(propNVList);
    }
  }

  /**
   * @param nvList
   */
  private static void updatePropertiesFromMap(String[] nvList) {
    if (nvList == null) {
      return;
    }
    logger.info("Trying to load additional proeprties from argument paramters. nvList.length="
      + nvList.length);
    if (nvList != null && nvList.length > 0) {
      for (String nv : nvList) {
        logger.info("Passed nv=" + nv);
        if (nv.startsWith("-") && nv.length() > 1) {
          nv = nv.substring(1);
          logger.info("Stripped nv=" + nv);
          int i = nv.indexOf("=");
          if (nv.length() > i) {
            logger.info("Candidate nv=" + nv);
            String name = nv.substring(0, i);
            String value = nv.substring(i + 1);
            logger.info("Adding property from argument to properties. name="
              + name + ", value=" + value);
            props.put(name, value);
          }
        }
      }
    }
  }

  static public String getStringProperty(String key) {
    if (props != null) {
      return props.getProperty(key);
    }
    return null;
  }

  static public String getStringProperty(String key, String defaultValue) {
    if (props != null) {
      return props.getProperty(key, defaultValue);
    }
    return defaultValue;
  }

  static public boolean getBooleanProperty(String key, boolean defaultValue) {
    String strValue = getStringProperty(key);
    return toBoolean(strValue, defaultValue);
  }

  private static boolean toBoolean(String strValue, boolean defaultValue) {
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

  static public int getIntProperty(String key, int defaultValue) {
    String strValue = getStringProperty(key);
    int retValue = defaultValue;
    retValue = objectToInt(strValue, retValue, ", key=" + key);
    return retValue;
  }

  public static int objectToInt(Object objValue, int retValue,
                                String errMessage) {
    if (objValue == null) {
      return retValue;
    }
    String strValue = objValue.toString();
    if (!StringUtils.isEmpty(strValue)) {
      try {
        retValue = Integer.parseInt(strValue);
      } catch (Throwable t) {
        logger.error("Error parsing integer value. str=" + strValue
          + ", " + errMessage);
      }
    }
    return retValue;
  }

  static public boolean isEnabled(Map<String, Object> configs) {
    return isEnabled(configs, configs);
  }

  static public boolean isEnabled(Map<String, Object> conditionConfigs,
                                  Map<String, Object> valueConfigs) {
    boolean allow = toBoolean((String) valueConfigs.get("is_enabled"), true);
    @SuppressWarnings("unchecked")
    Map<String, Object> conditions = (Map<String, Object>) conditionConfigs
      .get("conditions");
    if (conditions != null && conditions.size() > 0) {
      allow = false;
      for (String conditionType : conditions.keySet()) {
        if (conditionType.equalsIgnoreCase("fields")) {
          @SuppressWarnings("unchecked")
          Map<String, Object> fields = (Map<String, Object>) conditions
            .get("fields");
          for (String fieldName : fields.keySet()) {
            Object values = fields.get(fieldName);
            if (values instanceof String) {
              allow = isFieldConditionMatch(valueConfigs,
                fieldName, (String) values);
            } else {
              @SuppressWarnings("unchecked")
              List<String> listValues = (List<String>) values;
              for (String stringValue : listValues) {
                allow = isFieldConditionMatch(valueConfigs,
                  fieldName, stringValue);
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
    }
    return allow;
  }

  static public boolean isFieldConditionMatch(Map<String, Object> configs,
                                              String fieldName, String stringValue) {
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

  static public void logStatForMetric(MetricCount metric, String prefixStr,
                                      String postFix) {
    long currStat = metric.count;
    long currMS = System.currentTimeMillis();
    if (currStat > metric.prevLogCount) {
      if (postFix == null) {
        postFix = "";
      }
      logger.info(prefixStr + ": total_count=" + metric.count
        + ", duration=" + (currMS - metric.prevLogMS) / 1000
        + " secs, count=" + (currStat - metric.prevLogCount)
        + postFix);
    }
    metric.prevLogCount = currStat;
    metric.prevLogMS = currMS;
  }

  static public void logCountForMetric(MetricCount metric, String prefixStr,
                                       String postFix) {
    logger.info(prefixStr + ": count=" + metric.count + postFix);
  }

  public static Map<String, Object> cloneObject(Map<String, Object> map) {
    if (map == null) {
      return null;
    }
    String jsonStr = gson.toJson(map);
    // We need to clone it, so we will create a JSON string and convert it
    // back
    Type type = new TypeToken<Map<String, Object>>() {
    }.getType();
    return gson.fromJson(jsonStr, type);
  }

  public static Map<String, Object> toJSONObject(String jsonStr) {
    if(jsonStr==null || jsonStr.trim().isEmpty()){
      return new HashMap<String, Object>();
    }
    Type type = new TypeToken<Map<String, Object>>() {
    }.getType();
    return gson.fromJson(jsonStr, type);
  }

  static public boolean logErrorMessageByInterval(String key, String message,
                                                  Throwable e, Logger callerLogger, Level level) {

    LogHistory log = logHistoryList.get(key);
    if (log == null) {
      log = new LogHistory();
      logHistoryList.put(key, log);
    }
    if ((System.currentTimeMillis() - log.lastLogTime) > logInterval) {
      log.lastLogTime = System.currentTimeMillis();
      int counter = log.counter;
      log.counter = 0;
      if (counter > 0) {
        message += ". Messages suppressed before: " + counter;
      }
      if (e == null) {
        callerLogger.log(level, message);
      } else {
        callerLogger.log(level, message, e);
      }

      return true;
    } else {
      log.counter++;
    }
    return false;

  }

  static public String subString(String str, int maxLength) {
    if (str == null || str.length() == 0) {
      return "";
    }
    maxLength = str.length() < maxLength ? str.length() : maxLength;
    return str.substring(0, maxLength);
  }

  static public long genHash(String value) {
    if (value == null) {
      value = "null";
    }
    return MurmurHash.hash64A(value.getBytes(), HASH_SEED);
  }

  static class LogHistory {
    long lastLogTime = 0;
    int counter = 0;
  }

  public static String getDate(String timeStampStr) {
    try {
      DateFormat sdf = new SimpleDateFormat(SOLR_DATE_FORMAT);
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
      Date netDate = (new Date(Long.parseLong(timeStampStr)));
      return sdf.format(netDate);
    } catch (Exception ex) {
      return null;
    }
  }

  public static File getFileFromClasspath(String filename) {
    URL fileCompleteUrl = Thread.currentThread().getContextClassLoader()
      .getResource(filename);
    logger.debug("File Complete URI :" + fileCompleteUrl);
    File file = null;
    try {
      file = new File(fileCompleteUrl.toURI());
    } catch (Exception exception) {
      logger.debug(exception.getMessage(), exception.getCause());
    }
    return file;
  }

  public static Object getClassInstance(String classFullName, AliasUtil.ALIAS_TYPE aliasType) {
    Object instance = null;
    try {
      instance = (Object) Class.forName(classFullName).getConstructor().newInstance();
    } catch (Exception exception) {
      logger.error("Unsupported class =" + classFullName, exception.getCause());
    }
    // check instance class as par aliasType
    if (instance != null) {
      boolean isValid = false;
      switch (aliasType) {
        case FILTER:
          isValid = Filter.class.isAssignableFrom(instance.getClass());
          break;
        case INPUT:
          isValid = Input.class.isAssignableFrom(instance.getClass());
          break;
        case OUTPUT:
          isValid = Output.class.isAssignableFrom(instance.getClass());
          break;
        case MAPPER:
          isValid = Mapper.class.isAssignableFrom(instance.getClass());
          break;
        default:
          // by default consider all are valid class
          isValid = true;
      }
      if (!isValid) {
        logger.error("Not a valid class :" + classFullName + " AliasType :" + aliasType.name());
      }
    }
    return instance;
  }

  /**
   * @param fileName
   * @return
   */
  public static HashMap<String, Object> readJsonFromFile(File jsonFile) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      HashMap<String, Object> jsonmap = mapper.readValue(jsonFile, new TypeReference<HashMap<String, Object>>() {
      });
      return jsonmap;
    } catch (JsonParseException e) {
      logger.error(e, e.getCause());
    } catch (JsonMappingException e) {
      logger.error(e, e.getCause());
    } catch (IOException e) {
      logger.error(e, e.getCause());
    }
    return new HashMap<String, Object>();
  }

  public static boolean isListContains(List<String> list, String str, boolean caseSensitive) {
    if (list != null) {
      for (String value : list) {
        if (value != null) {
          if (caseSensitive) {
            if (value.equals(str)) {
              return true;
            }
          } else {
            if (value.equalsIgnoreCase(str)) {
              return true;
            }
          }
          if (value.equalsIgnoreCase(LogFeederConstants.ALL)) {
            return true;
          }
        }
      }
    }
    return false;
  }

}
