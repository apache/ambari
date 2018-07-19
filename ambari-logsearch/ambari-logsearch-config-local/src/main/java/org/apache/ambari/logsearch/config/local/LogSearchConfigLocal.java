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
package org.apache.ambari.logsearch.config.local;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ambari.logsearch.config.api.LogLevelFilterManager;
import org.apache.ambari.logsearch.config.api.LogSearchConfig;

import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Abstract local config location handler - holds common operations for Log Search Server and Log Feeder local config handler
 */
public abstract class LogSearchConfigLocal implements LogSearchConfig {

  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

  protected Map<String, String> properties;
  protected final Map<String, Map<String, String>> clusterInputConfigMap = new ConcurrentHashMap<>();
  protected final Map<String, String> inputFileContentsMap = new ConcurrentHashMap<>();
  protected Gson gson;
  protected final FilenameFilter inputConfigFileFilter = (dir, name) -> name.startsWith("input.config-") && name.endsWith(".json");
  protected final Pattern serviceNamePattern = Pattern.compile("input.config-(.+).json");
  protected final ExecutorService executorService = Executors.newCachedThreadPool();
  protected LogLevelFilterManager logLevelFilterManager;

  public void init(Map<String, String> properties) throws Exception {
    this.properties = properties;
    gson = new GsonBuilder().setDateFormat(DATE_FORMAT).setPrettyPrinting().create();
  }

  @Override
  public void createInputConfig(String clusterName, String serviceName, String inputConfig) throws Exception {
    Path filePath = Paths.get(getConfigDir(), String.format("input.config-%s.json", serviceName.toLowerCase()));
    byte[] data = inputConfig.getBytes(StandardCharsets.UTF_8);
    Files.write(filePath, data);
    inputFileContentsMap.put(filePath.toAbsolutePath().toString(), inputConfig);
    if (!clusterInputConfigMap.containsKey(clusterName)) {
      clusterInputConfigMap.put(clusterName, inputFileContentsMap);
    }
  }

  @Override
  public void close() throws IOException {
  }

  public abstract String getConfigDir();

  public abstract void setConfigDir(String configDir);

  @Override
  public LogLevelFilterManager getLogLevelFilterManager() {
    return logLevelFilterManager;
  }

  @Override
  public void setLogLevelFilterManager(LogLevelFilterManager logLevelFilterManager) {
    this.logLevelFilterManager = logLevelFilterManager;
  }
}
