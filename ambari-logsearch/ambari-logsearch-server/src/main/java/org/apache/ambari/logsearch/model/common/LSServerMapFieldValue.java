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

package org.apache.ambari.logsearch.model.common;

import javax.validation.constraints.NotNull;

import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldValueDescriptor;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;

@ApiModel
public class LSServerMapFieldValue extends LSServerMapField {
  @Override
  public String getName() {
    return "map_field_value";
  }

  @NotNull
  @JsonProperty("pre_value")
  private String preValue;

  @NotNull
  @JsonProperty("post_value")
  private String postValue;

  public LSServerMapFieldValue() {}

  public LSServerMapFieldValue(MapFieldValueDescriptor mapFieldValueDescriptor) {
    this.preValue = mapFieldValueDescriptor.getPreValue();
    this.postValue = mapFieldValueDescriptor.getPostValue();
  }

  public String getPreValue() {
    return preValue;
  }

  public void setPreValue(String preValue) {
    this.preValue = preValue;
  }

  public String getPostValue() {
    return postValue;
  }

  public void setPostValue(String postValue) {
    this.postValue = postValue;
  }
}
