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

package org.apache.ambari.logfeeder.loglevelfilter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.util.LogFeederPropertiesUtil;
import org.apache.ambari.logsearch.config.api.LogLevelFilterMonitor;
import org.apache.ambari.logsearch.config.api.LogSearchConfig;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class LogLevelFilterHandler implements LogLevelFilterMonitor {
  private static final Logger LOG = Logger.getLogger(LogLevelFilterHandler.class);

  private static final String TIMEZONE = "GMT";
  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
  
  private static ThreadLocal<DateFormat> formatter = new ThreadLocal<DateFormat>() {
    protected DateFormat initialValue() {
      SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
      dateFormat.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
      return dateFormat;
    }
  };
  
  private static LogSearchConfig config;
  private static boolean filterEnabled;
  private static List<String> defaultLogLevels;
  private static Map<String, LogLevelFilter> filters = new HashMap<>();

  public static void init(LogSearchConfig config_) {
    config = config_;
    filterEnabled = LogFeederPropertiesUtil.isLogFilterEnabled();
    defaultLogLevels = Arrays.asList(LogFeederPropertiesUtil.getIncludeDefaultLevel().split(","));
    TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE));
  }

  @Override
  public void setLogLevelFilter(String logId, LogLevelFilter logLevelFilter) {
    synchronized (LogLevelFilterHandler.class) {
      filters.put(logId, logLevelFilter);
    }
  }

  @Override
  public void removeLogLevelFilter(String logId) {
    synchronized (LogLevelFilterHandler.class) {
      filters.remove(logId);
    }
  }

  public static boolean isAllowed(String hostName, String logId, String level) {
    if (!filterEnabled) {
      return true;
    }
    
    LogLevelFilter logFilter = findLogFilter(logId);
    List<String> allowedLevels = getAllowedLevels(hostName, logFilter);
    return allowedLevels.isEmpty() || allowedLevels.contains(level);
  }

  private static synchronized LogLevelFilter findLogFilter(String logId) {
    LogLevelFilter logFilter = filters.get(logId);
    if (logFilter != null) {
      return logFilter;
    }

    LOG.info("Filter is not present for log " + logId + ", creating default filter");
    LogLevelFilter defaultFilter = new LogLevelFilter();
    defaultFilter.setLabel(logId);
    defaultFilter.setDefaultLevels(defaultLogLevels);

    try {
      config.createLogLevelFilter(LogFeederPropertiesUtil.getClusterName(), logId, defaultFilter);
      filters.put(logId, defaultFilter);
    } catch (Exception e) {
      LOG.warn("Could not persist the default filter for log " + logId, e);
    }

    return defaultFilter;
  }

  private static List<String> getAllowedLevels(String hostName, LogLevelFilter componentFilter) {
    String componentName = componentFilter.getLabel();
    List<String> hosts = componentFilter.getHosts();
    List<String> defaultLevels = componentFilter.getDefaultLevels();
    List<String> overrideLevels = componentFilter.getOverrideLevels();
    Date expiryTime = componentFilter.getExpiryTime();

    // check is user override or not
    if (expiryTime != null || CollectionUtils.isNotEmpty(overrideLevels) || CollectionUtils.isNotEmpty(hosts)) {
      if (CollectionUtils.isEmpty(hosts)) { // hosts list is empty or null consider it apply on all hosts
        hosts.add(LogFeederConstants.ALL);
      }

      if (hosts.isEmpty() || hosts.contains(hostName)) {
        if (isFilterExpired(componentFilter)) {
          LOG.debug("Filter for component " + componentName + " and host :" + hostName + " is expired at " +
              componentFilter.getExpiryTime());
          return defaultLevels;
        } else {
          return overrideLevels;
        }
      }
    }
    return defaultLevels;
  }

  private static boolean isFilterExpired(LogLevelFilter logLevelFilter) {
    if (logLevelFilter == null)
      return false;

    Date filterEndDate = logLevelFilter.getExpiryTime();
    if (filterEndDate == null) {
      return false;
    }

    Date currentDate = new Date();
    if (!currentDate.before(filterEndDate)) {
      LOG.debug("Filter for  Component :" + logLevelFilter.getLabel() + " and Hosts : [" +
          StringUtils.join(logLevelFilter.getHosts(), ',') + "] is expired because of filter endTime : " +
          formatter.get().format(filterEndDate) + " is older than currentTime :" + formatter.get().format(currentDate));
      return true;
    } else {
      return false;
    }
  }
}
