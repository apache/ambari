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
package org.apache.ambari.logfeeder.output;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ambari.logfeeder.LogFeeder;
import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.filter.Filter;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.output.spool.LogSpooler;
import org.apache.ambari.logfeeder.output.spool.LogSpoolerContext;
import org.apache.ambari.logfeeder.output.spool.RolloverCondition;
import org.apache.ambari.logfeeder.output.spool.RolloverHandler;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logfeeder.util.S3Util;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;


/**
 * Write log file into s3 bucket.
 *
 * This class supports two modes of upload:
 * <ul>
 * <li>A one time upload of files matching a pattern</li>
 * <li>A batch mode, asynchronous, periodic upload of files</li>
 * </ul>
 */
public class OutputS3File extends Output implements RolloverCondition, RolloverHandler {
  private static final Logger LOG = Logger.getLogger(OutputS3File.class);

  public static final String INPUT_ATTRIBUTE_TYPE = "type";
  public static final String GLOBAL_CONFIG_S3_PATH_SUFFIX = "global.config.json";

  private LogSpooler logSpooler;
  private S3OutputConfiguration s3OutputConfiguration;
  private S3Uploader s3Uploader;

  @Override
  public void init() throws Exception {
    super.init();
    s3OutputConfiguration = S3OutputConfiguration.fromConfigBlock(this);
  }

  private static boolean uploadedGlobalConfig = false;

  /**
   * Copy local log files and corresponding config to S3 bucket one time.
   * @param inputFile The file to be copied
   * @param inputMarker Contains information about the configuration to be uploaded.
   */
  @Override
  public void copyFile(File inputFile, InputMarker inputMarker) {
    String type = inputMarker.input.getStringValue(INPUT_ATTRIBUTE_TYPE);
    S3Uploader s3Uploader = new S3Uploader(s3OutputConfiguration, false, type);
    String resolvedPath = s3Uploader.uploadFile(inputFile, inputMarker.input.getStringValue(INPUT_ATTRIBUTE_TYPE));

    uploadConfig(inputMarker, type, s3OutputConfiguration, resolvedPath);
  }

  private void uploadConfig(InputMarker inputMarker, String type, S3OutputConfiguration s3OutputConfiguration,
      String resolvedPath) {

    ArrayList<Map<String, Object>> filters = new ArrayList<>();
    addFilters(filters, inputMarker.input.getFirstFilter());
    Map<String, Object> inputConfig = new HashMap<>();
    inputConfig.putAll(inputMarker.input.getConfigs());
    String s3CompletePath = LogFeederConstants.S3_PATH_START_WITH + s3OutputConfiguration.getS3BucketName() +
        LogFeederConstants.S3_PATH_SEPARATOR + resolvedPath;
    inputConfig.put("path", s3CompletePath);

    ArrayList<Map<String, Object>> inputConfigList = new ArrayList<>();
    inputConfigList.add(inputConfig);
    // set source s3_file
    // remove global config from filter config
    removeGlobalConfig(inputConfigList);
    removeGlobalConfig(filters);
    // write config into s3 file
    Map<String, Object> config = new HashMap<>();
    config.put("filter", filters);
    config.put("input", inputConfigList);
    writeConfigToS3(config, getComponentConfigFileName(type), s3OutputConfiguration);
    // write global config
    writeGlobalConfig(s3OutputConfiguration);
  }

  private void addFilters(ArrayList<Map<String, Object>> filters, Filter filter) {
    if (filter != null) {
      Map<String, Object> filterConfig = new HashMap<String, Object>();
      filterConfig.putAll(filter.getConfigs());
      filters.add(filterConfig);
      if (filter.getNextFilter() != null) {
        addFilters(filters, filter.getNextFilter());
      }
    }
  }

  private void writeConfigToS3(Map<String, Object> configToWrite, String s3KeySuffix, S3OutputConfiguration s3OutputConfiguration) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String configJson = gson.toJson(configToWrite);

    String s3ResolvedKey = new S3LogPathResolver().getResolvedPath(getStringValue("s3_config_dir"), s3KeySuffix,
        s3OutputConfiguration.getCluster());

    S3Util.writeIntoS3File(configJson, s3OutputConfiguration.getS3BucketName(), s3ResolvedKey,
        s3OutputConfiguration.getS3AccessKey(), s3OutputConfiguration.getS3SecretKey());
  }

  private String getComponentConfigFileName(String componentName) {
    return "input.config-" + componentName + ".json";
  }


  private Map<String, Object> getGlobalConfig() {
    Map<String, Object> globalConfig = LogFeeder.globalConfigs;
    if (globalConfig == null) {
      globalConfig = new HashMap<>();
    }
    return globalConfig;
  }

  private void removeGlobalConfig(List<Map<String, Object>> configList) {
    Map<String, Object> globalConfig = getGlobalConfig();
    if (configList != null && globalConfig != null) {
      for (Entry<String, Object> globalConfigEntry : globalConfig.entrySet()) {
        if (globalConfigEntry != null) {
          String globalKey = globalConfigEntry.getKey();
          if (globalKey != null && !globalKey.trim().isEmpty()) {
            for (Map<String, Object> config : configList) {
              if (config != null) {
                config.remove(globalKey);
              }
            }
          }
        }
      }
    }
  }

  /**
   * write global config in s3 file Invoke only once
   */
  @SuppressWarnings("unchecked")
  private synchronized void writeGlobalConfig(S3OutputConfiguration s3OutputConfiguration) {
    if (!uploadedGlobalConfig) {
      Map<String, Object> globalConfig = LogFeederUtil.cloneObject(getGlobalConfig());
      //updating global config before write to s3
      globalConfig.put("source", "s3_file");
      globalConfig.put("copy_file", false);
      globalConfig.put("process_file", true);
      globalConfig.put("tail", false);
      Map<String, Object> addFields = (Map<String, Object>) globalConfig.get("add_fields");
      if (addFields == null) {
        addFields = new HashMap<>();
      }
      addFields.put("ip", LogFeederUtil.ipAddress);
      addFields.put("host", LogFeederUtil.hostName);
      // add bundle id same as cluster if its not there
      String bundle_id = (String) addFields.get("bundle_id");
      if (bundle_id == null || bundle_id.isEmpty()) {
        String cluster = (String) addFields.get("cluster");
        if (cluster != null && !cluster.isEmpty()) {
          addFields.put("bundle_id", bundle_id);
        }
      }
      globalConfig.put("add_fields", addFields);
      Map<String, Object> config = new HashMap<String, Object>();
      config.put("global", globalConfig);
      writeConfigToS3(config, GLOBAL_CONFIG_S3_PATH_SUFFIX, s3OutputConfiguration);
      uploadedGlobalConfig = true;
    }
  }

  /**
   * Write a log line to local file, to upload to S3 bucket asynchronously.
   *
   * This method uses a {@link LogSpooler} to spool the log lines to a local file.

   * @param block The log event to upload
   * @param inputMarker Contains information about the log file feeding the lines.
   * @throws Exception
   */
  @Override
  public void write(String block, InputMarker inputMarker) throws Exception {
    if (logSpooler == null) {
      logSpooler = createSpooler(inputMarker.input.getFilePath());
      s3Uploader = createUploader(inputMarker.input.getStringValue(INPUT_ATTRIBUTE_TYPE));
    }
    logSpooler.add(block);
  }

  @VisibleForTesting
  protected S3Uploader createUploader(String logType) {
    S3Uploader uploader = new S3Uploader(s3OutputConfiguration, true, logType);
    uploader.startUploaderThread();
    return uploader;
  }

  @VisibleForTesting
  protected LogSpooler createSpooler(String filePath) {
    String spoolDirectory = LogFeederUtil.getLogfeederTempDir() + "/s3/service";
    LOG.info(String.format("Creating spooler with spoolDirectory=%s, filePath=%s", spoolDirectory, filePath));
    return new LogSpooler(spoolDirectory, new File(filePath).getName()+"-", this, this,
        s3OutputConfiguration.getRolloverTimeThresholdSecs());
  }

  /**
   * Check whether the locally spooled file should be rolled over, based on file size.
   *
   * @param currentSpoolerContext {@link LogSpoolerContext} that holds state about the file being checked
   *                                                       for rollover.
   * @return true if sufficient size has been reached based on {@link S3OutputConfiguration#getRolloverSizeThresholdBytes()},
   *              false otherwise
   */
  @Override
  public boolean shouldRollover(LogSpoolerContext currentSpoolerContext) {
    File spoolFile = currentSpoolerContext.getActiveSpoolFile();
    long currentSize = spoolFile.length();
    boolean result = (currentSize >= s3OutputConfiguration.getRolloverSizeThresholdBytes());
    if (result) {
      LOG.info(String.format("Rolling over %s, current size %d, threshold size %d", spoolFile, currentSize,
          s3OutputConfiguration.getRolloverSizeThresholdBytes()));
    }
    return result;
  }

  /**
   * Stops dependent objects that consume resources.
   */
  @Override
  public void close() {
    if (s3Uploader != null) {
      s3Uploader.stopUploaderThread();
    }
    if (logSpooler != null) {
      logSpooler.close();
    }
  }

  /**
   * Adds the locally spooled file to the {@link S3Uploader} to be uploaded asynchronously.
   *
   * @param rolloverFile The file that has been rolled over.
   */
  @Override
  public void handleRollover(File rolloverFile) {
    s3Uploader.addFileForUpload(rolloverFile.getAbsolutePath());
  }
}
