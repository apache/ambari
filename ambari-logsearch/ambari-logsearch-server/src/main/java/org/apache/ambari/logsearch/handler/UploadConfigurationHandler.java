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
import org.apache.commons.configuration.XMLConfiguration;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UploadConfigurationHandler implements SolrZkRequestHandler<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(UploadConfigurationHandler.class);

  private static final String SCHEMA_FILE = "managed-schema";
  private static final String SOLR_CONFIG_FILE = "solrconfig.xml";
  private static final String FIELD_NAME_PATH = "field[@name]";
  private static final String FIELD_TYPE_NAME_PATH = "fieldType[@name]";
  private static final String DYNAMIC_FIELD_NAME_PATH = "dynamicField[@name]";

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
            if (file.getName().equals(SOLR_CONFIG_FILE) && !FileUtils.contentEquals(file, new File(String.format("%s%s%s", downloadFolderLocation, separator, file.getName())))) {
              LOG.info("Solr config file differs ('{}'), upload config set to zookeeper", file.getName());
              zkConfigManager.uploadConfigDir(configSetFolder.toPath(), solrPropsConfig.getConfigName());
              reloadCollectionNeeded = true;
              break;
            }
            if (file.getName().equals(SCHEMA_FILE) && localSchemaFileHasMoreFields(file, new File(String.format("%s%s%s", downloadFolderLocation, separator, file.getName())))) {
              LOG.info("Solr schema file differs ('{}'), upload config set to zookeeper", file.getName());
              zkConfigManager.uploadConfigDir(configSetFolder.toPath(), solrPropsConfig.getConfigName());
              reloadCollectionNeeded = true;
              break;
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

  private boolean localSchemaFileHasMoreFields(File localFile, File downloadedFile) {
    try {
      XMLConfiguration localFileXml = new XMLConfiguration(localFile);
      XMLConfiguration downloadedFileXml = new XMLConfiguration(downloadedFile);

      List<String> localFieldNames = (ArrayList<String>) localFileXml.getProperty(FIELD_NAME_PATH);
      List<String> localFieldTypes = (ArrayList<String>) localFileXml.getProperty(FIELD_TYPE_NAME_PATH);
      List<String> localDynamicFields = (ArrayList<String>) localFileXml.getProperty(DYNAMIC_FIELD_NAME_PATH);

      List<String> fieldNames = (ArrayList<String>) downloadedFileXml.getProperty(FIELD_NAME_PATH);
      List<String> fieldTypes = (ArrayList<String>) downloadedFileXml.getProperty(FIELD_TYPE_NAME_PATH);
      List<String> dynamicFields = (ArrayList<String>) downloadedFileXml.getProperty(DYNAMIC_FIELD_NAME_PATH);

      boolean fieldNameHasDiff = hasMoreFields(localFieldNames, fieldNames, FIELD_NAME_PATH);
      boolean fieldTypeHasDiff = hasMoreFields(localFieldTypes, fieldTypes, FIELD_TYPE_NAME_PATH);
      boolean dynamicFieldNameHasDiff = hasMoreFields(localDynamicFields, dynamicFields, DYNAMIC_FIELD_NAME_PATH);

      return fieldNameHasDiff || fieldTypeHasDiff || dynamicFieldNameHasDiff;
    } catch (Exception e) {
      throw new RuntimeException("Exception during schema xml parsing.", e);
    }
  }

  private boolean hasMoreFields(List<String> localFields, List<String> fields, String tag) {
    boolean result = false;
    if (localFields != null) {
      if (fields == null) {
        result = true;
      } else {
        localFields.removeAll(fields);
        if (!localFields.isEmpty()) {
          result = true;
        }
      }
    }
    if (result) {
      LOG.info("Found new fields or field types in local schema file.: {} ({})", localFields.toString(), tag);
    }
    return result;
  }

}
