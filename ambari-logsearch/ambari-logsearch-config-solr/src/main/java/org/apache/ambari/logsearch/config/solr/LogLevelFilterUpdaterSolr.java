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
package org.apache.ambari.logsearch.config.solr;

import com.google.gson.Gson;
import org.apache.ambari.logsearch.config.api.LogLevelFilterMonitor;
import org.apache.ambari.logsearch.config.api.LogLevelFilterUpdater;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodically checks log level filters in Solr, and send a notification about any change to a log level filter monitor.
 */
public class LogLevelFilterUpdaterSolr extends LogLevelFilterUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(LogLevelFilterUpdaterSolr.class);

  private final LogLevelFilterManagerSolr logLevelFilterManagerSolr;
  private final String cluster;

  public LogLevelFilterUpdaterSolr(String threadName, LogLevelFilterMonitor logLevelFilterMonitor, Integer interval,
                                   LogLevelFilterManagerSolr logLevelFilterManagerSolr, String cluster) {
    super(threadName, logLevelFilterMonitor, interval);
    this.logLevelFilterManagerSolr = logLevelFilterManagerSolr;
    this.cluster = cluster;
  }

  @Override
  protected void checkFilters(LogLevelFilterMonitor logLevelFilterMonitor) {
    try {
      LOG.debug("Start checking log level filters in Solr ...");
      LogLevelFilterMap logLevelFilterMap = logLevelFilterManagerSolr.getLogLevelFilters(cluster);
      Map<String, LogLevelFilter> filters = logLevelFilterMap.getFilter();
      Map<String, LogLevelFilter> copiedStoredFilters = new ConcurrentHashMap<>(logLevelFilterMonitor.getLogLevelFilters());
      final Gson gson = logLevelFilterManagerSolr.getGson();
      for (Map.Entry<String, LogLevelFilter> logFilterEntry : filters.entrySet()){
        if (copiedStoredFilters.containsKey(logFilterEntry.getKey())) {
          String remoteValue = gson.toJson(logFilterEntry.getValue());
          String storedValue = gson.toJson(copiedStoredFilters.get(logFilterEntry.getKey()));
          if (!storedValue.equals(remoteValue)) {
            LOG.info("Log level filter updated for {}", logFilterEntry.getKey());
            logLevelFilterMonitor.setLogLevelFilter(logFilterEntry.getKey(), logFilterEntry.getValue());
          }
        } else {
          LOG.info("New log level filter registered: {}", logFilterEntry.getKey());
          logLevelFilterMonitor.setLogLevelFilter(logFilterEntry.getKey(), logFilterEntry.getValue());
        }
      }
      for (Map.Entry<String, LogLevelFilter> storedLogFilterEntry : copiedStoredFilters.entrySet()) {
        if (!filters.containsKey(storedLogFilterEntry.getKey())) {
          LOG.info("Removing log level filter: {}", storedLogFilterEntry.getKey());
          logLevelFilterMonitor.removeLogLevelFilter(storedLogFilterEntry.getKey());
        }
      }
    } catch (Exception e) {
      LOG.error("Error during filter Solr check: {}",e);
    }
  }
}
