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

package org.apache.ambari.server.state;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigImpl implements Config {
  
  private final String type;
  
  private String versionTag;
  
  private Map<String, String> properties;

  public ConfigImpl(String type, String versionTag,
      Map<String, String> properties) {
    this.type = type;
    this.versionTag = versionTag;
    this.properties = properties;
  }
  
  public ConfigImpl(String type, String versionTag) {
    this(type, versionTag, new HashMap<String, String>());
  }
  
  @Override
  public String getType() {
    return type;
  }

  @Override
  public synchronized String getVersionTag() {
    return versionTag;
  }

  @Override
  public synchronized Map<String, String> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  @Override
  public synchronized void setVersionTag(String versionTag) {
    this.versionTag = versionTag;
  }

  @Override
  public synchronized void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  @Override
  public synchronized void updateProperties(Map<String, String> properties) {
    this.properties.putAll(properties);
  }

  @Override
  public synchronized void deleteProperties(List<String> properties) {
    for (String key : properties) {
      this.properties.remove(key);
    }
  }
  
  

}
