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

package org.apache.ambari.logsearch.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logsearch.dao.SolrDaoBase;
import org.apache.ambari.logsearch.manager.ManagerBase;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ConfigHelper {
  private static final Logger logger = Logger.getLogger(ManagerBase.class);

  private ConfigHelper() {
    throw new UnsupportedOperationException();
  }

  public static void extractSchemaFieldsName(String responseString, HashMap<String, String> schemaFieldsNameMap,
      HashMap<String, String> schemaFieldTypeMap) {
    try {
      JSONObject jsonObject = new JSONObject(responseString);
      JSONObject schemajsonObject = jsonObject.getJSONObject("schema");
      JSONArray jsonArrayList = schemajsonObject.getJSONArray("fields");
      JSONArray fieldTypeJsonArray = schemajsonObject
          .getJSONArray("fieldTypes");
      if (jsonArrayList == null) {
        return;
      }
      if (fieldTypeJsonArray == null) {
        return;
      }
      HashMap<String, String> _schemaFieldTypeMap = new HashMap<String, String>();
      HashMap<String, String> _schemaFieldsNameMap = new HashMap<String, String>();
      for (int i = 0; i < fieldTypeJsonArray.length(); i++) {
        JSONObject typeObject = fieldTypeJsonArray.getJSONObject(i);
        String name = typeObject.getString("name");
        String fieldTypeJson = typeObject.toString();
        _schemaFieldTypeMap.put(name, fieldTypeJson);
      }

      for (int i = 0; i < jsonArrayList.length(); i++) {
        JSONObject explrObject = jsonArrayList.getJSONObject(i);
        String name = explrObject.getString("name");
        String type = explrObject.getString("type");
        if (!name.contains("@") && !name.startsWith("_") && !name.contains("_md5") && !name.contains("_ms") &&
            !name.contains(LogSearchConstants.NGRAM_SUFFIX) && !name.contains("tags") && !name.contains("_str")) {
          _schemaFieldsNameMap.put(name, type);
        }
      }
      schemaFieldsNameMap.clear();
      schemaFieldTypeMap.clear();
      schemaFieldsNameMap.putAll(_schemaFieldsNameMap);
      schemaFieldTypeMap.putAll(_schemaFieldTypeMap);
    } catch (Exception e) {
      logger.error(e + "Credentials not specified in logsearch.properties " + MessageEnums.ERROR_SYSTEM);
    }
  }

  @SuppressWarnings("rawtypes")
  public static void getSchemaFieldsName(String excludeArray[], List<String> fieldNames, SolrDaoBase solrDaoBase) {
    if (!solrDaoBase.schemaFieldsNameMap.isEmpty()) {
      Iterator iteratorSechmaFieldsName = solrDaoBase.schemaFieldsNameMap.entrySet().iterator();
      while (iteratorSechmaFieldsName.hasNext()) {
        Map.Entry fieldName = (Map.Entry) iteratorSechmaFieldsName.next();
        String field = "" + fieldName.getKey();
        if (!isExclude(field, excludeArray)) {
          fieldNames.add(field);
        }
      }
    }
  }

  private static boolean isExclude(String name, String excludeArray[]) {
    if (!ArrayUtils.isEmpty(excludeArray)) {
      for (String exclude : excludeArray) {
        if (name.equals(exclude)){
          return true;
        }
      }
    }
    return false;
  }
}
