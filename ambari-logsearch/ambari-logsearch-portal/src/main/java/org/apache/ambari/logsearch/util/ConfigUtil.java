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

package org.apache.ambari.logsearch.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.manager.MgrBase;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ConfigUtil {
  static Logger logger = Logger.getLogger(MgrBase.class);

  public static List<String> logLevels = new ArrayList<String>();

  public static HashMap<String, String> serviceLogsColumnMapping = new HashMap<String, String>();

  public static HashMap<String, String> auditLogsColumnMapping = new HashMap<String, String>();

  public static HashMap<String, String> schemaFieldsName = new HashMap<String, String>();

  public static void initializeApplicationConfig() {
    intializeLogLevels();
    initializeColumnMapping();
  }

  private static void intializeUISolrColumnMapping(
    String columnMappingArray[],
    HashMap<String, String> columnMappingMap) {

    if (columnMappingArray != null && columnMappingArray.length > 0) {
      for (String columnMapping : columnMappingArray) {
        String mapping[] = columnMapping.split(":");
        if (mapping.length > 1) {
          String solrField = mapping[0];
          String uiField = mapping[1];

          String modifiedUIField = getModifiedUIField(uiField);
          columnMappingMap.put(solrField + LogSearchConstants.SOLR_SUFFIX,
              modifiedUIField);
          columnMappingMap.put(modifiedUIField + LogSearchConstants.UI_SUFFIX,
              solrField);
        }
      }
    }
  }

  private static String getModifiedUIField(String uiField) {
    String modifiedUIField = "";
    String temp = serviceLogsColumnMapping.get(uiField
      + LogSearchConstants.UI_SUFFIX);
    if (temp == null){
      return uiField;
    }else {
      String lastChar = uiField.substring(uiField.length() - 1,
        uiField.length());
      int k = 1;
      try {
        k = Integer.parseInt(lastChar);
        k = k + 1;
        modifiedUIField = uiField.substring(0, uiField.length() - 2);
      } catch (Exception e) {

      }
      modifiedUIField = uiField + "_" + k;
    }
    return getModifiedUIField(modifiedUIField);
  }

  private static void intializeLogLevels() {
    logLevels.add(LogSearchConstants.TRACE);
    logLevels.add(LogSearchConstants.DEBUG);
    logLevels.add(LogSearchConstants.INFO);
    logLevels.add(LogSearchConstants.WARN);
    logLevels.add(LogSearchConstants.ERROR);
    logLevels.add(LogSearchConstants.FATAL);
  }

  private static void initializeColumnMapping() {
    String serviceLogsColumnMappingArray[] = PropertiesUtil
      .getPropertyStringList("servicelog.column.mapping");
    String auditLogsColumnMappingArray[] = PropertiesUtil
      .getPropertyStringList("auditlog.column.mapping");

    // Initializing column mapping for Service Logs
    intializeUISolrColumnMapping(serviceLogsColumnMappingArray,
      serviceLogsColumnMapping);

    // Initializing column mapping for Audit Logs
    intializeUISolrColumnMapping(auditLogsColumnMappingArray,
      auditLogsColumnMapping);
  }

  public static void extractSchemaFieldsName(String responseString,
                                             String suffix) {
    try {
      JSONObject jsonObject = new JSONObject(responseString);
      JSONArray jsonArrayList = jsonObject.getJSONArray("fields");
      
      if(jsonArrayList == null){
        return;
      }

      for (int i = 0; i < jsonArrayList.length(); i++) {
        JSONObject explrObject = jsonArrayList.getJSONObject(i);
        String name = explrObject.getString("name");
        String type = explrObject.getString("type");

        if (!name.contains("@") && !name.startsWith("_")
          && !name.contains("_md5") && !name.contains("_ms")
          && !name.contains(LogSearchConstants.NGRAM_SUFFIX)) {
          schemaFieldsName.put(name + suffix, type);
        }
      }

    } catch (Exception e) {

      logger.error(e + "Credentials not specified in logsearch.properties "
        + MessageEnums.ERROR_SYSTEM);

    }

  }

  @SuppressWarnings("rawtypes")
  public static void getSchemaFieldsName(String suffix, String excludeArray[],
                                         List<String> fieldNames) {
    if (!schemaFieldsName.isEmpty()) {
      Iterator iteratorSechmaFieldsName = schemaFieldsName.entrySet()
        .iterator();

      while (iteratorSechmaFieldsName.hasNext()) {

        Map.Entry fieldName = (Map.Entry) iteratorSechmaFieldsName
          .next();
        String field = "" + fieldName.getKey();

        if (field.contains(suffix)) {
          field = field.replace(suffix, "");
          if (!isExclude(field, excludeArray)) {
            fieldNames.add(field);
          }

        }
      }
    }
  }

  private static boolean isExclude(String name, String excludeArray[]) {
    if (excludeArray != null && excludeArray.length > 0) {
      for (String exclude : excludeArray) {
        if (name.equals(exclude)){
          return true;
        }
      }
    }
    return false;
  }

}
