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

package org.apache.ambari.view.slider;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

public class SliderAppType {
  private String id;
  private String typeName;
  private String typeVersion;
  private String typeDescription;
  Map<String, String> typeConfigsUnsecured;
  Map<String, String> typeConfigsSecured;
  private Map<String, String> typeConfigs;
  private List<SliderAppTypeComponent> typeComponents;
  private String typePackageFileName;
  private List<String> supportedMetrics;
  JsonObject resourcesSecured;
  JsonObject resourcesUnsecured;

  public List<String> getSupportedMetrics() {
    return supportedMetrics;
  }

  public void setSupportedMetrics(List<String> supportedMetrics) {
    this.supportedMetrics = supportedMetrics;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(String name) {
    this.typeName = name;
  }

  public String getTypeDescription() {
    return typeDescription;
  }

  public void setTypeDescription(String description) {
    this.typeDescription = description;
  }

  public Map<String, String> getTypeConfigs() {
    return typeConfigs;
  }

  public void setTypeConfigs(Map<String, String> configs) {
    this.typeConfigs = configs;
  }

  public List<SliderAppTypeComponent> getTypeComponents() {
    return typeComponents;
  }

  public void setTypeComponents(List<SliderAppTypeComponent> components) {
    this.typeComponents = components;
  }

  public String getTypeVersion() {
    return typeVersion;
  }

  public void setTypeVersion(String version) {
    this.typeVersion = version;
  }

  public String getTypePackageFileName() {
    return typePackageFileName;
  }

  public void setTypePackageFileName(String typePackageFileName) {
    this.typePackageFileName = typePackageFileName;
  }

  public String uniqueName() {
    return getTypeName() + "-" + getTypeVersion();
  }
}
