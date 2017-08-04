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

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.ambari.logfeeder.metrics.MetricData;
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
}
