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

import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.config.api.ShipperConfigTypeDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldNameDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@ShipperConfigTypeDescription(
    name = "Map Field Name",
    description = "The name of the mapping element should be map_field_name. The value json element should contain the following parameter:"
)
public class MapFieldNameDescriptorImpl extends MapFieldDescriptorImpl implements MapFieldNameDescriptor {
  @Override
  public String getJsonName() {
    return "map_field_name";
  }

  @ShipperConfigElementDescription(
    path = "/filter/[]/post_map_values/{field_name}/[]/map_field_name/new_field_name",
    type = "string",
    description = "The name of the renamed field",
    examples = {"new_name"}
  )
  @Expose
  @SerializedName("new_field_name")
  private String newFieldName;

  @Override
  public String getNewFieldName() {
    return newFieldName;
  }

  public void setNewFieldName(String newFieldName) {
    this.newFieldName = newFieldName;
  }
}
