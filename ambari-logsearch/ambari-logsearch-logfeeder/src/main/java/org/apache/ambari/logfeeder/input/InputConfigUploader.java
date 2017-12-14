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

package org.apache.ambari.logfeeder.input;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.logfeeder.common.ConfigHandler;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.loglevelfilter.LogLevelFilterHandler;
import org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class InputConfigUploader extends Thread {

  protected static final Logger LOG = LoggerFactory.getLogger(InputConfigUploader.class);

  private static final long SLEEP_BETWEEN_CHECK = 2000;

  private File configDir;
  private final FilenameFilter inputConfigFileFilter = (dir, name) -> name.startsWith("input.config-") && name.endsWith(".json");
  private final Set<String> filesHandled = new HashSet<>();
  private final Pattern serviceNamePattern = Pattern.compile("input.config-(.+).json");

  @Inject
  private LogSearchConfigLogFeeder config;

  @Inject
  private LogFeederProps logFeederProps;

  @Inject
  private LogLevelFilterHandler logLevelFilterHandler;

  @Inject
  private ConfigHandler configHandler;

  public InputConfigUploader() {
    super("Input Config Loader");
    setDaemon(true);
  }

  @PostConstruct
  public void init() throws Exception {
    this.configDir = new File(logFeederProps.getConfDir());
    this.start();
    config.monitorInputConfigChanges(configHandler, logLevelFilterHandler, logFeederProps.getClusterName());
  }
  
  @Override
  public void run() {
    while (true) {
      File[] inputConfigFiles = configDir.listFiles(inputConfigFileFilter);
      if (inputConfigFiles != null) {
        for (File inputConfigFile : inputConfigFiles) {
          if (!filesHandled.contains(inputConfigFile.getAbsolutePath())) {
            try {
              Matcher m = serviceNamePattern.matcher(inputConfigFile.getName());
              m.find();
              String serviceName = m.group(1);
              String inputConfig = Files.toString(inputConfigFile, Charset.defaultCharset());
              if (!config.inputConfigExists(serviceName)) {
                config.createInputConfig(logFeederProps.getClusterName(), serviceName, inputConfig);
              }
              filesHandled.add(inputConfigFile.getAbsolutePath());
            } catch (Exception e) {
              LOG.warn("Error handling file " + inputConfigFile.getAbsolutePath(), e);
            }
          }
        }
      } else {
        LOG.warn("Cannot find input config files in config dir ({})", logFeederProps.getConfDir());
      }
      
      try {
        Thread.sleep(SLEEP_BETWEEN_CHECK);
      } catch (InterruptedException e) {
        LOG.debug("Interrupted during sleep", e);
      }
    }
  }
}
