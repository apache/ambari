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

import org.apache.ambari.logfeeder.filter.Filter;
import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.mapper.Mapper;
import org.apache.ambari.logfeeder.output.Output;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class AliasUtil {

  private static final Logger LOG = Logger.getLogger(AliasUtil.class);

  private static final String ALIAS_CONFIG_JSON = "alias_config.json";
  private static HashMap<String, Object> aliasMap = null;

  static {
    File jsonFile = FileUtil.getFileFromClasspath(ALIAS_CONFIG_JSON);
    if (jsonFile != null) {
      aliasMap = FileUtil.readJsonFromFile(jsonFile);
    }
  }

  public static enum AliasType {
    INPUT, FILTER, MAPPER, OUTPUT
  }

  private AliasUtil() {
    throw new UnsupportedOperationException();
  }

  public static Object getClassInstance(String key, AliasType aliasType) {
    String classFullName = getClassFullName(key, aliasType);
    
    Object instance = null;
    try {
      instance = (Object) Class.forName(classFullName).getConstructor().newInstance();
    } catch (Exception exception) {
      LOG.error("Unsupported class = " + classFullName, exception.getCause());
    }

    if (instance != null) {
      boolean isValid = false;
      switch (aliasType) {
        case FILTER:
          isValid = Filter.class.isAssignableFrom(instance.getClass());
          break;
        case INPUT:
          isValid = Input.class.isAssignableFrom(instance.getClass());
          break;
        case OUTPUT:
          isValid = Output.class.isAssignableFrom(instance.getClass());
          break;
        case MAPPER:
          isValid = Mapper.class.isAssignableFrom(instance.getClass());
          break;
        default:
          LOG.warn("Unhandled aliasType: " + aliasType);
          isValid = true;
      }
      if (!isValid) {
        LOG.error("Not a valid class :" + classFullName + " AliasType :" + aliasType.name());
      }
    }
    return instance;
  }

  private static String getClassFullName(String key, AliasType aliastype) {
    String className = null;// key as a default value;
    
    HashMap<String, String> aliasInfo = getAliasInfo(key, aliastype);
    String value = aliasInfo.get("klass");
    if (!StringUtils.isEmpty(value)) {
      className = value;
      LOG.debug("Class name found for key :" + key + ", class name :" + className + " aliastype:" + aliastype.name());
    } else {
      LOG.debug("Class name not found for key :" + key + " aliastype:" + aliastype.name());
    }
    
    return className;
  }

  @SuppressWarnings("unchecked")
  private static HashMap<String, String> getAliasInfo(String key, AliasType aliastype) {
    HashMap<String, String> aliasInfo = new HashMap<String, String>();
    
    if (aliasMap != null) {
      String typeKey = aliastype.name().toLowerCase();
      HashMap<String, Object> typeJson = (HashMap<String, Object>) aliasMap.get(typeKey);
      if (typeJson != null) {
        aliasInfo = (HashMap<String, String>) typeJson.get(key);
      }
    }
    
    return aliasInfo;
  }
}
