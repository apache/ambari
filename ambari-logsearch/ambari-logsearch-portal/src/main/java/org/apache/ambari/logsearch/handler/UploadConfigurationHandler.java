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
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.UUID;

public class UploadConfigurationHandler implements SolrZkRequestHandler<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(UploadConfigurationHandler.class);

  private static final String SCHEMA_FILE = "managed-schema";
  private static final String SOLR_CONFIG_FILE = "solrconfig.xml";

  private File configSetFolder;

  public UploadConfigurationHandler(File configSetFolder) {
    this.configSetFolder = configSetFolder;
  }

  @Override
  public Boolean handle(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig) throws Exception {
    boolean reloadCollectionNeeded = false;
    String separator = FileSystems.getDefault().getSeparator();
    String downloadFolderLocation = String.format("%s%s%s%s%s", System.getProperty("java.io.tmpdir"), separator,
      UUID.randomUUID().toString(), separator, solrPropsConfig.getConfigName());
    solrClient.connect();
    SolrZkClient zkClient = solrClient.getZkStateReader().getZkClient();
    File tmpDir = new File(downloadFolderLocation);
    try {
      ZkConfigManager zkConfigManager = new ZkConfigManager(zkClient);
      boolean configExists = zkConfigManager.configExists(solrPropsConfig.getConfigName());
      if (configExists) {
        LOG.info("Config set exists for '{}' collection. Refreshing it if needed...", solrPropsConfig.getCollection());
        if (!tmpDir.mkdirs()) {
          LOG.error("Cannot create directories for '{}'", tmpDir.getAbsolutePath());
        }
        zkConfigManager.downloadConfigDir(solrPropsConfig.getConfigName(), Paths.get(downloadFolderLocation));
        File[] listOfFiles = configSetFolder.listFiles();
        if (listOfFiles != null) {
          for (File file : listOfFiles) {
            if (file.getName().equals(SOLR_CONFIG_FILE) || file.getName().equals(SCHEMA_FILE)) { // TODO: try to find an another solution to reload schema
              if (!FileUtils.contentEquals(file, new File(String.format("%s%s%s", downloadFolderLocation, separator, file.getName())))){
                LOG.info("One of the local solr config file differs ('{}'), upload config set to zookeeper", file.getName());
                zkConfigManager.uploadConfigDir(configSetFolder.toPath(), solrPropsConfig.getConfigName());
                reloadCollectionNeeded = true;
                break;
              }
            }
          }
        }
      } else {
        LOG.info("Config set does not exist for '{}' collection. Uploading it to zookeeper...", solrPropsConfig.getCollection());
        File[] listOfFiles = configSetFolder.listFiles();
        if (listOfFiles != null) {
          zkConfigManager.uploadConfigDir(configSetFolder.toPath(), solrPropsConfig.getConfigName());
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(String.format("Cannot upload configurations to zk. (collection: %s, config set folder: %s)",
        solrPropsConfig.getCollection(), solrPropsConfig.getConfigSetFolder()), e);
    } finally {
      if (tmpDir.exists()) {
        try {
          FileUtils.deleteDirectory(tmpDir);
        } catch (IOException e){
          LOG.error("Cannot delete temp directory.", e);
        }
      }
    }
    return reloadCollectionNeeded;
  }

}
