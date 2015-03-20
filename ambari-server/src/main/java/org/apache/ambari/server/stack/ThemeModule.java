/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.stack;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.ThemeInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ThemeModule extends BaseModule<ThemeModule, ThemeInfo> implements Validable {

  private static final Logger LOG = LoggerFactory.getLogger(ThemeModule.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  static {
  }


  private ThemeInfo moduleInfo = new ThemeInfo();
  private boolean valid = true;
  private Set<String> errors = new HashSet<String>();

  public ThemeModule(File themeFile) {

    if (themeFile == null) {
    } else {
      FileReader reader = null;
      try {
        reader = new FileReader(themeFile);
      } catch (FileNotFoundException e) {
        LOG.error("Theme file not found");
      }
      try {
        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
        Map<String, Object> map = mapper.readValue(reader, typeRef);
        moduleInfo.setThemeMap(map);
        LOG.info("Loaded theme: {}", moduleInfo);
      } catch (IOException e) {
        LOG.error("Unable to parse theme file ", e);
        setValid(false);
        setErrors("Unable to parse theme file " + themeFile);
      }
    }
  }

  public ThemeModule(ThemeInfo moduleInfo) {
    this.moduleInfo = moduleInfo;
  }

  @Override
  public void resolve(ThemeModule parent, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices) throws AmbariException {
    if (parent.getModuleInfo() != null) {
      moduleInfo.setThemeMap(mergedMap(parent.getModuleInfo().getThemeMap(), moduleInfo.getThemeMap()));
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> mergedMap(Map<String, Object> parent, Map<String, Object> child) {
    Map<String, Object> mergedMap = new HashMap<String, Object>();
    if (parent == null) {
      mergedMap = child;
    }else if (child == null) {
      mergedMap.putAll(parent);
    } else {
      mergedMap.putAll(parent);
      for (Map.Entry<String, Object> entry : child.entrySet()) {
        String key = entry.getKey();
        Object childValue = entry.getValue();
        if (childValue == null) {
          mergedMap.remove(key);
        }else if (!mergedMap.containsKey(key) || !(childValue instanceof Map)) {
          //insert if absent, override if explicitly null, override primitives and arrays
          mergedMap.put(key, childValue);
        }else {
          Object parentValue = mergedMap.get(key);
          if (!(parentValue instanceof Map)) {
            //override on type mismatch also
            mergedMap.put(key, childValue);
          } else {
            mergedMap.put(key, mergedMap((Map<String, Object>)parentValue, (Map<String, Object>)childValue));
          }
        }
      }

    }

    return mergedMap;
  }

  @Override
  public ThemeInfo getModuleInfo() {
    return moduleInfo;
  }

  @Override
  public boolean isDeleted() {
    return false;
  }

  @Override
  public String getId() {
    return "theme";
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  @Override
  public void setErrors(String error) {
    errors.add(error);
  }

  @Override
  public void setErrors(Collection<String> error) {
    errors.addAll(error);
  }

  @Override
  public Collection<String> getErrors() {
    return errors;
  }
}
