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

package org.apache.ambari.logfeeder.logconfig;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class LogConfigHandler extends Thread {
  private static final Logger LOG = Logger.getLogger(LogConfigHandler.class);
  
  private static final int DEFAULT_SOLR_CONFIG_INTERVAL = 5;
  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
  private static final String TIMEZONE = "GMT";
  
  static {
    TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE));
  }
  
  private static ThreadLocal<DateFormat> formatter = new ThreadLocal<DateFormat>() {
    protected DateFormat initialValue() {
      SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
      dateFormat.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
      return dateFormat;
    }
  };
  
  private static LogFeederFilterWrapper logFeederFilterWrapper;

  private static boolean running = false;

  public static void handleConfig() {
    boolean filterEnable = LogFeederUtil.getBooleanProperty("logfeeder.log.filter.enable", false);
    if (!filterEnable) {
      LOG.info("Logfeeder filter Scheduler is disabled.");
      return;
    }
    if (!running) {
      new LogConfigHandler().start();
      running = true;
      LOG.info("Logfeeder Filter Thread started!");
    } else {
      LOG.warn("Logfeeder Filter Thread is already running.");
    }
  }
  
  private LogConfigHandler() {
    setName(getClass().getSimpleName());
    setDaemon(true);
  }

  @Override
  public void run() {
    String zkConnectString = LogFeederUtil.getStringProperty("logfeeder.solr.zk_connect_string");
    String solrUrl = LogFeederUtil.getStringProperty("logfeeder.solr.url");
    if (StringUtils.isBlank(zkConnectString) && StringUtils.isBlank(solrUrl)) {
      LOG.warn("Neither Solr ZK Connect String nor solr Url for UserConfig/History is set." +
          "Won't look for level configuration from Solr.");
      return;
    }
    
    int solrConfigInterval = LogFeederUtil.getIntProperty("logfeeder.solr.config.interval", DEFAULT_SOLR_CONFIG_INTERVAL);
    do {
      LOG.debug("Updating config from solr after every " + solrConfigInterval + " sec.");
      fetchConfig();
      try {
        Thread.sleep(1000 * solrConfigInterval);
      } catch (InterruptedException e) {
        LOG.error(e.getLocalizedMessage(), e.getCause());
      }
    } while (true);
  }

  private synchronized void fetchConfig() {
    LogConfigFetcher fetcher = LogConfigFetcher.getInstance();
    if (fetcher != null) {
      Map<String, Object> configDocMap = fetcher.getConfigDoc();
      String configJson = (String) configDocMap.get(LogFeederConstants.VALUES);
      if (configJson != null) {
        logFeederFilterWrapper = LogFeederUtil.getGson().fromJson(configJson, LogFeederFilterWrapper.class);
      }
    }
  }

  public static boolean isFilterAvailable() {
    return logFeederFilterWrapper != null;
  }

  public static List<String> getAllowedLevels(String hostName, LogFeederFilter componentFilter) {
    String componentName = componentFilter.getLabel();
    List<String> hosts = componentFilter.getHosts();
    List<String> defaultLevels = componentFilter.getDefaultLevels();
    List<String> overrideLevels = componentFilter.getOverrideLevels();
    String expiryTime = componentFilter.getExpiryTime();
    
    // check is user override or not
    if (StringUtils.isNotEmpty(expiryTime) || CollectionUtils.isNotEmpty(overrideLevels) || CollectionUtils.isNotEmpty(hosts)) {
      if (CollectionUtils.isEmpty(hosts)) { // hosts list is empty or null consider it apply on all hosts
        hosts.add(LogFeederConstants.ALL);
      }
      
      if (LogFeederUtil.isListContains(hosts, hostName, false)) {
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

  private static boolean isFilterExpired(LogFeederFilter logfeederFilter) {
    if (logfeederFilter == null)
      return false;
    
    Date filterEndDate = parseFilterExpireDate(logfeederFilter);
    if (filterEndDate == null) {
      return false;
    }
    
    Date currentDate = new Date();
    if (!currentDate.before(filterEndDate)) {
      LOG.debug("Filter for  Component :" + logfeederFilter.getLabel() + " and Hosts : [" +
          StringUtils.join(logfeederFilter.getHosts(), ',') + "] is expired because of filter endTime : " +
          formatter.get().format(filterEndDate) + " is older than currentTime :" + formatter.get().format(currentDate));
      return true;
    } else {
      return false;
    }
  }

  private static Date parseFilterExpireDate(LogFeederFilter vLogfeederFilter) {
    String expiryTime = vLogfeederFilter.getExpiryTime();
    if (StringUtils.isNotEmpty(expiryTime)) {
      try {
        return formatter.get().parse(expiryTime);
      } catch (ParseException e) {
        LOG.error("Filter have invalid ExpiryTime : " + expiryTime + " for component :" + vLogfeederFilter.getLabel()
          + " and hosts : [" + StringUtils.join(vLogfeederFilter.getHosts(), ',') + "]");
      }
    }
    return null;
  }
  
  public static LogFeederFilter findComponentFilter(String componentName) {
    if (logFeederFilterWrapper != null) {
      HashMap<String, LogFeederFilter> filter = logFeederFilterWrapper.getFilter();
      if (filter != null) {
        LogFeederFilter componentFilter = filter.get(componentName);
        if (componentFilter != null) {
          return componentFilter;
        }
      }
    }
    LOG.trace("Filter is not there for component :" + componentName);
    return null;
  }
}
