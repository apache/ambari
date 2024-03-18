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
package org.apache.ambari.logfeeder.input.monitor;

import org.apache.ambari.logfeeder.plugin.manager.InputManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckpointCleanupMonitor implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(CheckpointCleanupMonitor.class);

  private long waitIntervalMin;
  private InputManager inputManager;

  public CheckpointCleanupMonitor(InputManager inputManager, long waitIntervalMin) {
    this.waitIntervalMin = waitIntervalMin;
    this.inputManager = inputManager;
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        Thread.sleep(1000 * 60 * waitIntervalMin);
        inputManager.cleanCheckPointFiles();
      } catch (Exception e) {
        LOG.error("Cleanup checkpoint files thread interrupted.", e);
      }
    }
  }
}
