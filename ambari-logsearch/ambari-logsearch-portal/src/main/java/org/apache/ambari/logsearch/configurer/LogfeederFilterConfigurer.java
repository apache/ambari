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

import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.dao.UserConfigSolrDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogfeederFilterConfigurer implements SolrConfigurer {

  private static final Logger LOG = LoggerFactory.getLogger(LogfeederFilterConfigurer.class);

  private static final int SETUP_RETRY_SECOND = 10;

  private final UserConfigSolrDao userConfigSolrDao;

  public LogfeederFilterConfigurer(final UserConfigSolrDao userConfigSolrDao) {
    this.userConfigSolrDao = userConfigSolrDao;
  }

  @Override
  public void start() {
    final SolrPropsConfig solrPropsConfig = userConfigSolrDao.getSolrPropsConfig();
    final SolrCollectionState state = userConfigSolrDao.getSolrCollectionState();
    Thread setupFiltersThread = new Thread("logfeeder_filter_setup") {
      @Override
      public void run() {
        LOG.info("logfeeder_filter_setup thread started (to upload logfeeder config)");
        while (true) {
          int retryCount = 0;
          try {
            retryCount++;
            Thread.sleep(SETUP_RETRY_SECOND * 1000);
            if (state.isSolrCollectionReady()) {
              LOG.info("Tries to initialize logfeeder filters in '{}' collection", solrPropsConfig.getCollection());
              userConfigSolrDao.getUserFilter();
              break;
            }
          } catch (Exception e) {
            LOG.error("Not able to save logfeeder filter while initialization, retryCount=" + retryCount, e);
          }
        }
      }
    };
    setupFiltersThread.setDaemon(true);
    setupFiltersThread.start();
  }
}
