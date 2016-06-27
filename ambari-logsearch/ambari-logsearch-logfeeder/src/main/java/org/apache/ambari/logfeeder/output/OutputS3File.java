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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ambari.logfeeder.LogFeeder;
import org.apache.ambari.logfeeder.LogFeederUtil;
import org.apache.ambari.logfeeder.filter.Filter;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.s3.S3Util;
import org.apache.ambari.logfeeder.util.CompressionUtil;
import org.apache.ambari.logfeeder.util.PlaceholderUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Write log file into s3 bucket
 */
public class OutputS3File extends Output {


  private static boolean uploadedGlobalConfig = false;

  @Override
  public void copyFile(File inputFile, InputMarker inputMarker) {
    String bucketName = getStringValue("s3_bucket");
    String s3LogDir = getStringValue("s3_log_dir");
    HashMap<String, String> contextParam = buildContextParam();
    s3LogDir = PlaceholderUtil.replaceVariables(s3LogDir, contextParam);
    String s3AccessKey = getStringValue("s3_access_key");
    String s3SecretKey = getStringValue("s3_secret_key");
    String compressionAlgo = getStringValue("compression_algo");
    String fileName = inputFile.getName();
    // create tmp compressed File
    String tmpDir = LogFeederUtil.getLogfeederTempDir();
    File outputFile = new File(tmpDir + fileName + "_"
        + new Date().getTime() + "." + compressionAlgo);
    outputFile = CompressionUtil.compressFile(inputFile, outputFile,
        compressionAlgo);
    String type = inputMarker.input.getStringValue("type");
    String s3Path = s3LogDir + S3Util.INSTANCE.S3_PATH_SEPARATOR + type
        + S3Util.INSTANCE.S3_PATH_SEPARATOR + fileName + "."
        + compressionAlgo;
    S3Util.INSTANCE.uploadFileTos3(bucketName, s3Path, outputFile, s3AccessKey,
        s3SecretKey);
    // delete local compressed file
    outputFile.deleteOnExit();
    ArrayList<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();
    addFilters(filters, inputMarker.input.getFirstFilter());
    Map<String, Object> inputConfig = new HashMap<String, Object>();
    inputConfig.putAll(inputMarker.input.getConfigs());
    String s3CompletePath = S3Util.INSTANCE.S3_PATH_START_WITH + bucketName
        + S3Util.INSTANCE.S3_PATH_SEPARATOR + s3Path;
    inputConfig.put("path", s3CompletePath);

    ArrayList<Map<String, Object>> inputConfigList = new ArrayList<Map<String, Object>>();
    inputConfigList.add(inputConfig);
    // set source s3_file
    // remove global config from filter config
    removeGlobalConfig(inputConfigList);
    removeGlobalConfig(filters);
    // write config into s3 file
    String s3Key = getComponentConfigFileName(type);
    Map<String, Object> config = new HashMap<String, Object>();
    config.put("filter", filters);
    config.put("input", inputConfigList);
    writeConfigToS3(config, bucketName, s3AccessKey, s3SecretKey, contextParam,
        s3Key);
    // write global config
    writeGlobalConfig();
  }

  public void addFilters(ArrayList<Map<String, Object>> filters, Filter filter) {
    if (filter != null) {
      Map<String, Object> filterConfig = new HashMap<String, Object>();
      filterConfig.putAll(filter.getConfigs());
      filters.add(filterConfig);
      if (filter.getNextFilter() != null) {
        addFilters(filters, filter.getNextFilter());
      }
    }
  }

  public void writeConfigToS3(Map<String, Object> config, String bucketName,
      String accessKey, String secretKey, HashMap<String, String> contextParam,
      String s3Key) {
    String s3ConfigDir = getStringValue("s3_config_dir");
    s3ConfigDir = PlaceholderUtil.replaceVariables(s3ConfigDir, contextParam);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String configJson = gson.toJson(config);

    s3Key = s3ConfigDir + S3Util.INSTANCE.S3_PATH_SEPARATOR + s3Key;
    S3Util.INSTANCE.writeIntoS3File(configJson, bucketName, s3Key, accessKey,
        secretKey);
  }

  public String getComponentConfigFileName(String componentName) {
    String fileName = "input.config-" + componentName + ".json";
    return fileName;
  }

  public HashMap<String, String> buildContextParam() {
    HashMap<String, String> contextParam = new HashMap<String, String>();
    contextParam.put("host", LogFeederUtil.hostName);
    contextParam.put("ip", LogFeederUtil.ipAddress);
    String cluster = getNVList("add_fields").get("cluster");
    contextParam.put("cluster", cluster);
    return contextParam;
  }

  
  private Map<String, Object> getGlobalConfig() {
    Map<String, Object> globalConfig = LogFeeder.globalMap;
    if (globalConfig == null) {
      globalConfig = new HashMap<String, Object>();
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
  private synchronized void writeGlobalConfig() {
    if (!uploadedGlobalConfig) {
      Map<String, Object> globalConfig = LogFeederUtil.cloneObject(getGlobalConfig());
      //updating global config before write to s3
      globalConfig.put("source", "s3_file");
      globalConfig.put("copy_file", false);
      globalConfig.put("process_file", true);
      globalConfig.put("tail", false);
      Map<String, Object> addFields = (Map<String, Object>) globalConfig
          .get("add_fields");
      if (addFields == null) {
        addFields = new HashMap<String, Object>();
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
      String s3AccessKey = getStringValue("s3_access_key");
      String s3SecretKey = getStringValue("s3_secret_key");
      String bucketName = getStringValue("s3_bucket");
      String s3Key = "global.config.json";
      HashMap<String, String> contextParam = buildContextParam();
      writeConfigToS3(config, bucketName, s3AccessKey, s3SecretKey,
          contextParam, s3Key);
      uploadedGlobalConfig = true;
    }
  }

  @Override
  public void write(String block, InputMarker inputMarker) throws Exception {
    throw new UnsupportedOperationException(
        "write method is not yet supported for output=s3_file");
  }
}
