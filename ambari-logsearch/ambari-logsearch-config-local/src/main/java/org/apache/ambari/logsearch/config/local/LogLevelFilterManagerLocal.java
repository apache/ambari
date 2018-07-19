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
import org.apache.ambari.logsearch.config.api.LogLevelFilterManager;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

/**
 * Local implementation of Log Level Filter manager - keep the data in [config-dir]/filters folder in [service_name]-filter.json files
 */
public class LogLevelFilterManagerLocal implements LogLevelFilterManager {

  private static final Logger LOG = LoggerFactory.getLogger(LogLevelFilterManagerLocal.class);

  private final String configDir;
  private final Gson gson;

  private final FilenameFilter filterConfigFilenameFilter = (dir, name) -> name.endsWith("-filter.json");

  public LogLevelFilterManagerLocal(String configDir, Gson gson) {
    this.configDir = configDir;
    this.gson = gson;
  }

  @Override
  public void createLogLevelFilter(String clusterName, String logId, LogLevelFilter filter) throws Exception {
    Path filterDirs = Paths.get(configDir, "filters");
    if (!filterDirs.toFile().exists()) {
      Files.createDirectory(filterDirs);
    }
    String logLevelFilterJson = gson.toJson(filter);
    Path filePath = Paths.get(filterDirs.toAbsolutePath().toString(), String.format("%s-filter.json", logId.toLowerCase()));
    byte[] data = logLevelFilterJson.getBytes(StandardCharsets.UTF_8);
    Files.write(filePath, data);
  }

  @Override
  public void setLogLevelFilters(String clusterName, LogLevelFilterMap filters) throws Exception {
    for (Map.Entry<String, LogLevelFilter> e : filters.getFilter().entrySet()) {
      Path filterDirs = Paths.get(configDir, "filters");
      String logLevelFilterJson = gson.toJson(e.getValue());
      Path filePath = Paths.get(filterDirs.toAbsolutePath().toString(), String.format("%s-filter.json", e.getKey()));
      if (filePath.toFile().exists()) {
        String currentLogLevelFilterJson = new String(Files.readAllBytes(filePath));
        if (!logLevelFilterJson.equals(currentLogLevelFilterJson)) {
          byte[] data = logLevelFilterJson.getBytes(StandardCharsets.UTF_8);
          Files.write(filePath, data);
          LOG.info("Set log level filter for the log " + e.getKey() + " for cluster " + clusterName);
        }
      }
    }
  }

  @Override
  public LogLevelFilterMap getLogLevelFilters(String clusterName) {
    TreeMap<String, LogLevelFilter> filters = new TreeMap<>();
    File filterDirs = Paths.get(configDir, "filters").toFile();
    if (filterDirs.exists()) {
      File[] logLevelFilterFiles = filterDirs.listFiles(filterConfigFilenameFilter);
      if (logLevelFilterFiles != null) {
        for (File file : logLevelFilterFiles) {
          try {
            String serviceName = file.getName().replace("-filter.json", "").toLowerCase();
            String logLevelFilterStr = new String(Files.readAllBytes(file.toPath()));
            LogLevelFilter logLevelFilter = gson.fromJson(logLevelFilterStr, LogLevelFilter.class);
            filters.put(serviceName, logLevelFilter);
          } catch (IOException e) {
            // skip
          }
        }
      }
    }
    LogLevelFilterMap logLevelFilters = new LogLevelFilterMap();
    logLevelFilters.setFilter(filters);
    return logLevelFilters;
  }
}
