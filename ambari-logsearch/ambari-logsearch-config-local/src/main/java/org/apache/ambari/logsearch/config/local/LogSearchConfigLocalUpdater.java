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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.ambari.logsearch.config.api.InputConfigMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.ambari.logsearch.config.json.JsonHelper;
import org.apache.ambari.logsearch.config.json.model.inputconfig.impl.InputConfigGson;
import org.apache.ambari.logsearch.config.json.model.inputconfig.impl.InputConfigImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Watch specific config folder, and check against input.config-*.json file changes (create/update/remove),
 * a change can trigger an input config monitor (which should start to monitor input files with the new or updated settings)
 */
public class LogSearchConfigLocalUpdater implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(LogSearchConfigLocalUpdater.class);

  private final Path path;
  private final WatchService watchService;
  private final InputConfigMonitor inputConfigMonitor;
  private final Map<String, String> inputFileContentsMap;
  private final JsonParser parser;
  private final JsonArray globalConfigNode;
  private final Pattern serviceNamePattern;

  public LogSearchConfigLocalUpdater(final Path path, final WatchService watchService,
                                     final InputConfigMonitor inputConfigMonitor, final Map<String, String> inputFileContentsMap,
                                     final JsonParser parser, final JsonArray globalConfigNode, final Pattern serviceNamePattern) {
    this.path = path;
    this.watchService = watchService;
    this.inputConfigMonitor = inputConfigMonitor;
    this.inputFileContentsMap = inputFileContentsMap;
    this.parser = parser;
    this.globalConfigNode = globalConfigNode;
    this.serviceNamePattern = serviceNamePattern;
  }

  @Override
  public void run() {
    final Map<WatchKey, Path> keys = new ConcurrentHashMap<>();
    try {
      register(this.path, keys, watchService);
    } catch (IOException e) {
      LOG.error("{}", e);
      throw new RuntimeException(e);
    }
    while (!Thread.interrupted()) {
      WatchKey key;
      try {
        key = watchService.poll(10, TimeUnit.SECONDS);
      } catch (InterruptedException | ClosedWatchServiceException e) {
        break;
      }
      if (key != null) {
        Path path = keys.get(key);
        for (WatchEvent<?> ev : key.pollEvents()) {
          WatchEvent<Path> event = cast(ev);
          WatchEvent.Kind<Path> kind = event.kind();
          Path name = event.context();
          Path monitoredInput = path.resolve(name);
          File file = monitoredInput.toFile();
          String absPath = monitoredInput.toAbsolutePath().toString();
          if (file.getName().startsWith("input.config-") && file.getName().endsWith(".json")) {
            Matcher m = serviceNamePattern.matcher(file.getName());
            m.find();
            String serviceName = m.group(1);
            try {
              if (kind == ENTRY_CREATE) {
                LOG.info("New input config entry found: {}", absPath);
                String inputConfig = new String(Files.readAllBytes(monitoredInput));
                JsonElement inputConfigJson = JsonHelper.mergeGlobalConfigWithInputConfig(parser, inputConfig, globalConfigNode);
                inputConfigMonitor.loadInputConfigs(serviceName, InputConfigGson.gson.fromJson(inputConfigJson, InputConfigImpl.class));
                inputFileContentsMap.put(absPath, inputConfig);
              } else if (kind == ENTRY_MODIFY) {
                LOG.info("Input config entry modified: {}", absPath);
                if (inputFileContentsMap.containsKey(absPath)) {
                  String oldContent = inputFileContentsMap.get(absPath);
                  String inputConfig = new String(Files.readAllBytes(monitoredInput));
                  if (!inputConfig.equals(oldContent)) {
                    inputConfigMonitor.removeInputs(serviceName);
                    inputFileContentsMap.remove(absPath);
                    JsonElement inputConfigJson = JsonHelper.mergeGlobalConfigWithInputConfig(parser, inputConfig, globalConfigNode);
                    inputConfigMonitor.loadInputConfigs(serviceName, InputConfigGson.gson.fromJson(inputConfigJson, InputConfigImpl.class));
                    inputFileContentsMap.put(absPath, inputConfig);
                  }
                }
              } else if (kind == ENTRY_DELETE) {
                LOG.info("Input config deleted: {}", absPath);
                if (inputFileContentsMap.containsKey(absPath)) {
                  inputConfigMonitor.removeInputs(serviceName);
                  inputFileContentsMap.remove(absPath);
                }
              }
            } catch (Exception e) {
              LOG.error("{}", e);
              break;
            }
          }
        }
        if (!key.reset()) {
          LOG.info("{} is invalid", key);
          keys.remove(key);
          if (keys.isEmpty()) {
            break;
          }
        }
      }
    }
  }

  private void register(Path dir, Map<WatchKey, Path> keys, WatchService watchService)
    throws IOException {
    WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE,
      ENTRY_MODIFY);
    keys.put(key, dir);
  }

  @SuppressWarnings("unchecked")
  private <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }
}
