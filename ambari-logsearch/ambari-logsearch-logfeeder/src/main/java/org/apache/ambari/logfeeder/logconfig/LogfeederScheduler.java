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

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.log4j.Logger;

public enum LogfeederScheduler {

  INSTANCE;

  private Logger logger = Logger.getLogger(LogfeederScheduler.class);

  private static boolean running = false;

  public synchronized void start() {
    boolean filterEnable = LogFeederUtil.getBooleanProperty("logfeeder.log.filter.enable", false);
    if (!filterEnable) {
      logger.info("Logfeeder  filter Scheduler is disabled.");
      return;
    }
    if (!running) {
      for (Thread thread : getThreadList()) {
        thread.start();
      }
      running = true;
      logger.info("Logfeeder Scheduler started!");
    } else {
      logger.warn("Logfeeder Scheduler is already running.");
    }
  }

  private List<Thread> getThreadList() {
    List<Thread> tasks = new ArrayList<Thread>();
    Thread configMonitor = new FetchConfigFromSolr(true);
    tasks.add(configMonitor);
    return tasks;
  }
}
