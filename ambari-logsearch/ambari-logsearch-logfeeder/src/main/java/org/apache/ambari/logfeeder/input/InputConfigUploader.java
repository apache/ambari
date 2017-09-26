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

import org.apache.ambari.logfeeder.util.LogFeederPropertiesUtil;
import org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder;
import org.apache.log4j.Logger;

import com.google.common.io.Files;

public class InputConfigUploader extends Thread {
  protected static final Logger LOG = Logger.getLogger(InputConfigUploader.class);

  private static final long SLEEP_BETWEEN_CHECK = 2000;

  private final File configDir;
  private final FilenameFilter inputConfigFileFilter = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      return name.startsWith("input.config-") && name.endsWith(".json");
    }
  };
  private final Set<String> filesHandled = new HashSet<>();
  private final Pattern serviceNamePattern = Pattern.compile("input.config-(.+).json");
  private final LogSearchConfigLogFeeder config;
  
  public static void load(LogSearchConfigLogFeeder config) {
    new InputConfigUploader(config).start();
  }
  
  private InputConfigUploader(LogSearchConfigLogFeeder config) {
    super("Input Config Loader");
    setDaemon(true);
    
    this.configDir = new File(LogFeederPropertiesUtil.getConfigDir());
    this.config = config;
  }
  
  @Override
  public void run() {
    while (true) {
      File[] inputConfigFiles = configDir.listFiles(inputConfigFileFilter);
      for (File inputConfigFile : inputConfigFiles) {
        if (!filesHandled.contains(inputConfigFile.getAbsolutePath())) {
          try {
            Matcher m = serviceNamePattern.matcher(inputConfigFile.getName());
            m.find();
            String serviceName = m.group(1);
            String inputConfig = Files.toString(inputConfigFile, Charset.defaultCharset());
            
            if (!config.inputConfigExists(serviceName)) {
              config.createInputConfig(LogFeederPropertiesUtil.getClusterName(), serviceName, inputConfig);
            }
            filesHandled.add(inputConfigFile.getAbsolutePath());
          } catch (Exception e) {
            LOG.warn("Error handling file " + inputConfigFile.getAbsolutePath(), e);
          }
        }
      }
      
      try {
        Thread.sleep(SLEEP_BETWEEN_CHECK);
      } catch (InterruptedException e) {
        LOG.debug("Interrupted during sleep", e);
      }
    }
  }
}
