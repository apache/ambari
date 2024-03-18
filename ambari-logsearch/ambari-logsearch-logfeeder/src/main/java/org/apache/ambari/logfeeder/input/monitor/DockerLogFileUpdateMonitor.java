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

import org.apache.ambari.logfeeder.docker.DockerContainerRegistry;
import org.apache.ambari.logfeeder.docker.DockerMetadata;
import org.apache.ambari.logfeeder.input.InputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Periodically check docker containers metadata registry, stop monitoring container log files if those do not exist or stopped too long time ago.
 * If it finds a new container log for the specific type, it will start to monitoring it.
 * <br/>
 * Use cases:<br/>
 * - input has not monitored yet - found new container -> start monitoring it <br/>
 * - input has not monitored yet - found new stopped container -> start monitoring it <br/>
 * - input has not monitored yet - found new stopped container but log is too old -> do not monitoring it <br/>
 * - input has monitored already - container stopped - if it's stopped for too long time -> remove it from the monitoed list<br/>
 * - input has monitored already - container stopped - log is not too old -> keep in the monitored list <br/>
 * - input has monitored already - container does not exist - remove it from the monitoed list (and all other input with the same log type) <br/>
 */
public class DockerLogFileUpdateMonitor extends AbstractLogFileMonitor {

  private Logger LOG = LoggerFactory.getLogger(DockerLogFileUpdateMonitor.class);

  public DockerLogFileUpdateMonitor(InputFile inputFile, int waitInterval, int detachTime) {
    super(inputFile, waitInterval, detachTime);
  }

  @Override
  protected String getStartLog() {
    return "Start docker component type log files monitor thread for " + getInputFile().getLogType();
  }

  @Override
  protected void monitorAndUpdate() throws Exception {
    DockerContainerRegistry dockerContainerRegistry = getInputFile().getDockerContainerRegistry();
    Map<String, Map<String, DockerMetadata>> dockerMetadataMapByType = dockerContainerRegistry.getContainerMetadataMap();
    String logType = getInputFile().getLogType();
    Map<String, InputFile> copiedChildMap = new HashMap<>(getInputFile().getInputChildMap());

    if (dockerMetadataMapByType.containsKey(logType)) {
      Map<String, DockerMetadata> dockerMetadataMap = dockerMetadataMapByType.get(logType);
      for (Map.Entry<String, DockerMetadata> containerEntry : dockerMetadataMap.entrySet()) {
        String logPath = containerEntry.getValue().getLogPath();
        String containerId = containerEntry.getValue().getId();
        long timestamp = containerEntry.getValue().getTimestamp();
        boolean running = containerEntry.getValue().isRunning();
        LOG.debug("Found log path: {} (container id: {})", logPath, containerId);
        if (!copiedChildMap.containsKey(logPath)) {
          if (!running && isItTooOld(timestamp, new Date().getTime(), getDetachTime())) {
            LOG.debug("Container with id {} is stopped, won't monitor as it stopped for long time.", containerId);
          } else {
            LOG.info("Found new container (id: {}) with new log path: {}", logPath, containerId);
            getInputFile().startNewChildDockerInputFileThread(containerEntry.getValue());
          }
        } else {
          if (!running && isItTooOld(timestamp, new Date().getTime(), getDetachTime())) {
            LOG.info("Removing: {}", logPath);
            getInputFile().stopChildDockerInputFileThread(containerEntry.getKey());
          }
        }
      }
    } else {
      if (!copiedChildMap.isEmpty()) {
        LOG.info("Removing all inputs with type: {}", logType);
        for (Map.Entry<String, InputFile> inputFileEntry : copiedChildMap.entrySet()) {
          LOG.info("Removing: {}", inputFileEntry.getKey());
          getInputFile().stopChildDockerInputFileThread(inputFileEntry.getKey());
        }
      }
    }
  }

  private boolean isItTooOld(long timestamp, long actualTimestamp, long maxDiffMinutes) {
    long diff = actualTimestamp - timestamp;
    long maxDiffMins = maxDiffMinutes * 1000 * 60;
    return diff > maxDiffMins;
  }
}
