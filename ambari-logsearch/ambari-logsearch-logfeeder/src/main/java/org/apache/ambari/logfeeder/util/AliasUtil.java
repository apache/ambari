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
package org.apache.ambari.logfeeder.util;

import java.io.File;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class AliasUtil {

  private static Logger logger = Logger.getLogger(AliasUtil.class);

  private static AliasUtil instance = null;

  private static String aliasConfigJson = "alias_config.json";

  private HashMap<String, Object> aliasMap = null;

  public static enum ALIAS_TYPE {
    INPUT, FILTER, MAPPER, OUTPUT
  }

  public static enum ALIAS_PARAM {
    KLASS
  }

  private AliasUtil() {
    init();
  }

  public static AliasUtil getInstance() {
    if (instance == null) {
      synchronized (AliasUtil.class) {
        if (instance == null) {
          instance = new AliasUtil();
        }
      }
    }
    return instance;
  }

  /**
   */
  private void init() {
    File jsonFile = LogFeederUtil.getFileFromClasspath(aliasConfigJson);
    if (jsonFile != null) {
      this.aliasMap = LogFeederUtil.readJsonFromFile(jsonFile);
    }

  }


  public String readAlias(String key, ALIAS_TYPE aliastype, ALIAS_PARAM aliasParam) {
    String result = key;// key as a default value;
    HashMap<String, String> aliasInfo = getAliasInfo(key, aliastype);
    String value = aliasInfo.get(aliasParam.name().toLowerCase());
    if (value != null && !value.isEmpty()) {
      result = value;
      logger.debug("Alias found for key :" + key + ",  param :" + aliasParam.name().toLowerCase() + ", value :"
        + value + " aliastype:" + aliastype.name());
    } else {
      logger.debug("Alias not found for key :" + key + ", param :" + aliasParam.name().toLowerCase());
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private HashMap<String, String> getAliasInfo(String key, ALIAS_TYPE aliastype) {
    HashMap<String, String> aliasInfo = null;
    if (aliasMap != null) {
      String typeKey = aliastype.name().toLowerCase();
      HashMap<String, Object> typeJson = (HashMap<String, Object>) aliasMap.get(typeKey);
      if (typeJson != null) {
        aliasInfo = (HashMap<String, String>) typeJson.get(key);
      }
    }
    if (aliasInfo == null) {
      aliasInfo = new HashMap<String, String>();
    }
    return aliasInfo;
  }
}
