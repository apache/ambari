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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JSONUtil {
  private static final Logger logger = Logger.getLogger(JSONUtil.class);

  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  private static final Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();

  private JSONUtil() {
    throw new UnsupportedOperationException();
  }
  
  @SuppressWarnings("unchecked")
  public static HashMap<String, Object> jsonToMapObject(String jsonStr) {
    if (StringUtils.isBlank(jsonStr)) {
      logger.info("jsonString is empty, cannot conver to map");
      return null;
    }
    ObjectMapper mapper = new ObjectMapper();
    try {
      Object tempObject = mapper.readValue(jsonStr, new TypeReference<HashMap<String, Object>>() {});
      return (HashMap<String, Object>) tempObject;

    } catch (JsonParseException e) {
      throw RESTErrorUtil.createRESTException("Invalid input data: " + e.getMessage(), MessageEnums.INVALID_INPUT_DATA);
    } catch (JsonMappingException e) {
      throw RESTErrorUtil.createRESTException("Invalid input data: " + e.getMessage(), MessageEnums.INVALID_INPUT_DATA);
    } catch (IOException e) {
      throw RESTErrorUtil.createRESTException("Invalid input data: " + e.getMessage(), MessageEnums.INVALID_INPUT_DATA);
    }

  }

  @SuppressWarnings("unchecked")
  public static List<HashMap<String, Object>> jsonToMapObjectList(String jsonStr) {
    if (StringUtils.isBlank(jsonStr)) {
      return null;
    }
    ObjectMapper mapper = new ObjectMapper();
    try {
      Object tempObject = mapper.readValue(jsonStr, new TypeReference<List<HashMap<String, Object>>>() {});
      return (List<HashMap<String, Object>>) tempObject;

    } catch (JsonParseException e) {
      throw RESTErrorUtil.createRESTException("Invalid input data: " + e.getMessage(), MessageEnums.INVALID_INPUT_DATA);
    } catch (JsonMappingException e) {
      throw RESTErrorUtil.createRESTException("Invalid input data: " + e.getMessage(), MessageEnums.INVALID_INPUT_DATA);
    } catch (IOException e) {
      throw RESTErrorUtil.createRESTException("Invalid input data: " + e.getMessage(), MessageEnums.INVALID_INPUT_DATA);
    }

  }

  public static boolean isJSONValid(String jsonString) {
    try {
      new JSONObject(jsonString);
    } catch (JSONException ex) {
      try {
        new JSONArray(jsonString);
      } catch (JSONException ex1) {
        return false;
      }
    }
    return true;
  }

  public static HashMap<String, Object> readJsonFromFile(File jsonFile) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      HashMap<String, Object> jsonmap = mapper.readValue(jsonFile, new TypeReference<HashMap<String, Object>>() {});
      return jsonmap;
    } catch (IOException e) {
      logger.error(e, e.getCause());
    }
    return new HashMap<String, Object>();
  }

  public static String mapToJSON(Map<String, Object> map) {
    ObjectMapper om = new ObjectMapper();
    try {
      String json = om.writeValueAsString(map);

      return json;
    } catch (IOException e) {
      logger.error(e, e.getCause());
    }
    return "";
  }

  /**
   * WRITE JOSN IN FILE ( Delete existing file and create new file)
   */
  public static synchronized void writeJSONInFile(String jsonStr, File outputFile, boolean beautify) {
    FileWriter fileWriter = null;
    if (outputFile == null) {
      logger.error("user_pass json file can't be null.");
      return;
    }
    try {
      boolean writePermission = false;
      if (outputFile.exists() && outputFile.canWrite()) {
        writePermission = true;
      }
      if (writePermission) {
        fileWriter = new FileWriter(outputFile);
        if (beautify) {
          ObjectMapper mapper = new ObjectMapper();
          Object json = mapper.readValue(jsonStr, Object.class);
          jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        }
        fileWriter.write(jsonStr);
      } else {
        logger.error("Applcation does not have permission to update file to write enc_password. file="+ outputFile.getAbsolutePath());
      }
    } catch (IOException e) {
      logger.error("Error writing to password file.", e.getCause());
    } finally {
      if (fileWriter != null) {
        try {
          fileWriter.flush();
          fileWriter.close();
        } catch (Exception exception) {
          logger.error(exception);
        }
      }
    }
  }

  public static String objToJson(Object obj) {
    return gson.toJson(obj);
  }

  public static Object jsonToObj(String json, Class<?> klass) {
    return gson.fromJson(json, klass);
  }

  /**
   * GET VALUES FROM JSON BY GIVING KEY RECURSIVELY
   */
  @SuppressWarnings("rawtypes")
  public static String getValuesOfKey(String jsonStr, String keyName, List<String> values) {
    if (values == null) {
      return null;
    }
    Object jsonObj = null;
    try {
      jsonObj = new JSONObject(jsonStr);
    } catch (Exception e) {
      // ignore
    }
    if (jsonObj == null) {
      try {
        JSONArray jsonArray = new JSONArray(jsonStr);
        String str = null;
        for (int i = 0; i < jsonArray.length(); i++) {

          str = getValuesOfKey(jsonArray.getString(i), keyName, values);
          if (str != null) {
            return str;
          }
        }

      } catch (Exception e) {
        // ignore
      }
    }
    if (jsonObj == null) {
      return null;
    }

    Iterator iterator = ((JSONObject) jsonObj).keys();
    if (iterator == null) {
      return null;
    }
    while (iterator.hasNext()) {
      String key = (String) iterator.next();

      if (key != null && key.equals(keyName)) {

        try {
          String val = ((JSONObject) jsonObj).getString(key);
          values.add(val);
        } catch (Exception e) {
          // ignore
        }

      } else if ((((JSONObject) jsonObj).optJSONArray(key) != null) || (((JSONObject) jsonObj).optJSONObject(key) != null)) {

        String str = null;
        try {
          str = getValuesOfKey("" + ((JSONObject) jsonObj).getString(key), keyName, values);
        } catch (Exception e) {
          // ignore
        }
        if (str != null) {
          return str;
        }

      }

    }
    return null;
  }
}