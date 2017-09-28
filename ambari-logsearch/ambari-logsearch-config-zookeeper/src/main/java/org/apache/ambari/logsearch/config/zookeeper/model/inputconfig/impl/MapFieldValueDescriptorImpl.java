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
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldValueDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@ShipperConfigTypeDescription(
    name = "Map Field Value",
    description = "The name of the mapping element should be map_fieldvalue. The value json element should contain the following parameter:"
)
public class MapFieldValueDescriptorImpl extends MapFieldDescriptorImpl implements MapFieldValueDescriptor {
  @Override
  public String getJsonName() {
    return "map_fieldvalue";
  }

  @ShipperConfigElementDescription(
    path = "/filter/[]/post_map_values/{field_name}/[]/map_fieldvalue/pre_value",
    type = "string",
    description = "The value that the field must match (ignoring case) to be mapped",
    examples = {"old_value"}
  )
  @Expose
  @SerializedName("pre_value")
  private String preValue;

  @ShipperConfigElementDescription(
      path = "/filter/[]/post_map_values/{field_name}/[]/map_fieldvalue/post_value",
      type = "string",
      description = "The value to which the field is modified to",
      examples = {"new_value"}
    )
  @Expose
  @SerializedName("post_value")
  private String postValue;

  @Override
  public String getPreValue() {
    return preValue;
  }

  public void setPreValue(String preValue) {
    this.preValue = preValue;
  }

  @Override
  public String getPostValue() {
    return postValue;
  }

  public void setPostValue(String postValue) {
    this.postValue = postValue;
  }
}
