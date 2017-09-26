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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ambari.logsearch.conf.LogSearchConfigMapHolder;
import org.apache.ambari.logsearch.conf.global.LogSearchConfigState;
import org.apache.ambari.logsearch.config.api.LogSearchConfigFactory;
import org.apache.ambari.logsearch.config.api.LogSearchConfigServer;
import org.apache.ambari.logsearch.config.zookeeper.LogSearchConfigServerZK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class LogSearchConfigConfigurer implements Configurer {
  private static final Logger logger = LoggerFactory.getLogger(LogSearchConfigConfigurer.class);
  
  private static final int RETRY_INTERVAL_SECONDS = 10;
  
  private LogSearchConfigServer logSearchConfig;
  public LogSearchConfigServer getConfig() {
    return logSearchConfig;
  }
  
  @Inject
  private LogSearchConfigState logSearchConfigState;

  @Inject
  private LogSearchConfigMapHolder logSearchConfigMapHolder;

  @PostConstruct
  @Override
  public void start() {
    Thread setupThread = new Thread("setup_logsearch_config") {
      @Override
      public void run() {
        logger.info("Started thread to set up log search config");
        while (true) {
          try {
            logSearchConfig = LogSearchConfigFactory.createLogSearchConfigServer(logSearchConfigMapHolder.getLogsearchProperties(),
                LogSearchConfigServerZK.class);
            logSearchConfigState.setLogSearchConfigAvailable(true);
            break;
          } catch (Exception e) {
            logger.warn("Could not initialize Log Search config, going to sleep for " + RETRY_INTERVAL_SECONDS + " seconds ", e);
            try { Thread.sleep(RETRY_INTERVAL_SECONDS * 1000); } catch (Exception e2) {/* ignore */}
          }
        }
      }
    };
    setupThread.setDaemon(true);
    setupThread.start();
  }

}
