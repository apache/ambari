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
package org.apache.ambari.logsearch.configurer;

import org.apache.ambari.logsearch.conf.LogSearchConfigApiConfig;
import org.apache.ambari.logsearch.conf.global.SolrLogLevelFilterManagerState;
import org.apache.ambari.logsearch.config.solr.LogLevelFilterManagerSolr;
import org.apache.ambari.logsearch.dao.EventHistorySolrDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class SolrLogLevelFilterConfigurer implements Configurer {
  private static final Logger logger = LoggerFactory.getLogger(SolrLogLevelFilterConfigurer.class);

  private static final int RETRY_INTERVAL_SECONDS = 10;

  private final EventHistorySolrDao eventHistorySolrDao;
  private final SolrLogLevelFilterManagerState solrLogLevelFilterManagerState;
  private final LogSearchConfigApiConfig logSearchConfigApiConfig;

  private LogLevelFilterManagerSolr logLevelFilterManagerSolr;

  @Inject
  public SolrLogLevelFilterConfigurer(final LogSearchConfigApiConfig logSearchConfigApiConfig,
                                      final SolrLogLevelFilterManagerState solrLogLevelFilterManagerState,
                                      final EventHistorySolrDao eventHistorySolrDao) {
    this.logSearchConfigApiConfig = logSearchConfigApiConfig;
    this.solrLogLevelFilterManagerState = solrLogLevelFilterManagerState;
    this.eventHistorySolrDao = eventHistorySolrDao;
  }

  @PostConstruct
  @Override
  public void start() {
    Thread setupThread = new Thread("setup_solr_loglevel_filter_manager") {
      @Override
      public void run() {
        logger.info("Start initializing log level filter manager ...");
        if (logSearchConfigApiConfig.isSolrFilterStorage()) {
          while (true) {
            try {
              if (eventHistorySolrDao.getSolrCollectionState().isSolrCollectionReady()) {
                setLogLevelFilterManagerSolr(new LogLevelFilterManagerSolr(eventHistorySolrDao.getSolrClient()));
                solrLogLevelFilterManagerState.setLogLevelFilterManagerIsReady(true);
                logger.info("Log level filter manager successfully initialized.");
                break;
              }
            } catch (Exception ex) {
              logger.warn("Could not initialize log level Solr filter manager, going to sleep for " + RETRY_INTERVAL_SECONDS + " seconds ", ex);
            }
            try {
              Thread.sleep(RETRY_INTERVAL_SECONDS * 1000);
            } catch (Exception e) {/* ignore */}
          }
        } else {
          logger.info("Solr is not used as a log level filter storage.");
        }
      }
    };
    setupThread.setDaemon(true);
    setupThread.start();
  }

  public LogLevelFilterManagerSolr getLogLevelFilterManagerSolr() {
    return logLevelFilterManagerSolr;
  }

  public void setLogLevelFilterManagerSolr(final LogLevelFilterManagerSolr logLevelFilterManagerSolr) {
    this.logLevelFilterManagerSolr = logLevelFilterManagerSolr;
  }
}
