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
package org.apache.ambari.logfeeder.docker;

import org.apache.ambari.logfeeder.ContainerRegistry;
import org.apache.ambari.logfeeder.docker.command.DockerInspectContainerCommand;
import org.apache.ambari.logfeeder.docker.command.DockerListContainerCommand;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Register docker metadata from docker containers on the host (with listing containers and inspecting them)
 */
public final class DockerContainerRegistry implements ContainerRegistry<DockerMetadata> {

  private static final String LOGFEEDER_CONTAINER_REGISTRY_DOCKER_INTERVAL = "logfeeder.container.registry.docker.interval";
  private static final Logger logger = LoggerFactory.getLogger(DockerContainerRegistry.class);

  private static DockerContainerRegistry INSTANCE = null;
  private final Properties configs;
  private Map<String, Map<String, DockerMetadata>> dockerMetadataMap = new ConcurrentHashMap<>();
  private int waitIntervalMin = 5;

  private DockerContainerRegistry(Properties configs) {
    this.configs = configs;
    init(configs);
  }

  @Override
  public synchronized void register() {
    Map<String, Map<String, DockerMetadata>> actualDockerMetadataMap = renewMetadata();
    if (!actualDockerMetadataMap.isEmpty()) {
      dockerMetadataMap.putAll(actualDockerMetadataMap);
      dockerMetadataMap = dockerMetadataMap
        .entrySet()
        .stream()
        .filter(e -> actualDockerMetadataMap.keySet().contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      for (Map.Entry<String, Map<String, DockerMetadata>> entry : dockerMetadataMap.entrySet()) {
        for (Map.Entry<String, DockerMetadata> metadataEntry : entry.getValue().entrySet()) {
          logger.debug("Found container metadata: {}", entry.getValue().toString());
        }
      }
    }
  }

  private Map<String, Map<String, DockerMetadata>> renewMetadata() {
    final Map<String, Map<String, DockerMetadata>> actualDockerMetadataMap = new HashMap<>();
    final List<String> containerIds = new DockerListContainerCommand().execute(null);
    final Map<String, String> params = new HashMap<>();

    params.put("containerIds", StringUtils.join(containerIds, ","));
    List<Map<String, Object>> containerDataList = new DockerInspectContainerCommand().execute(params);

    for (Map<String, Object> containerDataMap : containerDataList) {
      String id = containerDataMap.get("Id").toString();
      String name = containerDataMap.get("Name").toString();
      String logPath = containerDataMap.get("LogPath").toString();
      Map<String, Object> dockerConfigMap = (HashMap<String, Object>) containerDataMap.get("Config");
      String hostname = dockerConfigMap.get("Hostname").toString();
      Map<String, String> labels = (Map<String, String>) dockerConfigMap.get("Labels");
      Map<String, Object> stateMap = (HashMap<String, Object>) containerDataMap.get("State");
      String componentType = labels.get("logfeeder.log.type");
      boolean running = (Boolean) stateMap.get("Running");
      long timestamp = running ? convertDateStrToLong((String)stateMap.get("StartedAt")) : convertDateStrToLong((String)stateMap.get("FinishedAt"));

      if (componentType != null) {
        if (actualDockerMetadataMap.containsKey(componentType)) {
          Map<String, DockerMetadata> componentMetadataMap = actualDockerMetadataMap.get(componentType);
          componentMetadataMap.put(id, new DockerMetadata(id, name, hostname, componentType, logPath, running, timestamp));
          actualDockerMetadataMap.put(componentType, componentMetadataMap);
        } else {
          Map<String, DockerMetadata> componentMetadataMap = new HashMap<>();
          componentMetadataMap.put(id, new DockerMetadata(id, name, hostname, componentType, logPath, running, timestamp));
          actualDockerMetadataMap.put(componentType, componentMetadataMap);
        }
      } else {
        logger.debug("Ignoring docker metadata from registry as container (id: {}, name: {}) as it has no 'logfeeder.log.type' label", id, name);
      }
    }

    return actualDockerMetadataMap;
  }

  @Override
  public synchronized Map<String, Map<String, DockerMetadata>> getContainerMetadataMap() {
    return dockerMetadataMap;
  }

  public void init(Properties configs) {
    // init docker related data
    String waitStr = configs.getProperty(LOGFEEDER_CONTAINER_REGISTRY_DOCKER_INTERVAL, "5");
    setWaitIntervalMin(Integer.parseInt(waitStr));
    // TODO: add docker authentication settings through this
  }

  public static synchronized DockerContainerRegistry getInstance(Properties dockerConfig) {
    if (INSTANCE == null) {
      return new DockerContainerRegistry(dockerConfig);
    } else {
      return INSTANCE;
    }
  }

  public int getWaitIntervalMin() {
    return waitIntervalMin;
  }

  public void setWaitIntervalMin(int waitIntervalMin) {
    this.waitIntervalMin = waitIntervalMin;
  }

  private long convertDateStrToLong(String timestampStr) {
    LocalDateTime localDateTime = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);
    return localDateTime.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();
  }
}
