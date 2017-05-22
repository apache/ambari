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

import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldValueDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MapFieldValueDescriptorImpl implements MapFieldValueDescriptor {
  @Override
  public String getJsonName() {
    return "map_fieldvalue";
  }

  @Expose
  @SerializedName("pre_value")
  private String preValue;

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
