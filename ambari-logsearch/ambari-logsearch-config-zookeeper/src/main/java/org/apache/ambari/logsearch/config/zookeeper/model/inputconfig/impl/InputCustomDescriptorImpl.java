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
package org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputCustomDescriptor;

import java.util.Map;

public class InputCustomDescriptorImpl extends InputDescriptorImpl implements InputCustomDescriptor {

  @ShipperConfigElementDescription(
    path = "/input/[]/properties",
    type = "map",
    description = "Custom key value pairs",
    examples = {"{k1 : v1, k2: v2}"},
    defaultValue = ""
  )
  @Expose
  @SerializedName("properties")
  private Map<String, Object> properties;

  @ShipperConfigElementDescription(
    path = "/input/[]/class_name",
    type = "string",
    description = "Custom class which implements an input type",
    examples = {"org.example.MyInputSource"},
    defaultValue = ""
  )
  @Expose
  @SerializedName("class")
  private String mapperClassName;

  @Override
  public Map<String, Object> getProperties() {
    return this.properties;
  }

  @Override
  public String getMapperClassName() {
    return this.mapperClassName;
  }

  @Override
  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  @Override
  public void setMapperClassName(String className) {
    this.mapperClassName = className;
  }
}
