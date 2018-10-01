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

import static org.apache.solr.common.cloud.ZkConfigManager.CONFIGS_ZKNODE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;

import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkConfigManager;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSolrConfigHandler implements SolrZkRequestHandler<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractSolrConfigHandler.class);

  private File configSetFolder;

  public AbstractSolrConfigHandler(File configSetFolder) {
    this.configSetFolder = configSetFolder;
  }

  @Override
  public Boolean handle(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig) throws Exception {
    boolean reloadCollectionNeeded = false;
    String separator = FileSystems.getDefault().getSeparator();
    solrClient.connect();
    SolrZkClient zkClient = solrClient.getZkStateReader().getZkClient();
    try {
      ZkConfigManager zkConfigManager = new ZkConfigManager(zkClient);
      boolean configExists = zkConfigManager.configExists(solrPropsConfig.getConfigName());
      if (configExists) {
        uploadMissingConfigFiles(zkClient, zkConfigManager, solrPropsConfig.getConfigName());
        reloadCollectionNeeded = doIfConfigExists(solrPropsConfig, zkClient, separator);
      } else {
        doIfConfigNotExist(solrPropsConfig, zkConfigManager);
        uploadMissingConfigFiles(zkClient, zkConfigManager, solrPropsConfig.getConfigName());
      }
    } catch (Exception e) {
      throw new RuntimeException(String.format("Cannot upload configurations to zk. (collection: %s, config set folder: %s)",
        solrPropsConfig.getCollection(), solrPropsConfig.getConfigSetFolder()), e);
    }
    return reloadCollectionNeeded;
  }

  /**
   * Update config file (like solrconfig.xml) to zookeeper znode of solr
   */
  public abstract boolean updateConfigIfNeeded(SolrPropsConfig solrPropsConfig, SolrZkClient zkClient, File file,
                                               String separator, byte[] content) throws IOException;

  /**
   * Config file name which should be uploaded to zookeeper
   */
  public abstract String getConfigFileName();

  @SuppressWarnings("unused")
  public void doIfConfigNotExist(SolrPropsConfig solrPropsConfig, ZkConfigManager zkConfigManager) throws IOException {
    // Do nothing
  }

  @SuppressWarnings("unused")
  public void uploadMissingConfigFiles(SolrZkClient zkClient, ZkConfigManager zkConfigManager, String configName) throws IOException {
    // do Nothing
  }

  public boolean doIfConfigExists(SolrPropsConfig solrPropsConfig, SolrZkClient zkClient, String separator) throws IOException {
    LOG.info("Config set exists for '{}' collection. Refreshing it if needed...", solrPropsConfig.getCollection());
    try {
      File[] listOfFiles = getConfigSetFolder().listFiles();
      if (listOfFiles == null)
        return false;
      byte[] data = zkClient.getData(String.format("%s/%s/%s", CONFIGS_ZKNODE, solrPropsConfig.getConfigName(), getConfigFileName()), null, null, true);

      for (File file : listOfFiles) {
        if (file.getName().equals(getConfigFileName()) && updateConfigIfNeeded(solrPropsConfig, zkClient, file, separator, data)) {
          return true;
        }
      }
      return false;
    } catch (KeeperException | InterruptedException e) {
      throw new IOException("Error downloading files from zookeeper path " + solrPropsConfig.getConfigName(),
              SolrZkClient.checkInterrupted(e));
    }
  }

  protected File getConfigSetFolder() {
    return configSetFolder;
  }
}
