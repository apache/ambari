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
import org.apache.ambari.logsearch.config.api.ShipperConfigTypeDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapCustomDescriptor;

import java.util.Map;

@ShipperConfigTypeDescription(
  name = "Map Custom",
  description = "The name of the mapping element should be map_custom. The value json element may contain the following parameters:"
)
public class MapCustomDescriptorImpl implements MapCustomDescriptor {

  @ShipperConfigElementDescription(
    path = "/filter/[]/post_map_values/{field_name}/[]/map_custom/properties",
    type = "map",
    description = "Custom key value pairs",
    examples = {"{k1 : v1, k2: v2}"},
    defaultValue = ""
  )
  @Expose
  @SerializedName("properties")
  private Map<String, Object> properties;

  @ShipperConfigElementDescription(
    path = "/filter/[]/post_map_values/{field_name}/[]/map_custom/class_name",
    type = "string",
    description = "Custom class which implements a mapper type",
    examples = {"org.example.MyMapper"},
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
  public String getJsonName() {
    return "map_custom";
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
