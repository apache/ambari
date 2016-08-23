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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logfeeder.util.SolrUtil;
import org.apache.ambari.logfeeder.view.VLogfeederFilter;
import org.apache.ambari.logfeeder.view.VLogfeederFilterWrapper;
import org.apache.log4j.Logger;

public class FetchConfigFromSolr extends Thread {
  private static Logger logger = Logger.getLogger(FetchConfigFromSolr.class);
  private static VLogfeederFilterWrapper logfeederFilterWrapper = null;
  private static int solrConfigInterval = 5;// 5 sec;
  private static long delay;
  private static String endTimeDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS";//2016-04-05T04:30:00.000Z
  private static String sysTimeZone = "GMT";

  FetchConfigFromSolr(boolean isDaemon) {
    this.setName(this.getClass().getSimpleName());
    this.setDaemon(isDaemon);
  }

  @Override
  public void run() {
    String zkConnectString = LogFeederUtil.getStringProperty("logfeeder.solr.zk_connect_string");
    String solrUrl = LogFeederUtil.getStringProperty("logfeeder.solr.url");
    if ((zkConnectString == null || zkConnectString.trim().length() == 0 )
        && (solrUrl == null || solrUrl.trim().length() == 0)) {
      logger.warn("Neither Solr ZK Connect String nor solr Uril for UserConfig/History is set." +
          "Won't look for level configuration from Solr.");
      return;
    }
    solrConfigInterval = LogFeederUtil.getIntProperty("logfeeder.solr.config.interval", solrConfigInterval);
    delay = 1000 * solrConfigInterval;
    do {
      logger.debug("Updating config from solr after every " + solrConfigInterval + " sec.");
      pullConfigFromSolr();
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        logger.error(e.getLocalizedMessage(), e.getCause());
      }
    } while (true);
  }

  private synchronized void pullConfigFromSolr() {
    SolrUtil solrUtil = SolrUtil.getInstance();
    if(solrUtil!=null){
      HashMap<String, Object> configDocMap = solrUtil.getConfigDoc();
      if (configDocMap != null) {
        String configJson = (String) configDocMap.get(LogFeederConstants.VALUES);
        if (configJson != null) {
          logfeederFilterWrapper = LogFeederUtil.getGson().fromJson(configJson, VLogfeederFilterWrapper.class);
        }
      }
    }
  }

  private static boolean isFilterExpired(VLogfeederFilter logfeederFilter) {
    boolean isFilterExpired = false;// default is false
    if (logfeederFilter != null) {
      Date filterEndDate = parseFilterExpireDate(logfeederFilter);
      if (filterEndDate != null) {
        Date currentDate = getCurrentDate();
        if (currentDate.compareTo(filterEndDate) >= 0) {
          logger.debug("Filter for  Component :" + logfeederFilter.getLabel() + " and Hosts :"
            + listToStr(logfeederFilter.getHosts()) + "Filter is expired because of filter endTime : "
            + dateToStr(filterEndDate) + " is older than currentTime :" + dateToStr(currentDate));
          isFilterExpired = true;
        }
      }
    }
    return isFilterExpired;
  }

  private static String dateToStr(Date date) {
    if (date == null) {
      return "";
    }
    SimpleDateFormat formatter = new SimpleDateFormat(endTimeDateFormat);
    TimeZone timeZone = TimeZone.getTimeZone(sysTimeZone);
    formatter.setTimeZone(timeZone);
    return formatter.format(date);
  }

  private static Date parseFilterExpireDate(VLogfeederFilter vLogfeederFilter) {
    String expiryTime = vLogfeederFilter.getExpiryTime();
    if (expiryTime != null && !expiryTime.isEmpty()) {
      SimpleDateFormat formatter = new SimpleDateFormat(endTimeDateFormat);
      TimeZone timeZone = TimeZone.getTimeZone(sysTimeZone);
      formatter.setTimeZone(timeZone);
      try {
        return formatter.parse(expiryTime);
      } catch (ParseException e) {
        logger.error("Filter have invalid ExpiryTime : " + expiryTime + " for component :" + vLogfeederFilter.getLabel()
          + " and hosts :" + listToStr(vLogfeederFilter.getHosts()));
      }
    }
    return null;
  }

  public static List<String> getAllowedLevels(String hostName, VLogfeederFilter componentFilter) {
    String componentName = componentFilter.getLabel();
    List<String> hosts = componentFilter.getHosts();
    List<String> defaultLevels = componentFilter.getDefaultLevels();
    List<String> overrideLevels = componentFilter.getOverrideLevels();
    String expiryTime=componentFilter.getExpiryTime();
    //check is user override or not
    if ((expiryTime != null && !expiryTime.isEmpty())
        || (overrideLevels != null && !overrideLevels.isEmpty())
        || (hosts != null && !hosts.isEmpty())) {
      if (hosts == null || hosts.isEmpty()) {
        // hosts list is empty or null consider it apply on all hosts
        hosts.add(LogFeederConstants.ALL);
      }
      if (LogFeederUtil.isListContains(hosts, hostName, false)) {
        if (isFilterExpired(componentFilter)) {
          logger.debug("Filter for component " + componentName + " and host :"
              + hostName + " is expired at " + componentFilter.getExpiryTime());
          return defaultLevels;
        } else {
          return overrideLevels;
        }
      }
    }
    return defaultLevels;
  }

  public static boolean isFilterAvailable() {
    return logfeederFilterWrapper != null;
  }
  
  public static VLogfeederFilter findComponentFilter(String componentName) {
    if (logfeederFilterWrapper != null) {
      HashMap<String, VLogfeederFilter> filter = logfeederFilterWrapper.getFilter();
      if (filter != null) {
        VLogfeederFilter componentFilter = filter.get(componentName);
        if (componentFilter != null) {
          return componentFilter;
        }
      }
    }
    logger.trace("Filter is not there for component :" + componentName);
    return null;
  }


  public static Date getCurrentDate() {
    TimeZone.setDefault(TimeZone.getTimeZone(sysTimeZone));
    Date date = new Date();
    return date;
  }

  public static String listToStr(List<String> strList) {
    StringBuilder out = new StringBuilder("[");
    if (strList != null) {
      int counter = 0;
      for (Object o : strList) {
        if (counter > 0) {
          out.append(",");
        }
        out.append(o.toString());
        counter++;
      }
    }
    out.append("]");
    return out.toString();
  }
}
