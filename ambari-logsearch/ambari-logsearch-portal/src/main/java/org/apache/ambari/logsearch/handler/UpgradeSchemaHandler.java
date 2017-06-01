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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.HierarchicalConfiguration.Node;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.common.cloud.SolrZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpgradeSchemaHandler extends AbstractSolrConfigHandler {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeSchemaHandler.class);

  private static final String SCHEMA_FILE = "managed-schema";
  private static final String FIELD_NAME_PATH = "field[@name]";
  private static final String FIELD_TYPE_NAME_PATH = "fieldType[@name]";
  private static final String DYNAMIC_FIELD = "dynamicField";
  private static final String DYNAMIC_FIELD_NAME_PATH = DYNAMIC_FIELD + "[@name]";

  private CloudSolrClient cloudSolrClient;

  private XMLConfiguration localFileXml;
  private List<String> localDynamicFields;

  public UpgradeSchemaHandler(CloudSolrClient cloudSolrClient, File configSetFolder) {
    super(configSetFolder);
    this.cloudSolrClient = cloudSolrClient;
  }

  @Override
  public boolean updateConfigIfNeeded(SolrPropsConfig solrPropsConfig, SolrZkClient zkClient, File file, String separator, String downloadFolderLocation) throws IOException {
    boolean result = false;
    if (localSchemaFileHasMoreFields(file, new File(String.format("%s%s%s", downloadFolderLocation, separator, file.getName())))) {
      LOG.info("Solr schema file differs ('{}'), update config schema...", file.getName());
      try {
        upgradeDynamicFields();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      result = true;
    }
    return result;
  }

  // for now we only upgrades dynamic fields, later we can extend this feature if needed
  private void upgradeDynamicFields() throws IOException, SolrServerException {
    if (localFileXml.getRoot() != null && CollectionUtils.isNotEmpty(localDynamicFields)) {
      List<Node> children = localFileXml.getRoot().getChildren(DYNAMIC_FIELD);
      for (Node dynamicFieldNode : children) {
        List<Node> attributes = dynamicFieldNode.getAttributes();
        Map<String, Object> attributesMap = new HashMap<>();
        for (Node attribute : attributes) {
          attributesMap.put(attribute.getName(), attribute.getValue());
        }
        if (attributesMap.get("name") != null && localDynamicFields.contains(attributesMap.get("name").toString())) {
          SchemaRequest.AddDynamicField addDynamicFieldRequest = new SchemaRequest.AddDynamicField(attributesMap);
          addDynamicFieldRequest.process(cloudSolrClient);
          LOG.info("Added dynamic field request sent. (field name: {})", attributesMap.get("name"));
        }
      }
    }
  }

  @Override
  public String getConfigFileName() {
    return SCHEMA_FILE;
  }

  private boolean localSchemaFileHasMoreFields(File localFile, File downloadedFile) {
    try {
      localFileXml = new XMLConfiguration(localFile);
      XMLConfiguration downloadedFileXml = new XMLConfiguration(downloadedFile);

      List<String> localFieldNames = (ArrayList<String>) localFileXml.getProperty(FIELD_NAME_PATH);
      List<String> localFieldTypes = (ArrayList<String>) localFileXml.getProperty(FIELD_TYPE_NAME_PATH);
      localDynamicFields = (ArrayList<String>) localFileXml.getProperty(DYNAMIC_FIELD_NAME_PATH);

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
