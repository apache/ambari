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
package org.apache.ambari.logsearch.handler;

import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkConfigManager;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;

public class UploadConfigurationHandler extends AbstractSolrConfigHandler {

  private static final Logger LOG = LoggerFactory.getLogger(UploadConfigurationHandler.class);

  private static final String SOLR_CONFIG_FILE = "solrconfig.xml";
  private static final String[] configFiles = {
    "admin-extra.html", "admin-extra.menu-bottom.html", "admin-extra.menu-top.html",
    "elevate.xml", "enumsConfig.xml", "managed-schema", "solrconfig.xml"
  };
  private boolean hasEnumConfig;

  public UploadConfigurationHandler(File configSetFolder, boolean hasEnumConfig) {
    super(configSetFolder);
    this.hasEnumConfig = hasEnumConfig;
  }

  @Override
  public boolean updateConfigIfNeeded(SolrPropsConfig solrPropsConfig, SolrZkClient zkClient, File file,
                                      String separator, String downloadFolderLocation) throws IOException {
    boolean result = false;
    if (!FileUtils.contentEquals(file, new File(String.format("%s%s%s", downloadFolderLocation, separator, file.getName())))) {
      LOG.info("Solr config file differs ('{}'), upload config set to zookeeper", file.getName());
      ZkConfigManager zkConfigManager = new ZkConfigManager(zkClient);
      zkConfigManager.uploadConfigDir(getConfigSetFolder().toPath(), solrPropsConfig.getConfigName());
      String filePath = String.format("%s%s%s", getConfigSetFolder(), separator, getConfigFileName());
      String configsPath = String.format("/%s/%s/%s", "configs", solrPropsConfig.getConfigName(), getConfigFileName());
      uploadFileToZk(zkClient, filePath, configsPath);
      result = true;
    }
    return result;
  }

  @Override
  public void doIfConfigNotExist(SolrPropsConfig solrPropsConfig, ZkConfigManager zkConfigManager) throws IOException {
    LOG.info("Config set does not exist for '{}' collection. Uploading it to zookeeper...", solrPropsConfig.getCollection());
    File[] listOfFiles = getConfigSetFolder().listFiles();
    if (listOfFiles != null) {
      zkConfigManager.uploadConfigDir(getConfigSetFolder().toPath(), solrPropsConfig.getConfigName());
    }
  }

  @Override
  public String getConfigFileName() {
    return SOLR_CONFIG_FILE;
  }

  @Override
  public void uploadMissingConfigFiles(SolrZkClient zkClient, ZkConfigManager zkConfigManager, String configName) throws IOException {
    LOG.info("Check any of the configs files are missing for config ({})", configName);
    for (String configFile : configFiles) {
      if ("enumsConfig.xml".equals(configFile) && !hasEnumConfig) {
        LOG.info("Config file ({}) is not needed for {}", configFile, configName);
        continue;
      }
      String zkPath = String.format("%s/%s", configName, configFile);
      if (zkConfigManager.configExists(zkPath)) {
        LOG.info("Config file ({}) has already uploaded properly.", configFile);
      } else {
        LOG.info("Config file ({}) is missing. Reupload...", configFile);
        FileSystems.getDefault().getSeparator();
        uploadFileToZk(zkClient,
          String.format("%s%s%s", getConfigSetFolder(), FileSystems.getDefault().getSeparator(), configFile),
          String.format("%s%s", "/configs/", zkPath));
      }
    }
  }

  private void uploadFileToZk(SolrZkClient zkClient, String filePath, String configsPath) throws FileNotFoundException {
    InputStream is = new FileInputStream(filePath);
    try {
      if (zkClient.exists(configsPath, true)) {
        zkClient.setData(configsPath, IOUtils.toByteArray(is), true);
      } else {
        zkClient.create(configsPath, IOUtils.toByteArray(is), CreateMode.PERSISTENT, true);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}
