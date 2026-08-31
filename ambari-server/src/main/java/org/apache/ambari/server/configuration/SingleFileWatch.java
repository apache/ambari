/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.configuration;

import java.io.File;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watchdog that notifies a listener on a file content change.
 */
public class SingleFileWatch {
  private static final Logger LOG = LoggerFactory.getLogger(SingleFileWatch.class);
  private static final long WATCH_INTERVAL_MS = 1000;
  private final File file;
  private final Thread watchdog;
  private final Consumer<File> changeListener;
  private volatile boolean started = false;

  /**
   * @param file to be watched
   * @param changeListener to be notified if the file content changes
   */
  public SingleFileWatch(File file, Consumer<File> changeListener) {
    this.file = file;
    this.changeListener = changeListener;
    this.watchdog = createWatchDog();
  }

  private Thread createWatchDog() {
    Thread fileWatch = new Thread(() -> {
      long lastModified = file.lastModified();
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Thread.sleep(WATCH_INTERVAL_MS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }

        long currentLastModified = file.lastModified();
        if (currentLastModified != lastModified) {
          lastModified = currentLastModified;
          if (started) {
            notifyListener();
          }
        }
      }
    }, "FileWatchdog:" + file.getName());
    fileWatch.setDaemon(true);
    return fileWatch;
  }

  private void notifyListener() {
    LOG.info("{} changed. Sending notification.", file);
    try {
      changeListener.accept(file);
    } catch (Exception e) {
      LOG.warn("Error while notifying " + this + " listener", e);
    }
  }

  /**
   * Start the watch service in the background
   */
  public void start() {
    LOG.info("Starting " + this);
    started = true;
    watchdog.start();
  }

  /**
   * Stop the watch service
   */
  public void stop() {
    LOG.info("Stopping " + this);
    started = false;
    watchdog.interrupt();
  }

  @Override
  public String toString() {
    return "SingleFileWatcher:" + file.getName();
  }
}
