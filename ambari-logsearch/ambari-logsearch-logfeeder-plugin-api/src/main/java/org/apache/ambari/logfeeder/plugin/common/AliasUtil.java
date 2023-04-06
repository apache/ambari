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
package org.apache.ambari.logfeeder.plugin.common;

import org.apache.ambari.logfeeder.plugin.filter.Filter;
import org.apache.ambari.logfeeder.plugin.filter.mapper.Mapper;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.ambari.logfeeder.plugin.output.Output;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class AliasUtil {

  private static final Logger LOG = LoggerFactory.getLogger(AliasUtil.class);

  private static final String ALIAS_CONFIG_JSON = "alias_config.json";
  private static HashMap<String, Object> aliasMap = null;

  static {
    aliasMap = getJsonFileContentFromClassPath(ALIAS_CONFIG_JSON);
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
      instance = Class.forName(classFullName).getConstructor().newInstance();
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
    if (value != null && !value.isEmpty()) {
      className = value;
      LOG.debug("Class name found for key :" + key + ", class name :" + className + " aliastype:" + aliastype.name());
    } else {
      LOG.debug("Class name not found for key :" + key + " aliastype:" + aliastype.name());
    }

    return className;
  }

  @SuppressWarnings("unchecked")
  private static HashMap<String, String> getAliasInfo(String key, AliasType aliastype) {
    HashMap<String, String> aliasInfo = new HashMap<>();

    if (aliasMap != null) {
      String typeKey = aliastype.name().toLowerCase();
      HashMap<String, Object> typeJson = (HashMap<String, Object>) aliasMap.get(typeKey);
      if (typeJson != null) {
        aliasInfo = (HashMap<String, String>) typeJson.get(key);
      }
    }

    return aliasInfo;
  }

  public static HashMap<String, Object> getJsonFileContentFromClassPath(String fileName) {
    ObjectMapper mapper = new ObjectMapper();
    try (InputStream inputStream = AliasUtil.class.getClassLoader().getResourceAsStream(fileName)) {
      return mapper.readValue(inputStream, new TypeReference<HashMap<String, Object>>() {});
    } catch (IOException e) {
      LOG.error("Error occurred during loading alias json file: {}", e);
    }
    return new HashMap<String, Object>();
  }

}
