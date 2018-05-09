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

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watchdog that notifies a listener on a file content change.
 */
public class SingleFileWatch implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(SingleFileWatch.class);
  private final File file;
  private final Consumer<File> changeListener;
  private final Thread thread;
  private volatile boolean started = false;

  /**
   * @param file to be watched
   * @param changeListener to be notified if the file content changes
   */
  public SingleFileWatch(File file, Consumer<File> changeListener) {
    this.file = file;
    this.changeListener = changeListener;
    this.thread = new Thread(this, toString());
  }

  /**
   * Start the watch service in the background
   */
  public void start() {
    LOG.info("Starting " + this);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Stop the watch service
   */
  public void stop() {
    LOG.info("Stopping " + this);
    started = false;
    thread.interrupt();
  }

  /**
   * @return true if the WatchService is started in the background and registered on the given directory
   */
  public boolean isStarted() {
    return started;
  }

  public void run() {
    try {
      checkForFileModifications();
    } catch (IOException e) {
      LOG.error(this + " error", e);
      started = false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.info(this + " interrupted");
      started = false;
    }
  }

  /**
   * WatchService can only watch directories not individual files, we're watching the parent directory and filter events by filename
   */
  private void checkForFileModifications() throws IOException, InterruptedException {
    try (WatchService watch = FileSystems.getDefault().newWatchService()) {
      register(watch);
      while (started) {
        WatchKey key = watch.take();
        key.pollEvents().stream()
          .map(event -> (Path) event.context())
          .filter(path -> file.toPath().getFileName().equals(path.getFileName()))
          .findAny()
          .ifPresent(this::notifyListener);
        key.reset();
      }
    }
  }

  private void register(WatchService watch) throws IOException {
    Path parent = parentDirectory();
    LOG.info("Registering on {}", parent);
    parent.register(watch, ENTRY_MODIFY);
    started = true;
  }

  private void notifyListener(Path path) {
    LOG.info(path + " changed. Sending notification.");
    try {
      changeListener.accept(file);
    } catch (Exception e) {
      LOG.warn("Error while notifying " + this + " listener", e);
    }
  }

  private Path parentDirectory() {
    return file.getParentFile().toPath();
  }

  public String toString() {
    return "SingleFileWatcher:" + file.getName();
  }
}
