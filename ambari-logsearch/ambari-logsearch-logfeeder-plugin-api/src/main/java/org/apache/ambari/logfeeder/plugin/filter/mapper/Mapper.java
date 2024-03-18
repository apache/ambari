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
package org.apache.ambari.logfeeder.plugin.filter.mapper;

import org.apache.ambari.logfeeder.plugin.common.LogFeederProperties;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldDescriptor;

import java.util.Map;

public abstract class Mapper<PROP_TYPE extends LogFeederProperties> {

  private MapFieldDescriptor mapFieldDescriptor;
  private PROP_TYPE logFeederProperties;

  private String inputDesc;
  private String fieldName;
  private String mapClassCode;

  protected void init(String inputDesc, String fieldName, String mapClassCode) {
    this.inputDesc = inputDesc;
    this.fieldName = fieldName;
    this.mapClassCode = mapClassCode;
  }

  public void loadConfigs(MapFieldDescriptor mapFieldDescriptor, PROP_TYPE logFeederProperties) {
    this.mapFieldDescriptor = mapFieldDescriptor;
    this.logFeederProperties = logFeederProperties;
  }

  public MapFieldDescriptor getMapFieldDescriptor() {
    return mapFieldDescriptor;
  }

  public PROP_TYPE getLogFeederProperties() {
    return logFeederProperties;
  }

  public abstract boolean init(String inputDesc, String fieldName, String mapClassCode, MapFieldDescriptor mapFieldDescriptor);

  public abstract Object apply(Map<String, Object> jsonObj, Object value);

  public String getInputDesc() {
    return inputDesc;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getMapClassCode() {
    return mapClassCode;
  }

  @Override
  public String toString() {
    return "mapClass=" + mapClassCode + ", input=" + inputDesc + ", fieldName=" + fieldName;
  }
}
