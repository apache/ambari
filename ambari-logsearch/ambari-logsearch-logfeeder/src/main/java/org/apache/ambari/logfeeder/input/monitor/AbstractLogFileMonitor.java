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

import org.apache.ambari.logfeeder.input.InputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractLogFileMonitor implements Runnable {

  private Logger LOG = LoggerFactory.getLogger(AbstractLogFileMonitor.class);

  private final InputFile inputFile;
  private final int waitInterval;
  private final int detachTime;

  AbstractLogFileMonitor(InputFile inputFile, int waitInterval, int detachTime) {
    this.inputFile = inputFile;
    this.waitInterval = waitInterval;
    this.detachTime = detachTime;
  }

  public InputFile getInputFile() {
    return inputFile;
  }

  public int getDetachTime() {
    return detachTime;
  }

  @Override
  public void run() {
    LOG.info(getStartLog());

    while (!Thread.currentThread().isInterrupted()) {
      try {
        Thread.sleep(1000 * waitInterval);
        monitorAndUpdate();
      } catch (Exception e) {
        LOG.error("Monitor thread interrupted.", e);
      }
    }
  }

  protected abstract String getStartLog();

  protected abstract void monitorAndUpdate() throws Exception;
}
