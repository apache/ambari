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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.gson.JsonParseException;

public class HadoopServiceConfigHelper {
  private static final Logger LOG = Logger.getLogger(HadoopServiceConfigHelper.class);
  
  public static String getHadoopServiceConfigJSON() {
    String fileContent = null;

    try {
      ClassLoader classLoader = HadoopServiceConfigHelper.class.getClassLoader();
      File file = new File(classLoader.getResource("HadoopServiceConfig.json").getFile());
      fileContent = FileUtils.readFileToString(file);
    } catch (IOException e) {
      LOG.error("Unable to read HadoopServiceConfig.json", e);
    }

    return JSONUtil.isJSONValid(fileContent) ? fileContent : null;
  }
  
  @SuppressWarnings("unchecked")
  public static Set<String> getAllLogIds() {
    Set<String> logIds = new TreeSet<>();
    
    String key = null;
    JSONArray componentArray = null;
    try {
      String hadoopServiceConfigJSON = getHadoopServiceConfigJSON();
      JSONObject hadoopServiceJsonObject = new JSONObject(hadoopServiceConfigJSON).getJSONObject("service");
      Iterator<String> hadoopSerivceKeys = hadoopServiceJsonObject.keys();
      while (hadoopSerivceKeys.hasNext()) {
        key = hadoopSerivceKeys.next();
        componentArray = hadoopServiceJsonObject.getJSONObject(key).getJSONArray("components");
        for (int i = 0; i < componentArray.length(); i++) {
          JSONObject componentJsonObject = (JSONObject) componentArray.get(i);
          String logId = componentJsonObject.getString("name");
          logIds.add(logId);
        }
      }
    } catch (JsonParseException | JSONException je) {
      LOG.error("Error parsing JSON. key=" + key + ", componentArray=" + componentArray, je);
      return null;
    }

    return logIds;
  }
}
